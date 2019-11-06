(ns reaction.pricewatch.topic-registry
  "The topic registry holds configuration for a set of topics used by the app.

  Topic metadata is held in the `:topic-metadata` attribute which is a map. The
  metadata value is a topic configuration from the Jackdaw library.

  When the component is started the topic serdes are resolved. The fully
  resolved and ready topics are stored in the `:topic-configs` keys."
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


(defmethod ig/init-key :reaction.pricewatch/topic-registry
  [_ opts]
  (-> opts map->TopicRegistry start))

(defmethod ig/halt-key! :reaction.pricewatch/topic-registry
  [_ this]
  (stop this))

(defmethod ig/pre-init-spec :reaction.pricewatch/topic-registry [_]
  ::topic-registry)
