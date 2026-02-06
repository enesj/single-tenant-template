#!/usr/bin/env bb

;; Brave Search API helper (Babashka)
;;
;; Requires environment variables:
;; - BRAVE_SEARCH_API_KEY
;;
;; Usage:
;;   bb scripts/bb/web/brave_search.clj "query"
;;   bb brave-search "query" --count 5 --offset 0 --country US --search-lang en --freshness pw --format pretty
;;
;; Output formats:
;; - pretty (default): human-readable titles + links
;; - edn: machine-friendly EDN map
;; - json: machine-friendly JSON

(ns scripts.bb.web.brave-search
  (:require
    [babashka.http-client :as http]
    [clojure.data.json :as json]
    [clojure.string :as str]))

(def ^:private web-endpoint "https://api.search.brave.com/res/v1/web/search")
(def ^:private news-endpoint "https://api.search.brave.com/res/v1/news/search")

(defn- die!
  [msg]
  (binding [*out* *err*]
    (println msg))
  (System/exit 2))

(defn- parse-int
  [s default]
  (try
    (Integer/parseInt (str s))
    (catch Exception _ default)))

(defn- parse-bool
  [s default]
  (cond
    (nil? s) default
    (contains? #{"true" "t" "1" "yes" "y"} (-> s str/lower-case str/trim)) true
    (contains? #{"false" "f" "0" "no" "n"} (-> s str/lower-case str/trim)) false
    :else default))

(defn- usage!
  ([]
   (usage! nil))
  ([msg]
   (when msg
     (binding [*out* *err*]
       (println msg)
       (println)))
   (println "Brave Search API (bb helper)")
   (println "")
   (println "Usage:")
   (println "  bb brave-search \"<query>\" [options]")
   (println "")
   (println "Options:")
   (println "  --type T            web|news. Default: web")
   (println "  --count N           Results to return (1-20). Default: 5")
   (println "  --offset N          Result offset (>= 0). Default: 0")
   (println "  --country CC        Country code (e.g. US, GB). Optional")
   (println "  --search-lang LL    Search language (e.g. en). Optional")
   (println "  --ui-lang LL        UI language (e.g. en-US). Optional")
   (println "  --safesearch MODE   off|moderate|strict. Optional")
   (println "  --freshness F       pd|pw|pm|py (past day/week/month/year). Optional")
   (println "  --spellcheck BOOL   true|false. Optional")
   (println "  --site DOMAIN       Convenience: prefixes query with site:DOMAIN")
   (println "  --format FMT        pretty|edn|json. Default: pretty")
   (println "  --help              Show this help")
   (println "")
   (println "Environment:")
   (println "  BRAVE_SEARCH_API_KEY")
   (System/exit 0)))

(defn- parse-args
  [args]
  (loop [args args
         opts {:type :web
               :count 5
               :offset 0
               :country nil
               :search-lang nil
               :ui-lang nil
               :safesearch nil
               :freshness nil
               :spellcheck nil
               :site nil
               :format :pretty
               :query-parts []}]
    (if (empty? args)
      (let [query (->> (:query-parts opts)
                    (remove str/blank?)
                    (str/join " ")
                    (str/trim))
            query (cond-> query
                    (and (not (str/blank? query)) (some-> (:site opts) str/trim not-empty))
                    (str " site:" (str/trim (:site opts))))]
        (assoc opts :query query))
      (let [[a & more] args]
        (case a
          "--help" (usage!)

          "--type" (let [[v & more2] more]
                     (when-not v (die! "--type requires a value"))
                     (let [t (some-> v str/lower-case keyword)]
                       (when-not (contains? #{:web :news} t)
                         (die! (str "Unknown --type: " v " (expected web|news)")))
                       (recur more2 (assoc opts :type t))))

          "--count" (let [[n & more2] more]
                      (when-not n (die! "--count requires a value"))
                      (recur more2 (assoc opts :count (parse-int n 5))))

          "--offset" (let [[n & more2] more]
                       (when-not n (die! "--offset requires a value"))
                       (recur more2 (assoc opts :offset (parse-int n 0))))

          "--country" (let [[v & more2] more]
                        (when-not v (die! "--country requires a value"))
                        (recur more2 (assoc opts :country v)))

          "--search-lang" (let [[v & more2] more]
                            (when-not v (die! "--search-lang requires a value"))
                            (recur more2 (assoc opts :search-lang v)))

          "--ui-lang" (let [[v & more2] more]
                        (when-not v (die! "--ui-lang requires a value"))
                        (recur more2 (assoc opts :ui-lang v)))

          "--safesearch" (let [[v & more2] more]
                           (when-not v (die! "--safesearch requires a value"))
                           (recur more2 (assoc opts :safesearch v)))

          "--freshness" (let [[v & more2] more]
                          (when-not v (die! "--freshness requires a value"))
                          (recur more2 (assoc opts :freshness v)))

          "--spellcheck" (let [[v & more2] more]
                           (when-not v (die! "--spellcheck requires a value"))
                           (recur more2 (assoc opts :spellcheck (parse-bool v nil))))

          "--site" (let [[v & more2] more]
                     (when-not v (die! "--site requires a value"))
                     (recur more2 (assoc opts :site v)))

          "--format" (let [[v & more2] more
                           fmt (some-> v str/lower-case keyword)]
                       (when-not v (die! "--format requires a value"))
                       (when-not (contains? #{:pretty :edn :json} fmt)
                         (die! (str "Unknown --format: " v " (expected pretty|edn|json)")))
                       (recur more2 (assoc opts :format fmt)))

          ;; Default: treat as part of query
          (recur more (update opts :query-parts conj a)))))))

(defn- env
  [k]
  (some-> (System/getenv k) str/trim not-empty))

(defn- api-key
  []
  (env "BRAVE_SEARCH_API_KEY"))

(defn- clamp
  [n lo hi]
  (-> n (max lo) (min hi)))

(defn- request
  [{:keys [type query count offset country search-lang ui-lang safesearch freshness spellcheck]}]
  (when (str/blank? query)
    (usage! "Missing query."))

  (let [key (api-key)]
    (when-not key
      (die! (str "Missing API key. Set BRAVE_SEARCH_API_KEY in your environment.\n"
              "Tip: create a local .env (gitignored) and export it before running.")))

    (let [count (clamp (long (or count 5)) 1 20)
          offset (max 0 (long (or offset 0)))
          endpoint (case type
                     :news news-endpoint
                     :web web-endpoint
                     web-endpoint)
          qp (cond-> {"q" query
                      "count" count
                      "offset" offset}
               (some-> country str/trim not-empty) (assoc "country" country)
               (some-> search-lang str/trim not-empty) (assoc "search_lang" search-lang)
               (some-> ui-lang str/trim not-empty) (assoc "ui_lang" ui-lang)
               (some-> safesearch str/trim not-empty) (assoc "safesearch" safesearch)
               (some-> freshness str/trim not-empty) (assoc "freshness" freshness)
               (some? spellcheck) (assoc "spellcheck" spellcheck))
          resp (http/get endpoint
                 {:query-params qp
                  :headers {"Accept" "application/json"
                            "X-Subscription-Token" key}
                  :as :string
                  :throw false})]
      (when-not (= 200 (:status resp))
        (let [body (:body resp)
              parsed (try (json/read-str body :key-fn keyword)
                       (catch Exception _ nil))
              msg (or (:message parsed)
                    (get-in parsed [:error :message])
                    body
                    (str "HTTP " (:status resp)))]
          (die! (str "Brave Search API request failed (HTTP " (:status resp) ")\n" msg))))
      (json/read-str (:body resp) :key-fn keyword))))

(defn- simplify-web
  [raw]
  (let [web (or (:web raw) {})
        results (or (:results web) [])]
    {:type :web
     :query (get-in raw [:query :original])
     :count (count results)
     :results (mapv (fn [r]
                      {:title (:title r)
                       :url (:url r)
                       :description (:description r)
                       :age (:age r)
                       :language (:language r)
                       :favicon (:favicon r)})
                results)
     :raw-meta {:total (:total web)
                :extra (select-keys raw [:mixed :query])}}))

(defn- simplify-news
  [raw]
  (let [news (or (:news raw) {})
        results (or (:results news) [])]
    {:type :news
     :query (get-in raw [:query :original])
     :count (count results)
     :results (mapv (fn [r]
                      {:title (:title r)
                       :url (:url r)
                       :description (:description r)
                       :age (:age r)
                       :source (:source r)
                       :published-time (:published_time r)})
                results)
     :raw-meta {:total (:total news)
                :extra (select-keys raw [:mixed :query])}}))

(defn- simplify
  [raw]
  (cond
    (:web raw) (simplify-web raw)
    (:news raw) (simplify-news raw)
    :else {:type :unknown
           :raw raw}))

(defn- print-pretty
  [{:keys [type query results]}]
  (println (str "Type: " (name type)))
  (println (str "Query: " (or query "(unknown)")))
  (println "")
  (if (seq results)
    (doseq [[idx {:keys [title url description]}] (map-indexed vector results)]
      (println (str (inc idx) ". " (or title "(no title)")))
      (when url
        (println (str "   " url)))
      (when (and description (not (str/blank? description)))
        (println (str "   " description)))
      (println ""))
    (println "No results.")))

(defn -main
  [& args]
  (let [{:keys [format] :as opts} (parse-args args)
        raw (request opts)
        v (simplify raw)]
    (case format
      :pretty (print-pretty v)
      :edn (prn v)
      :json (println (json/write-str v)))))

(apply -main *command-line-args*)
