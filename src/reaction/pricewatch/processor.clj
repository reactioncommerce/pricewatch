(ns reaction.pricewatch.processor
  "A Kafka Streams app that processes The prices-by-id Kafka topic from the
  Reaction Pricing Engine and watches for price changes that match a set of
  pricewatch records.

  A very rough outline of the topology:

  - Consume price changes from the prices-by-id Kafka topic. This topic is
    produced by Reaction's Pricing Engine application. It contains a aggregate
    of all prices keyed by pricebook-entry-id.

  - For each SKU where price is changed:

    - Look for entries in the watches state-store that match the sku.
    - Evaluate pricewatch rules to determine if there are any active matches.
    - Publish matches to a Kafka topic. We'll leave it to another system to
      consume the match events and send notifications.
  "
  (:require
    [com.stuartsierra.component :refer [start stop]]
    [duct.logger :as logger]
    [integrant.core :as ig]
    [jackdaw.streams :as streams]
    [rp.jackdaw.processor :as processor])
  (:import
    [org.apache.kafka.streams.kstream Transformer]))


(defn lowest-price
  "Gets the lowest price available in the price-by-id record. This is a simple
  implementation that takes the minimum of all prices using the first pricing
  tier."
  [prices-by-id]
  (->> prices-by-id
       :payload
       (map #(-> % :price-tiers first :price))
       (reduce min)))

(defn pricewatch-match?
  "Predicate that returns true when pricewatch conditions are met. This
  implementation checks if the new price has dropped 20% from the price set at
  the start of the watch."
  [watch prices-by-id]
  (let [start-price (:start-price watch)
        lowest-current (lowest-price prices-by-id)]
    (try
      (when (> start-price 0)
        (let [change-threshold 0.8
              percent-change (/ lowest-current start-price)]
          (<= percent-change change-threshold)))
      (catch Exception _ false))))

(defn kv-tuple [key-value]
  ;; State store results are of type `org.apache.kafka.streams.KeyValue`, where
  ;; the value is a `ValueAndTimestamp`. 
  [(.key key-value) (.value (.value key-value)) (.timestamp (.value key-value))])

(defn unwrap-kv-store-value [record]
  (-> record kv-tuple second))

(defn query-range [store price-key]
  (let [from (str price-key ":")
        to   (str price-key ":" java.lang.Character/MAX_VALUE)]
    (iterator-seq (.range store from to))))

(defn pricematch
  "Stateful transformer that queries the watch state store to find pricewatches
  for a given input `prices-by-id` record. For all watches, determines if the
  watch conditions have been statisfied to trigger a price match.

  Forwards all matches (0 or more) to the output kstream.

  Requires the `opts` param for access to component configuration, using topic
  config and logger."
  [opts]
  (let [logger (:logger opts)
        topic-name (get-in opts [:topic-registry :topic-configs :watches
                                 :topic-name])]
    (fn []
     (let [ctx (atom nil)]
       (reify Transformer
         (init [_ processor-context]
           (swap! ctx (constantly processor-context)))
         (close [_] nil)
         (transform [_ price-key price-value]
           ;; Watches are keyed with pattern `{price-id}:{user-id}`.
           ;; This provides a known prefix for all watches on a given product.
           ;; Scan the state-store over range of all possible suffixes to find
           ;; all possible matches.
           ;;
           ;; Then filter on watches that match on the pricewatch rules.
           (let [store (.getStateStore @ctx topic-name)
                 watches (->> (query-range store price-key)
                              (map unwrap-kv-store-value)
                              (filter #(pricewatch-match? % price-value)))]

             (doseq [watch watches]
               ;; We have a match. 🎉
               ;; Forward the details downstream. Each message will be published
               ;; to an output topic. These messages can be used as signals to
               ;; notify users that they should buy now!
               (logger/debug logger :pricewatch-match {:watch watch})
               (.forward @ctx
                         (:id watch)
                         {:pricewatch watch
                          :pricing {:min (lowest-price price-value)}})))))))))

(defn topology-builder-fn
  "Returns a function that applies topology DSL to the provided builder."
  [opts]
  (let [topic-configs (get-in opts [:topic-registry :topic-configs])
        {:keys [prices-by-id matches watches]} topic-configs
        {:keys [topic-name]} watches]
    (fn [builder]
      (let [prices-kstream (streams/kstream builder prices-by-id)]

        ;; Create a ktable so we can query the watches state store.
        (streams/ktable builder watches topic-name)

        ;; Send price changes to stateful tranform processor and find matches.
        (-> prices-kstream
            (streams/transform (pricematch opts) [topic-name])
            (streams/to matches)))

      builder)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; Integrant methods
;;;;

(defmethod ig/init-key :reaction.pricewatch.processor/topology-builder-fn
  [_ _]
  topology-builder-fn)

(defmethod ig/halt-key! :reaction.pricewatch.processor/topology-builder-fn
  [_ _]
  nil)

(defmethod ig/init-key :reaction.pricewatch/processor
  [_ opts]
  (-> opts processor/map->Processor start))

(defmethod ig/halt-key! :reaction.pricewatch/processor
  [_ this]
  (stop this))

(defmethod ig/halt-key! :reaction.pricewatch/processor
  [_ this]
  (stop this))

(defmethod ig/init-key :reaction.pricewatch/mock-processor
  [_ opts]
  (-> opts processor/map->MockProcessor start))

(defmethod ig/halt-key! :reaction.pricewatch/mock-processor
  [_ this]
  (stop this))
