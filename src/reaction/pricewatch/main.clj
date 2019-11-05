(ns reaction.pricewatch.main
  "Main entry point for the application."
  (:gen-class)
  (:require
    [clojure.string :as string]
    [clojure.tools.cli :refer [parse-opts]]
    [duct.core :as duct]))

(duct/load-hierarchy)

(defn usage [options-summary]
  (->> ["An application."
        ""
        "Usage: [clojure] -m reaction.pricewatch.main [options] action"
        ""
        ""
        "Options: (Ensure options are in `option=value` format.)"
        options-summary
        ""
        "Actions:"
        "  run    Run the application server"
        ""
        "Examples:"
        ""
        "  Get help:"
        "      clojure -m reaction.pricewatch.main help"
        ""
        "  Run the app with the defaults:"
        "      clojure -m reaction.pricewatch.main run"
        ""
        "  Override the Duct config. Useful when using as a library and you wish to use your own config."
        "      clojure -m reaction.pricewatch.main --config=reaction.pricewatch/config.edn run"
        ""
        "  Activate certain Duct profiles. You can use this for environments, but you may activate the application in different modes."
        "      clojure -m reaction.pricewatch.main --profiles=\"[:duct.profile/prod]\" run"
        ""
        "  Override the keys that will be started as root components. Dependencies will also be started and provided to the root component. Recommended to leave this at default."
        "      clojure -m reaction.pricewatch.main --keys=\"[:duct/daemon]\" run"
        ""
        "  Override all options."
        "      clojure -m reaction.pricewatch.main --config=reaction.pricewatch/config.edn --profiles=\"[:duct.profile/prod]\" --keys=\"[:duct/daemon]\" run"]
       (string/join \newline)))

(def cli-options
  "Defines the CLI options that may be passed to the entrypoint."
  ;; An option with a required argument
  [["-c" "--config" "Resource path to a single Duct configuration EDN file."
    :id :config
    :default "reaction/pricewatch/config.edn"
    :validate [string? "config must a string resource path to an EDN config file."]]
   ;; A non-idempotent option (:default is applied first)
   ["-p" "--profiles" "Duct profiles to merge into the active system configuration. Value must be an EDN sequence of keywords."
    :id :profiles
    :default [:duct.profile/prod]
    :parse-fn read-string]
    ;:validate [#(and (not (empty? %))
    ;                 (every? keyword? %)]
   ["-k" "--keys" "Keys of top-level Duct components that will be executed. Value must be an EDN sequence of keywords. Default recommended."
    :id :keys
    :default [:duct/daemon]
    :parse-fn read-string]
    ;:validate [#(and (not (empty? %))
    ;                 (every? keyword? %)]
   ;; A boolean option defaulting to nil
   ["-h" "--help"]])

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (string/join \newline errors)))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with a error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [parsed-opts]
  (let [{:keys [options arguments errors summary]} parsed-opts]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}
      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}
      ;; custom validation on arguments
      (and (= 1 (count arguments))
           (#{"run"} (first arguments)))
      {:action (first arguments) :options options}
      :else ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))

(defn start
  "Starts the application. The Duct configuration file at config is loaded
  along with all of it's dependencies. The specified active-profiles will be
  activated merged into the system configuration. Finally, the root keys will be
  started, defaulting to keys that inherit from :duct/daemon."
  [config active-profiles root-keys]
  (-> (duct/resource config)
      (duct/read-config)
      (duct/exec-config active-profiles root-keys)))

(defn -main
  "The application entry point."
  [& args]
  (let [parsed-opts (parse-opts args cli-options)
        {{:keys [config profiles keys]} :options} parsed-opts
        {:keys [action exit-message ok?]} (validate-args parsed-opts)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (case action
          "run"  (start config profiles keys)))))
