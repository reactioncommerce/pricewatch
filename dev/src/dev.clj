(ns dev
  (:refer-clojure :exclude [test])
  (:require
    [clojure.java.io :as io]
    [clojure.pprint :refer [print-table]]
    [clojure.repl :refer :all]
    [clojure.spec.alpha :as s]
    [clojure.spec.gen.alpha :as gen]
    [clojure.tools.namespace.repl :refer [refresh]]
    [duct.core :as duct]
    [duct.core.repl :as duct-repl]
    [fipp.edn :refer [pprint]]
    [integrant.core :as ig]
    [integrant.repl :refer [clear halt go init prep reset]]
    [integrant.repl.state :refer [config system]]
    [kaocha.repl :as test :refer [run] :rename {run test}]
    [reaction.nrepl :as nrepl]))


(duct/load-hierarchy)

(defn nrepl
  "Starts an embedded nrepl."
  []
  (let [bind (or (System/getenv "NREPL_BIND") "0.0.0.0")
        port (Integer/parseInt (or (System/getenv "NREPL_PORT") "7375"))]
    (nrepl/start {:bind bind :port port})))

(defn read-config []
  (duct/read-config (io/resource "reaction/pricewatch/config.edn")))

(def profiles
  "These profiles are used when prepping the system."
  [:duct.profile/dev
   :duct.profile/local])

(clojure.tools.namespace.repl/set-refresh-dirs
  "dev/src" "dev/resources" "resources" "src" "test")

(when (io/resource "local.clj")
  (load "local"))

(integrant.repl/set-prep! #(duct/prep-config (read-config) profiles))
