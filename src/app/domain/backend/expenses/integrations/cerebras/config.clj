(ns app.domain.backend.expenses.integrations.cerebras.config
  "Cerebras (OpenAI-compatible) configuration.

  Currently used for the optional receipt refinement step after OCR markdown is
  produced by Mistral."
  (:require
    [clojure.string :as str]))

(defn- read-dotenv-file
  "Read a .env-style file into a string map.

  Supports lines like:
  - KEY=value
  - export KEY=value
  - quoted values: KEY=\"value\" or KEY='value'

  NOTE: This is intentionally minimal (not a full bash parser)."
  [path]
  (letfn [(find-dotenv-path*
            ([]
             (find-dotenv-path* (System/getProperty "user.dir")))
            ([start-dir]
             (loop [dir (some-> start-dir java.io.File. .getAbsoluteFile)
                    seen #{}]
               (when (and dir (not (contains? seen (.getPath dir))))
                 (let [dotenv (java.io.File. dir ".env")
                       project-marker? (or (.exists (java.io.File. dir "deps.edn"))
                                         (.exists (java.io.File. dir "bb.edn"))
                                         (.exists (java.io.File. dir ".git")))]
                   (cond
                     (.exists dotenv) (.getPath dotenv)
                     project-marker? nil
                     :else (recur (some-> dir .getParentFile)
                             (conj seen (.getPath dir)))))))))
          (strip-quotes [v]
            (cond
              (and (string? v)
                (<= 2 (count v))
                (str/starts-with? v "\"")
                (str/ends-with? v "\""))
              (subs v 1 (dec (count v)))

              (and (string? v)
                (<= 2 (count v))
                (str/starts-with? v "'")
                (str/ends-with? v "'"))
              (subs v 1 (dec (count v)))

              :else v))
          (parse-line [line]
            (let [line (some-> line str/trim)]
              (when (and (seq line) (not (str/starts-with? line "#")))
                (let [line (if (str/starts-with? line "export ")
                             (subs line (count "export "))
                             line)
                      idx (.indexOf ^String line "=")]
                  (when (pos? idx)
                    (let [k (some-> (subs line 0 idx) str/trim not-empty)
                          v (some-> (subs line (inc idx)) str/trim strip-quotes)]
                      (when k [k v])))))))]
    (let [path (or (some-> path str/trim not-empty) (find-dotenv-path*) ".env")
          f (java.io.File. path)]
      (if-not (.exists f)
        {}
        (try
          (->> (str/split-lines (slurp f))
            (keep parse-line)
            (into {}))
          (catch Exception _
            {}))))))

(def ^:private default-base-url "https://api.cerebras.ai/v1")
(def ^:private default-model "qwen-3-32b")
(def ^:private default-conn-timeout-ms 5000)
(def ^:private default-socket-timeout-ms 20000)
(def ^:private default-max-retries 2)
(def ^:private default-retry-sleep-ms 500)

(defn build-config
  "Build a Cerebras config from an app config map (Aero) and environment.

  Environment variables override app config when present for secret/provider
  settings (e.g. API keys).

  Receipt refinement is primarily controlled per-user (Expenses Settings).

  - `CEREBRAS_API_KEY`
  - `CEREBRAS_BASE_URL` (default https://api.cerebras.ai/v1)
  - `CEREBRAS_MODEL` (default qwen-3-32b)

  App config keys (optional):
  {:cerebras {:api-key <token>
              :base-url <url>
              :model <model>
              :conn-timeout-ms 5000
              :socket-timeout-ms 20000
              :max-retries 2
              :retry-sleep-ms 500}}"
  ([app-config]
   (build-config app-config nil))
  ([app-config {:keys [getenv dotenv-path]
                :or {getenv (fn [k] (System/getenv k))}}]
   (let [cfg (or (:cerebras app-config) {})
         dotenv-vars (delay (read-dotenv-file dotenv-path))
         getenv* (fn [k] (some-> (or (getenv k) (get @dotenv-vars k)) str/trim (not-empty)))
         parse-int (fn [s]
                     (when (and s (re-matches #"\\d+" s))
                       (Long/parseLong s)))
         api-key (or (getenv* "CEREBRAS_API_KEY") (:api-key cfg))
         enabled? (boolean (seq api-key))]
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
