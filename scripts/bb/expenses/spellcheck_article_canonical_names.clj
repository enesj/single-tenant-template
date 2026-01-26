#!/usr/bin/env clj

(ns scripts.bb.expenses.spellcheck-article-canonical-names
  "Spellcheck `articles.canonical_name` using the JSpell Checker MCP server and write suggestions to an EDN report."
  (:require
    [aero.core :as aero]
    [clojure.java.io :as io]
    [clojure.pprint :as pprint]
    [clojure.string :as str]
    [cheshire.core :as json]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.time Instant]))

(def ^:private default-out "article-canonical-name-spellcheck-suggestions.edn")
(def ^:private default-lang "bs")
(def ^:private default-max-suggestions 5)

;; JSpell Checker MCP (RapidAPI Hub)
;;
;; This script spawns an MCP stdio server (`mcp-remote https://mcp.rapidapi.com`) and
;; calls its spellchecking tool.
;;
;; IMPORTANT: Do not hardcode API keys. Provide your key via an environment variable.
(def ^:private default-mcp-url "https://mcp.rapidapi.com")
(def ^:private default-mcp-api-host "jspell-checker.p.rapidapi.com")
(def ^:private default-api-key-env "JSPELL_RAPIDAPI_KEY")

(defn- usage
  ([] (usage nil))
  ([msg]
   (when msg
     (binding [*out* *err*]
       (println msg)
       (println "")))
  (println "Spellcheck `articles.canonical_name` and write suggestions to an EDN file.")
   (println "")
   (println "Usage:")
  (println "  bb spellcheck-article-names [--dev|--test|dev|test] [--out <path>] [--lang <bs|hr|sr|...>] [--limit N] [--max-suggestions N]")
   (println "")
   (println "Defaults:")
   (println "  profile: dev")
   (println "  out:     " default-out)
   (println "  lang:    " default-lang)
   (println "  limit:   (no limit)")
  (println "  max-suggestions:" default-max-suggestions)
  (println "  JSpell MCP: uses RapidAPI via mcp-remote (API key required via env)")
  (println "  env vars:")
  (println "    " default-api-key-env " (preferred) or RAPIDAPI_KEY")
  (println "    JSPELL_MCP_URL (optional, default: " default-mcp-url ")")
  (println "    JSPELL_MCP_API_HOST (optional, default: " default-mcp-api-host ")")
   (println "")
   (println "Examples:")
   (println "  bb spellcheck-article-names --dev")
   (println "  bb spellcheck-article-names test --out /tmp/article-spellcheck.edn")
   (println "  bb spellcheck-article-names --lang bs --max-suggestions 10 --limit 200")))

(defn- parse-long-safe
  [s]
  (try
    (Long/parseLong (str s))
    (catch Exception _e
      nil)))

(defn- keep-rest
  [b more]
  (if (nil? b) more (cons b more)))

