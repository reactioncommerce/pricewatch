(defn system-key-search-by-regex
  "Returns a string representation of a system key as found by provided regex.

  Usage:

      (system-key-search-by-regex system #\".*ataraxy.*\")
  "
  [system regex]
  (-> system
      keys
      (->> (map str)
           (filter #(re-matches regex %)))))

(defn system-key-search
  "Returns a string representation of a system key as found by provided search
  string.

  Usage:

      (system-key-search system \"router\")
  "
  [system search-string]
  (let [regex (re-pattern (str ".*" search-string ".*"))]
    (-> system
          keys
          (->> (map str)
               (filter #(re-matches regex %))))))

(println :loaded "shared/system")
