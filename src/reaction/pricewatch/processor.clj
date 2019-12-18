(ns reaction.pricewatch.processor
  "A Kafka Streams app that processes The prices-by-id Kafka topic from the
  Reaction Pricing Engine and watches for price changes that match a set of
  pricewatch records.

  A very rough outline of the topology:

  - Consume price changes from the prices-by-id Kafka topic. This topic is
    produced by Reaction's Pricing Engine application. It contains a aggregate
    of all prices keyed by pricebook-entry-id.

  - For each SKU where price is changed:

    - Look for entries in the watches table that matches the sku
    - Evaluate pricewatch rules to determine if there are any active matches.
    - Take action when a match is found. This simple example This can be printing, producing to
      another topic, or taking direct action.

  The processor is a Lightweight component wrapper around jackdaw Streams (DSL)
  processor app."
  (:require
    [com.stuartsierra.component :refer [start stop]]
    [duct.logger :as logger]
    [integrant.core :as ig]
    [jackdaw.streams :as streams]
    [reaction.jackdaw.state-store :as store]
    [rp.jackdaw.streams-extras :as extras]
    [rp.jackdaw.processor :as processor])
  (:import
    [org.apache.kafka.streams.kstream Transformer]))


(defn lowest-price
  "Gets the lowest price available in the price-by-id record. This simple
  implmentation takes the minimum of all prices, using the first pricing tier."
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

;; State store results are of type org.apache.kafka.streams.KeyValue
;; Use interop to get at the values.
;; Is there a jackdaw function that provides this?
(defn- kv-tuple [key-value]
  [(.key key-value) (.value (.value key-value))])

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
           (let [store (.getStateStore @ctx topic-name)
                 watches (store/get-all-kvs store)] ;; TODO: use range query

             ;; This debug logging can/should be removed.
             (when logger
               (doseq [watch watches]
                 (logger/debug logger ::unfiltered-watch-found watch)))

             ;; Filter watches that match the price drop and
             ;; forward all matches downstream.
             (doseq [watch (->> watches
                                (map (comp second kv-tuple))
                                (filter #(pricewatch-match? % price-value)))]
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
