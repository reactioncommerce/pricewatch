(ns spec-test-helpers
  "Helpers for specs and generative tests."
  (:require
    [clojure.spec.alpha :as s]
    [clojure.spec.gen.alpha :as gen]
    [clojure.spec.test.alpha :as stest]
    [clojure.test :refer [is testing]]))

(defn exercising
  "Tests that all generated values from exercise-gen are valid."
  ([spec] (exercising spec 10))
  ([spec n]
   (is (empty? (->> (s/exercise spec n)
                    (filter #(= ::s/invalid (second %))))))))
(defn- exercise-gen
  "Generates a number (default 10) of values with gen and maps conform
  over them with spec. Returns a sequence of [val conformed-val] tuples.
  Expects that overrides are provided through the gen fn."
  ([spec gen] (exercise-gen spec gen 10))
  ([spec gen n]
   (map #(vector % (s/conform spec %)) (gen/sample gen n))))

(defn exercising-gen
  "Tests that all generated values from exercise-gen are valid."
  ([spec gen] (exercising-gen spec gen 10))
  ([spec gen n]
   (is (empty? (->> (exercise-gen spec gen)
                    (filter #(= ::s/invalid (second %))))))))

(defn- check [function num-tests]
  (if num-tests
    (stest/check function {:clojure.spec.test.check/opts {:num-tests num-tests}})
    (stest/check function)))

(defn checking
  ([function]
   (checking function nil))
  ([function num-tests]
   (testing (str "Schema checking " function)
     (let [result (-> (check function num-tests)
                      first
                      :clojure.spec.test.check/ret
                      :result)]
       (cond
         (true? result) (is result)
         (nil? (ex-data result)) (is (= {} result))
         :else (is (= {} (ex-data result))))))))

(defn fails-spec [f & args]
  (try
    (let [res (apply f args)]
      (is (instance? Exception res)))
    (catch Exception e
      (is (contains? (ex-data e) :clojure.spec/problems)))))
