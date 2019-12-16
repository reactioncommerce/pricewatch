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
    [integrant.core :as ig]
    [jackdaw.streams :as streams]
    [rp.jackdaw.processor :as processor])
  (:import
    [org.apache.kafka.streams.kstream Transformer]))


(def watches-table
  "A simple in-memory data store for the price watches. The data is an index of
  product-id to watches. The in-memory version is a map. Keys are the product-id
  and values are vectors of watches for that key. A production app would provide
  management for this data set."
  (atom {"sku1" [{:id "sku1:demo"
                  :user-id "demo"
                  :email "code-examples@reactioncommerce.com"
                  :product-id "sku1"
                  :start-price 100.00}]}))

(defn watches-for-product [db product-id] (get @db product-id))

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

(defn pricewatch-matches [db k v]
  (->> (watches-for-product db k)
       (filter #(pricewatch-match? % v))))

(defn pricematch
  "This is a stateful transformer. Within this transform context we can query
  state stores and forward 0 or many messages downstream.  With that capability
  we can query a state store for pricewatches and join them to price updates.
  When we find a match we can forward a message to downstream watches."
  []
  (let [ctx (atom nil)]
    (reify Transformer
      (init [_ processor-context]
        (swap! ctx (constantly processor-context)))
      (close [_] nil)
      (transform [_ k v]
        ; Forward all matches downstream.
        (doseq [match (pricewatch-matches watches-table k v)]
          (.forward @ctx (:id match)
                    {:pricewatch match
                     :pricing {:min (lowest-price v)}}))))))

(defn topology-builder-fn
  "Returns a function that applies topology DSL to the provided builder."
  [opts]
  (let [topic-configs (get-in opts [:topic-registry :topic-configs])
        {:keys [prices-by-id pricewatch-matches]} topic-configs]
    (fn [builder]
      (-> builder
          (streams/kstream prices-by-id)
          (streams/transform pricematch)
          (streams/to pricewatch-matches))
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
