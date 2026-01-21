(ns app.domain.backend.expenses.integrations.cerebras.config
  "Cerebras (OpenAI-compatible) configuration.

  Currently used for the optional receipt refinement step after OCR markdown is
  produced by Mistral."
  (:require
    [clojure.string :as str]))

(def ^:private default-base-url "https://api.cerebras.ai/v1")
(def ^:private default-model "llama-3.3-70b")
(def ^:private default-conn-timeout-ms 5000)
(def ^:private default-socket-timeout-ms 20000)
(def ^:private default-max-retries 2)
(def ^:private default-retry-sleep-ms 500)

(defn build-config
  "Build a Cerebras config from an app config map (Aero) and environment.

  Environment variables override app config when present:
  - `CEREBRAS_API_KEY`
  - `CEREBRAS_BASE_URL` (default https://api.cerebras.ai/v1)
  - `CEREBRAS_MODEL` (default llama-3.3-70b)
  - `CEREBRAS_RECEIPT_REFINE_ENABLED` (true/false, default false)

  App config keys (optional):
  {:cerebras {:api-key <token>
              :base-url <url>
              :model <model>
              :receipt-refine-enabled? false
              :conn-timeout-ms 5000
              :socket-timeout-ms 20000
              :max-retries 2
              :retry-sleep-ms 500}}"
  ([app-config]
   (build-config app-config nil))
  ([app-config {:keys [getenv]
                :or {getenv (fn [k] (System/getenv k))}}]
   (let [cfg (or (:cerebras app-config) {})
         getenv* (fn [k] (some-> (getenv k) str/trim (not-empty)))
         parse-bool (fn [s]
                      (when s
                        (let [s (str/lower-case (str/trim s))]
                          (cond
                            (contains? #{"1" "true" "yes" "y" "on"} s) true
                            (contains? #{"0" "false" "no" "n" "off"} s) false
                            :else nil))))
         parse-int (fn [s]
                     (when (and s (re-matches #"\\d+" s))
                       (Long/parseLong s)))
         env-enabled (some-> (getenv* "CEREBRAS_RECEIPT_REFINE_ENABLED") parse-bool)
         enabled? (if (some? env-enabled)
                    env-enabled
                    (boolean (:receipt-refine-enabled? cfg)))
         api-key (or (getenv* "CEREBRAS_API_KEY") (:api-key cfg))]
     {:enabled? enabled?
      :api-key api-key
      :base-url (or (getenv* "CEREBRAS_BASE_URL") (:base-url cfg) default-base-url)
      :model (or (getenv* "CEREBRAS_MODEL") (:model cfg) default-model)
      :conn-timeout-ms (or (some-> (getenv* "CEREBRAS_CONN_TIMEOUT_MS") parse-int)
                         (:conn-timeout-ms cfg)
                         default-conn-timeout-ms)
      :socket-timeout-ms (or (some-> (getenv* "CEREBRAS_SOCKET_TIMEOUT_MS") parse-int)
                           (:socket-timeout-ms cfg)
                           default-socket-timeout-ms)
      :max-retries (or (some-> (getenv* "CEREBRAS_MAX_RETRIES") parse-int)
                     (:max-retries cfg)
                     default-max-retries)
      :retry-sleep-ms (or (some-> (getenv* "CEREBRAS_RETRY_SLEEP_MS") parse-int)
                        (:retry-sleep-ms cfg)
                        default-retry-sleep-ms)})))

(defn default-base-url-value []
  default-base-url)

(defn default-model-name []
  default-model)
