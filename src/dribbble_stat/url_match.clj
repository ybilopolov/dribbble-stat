(ns dribbble-stat.url-match
  (:require [clojure.set]
            [clojure.string :as string]))

(defn zip-map [& ms]
  "Constructs new map where for each key that present in
  at least one of the given maps value is a list
  of it's values in each map (or nil if missed)"
  ;; todo: is there more clojure-idiomatic way to do this?
  (into {} (map (fn [k] [k (map #(% k) ms)]) (set (flatten (map keys ms))))))

(defn gen-pattern [pat-str]
  "Parses string url pattern into hash map"
  (let [pat-regex #"([^\(]+)\(([^\)]+)\); *"
        val-parsers {:host identity
                     :path #(string/split % #"/")
                     :queryparam #(apply hash-map (string/split % #"="))}
        item-parser (fn [[_ param value]]
                      (let [param (keyword param)]
                        {param ((get val-parsers param) value)}))
        parsed-items (map item-parser (re-seq pat-regex pat-str))

        merge-values (fn [v1 v2] (if (every? map? [v1 v2]) (merge v1 v2) v2))
        merged-pattern (apply merge-with merge-values {} parsed-items)]

    (clojure.set/rename-keys merged-pattern {:queryparam :query})))

(defn parse-url [url-str]
  "Parses URL string into hash map of it's parts"
  (let [url-regex #"([^:]+)://([^/]+)/?([^\?]+)?\??(.+)?"
        [_ protocol host path query] (first (re-seq url-regex url-str))]
    {:host host
     :path (and path (string/split path #"/"))
     :query (and query (into {} (map #(string/split % #"=")
                                     (string/split query #"&"))))}))

(defn guess-type [s]
  (if (re-matches #"\d+" s) (read-string s) s))

(defn recognize [pattern url]
  "Recognizes and destructs URL by: host, path and query
  Each string that is started from '?' is a 'bind'. Returns nil or seq of binds"
  (let [binded? #(string/starts-with? % "?")
        valid-param? (fn [[pat-value url-value]]
                       (or (and (binded? pat-value) (not (nil? url-value)))
                           (= pat-value url-value)
                           (println "invalid value for" pat-value ":" url-value)))
        get-bind #(keyword (string/replace % #"^\?" ""))

        parsed-url (parse-url url)

        ; combine path and query items into single map
        ; to use general binding abstraction
        bindings (into (or (:query pattern) {})
                       (map-indexed vector (:path pattern)))
        values (into (select-keys (:query parsed-url) (keys (:query pattern)))
                     (map-indexed vector (:path parsed-url)))
        zipped-vals (vals (zip-map bindings values))]

    (cond
      (not= (:host pattern) (:host parsed-url)) (println "host mismatch")
      (not (every? valid-param? zipped-vals)) (println "URL structure mismatch")
      :else
        (remove nil?
          (map (fn [[patv urlv]]
                 (if (binded? patv) [(get-bind patv) (guess-type urlv)]))
               zipped-vals)))))
