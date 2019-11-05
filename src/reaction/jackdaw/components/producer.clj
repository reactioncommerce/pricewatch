(ns reaction.jackdaw.components.producer
  "Lightweight component wrapper around jackdaw producer client."
  (:require
    [integrant.core :as ig]
    [jackdaw.client :as client]
    [rp.jackdaw.producer :refer [map->Producer]])
  (:import
    [org.apache.kafka.clients.producer KafkaProducer]))

;; Override the rp start stop because they're directly implemented in the
;; component protocol.

(defn- start
  "Implementation to start a component."
  [{:keys [producer-config topic-registry topic-kw] :as this}]
  (let [topic-config (get-in topic-registry [:topic-configs topic-kw])]
    (assert topic-config (str "Missing topic config for `" (pr-str topic-kw) "`. "
                              "Ensure :topic-kw is set properly in component config."))
    (assoc this
           :topic-config topic-config
           :producer (client/producer producer-config topic-config))))

(defn- stop
  "Implementation to stop a component."
  [{:keys [producer] :as this}]
  (when producer
    (.flush ^KafkaProducer producer)
    (.close ^KafkaProducer producer))
  (dissoc this :producer))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; Integrant methods
;;;;
(defmethod ig/init-key :reaction.jackdaw.components/producer
  [_ opts]
  (-> opts map->Producer start))

(defmethod ig/halt-key! :reaction.jackdaw.components/producer
  [_ this]
  (stop this))
