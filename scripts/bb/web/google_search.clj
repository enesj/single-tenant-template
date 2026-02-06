#!/usr/bin/env bb

;; Google Custom Search JSON API helper (Babashka)
;;
;; Requires environment variables:
;; - GOOGLE_SEARCH_API_KEY (or GOOGLE_API_KEY)
;; - GOOGLE_SEARCH_CX (or GOOGLE_CSE_ID)
;;
;; Usage:
;;   bb scripts/bb/web/google_search.clj "query"
;;   bb google-search "query" --num 5 --gl ba --lr lang_hr --format pretty
;;
;; Output formats:
;; - pretty (default): human-readable titles + links
;; - edn: machine-friendly EDN map
;; - json: machine-friendly JSON

(ns scripts.bb.web.google-search
  (:require
    [babashka.http-client :as http]
    [clojure.data.json :as json]
    [clojure.string :as str]))

(def ^:private endpoint "https://www.googleapis.com/customsearch/v1")

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

(defn- usage!
  ([]
   (usage! nil))
  ([msg]
   (when msg
     (binding [*out* *err*]
       (println msg)
       (println)))
   (println "Google Custom Search JSON API (bb helper)")
   (println "")
   (println "Usage:")
   (println "  bb google-search \"<query>\" [options]")
   (println "")
   (println "Options:")
   (println "  --num N         Results per page (1-10). Default: 5")
   (println "  --start N       Start index (1-based). Default: 1")
   (println "  --gl CC         Country code for geo localization (e.g. ba, hr). Optional")
   (println "  --hl LL         UI language (e.g. en, hr). Optional")
   (println "  --lr LR         Language restrict (e.g. lang_hr, lang_bs). Optional")
   (println "  --cr CR         Country restrict (e.g. countryBA). Optional")
   (println "  --safe MODE     SafeSearch: active|off. Optional")
   (println "  --site DOMAIN   Restrict results to a site (siteSearch param). Optional")
   (println "  --format FMT    pretty|edn|json. Default: pretty")
   (println "  --help          Show this help")
   (println "")
   (println "Environment:")
   (println "  GOOGLE_SEARCH_API_KEY (or GOOGLE_API_KEY)")
   (println "  GOOGLE_SEARCH_CX      (or GOOGLE_CSE_ID)")
   (System/exit 0)))

(defn- parse-args
  [args]
  (loop [args args
         opts {:num 5
               :start 1
               :gl nil
               :hl nil
               :lr nil
               :cr nil
               :safe nil
               :site nil
               :format :pretty
               :query-parts []}]
    (if (empty? args)
      (let [query (->> (:query-parts opts)
                    (remove str/blank?)
                    (str/join " ")
                    (str/trim))]
        (assoc opts :query query))
      (let [[a & more] args]
        (case a
          "--help" (usage!)

          "--num" (let [[n & more2] more]
                    (when-not n (die! "--num requires a value"))
                    (recur more2 (assoc opts :num (parse-int n 5))))

          "--start" (let [[n & more2] more]
                      (when-not n (die! "--start requires a value"))
                      (recur more2 (assoc opts :start (parse-int n 1))))

          "--gl" (let [[v & more2] more]
                   (when-not v (die! "--gl requires a value"))
                   (recur more2 (assoc opts :gl v)))

          "--hl" (let [[v & more2] more]
                   (when-not v (die! "--hl requires a value"))
                   (recur more2 (assoc opts :hl v)))

          "--lr" (let [[v & more2] more]
                   (when-not v (die! "--lr requires a value"))
                   (recur more2 (assoc opts :lr v)))

          "--cr" (let [[v & more2] more]
                   (when-not v (die! "--cr requires a value"))
                   (recur more2 (assoc opts :cr v)))

          "--safe" (let [[v & more2] more]
                     (when-not v (die! "--safe requires a value"))
                     (recur more2 (assoc opts :safe v)))

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
  (or (env "GOOGLE_SEARCH_API_KEY")
    (env "GOOGLE_API_KEY")))

(defn- cx
  []
  (or (env "GOOGLE_SEARCH_CX")
    (env "GOOGLE_CSE_ID")))

(defn- clamp
  [n lo hi]
  (-> n (max lo) (min hi)))

(defn- request
  [{:keys [query num start gl hl lr cr safe site]}]
  (let [key (api-key)
        cx (cx)]
    (when-not key
      (die! (str "Missing API key. Set GOOGLE_SEARCH_API_KEY (preferred) or GOOGLE_API_KEY.\n"
              "Tip: put placeholders in .env and source it before running.")))
    (when-not cx
      (die! (str "Missing CX. Set GOOGLE_SEARCH_CX (preferred) or GOOGLE_CSE_ID.\n"
              "CX is your Programmable Search Engine id.")))
    (when (str/blank? query)
      (usage! "Missing query."))

    (let [num (clamp (long (or num 5)) 1 10)
          start (max 1 (long (or start 1)))
          qp (cond-> {:key key
                      :cx cx
                      :q query
                      :num num
                      :start start}
               gl (assoc :gl gl)
               hl (assoc :hl hl)
               lr (assoc :lr lr)
               cr (assoc :cr cr)
               safe (assoc :safe safe)
               site (assoc :siteSearch site))
          resp (http/get endpoint {:query-params qp
                                   :as :string
                                   :throw false})]
      (when-not (= 200 (:status resp))
        (let [body (:body resp)
              parsed (try (json/read-str body :key-fn keyword)
                       (catch Exception _ nil))
              msg (or (get-in parsed [:error :message])
                    body
                    (str "HTTP " (:status resp)))]
          (die! (str "Google Search API request failed (HTTP " (:status resp) ")\n" msg))))
      (json/read-str (:body resp) :key-fn keyword))))

(defn- simplify
  [raw]
  (let [items (or (:items raw) [])]
    {:query (get-in raw [:queries :request 0 :searchTerms])
     :count (count items)
     :results (mapv (fn [it]
                      {:title (:title it)
                       :link (:link it)
                       :display-link (:displayLink it)
                       :snippet (:snippet it)})
                items)
     :search-information (:searchInformation raw)}))

(defn- print-pretty
  [{:keys [query results]}]
  (println (str "Query: " query))
  (println "")
  (if (seq results)
    (doseq [[idx {:keys [title link display-link snippet]}] (map-indexed vector results)]
      (println (str (inc idx) ". " (or title "(no title)")))
      (when display-link
        (println (str "   " display-link)))
      (when link
        (println (str "   " link)))
      (when (and snippet (not (str/blank? snippet)))
        (println (str "   " snippet)))
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
