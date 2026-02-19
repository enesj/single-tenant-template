(ns app.domain.backend.expenses.integrations.llamaparse.config
  "LlamaParse OCR configuration."
  (:require
    [clojure.string :as str]))

(def ^:private default-base-url "https://api.cloud.llamaindex.ai")
(def ^:private default-tier "agentic")
(def ^:private default-version "latest")
(def ^:private default-expand "items,markdown,text")
(def ^:private default-conn-timeout-ms 5000)
(def ^:private default-socket-timeout-ms 30000)
(def ^:private default-poll-interval-ms 1500)
(def ^:private default-poll-timeout-ms 120000)
(def ^:private default-max-retries 2)
(def ^:private default-retry-sleep-ms 500)

(defn- parse-bool [s]
  (when s
    (let [s (str/lower-case (str/trim s))]
      (cond
        (contains? #{"1" "true" "yes" "y" "on"} s) true
        (contains? #{"0" "false" "no" "n" "off"} s) false
        :else nil))))

(defn- parse-int [s]
  (when (and s (re-matches #"\\d+" s))
    (Long/parseLong s)))

(defn- parse-languages [s]
  (when (seq s)
    (->> (str/split s #",")
      (map str/trim)
      (remove str/blank?)
      vec
      not-empty)))

(defn build-config
  "Build LlamaParse provider config from app config + env overrides.

  Environment variables:
  - LLAMAPARSE_ENABLED=true|false (default true)
  - LLAMA_CLOUD_API_KEY (or LLAMAPARSE_API_KEY)
  - LLAMAPARSE_BASE_URL (default https://api.cloud.llamaindex.ai)
  - LLAMAPARSE_TIER (default agentic)
  - LLAMAPARSE_VERSION (default v2)
  - LLAMAPARSE_EXPAND (default items,markdown,text)
  - LLAMAPARSE_CONN_TIMEOUT_MS
  - LLAMAPARSE_SOCKET_TIMEOUT_MS
  - LLAMAPARSE_POLL_INTERVAL_MS
  - LLAMAPARSE_POLL_TIMEOUT_MS
  - LLAMAPARSE_MAX_RETRIES
  - LLAMAPARSE_RETRY_SLEEP_MS
  - LLAMAPARSE_OCR_LANGUAGES (comma-separated, e.g. \"en,bs\")"
  ([app-config]
   (build-config app-config nil))
  ([app-config {:keys [getenv]
                :or {getenv (fn [k] (System/getenv k))}}]
   (let [cfg (or (:llamaparse app-config) {})
         getenv* (fn [k] (some-> (getenv k) str/trim not-empty))
         env-enabled (some-> (getenv* "LLAMAPARSE_ENABLED") parse-bool)
         enabled? (if (some? env-enabled)
                    env-enabled
                    (if (contains? cfg :enabled?)
                      (:enabled? cfg)
                      true))
         tier (or (getenv* "LLAMAPARSE_TIER")
                (:tier cfg)
                default-tier)
         raw-version (or (getenv* "LLAMAPARSE_VERSION")
                       (:version cfg)
                       default-version)
         version (if (and (= "agentic" (some-> tier str/lower-case))
                       (= "v2" (some-> raw-version str/lower-case)))
                   "latest"
                   raw-version)]
     {:enabled? enabled?
      :auto-post-after-upload? (if (contains? cfg :ocr-auto-post-after-upload?)
                                 (:ocr-auto-post-after-upload? cfg)
                                 true)
      :api-key (or (getenv* "LLAMA_CLOUD_API_KEY")
                 (getenv* "LLAMAPARSE_API_KEY")
                 (:api-key cfg))
      :base-url (or (getenv* "LLAMAPARSE_BASE_URL")
                  (:base-url cfg)
                  default-base-url)
      :tier tier
      :version version
      :expand (or (getenv* "LLAMAPARSE_EXPAND")
                (:expand cfg)
                default-expand)
      :ocr-languages (or (some-> (getenv* "LLAMAPARSE_OCR_LANGUAGES") parse-languages)
                       (:ocr-languages cfg))
      :conn-timeout-ms (or (some-> (getenv* "LLAMAPARSE_CONN_TIMEOUT_MS") parse-int)
                         (:conn-timeout-ms cfg)
                         default-conn-timeout-ms)
      :socket-timeout-ms (or (some-> (getenv* "LLAMAPARSE_SOCKET_TIMEOUT_MS") parse-int)
                           (:socket-timeout-ms cfg)
                           default-socket-timeout-ms)
      :poll-interval-ms (or (some-> (getenv* "LLAMAPARSE_POLL_INTERVAL_MS") parse-int)
                          (:poll-interval-ms cfg)
                          default-poll-interval-ms)
      :poll-timeout-ms (or (some-> (getenv* "LLAMAPARSE_POLL_TIMEOUT_MS") parse-int)
                         (:poll-timeout-ms cfg)
                         default-poll-timeout-ms)
      :max-retries (or (some-> (getenv* "LLAMAPARSE_MAX_RETRIES") parse-int)
                     (:max-retries cfg)
                     default-max-retries)
      :retry-sleep-ms (or (some-> (getenv* "LLAMAPARSE_RETRY_SLEEP_MS") parse-int)
                        (:retry-sleep-ms cfg)
                        default-retry-sleep-ms)})))

(defn default-base-url-value [] default-base-url)
