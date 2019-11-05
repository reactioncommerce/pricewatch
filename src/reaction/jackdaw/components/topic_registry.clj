(ns reaction.jackdaw.components.topic-registry
  (:require
    [rp.jackdaw.resolver :as resolver]
    [rp.jackdaw.topic-registry :refer [map->TopicRegistry]]
    [integrant.core :as ig]))

;; Override the rp start stop because they're directly implemented in the
;; component protocol.

(defn- start
  [{:keys [topic-metadata schema-registry-url serde-resolver-fn type-registry]
    :as this}]
  (assoc this
         :topic-configs
         (resolver/resolve-topics topic-metadata serde-resolver-fn)))

(defn- stop
  [this]
  this)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; Integrant methods
;;;;
(defmethod ig/init-key :reaction.jackdaw.components/topic-registry
  [_ opts]
  (-> opts map->TopicRegistry start))

(defmethod ig/halt-key! :reaction.jackdaw.components/topic-registry
  [_ this]
  (stop this))
