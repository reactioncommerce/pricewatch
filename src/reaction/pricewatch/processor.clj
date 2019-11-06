(ns reaction.pricewatch.processor
  "A Kafka Streams app that processes data.

  The processor is a Lightweight component wrapper around jackdaw Streams (DSL)
  processor app."
  (:require
    [clojure.spec.alpha :as s]
    [com.stuartsierra.component :refer [start stop]]
    [integrant.core :as ig]
    [jackdaw.streams :as streams]
    [rp.jackdaw.processor :refer [map->Processor]]))


(defn topology-builder-fn
    [{:keys [topic-configs] :as component}]
    (fn [builder]
      (-> (streams/kstream builder (:prices-by-id topic-configs))
          (streams/for-each! (fn [[k v]] (clojure.pprint/pprint {:key k :value v}))))
      builder))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; Integrant methods
;;;;

(defmethod ig/init-key :reaction.pricewatch/processor
  [_ opts]
  (-> opts
      (assoc :topology-builder-fn topology-builder-fn)
      map->Processor
      start))

(defmethod ig/halt-key! :reaction.pricewatch/processor
  [_ this]
  (stop this))
