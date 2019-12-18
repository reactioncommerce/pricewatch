(ns reaction.jackdaw.state-store
  "Wrappers and utilities for interacting with KV state stores.

  Useful for explicit state store interaction using the low-level Processor API
  (ex: the Streams `transform` method)."
  (:require
    [jackdaw.serdes.edn :as edn]
    [jackdaw.streams :as streams]
    [jackdaw.streams.interop :as interop])
  (:import
    [org.apache.kafka.streams.state KeyValueStore StoreBuilder Stores]))

(defprotocol KVStore
  (get-key [this k] "Gets the value for key")
  (set-key! [this k v] "Sets the value for key; deletes key when value is nil."))

(extend-type KeyValueStore
  KVStore
  (delete-key [this k]
    (.delete this (name k)))
  (get-key [this k]
    (.get this (name k)))
  (set-key! [this k v]
    (.put this (name k) v)))

(defn state-store-builder
  "Returns a builder (for use with `.addStateStore`) for a persistent store with
  the specified name and serdes. Defaults to EDN serdes."
  ^StoreBuilder
  ([store-name key-serde value-serde]
   (Stores/keyValueStoreBuilder
    (Stores/persistentKeyValueStore store-name) key-serde value-serde))
  ([store-name]
   (state-store-builder store-name (edn/serde) (edn/serde))))

(defn topic->state-store-builder
  [{:keys [key-serde state-store-name topic-name value-serde]}]
  (state-store-builder (or state-store-name topic-name) key-serde value-serde))

(defn global-state-store
  [builder {:keys [key-serde state-store-name topic-name value-serde]
            :as topic-config}]
  (let [store-builder (topic->state-store-builder topic-config)
        topology-builder (streams/streams-builder* builder)
        consumed (interop/topic->consumed topic-config)]
    (.addGlobalStateStore topology-builder store-builder topic-name consumed)))

;;
;; Misc
;;

(defn get-all-kvs
  "Get all the keys/values in a KeyValueStore. Handy for dev debugging, but be
  careful not to call it on a huge store."
  [^KeyValueStore state-store]
  (iterator-seq (.all state-store)))

;;
;; A mock implementation for tests that uses a map atom as a fake store.
;;
(defrecord MockKVStore [store]
  KVStore
  (get-key [this k]
    (get @store (keyword k)))
  (set-key! [this k v]
    (if v
      (swap! store assoc (keyword k) v)
      (swap! store dissoc (keyword k)))
    nil))

;; Convenience factory fn
(defn make-mock-store
  ([init-map]
   (->MockKVStore (atom init-map)))
  ([]
   (make-mock-store {})))

;; Mock helper
(defn get-mock-data
  [mock-store]
  @(:store mock-store))
