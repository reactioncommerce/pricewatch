(ns reaction.jackdaw.components.processor
  (:require
    [clojure.spec.alpha :as s]
    [rp.jackdaw.processor :refer [map->Processor]]
    [com.stuartsierra.component :refer [start stop]]
    [integrant.core :as ig]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; Integrant methods
;;;;
(defmethod ig/init-key :reaction.jackdaw.components/processor
  [_ opts]
  (-> opts map->Processor start))

(defmethod ig/halt-key! :reaction.jackdaw.components/processor
  [_ this]
  (stop this))

(defmethod ig/pre-init-spec :reaction.jackdaw.components/processor [_]
  ::processor)
