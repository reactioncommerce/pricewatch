(ns reaction.pricewatch.processor-test
  (:require
    [clojure.test :refer [deftest is testing]]
    [integrant.core :as ig]
    [reaction.pricewatch.processor]
    [reaction.pricewatch.topic-registry]
    [rp.jackdaw.processor :as processor]))

(defn mock-topic-registry []
  (ig/init-key :reaction.pricewatch/mock-topic-registry
               {:topic-metadata
                {:prices-by-id
                 {:topic-name "reaction.pricing.prices-by-id-aggregates.avro-gen1"
                  :replication-factor 1
                  :partition-count 1
                  :key-serde {:serde-keyword :jackdaw.serdes/string-serde}
                  :value-serde {:serde-keyword :jackdaw.serdes.avro.confluent/serde
                                :schema-filename "reaction/pricewatch/avro/prices-by-id.json"}
                  :topic-config {}}
                 :pricewatch-matches
                 {:topic-name "reaction.pricewatch.matches.json-gen1"
                  :replication-factor 1
                  :partition-count 10
                  :key-serde {:serde-keyword :jackdaw.serdes/string-serde}
                  :value-serde {:serde-keyword :jackdaw.serdes.json/serde}
                  :topic-config {}}}}))


(defn mock-topology-builder-fn [topic-registry]
  (ig/init-key :reaction.pricewatch.processor/topology-builder-fn
               {:topic-registry topic-registry}))

(defn mock-processor [topic-registry topology-builder-fn]
 (ig/init-key :reaction.pricewatch/mock-processor
              {:topic-registry topic-registry
               :topology-builder-fn topology-builder-fn}))


(deftest topology-test
  (let [topic-registry (mock-topic-registry)
        topology-builder-fn (mock-topology-builder-fn topic-registry)
        processor (mock-processor topic-registry topology-builder-fn)]

    (testing "output is empty without any publishing"
      (is (= [] (processor/mock-get-keyvals processor :pricewatch-matches))))

    (testing "output equals input because we're temporarily using peek"
      ;; Produce one record.
      (processor/mock-produce!
        processor :prices-by-id "sku1"
                  {:causation-id "15531a0a-b4db-4bb8-8455-4faeee7afee5"
                   :correlation-id "15531a0a-b4db-4bb8-8455-4faeee7afee5"
                   :id "ddfb2baa-40b5-4e33-99a0-c6c0a223ecd9"
                   :payload [{:id "sku1"
                              :enabled true
                              :pricebook-id "system-check-usd"
                              :price-tiers [{:quantity 1 :price 5.0}]}]
                   :timestamp 1563851658109})

      (is (= [["sku1"
               {:causation-id "15531a0a-b4db-4bb8-8455-4faeee7afee5"
                :correlation-id "15531a0a-b4db-4bb8-8455-4faeee7afee5"
                :id "ddfb2baa-40b5-4e33-99a0-c6c0a223ecd9"
                :payload [{:id "sku1"
                           :enabled true
                           :pricebook-id "system-check-usd"
                           :price-tiers [{:quantity 1 :price 5.0}]}]
                :timestamp 1563851658109}]]
             (processor/mock-get-keyvals processor :pricewatch-matches))))))
