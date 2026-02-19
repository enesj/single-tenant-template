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

(def ^:private default-agentic-custom-prompt
  (str
    "Extract merchant header fields into: merchant.name, merchant.store_name, merchant.address, merchant.raw_address. "
    "Rules (in order): "
    "1) merchant.name (supplier) = legal/brand merchant line (usually first header line, often with doo/d.o.o./dd). Never take supplier from branch/store lines. "
    "2) merchant.store_name = branch/business-unit line (starts with PJ, Podruznica/Podružnica, Ogranak, Poslovnica, Filijala, etc.). "
    "3) merchant.address = street + postal/city lines that follow the store line. "
    "4) Keep postal code/city in address (do not move postal/city into store_name). "
    "5) merchant.raw_address = merchant.store_name + ', ' + merchant.address. "
    "6) If a value is missing, leave it null. "
    "7) For line items, never prefix product labels with table/header words like Artikal/Naziv/Opis/Label/Item; output only the real product name. "
    "8) Keep all purchased rows exactly once, including the first product row after a header line. "
    "Important: If store line contains quoted text (e.g. PJ 57, \"HIPERMARKET\" Otoka), keep it in store_name only. "
    "Never use that quoted branch text as merchant.name."))

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
  - LLAMAPARSE_AGENTIC_CUSTOM_PROMPT (optional, appended to LlamaParse system prompt)
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
                   raw-version)
         agentic-custom-prompt
         (or (getenv* "LLAMAPARSE_AGENTIC_CUSTOM_PROMPT")
           (some-> (:agentic-custom-prompt cfg) str str/trim not-empty)
           (some-> (:custom-prompt cfg) str str/trim not-empty)
           default-agentic-custom-prompt)]
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
      :agentic-custom-prompt agentic-custom-prompt
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