(defn- parse-args
  [args]
  (loop [args args
         parsed {:profile :dev
                 :out default-out
                 :lang default-lang
                 :limit nil
                 :max-suggestions default-max-suggestions}]
    (let [[a b & more] args]
      (cond
        (nil? a) parsed

        (#{ "dev" "test"} a)
        (recur (cons b more) (assoc parsed :profile (keyword a)))

        (= a "--dev")
        (recur (cons b more) (assoc parsed :profile :dev))

        (= a "--test")
        (recur (cons b more) (assoc parsed :profile :test))

        (or (= a "--help") (= a "-h"))
        (do (usage) (System/exit 0))

        (= a "--out")
        (if (str/blank? b)
          (do (usage "Missing value for --out") (System/exit 1))
          (recur more (assoc parsed :out b)))

        (str/starts-with? a "--out=")
        (recur (keep-rest b more) (assoc parsed :out (subs a (count "--out="))))

        (= a "--lang")
        (if (str/blank? b)
          (do (usage "Missing value for --lang") (System/exit 1))
          (recur more (assoc parsed :lang b)))

        (str/starts-with? a "--lang=")
        (recur (keep-rest b more) (assoc parsed :lang (subs a (count "--lang="))))

        (= a "--limit")
        (let [n (parse-long-safe b)]
          (cond
            (nil? n) (do (usage (str "Invalid --limit: " b)) (System/exit 1))
            (neg? n) (do (usage "--limit must be >= 0") (System/exit 1))
            (zero? n) (recur more (assoc parsed :limit nil))
            :else (recur more (assoc parsed :limit n))))

        (str/starts-with? a "--limit=")
        (let [n (parse-long-safe (subs a (count "--limit=")))]
          (cond
            (nil? n) (do (usage (str "Invalid --limit: " a)) (System/exit 1))
            (neg? n) (do (usage "--limit must be >= 0") (System/exit 1))
            (zero? n) (recur (keep-rest b more) (assoc parsed :limit nil))
            :else (recur (keep-rest b more) (assoc parsed :limit n))))

        (= a "--max-suggestions")
        (let [n (parse-long-safe b)]
          (cond
            (nil? n) (do (usage (str "Invalid --max-suggestions: " b)) (System/exit 1))
            (< n 1) (do (usage "--max-suggestions must be >= 1") (System/exit 1))
            :else (recur more (assoc parsed :max-suggestions n))))

        (str/starts-with? a "--max-suggestions=")
        (let [n (parse-long-safe (subs a (count "--max-suggestions=")))]
          (cond
            (nil? n) (do (usage (str "Invalid --max-suggestions: " a)) (System/exit 1))
            (< n 1) (do (usage "--max-suggestions must be >= 1") (System/exit 1))
            :else (recur (keep-rest b more) (assoc parsed :max-suggestions n))))

        :else
        (do
          (usage (str "Unknown arg: " a))
          (System/exit 1))))))

(defn- datasource-from-config
  [config]
  (let [{:keys [host port dbname user password]} (:database config)]
    (jdbc/get-datasource {:dbtype "postgresql"
                          :host host
                          :port port
                          :dbname dbname
                          :user user
                          :password password})))

(defn- fetch-articles
  [ds {:keys [limit]}]
  (jdbc/execute!
    ds
    (sql/format
      (cond-> {:select [:id :canonical_name]
               :from [:articles]
               :order-by [:canonical_name]}
        (some? limit) (assoc :limit limit)))
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- normalize-lang
  [lang]
  (-> (or lang default-lang)
    str
    str/trim
    (str/replace "_" "-")
    str/lower-case))

(defn- jspell-language
  "Map our CLI `--lang` value to a JSpell/RapidAPI language code.

  The JSpell backend expects locale-like strings (often `xx_YY`). If you pass a full
  locale already, we preserve it."
  [lang]
  (let [lang (normalize-lang lang)]
    (cond
      (re-find #"^[a-z]{2}_[A-Z]{2}$" (str/replace lang "-" "_"))
      (str/replace lang "-" "_")

      (#{"bs" "bs-ba"} lang) "bs_BA"
      (#{"hr" "hr-hr"} lang) "hr_HR"
      (#{"sr" "sr-rs" "sr-latn" "sr-latn-rs"} lang) "sr_RS"
      (#{"sr-cyrl" "sr-cyrl-rs"} lang) "sr_RS"

      :else
      ;; Fall back to the raw value; the API may accept additional locales.
      (str/replace lang "-" "_"))))

(defn- getenv*
  [k]
  (let [v (System/getenv k)]
    (when-not (str/blank? v)
      v)))

(defn- dotenv-value
  "Best-effort .env reader for a single key (KEY=VALUE). Ignores comments and blank lines.

  We intentionally keep this minimal and avoid pulling in extra deps." 
  [k]
  (let [f (io/file ".env")]
    (when (.exists f)
      (try
        (with-open [r (io/reader f)]
          (some
            (fn [line]
              (let [line (some-> line str/trim)]
                (when (and (some? line)
                        (not (str/blank? line))
                        (not (str/starts-with? line "#"))
                        (str/starts-with? line (str k "=")))
                  (let [raw (subs line (inc (count k)))
                        value (some-> raw str/trim not-empty)]
                    (when value
                      (cond
                        (and (str/starts-with? value "\"") (str/ends-with? value "\""))
                        (subs value 1 (dec (count value)))

                        (and (str/starts-with? value "'") (str/ends-with? value "'"))
                        (subs value 1 (dec (count value)))

                        :else
                        value))))))
            (line-seq r)))
        (catch Exception _
          nil)))))

(defn- resolve-api-key
  []
  (or (getenv* default-api-key-env)
      (getenv* "RAPIDAPI_KEY")
      (dotenv-value default-api-key-env)
      (dotenv-value "RAPIDAPI_KEY")))

(defn- start-jspell-mcp!
  [{:keys [mcp-url api-host api-key]}]
  (let [mcp-url (or mcp-url default-mcp-url)
        api-host (or api-host default-mcp-api-host)
        cmd ["npx"
             "mcp-remote"
             mcp-url
             "--header" (str "x-api-host: " api-host)
             "--header" (str "x-api-key: " api-key)]
        pb (doto (ProcessBuilder. (into-array String cmd))
             (.redirectErrorStream true))
        proc (.start pb)
        rdr (io/reader (.getInputStream proc))
        wtr (io/writer (.getOutputStream proc))
        next-id (atom 0)]
    {:process proc
     :reader rdr
     :writer wtr
     :next-id next-id
     :config {:mcp-url mcp-url
              :api-host api-host
              :api-key-env (if (getenv* default-api-key-env) default-api-key-env "RAPIDAPI_KEY")}}))

(defn- stop-mcp!
  [{:keys [^Process process reader writer]}]
  (try
    (when writer (.close writer))
    (catch Exception _e nil))
  (try
    (when reader (.close reader))
    (catch Exception _e nil))
  (try
    (when process
      (.destroy process))
    (catch Exception _e nil)))

(defn- mcp-send!
  [{:keys [writer]} msg]
  (.write writer (str (json/generate-string msg) "\n"))
  (.flush writer))

(defn- mcp-read-message
  [{:keys [reader]}]
  (when-let [line (.readLine ^java.io.BufferedReader reader)]
    (try
      (json/parse-string line true)
      (catch Exception _e
        ;; Some servers may emit non-JSON lines on stderr; since we redirect stderr
        ;; into stdout, ignore non-JSON lines.
        {:_non_json_line line}))))

(defn- mcp-call!
  [{:keys [next-id] :as client} method params]
  (let [id (swap! next-id inc)
        req {:jsonrpc "2.0"
             :id id
             :method method
             :params (or params {})}]
    (mcp-send! client req)
    (loop []
      (let [msg (mcp-read-message client)]
        (cond
          (nil? msg)
          (throw (ex-info "MCP server closed the connection" {:method method}))

          ;; Some adapters may emit JSON that is not a map (arrays, etc.). Ignore and continue.
          (not (map? msg))
          (recur)

          (contains? msg :_non_json_line)
          (recur)

          (not= id (:id msg))
          (recur)

          (contains? msg :error)
          (throw (ex-info "MCP call failed" {:method method
                                             :error (:error msg)}))

          :else
          (:result msg))))))

(defn- mcp-notify!
  [client method params]
  (mcp-send! client {:jsonrpc "2.0"
                     :method method
                     :params (or params {})}))

(defn- mcp-initialize!
  [client]
  ;; Protocol version string is best-effort; most servers accept this.
  (mcp-call!
    client
    "initialize"
    {:protocolVersion "2024-11-05"
     :capabilities {}
     :clientInfo {:name "spellcheck-article-canonical-names"
                  :version "0.1.0"}})
  (mcp-notify! client "notifications/initialized" {}))

(defn- mcp-tools-list
  [client]
  (mcp-call! client "tools/list" {}))

(defn- pick-jspell-tool-name
  [tools-list-result]
  (let [tools (or (:tools tools-list-result) [])
        by-name (into {} (map (juxt :name identity) tools))
        exact (or (get by-name "check")
                  (get by-name "jspell-check")
                  (get by-name "JSpell Checker"))
        candidate (or exact
                      (first (filter (fn [{:keys [name inputSchema]}]
                                       (or (re-find #"(?i)check" (str name))
                                           (get-in inputSchema [:properties :fieldvalues])
                                           (get-in inputSchema [:properties :language])))
                                     tools)))]
    (when-not candidate
      (throw (ex-info "Unable to find a spellcheck tool in MCP server" {:tool-names (mapv :name tools)})))
    (:name candidate)))

(defn- normalize-tool-result
  "`tools/call` results vary by MCP adapter.

  Returns {:raw <result> :data <possibly-parsed-json-or-string>}"
  [result]
  (let [content (:content result)
        text (when (sequential? content)
               (some (fn [item]
                       (when (and (map? item) (= "text" (:type item)) (string? (:text item)))
                         (:text item)))
                     content))
        data (cond
               (map? result) (or (:data result) (:result result))
               :else nil)
        best (or data text result)]
    {:raw result
     :data (if (and (string? best)
                 (or (str/starts-with? (str/trim best) "{")
                     (str/starts-with? (str/trim best) "[")))
             (try (json/parse-string best true) (catch Exception _e best))
             best)}))

(defn- jspell-response->issues
  "Best-effort conversion from JSpell responses to our EDN issue format.

  We intentionally support multiple potential shapes, because RapidAPI adapters
  differ (sometimes returning JSON, sometimes a text blob)."
  [text resp {:keys [max-suggestions]}]
  (let [max-suggestions (or max-suggestions default-max-suggestions)
        resp (cond
               (map? resp) resp
               :else {})
        ;; common-ish keys (we don't know which one the backend uses)
        corrected (or (:corrected_text resp)
                      (:correctedText resp)
                      (:corrected resp)
                      (:result resp)
                      (:output resp))
        corrections (or (:corrections resp)
                        (:matches resp)
                        (:issues resp)
                        [])
        range-issues
        (->> corrections
          (keep (fn [c]
                  (let [from (or (:from c) (:offset c) (:start c))
                        len (or (:len c) (:length c))
                        to (or (:to c) (when (and (number? from) (number? len)) (+ from len)))
                        replacement (or (:replacement c) (:suggestion c) (:replaceWith c))
                        suggestions (->> (or (:suggestions c) (:replacements c) (:alternatives c) (when replacement [replacement]))
                                      (remove str/blank?)
                                      distinct
                                      (take max-suggestions)
                                      vec)]
                    (when (and (number? from) (number? to) (<= 0 from to (count text)))
                      {:token (subs text from to)
                       :from from
                       :to to
                       :suggestions suggestions
                       :rule-id "JSPELL"
                       :message (or (:message c) (:shortMessage c) "Possible spelling issue.")}))))
          vec)
        fallback-issues
        (when (and (string? corrected) (not= corrected text))
          [{:token text
            :from 0
            :to (count text)
            :suggestions (->> [corrected]
                           (remove str/blank?)
                           vec)
            :rule-id "JSPELL"
            :message "Suggested corrected text differs from original."}])]
    (if (seq range-issues) range-issues (or fallback-issues []))))

(defn- jspell-suggest
  [client tool-name text {:keys [lang max-suggestions]}]
  (let [language (jspell-language lang)
        result (mcp-call! client "tools/call" {:name tool-name
                                               :arguments {:fieldvalues (str text)
                                                           :language language}})
        {:keys [data]} (normalize-tool-result result)
        issues (jspell-response->issues (str text) data {:max-suggestions max-suggestions})
        suggested (or (when (map? data) (or (:corrected_text data) (:correctedText data) (:corrected data)))
                      (when (and (seq issues)
                              (= 1 (count issues))
                              (= 0 (:from (first issues)))
                              (= (count (str text)) (:to (first issues))))
                        (first (get (first issues) :suggestions))))]
    {:issues issues
     :suggested suggested}))



(defn -main
  [& args]
  (let [{:keys [profile out lang limit max-suggestions]} (parse-args args)]
    (try
      (let [config (aero/read-config "config/base.edn" {:profile profile})
            ds (datasource-from-config config)
            articles (fetch-articles ds {:limit limit})

            api-key (resolve-api-key)
            _ (when-not api-key
                (binding [*out* *err*]
                  (println "Missing RapidAPI key for JSpell.")
                  (println (str "Set " default-api-key-env " (preferred) or RAPIDAPI_KEY in your environment.")))
                (System/exit 1))

            client (start-jspell-mcp! {:mcp-url (getenv* "JSPELL_MCP_URL")
                                       :api-host (or (getenv* "JSPELL_MCP_API_HOST") default-mcp-api-host)
                                       :api-key api-key})
            _ (mcp-initialize! client)
            tools (mcp-tools-list client)
            tool-name (pick-jspell-tool-name tools)

            results
            (try
              (->> articles
                (map (fn [{:keys [id canonical_name]}]
                       (let [text (str canonical_name)
                             {:keys [issues suggested]} (jspell-suggest client tool-name text {:lang lang
                                                                                               :max-suggestions max-suggestions})]
                         {:id id
                          :canonical_name text
                          :issues issues
                          :suggested_canonical_name suggested})))
                (filter (fn [{:keys [issues suggested_canonical_name canonical_name]}]
                          (or (seq issues)
                              (and (string? suggested_canonical_name)
                                (not= suggested_canonical_name canonical_name)))))
                vec)
              (finally
                (stop-mcp! client)))

            report
            {:generated_at (str (Instant/now))
             :profile profile
             :language lang
             :spellchecker {:type :mcp
                            :provider :jspell
                            :mcp {:url (get-in client [:config :mcp-url])
                                  :api_host (get-in client [:config :api-host])
                                  :api_key_env (get-in client [:config :api-key-env])
                                  :tool tool-name}}
             :out_file out
             :summary {:total_articles (count articles)
                       :articles_with_issues (count results)
                       :total_issues (reduce + 0 (map (comp count :issues) results))}
             :results results}]
        (spit out (with-out-str (pprint/pprint report)))
        (println "Wrote" (.getPath (io/file out)))
        (println "Articles checked:" (count articles))
        (println "Articles with issues:" (count results))
        (println "Total issues:" (get-in report [:summary :total_issues])))
      (catch Exception e
        (binding [*out* *err*]
          (println "Spellcheck failed.")
          (println "Profile:" (name profile))
          (println "Message:" (.getMessage e))
          (println "Tip: ensure Postgres is running and `config/.secrets.edn` has the DB password for that profile.")
          (println "Tip: ensure JSpell MCP prerequisites: Node/npx installed and RapidAPI key env var set."))
        (System/exit 1)))))

(apply -main *command-line-args*)
