(ns reaction.pricewatch.topology
  (:require
    [clojure.spec.alpha :as s]
    [rp.jackdaw.processor :refer [map->Processor]]
    [jackdaw.streams :as streams]
    [integrant.core :as ig]))


(defn topology-builder-fn
    [{:keys [topic-configs] :as component}]
    (fn [builder]
      (-> (streams/kstream builder (:prices-by-id topic-configs))
          (streams/for-each! (fn [[k v]] (clojure.pprint/pprint {:key k :value v}))))
      builder))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; Integrant methods
;;;;
(defmethod ig/init-key :reaction.pricewatch/topology
  [_ opts]
  topology-builder-fn)

(defmethod ig/halt-key! :reaction.pricewatch/topology
  [_ this]
  nil)
