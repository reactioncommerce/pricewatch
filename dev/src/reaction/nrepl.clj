(ns reaction.nrepl
  "An embeddable NREPL that safe for reloading."
  (:require [clojure.tools.namespace.repl :as repl]
            [nrepl.server :refer [start-server stop-server]]))

(repl/disable-reload!)

(def server nil)

(defn start
  "Start a network repl. Accepts an optional parameters map that may contain
  `:port` `:bind`, `:transport-fn`, `:handler`, `:ack-port` and `:greeting-fn`.
  Opts are forwarded to `clojure.tools.nrepl.server/start-server`. Has no effect
  and returns `nil` if the server is already running."
  ([] (start {}))
  ([{:keys [port bind transport-fn handler ack-port greeting-fn]}]
   (alter-var-root #'reaction.nrepl/server
                   (fn [server]
                     (when-not server
                       (start-server :port port
                                     :bind bind
                                     :transport-fn transport-fn
                                     :handler handler
                                     :ack-port ack-port
                                     :greeting-fn greeting-fn))))))

(defn stop
  []
  "Stops a running NREPL server."
  (when server (stop-server server)))
