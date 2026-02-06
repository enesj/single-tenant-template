#!/usr/bin/env bb

;; Serper.dev Google Search API helper (Babashka)
;;
;; Provider:
;;   https://serper.dev/
;;
;; Requires environment variables:
;; - SERPER_API_KEY
;;
;; Notes:
;; - Serper endpoints are hosted under https://google.serper.dev/*
;; - This script is intended as a general-purpose web search helper for other skills/tools.
;;
;; Usage:
;;   bb scripts/bb/web/serper_search.clj "query"
;;   bb serper-search "query" --type web --num 5 --page 1 --gl US --hl en --format pretty
;;
;; Output formats:
;; - pretty (default): human-readable titles + links
;; - edn: machine-friendly EDN map
;; - json: machine-friendly JSON

(ns scripts.bb.web.serper-search
  (:require
    [babashka.http-client :as http]
    [clojure.data.json :as json]
    [clojure.string :as str]))

(def ^:private web-endpoint "https://google.serper.dev/search")
(def ^:private news-endpoint "https://google.serper.dev/news")
(def ^:private images-endpoint "https://google.serper.dev/images")

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
   (println "Serper.dev Google Search API (bb helper)")
   (println "")
   (println "Usage:")
   (println "  bb serper-search \"<query>\" [options]")
   (println "")
   (println "Options:")
   (println "  --type T            web|news|images. Default: web")
   (println "  --num N             Results per page (provider-dependent). Default: 5")
   (println "  --page N            Page number (>= 1). Default: 1")
   (println "  --gl CC             Country code / geolocation (e.g. US, GB). Optional")
   (println "  --hl LL             UI language (e.g. en). Optional")
   (println "  --location STR      Location string (provider-dependent). Optional")
   (println "  --tbs STR           Raw Google tbs param (advanced). Optional")
   (println "  --autocorrect BOOL  true|false. Optional")
   (println "  --site DOMAIN       Convenience: prefixes query with site:DOMAIN")
   (println "  --format FMT        pretty|edn|json. Default: pretty")
   (println "  --help              Show this help")
   (println "")
   (println "Environment:")
   (println "  SERPER_API_KEY")
   (System/exit 0)))

(defn- env
  [k]
  (some-> (System/getenv k) str/trim not-empty))

(defn- api-key
  []
  (env "SERPER_API_KEY"))

(defn- clamp
  [n lo hi]
  (-> n (max lo) (min hi)))

(defn- parse-args
  [args]
  (loop [args args
         opts {:type :web
               :num 5
               :page 1
               :gl nil
               :hl nil
               :location nil
               :tbs nil
               :autocorrect nil
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
                       (when-not (contains? #{:web :news :images} t)
                         (die! (str "Unknown --type: " v " (expected web|news|images)")))
                       (recur more2 (assoc opts :type t))))

          "--num" (let [[n & more2] more]
                    (when-not n (die! "--num requires a value"))
                    (recur more2 (assoc opts :num (parse-int n 5))))

          "--page" (let [[n & more2] more]
                     (when-not n (die! "--page requires a value"))
                     (recur more2 (assoc opts :page (parse-int n 1))))

          "--gl" (let [[v & more2] more]
                   (when-not v (die! "--gl requires a value"))
                   (recur more2 (assoc opts :gl v)))

          "--hl" (let [[v & more2] more]
                   (when-not v (die! "--hl requires a value"))
                   (recur more2 (assoc opts :hl v)))

          "--location" (let [[v & more2] more]
                         (when-not v (die! "--location requires a value"))
                         (recur more2 (assoc opts :location v)))

          "--tbs" (let [[v & more2] more]
                    (when-not v (die! "--tbs requires a value"))
                    (recur more2 (assoc opts :tbs v)))

          "--autocorrect" (let [[v & more2] more]
                            (when-not v (die! "--autocorrect requires a value"))
                            (recur more2 (assoc opts :autocorrect (parse-bool v nil))))

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

(defn- request
  [{:keys [type query num page gl hl location tbs autocorrect]}]
  (when (str/blank? query)
    (usage! "Missing query."))

  (let [key (api-key)]
    (when-not key
      (die! (str "Missing API key. Set SERPER_API_KEY in your environment.\n"
              "Tip: add it to your local .env (gitignored); bb tasks in this repo source .env automatically.")))

    (let [num (clamp (long (or num 5)) 1 100)
          page (max 1 (long (or page 1)))
          endpoint (case type
                     :news news-endpoint
                     :images images-endpoint
                     :web web-endpoint
                     web-endpoint)
          body (cond-> {:q query
                        :num num
                        :page page}
                 (some-> gl str/trim not-empty) (assoc :gl gl)
                 (some-> hl str/trim not-empty) (assoc :hl hl)
                 (some-> location str/trim not-empty) (assoc :location location)
                 (some-> tbs str/trim not-empty) (assoc :tbs tbs)
                 (some? autocorrect) (assoc :autocorrect autocorrect))
          resp (http/post endpoint
                 {:headers {"Accept" "application/json"
                            "Content-Type" "application/json"
                            "X-API-KEY" key}
                  :body (json/write-str body)
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
          (die! (str "Serper API request failed (HTTP " (:status resp) ")\n" msg))))
      (json/read-str (:body resp) :key-fn keyword))))

(defn- simplify-web
  [raw]
  (let [results (or (:organic raw) [])]
    {:type :web
     :query (or (get raw :query) nil)
     :count (count results)
     :results (mapv (fn [r]
                      {:title (:title r)
                       :link (:link r)
                       :snippet (:snippet r)
                       :position (:position r)})
                results)
     :raw-meta (select-keys raw [:knowledgeGraph :answerBox :peopleAlsoAsk :relatedSearches])}))

(defn- simplify-news
  [raw]
  (let [results (or (:news raw) [])]
    {:type :news
     :query (or (get raw :query) nil)
     :count (count results)
     :results (mapv (fn [r]
                      {:title (:title r)
                       :link (:link r)
                       :snippet (:snippet r)
                       :date (:date r)
                       :source (:source r)
                       :position (:position r)})
                results)
     :raw-meta (select-keys raw [:relatedSearches])}))

(defn- simplify-images
  [raw]
  (let [results (or (:images raw) [])]
    {:type :images
     :query (or (get raw :query) nil)
     :count (count results)
     :results (mapv (fn [r]
                      {:title (:title r)
                       :image-url (:imageUrl r)
                       :thumbnail-url (:thumbnailUrl r)
                       :link (:link r)
                       :source (:source r)
                       :position (:position r)})
                results)}))

(defn- simplify
  [type raw]
  (case type
    :news (simplify-news raw)
    :images (simplify-images raw)
    (simplify-web raw)))

(defn- print-pretty
  [{:keys [type results]}]
  (println (str "Type: " (name type)))
  (println "")
  (if (seq results)
    (doseq [[idx r] (map-indexed vector results)]
      (println (str (inc idx) ". " (or (:title r) "(no title)")))
      (when-let [link (:link r)]
        (println (str "   " link)))
      (when-let [snippet (:snippet r)]
        (when-not (str/blank? (str snippet))
          (println (str "   " snippet))))
      (println ""))
    (println "No results.")))

(defn -main
  [& args]
  (let [{:keys [format type] :as opts} (parse-args args)
        raw (request opts)
        v (simplify type raw)]
    (case format
      :pretty (print-pretty v)
      :edn (prn v)
      :json (println (json/write-str v)))))

(apply -main *command-line-args*)
