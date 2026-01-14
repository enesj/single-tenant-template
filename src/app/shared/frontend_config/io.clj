(ns app.shared.frontend-config.io
  "Shared EDN I/O helpers for frontend configuration.

  Motivation:
  - Domain UI config EDNs are edited at runtime via admin screens; the backend
    must read them dynamically without treating them as build inputs.
  - Admin settings APIs also read/write EDN bundles; keeping I/O behavior
    centralized prevents drift (missing-file behavior, error logging, etc.)."
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.pprint :as pprint]
    [taoensso.timbre :as log]))

(defn- as-file
  ^java.io.File
  [path]
  (cond
    (nil? path) nil
    (instance? java.io.File path) path
    :else (io/file path)))

(defn read-edn-or-empty
  "Read EDN from `path` and return the parsed value.

  - Missing file -> `{}`
  - Unreadable/invalid EDN -> `{}` (logs a warning)

  Options (all optional):
  - `:log-message` (string)
  - `:log-context` (map)"
  ([path]
   (read-edn-or-empty path nil))
  ([path {:keys [log-message log-context]
          :or {log-message "Failed to read EDN file"}}]
   (let [f (as-file path)]
     (try
       (if (and f (.exists f))
         (edn/read-string (slurp f))
         {})
       (catch Exception e
         (log/warn e log-message (merge {:path (some-> f .getPath)} log-context))
         {})))))

(defn read-edn-or-throw
  "Read EDN from `path`.

  - Missing file -> `{}`
  - Unreadable/invalid EDN -> throws (logs an error)

  Options (all optional):
  - `:log-message` (string)
  - `:log-context` (map)"
  ([path]
   (read-edn-or-throw path nil))
  ([path {:keys [log-message log-context]
          :or {log-message "Failed to read EDN file"}}]
   (let [f (as-file path)]
     (try
       (if (and f (.exists f))
         (edn/read-string (slurp f))
         {})
       (catch Exception e
         (log/error e log-message (merge {:path (some-> f .getPath)} log-context))
         (throw e))))))

(defn read-edn-or-empty+validate
  "Read EDN from `path` using `read-edn-or-empty` and validate the parsed value.

  `validate-fn` should return a map containing `:valid?` and optionally
  `:errors`, `:warnings`, etc. Validation failures are logged but do not throw.

  Returns the parsed EDN (or `{}` on missing/unreadable)."
  [{:keys [config-key path validate-fn log-message log-context] :as _opts}]
  (let [data (read-edn-or-empty path {:log-message (or log-message "Failed to read EDN file")
                                     :log-context (merge {:config config-key} log-context)})]
    (when validate-fn
      (let [validation (validate-fn data)]
        (when-not (:valid? validation)
          (log/warn "EDN validation issues"
            (merge {:config config-key
                    :path (some-> (as-file path) .getPath)}
              (dissoc validation :valid?)
              log-context)))))
    data))

(defn read-edn-or-throw+validate
  "Read EDN from `path` using `read-edn-or-throw` and validate the parsed value.

  Validation failures are logged but do not throw. Read failures throw.

  Returns the parsed EDN (or `{}` when missing)."
  [{:keys [config-key path validate-fn log-message log-context] :as _opts}]
  (let [data (read-edn-or-throw path {:log-message (or log-message "Failed to read EDN file")
                                     :log-context (merge {:config config-key} log-context)})]
    (when validate-fn
      (let [validation (validate-fn data)]
        (when-not (:valid? validation)
          (log/warn "EDN validation issues"
            (merge {:config config-key
                    :path (some-> (as-file path) .getPath)}
              (dissoc validation :valid?)
              log-context)))))
    data))

(defn write-edn-pretty!
  "Pretty-print `data` to `path` as EDN.

  This helper does not validate; callers should validate before writing.
  Ensures parent directories exist."
  [path data]
  (let [f (as-file path)]
    (when-not f
      (throw (ex-info "Missing path for EDN write" {:path path})))
    (io/make-parents f)
    (spit f (with-out-str (pprint/pprint data)))))
