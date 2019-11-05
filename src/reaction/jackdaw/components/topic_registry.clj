(ns reaction.jackdaw.components.topic-registry
  (:require
    [clojure.spec.alpha :as s]
    [rp.jackdaw.topic-registry :refer [map->TopicRegistry]]
    [com.stuartsierra.component :refer [start stop]]
    [integrant.core :as ig]
    [jackdaw.specs :as jspecs]))

(s/def ::topic-metadata
  (s/map-of keyword? (s/merge :jackdaw.serde-client/topic
                              :jackdaw.creation-client/topic)))

(s/def ::topic-registry (s/keys :req-un [::topic-metadata]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; Integrant methods
;;;;
(defmethod ig/init-key :reaction.jackdaw.components/topic-registry
  [_ opts]
  (-> opts map->TopicRegistry start))

(defmethod ig/halt-key! :reaction.jackdaw.components/topic-registry
  [_ this]
  (stop this))

(defmethod ig/pre-init-spec :reaction.jackdaw.components/topic-registry [_]
  ::topic-registry)
