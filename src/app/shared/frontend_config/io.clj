(ns app.shared.frontend-config.io
  "Shared EDN I/O helpers for frontend configuration.

  Used by the bootstrap path to seed runtime config rows from
  source-controlled EDN defaults, and by the sync/validate tooling
  to keep those defaults aligned with the DB schema."
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

(defn- resource-path-candidates
  [path]
  (let [s (cond
            (nil? path) nil
            (instance? java.io.File path) (.getPath ^java.io.File path)
            :else (str path))]
    (->> [s
          (some-> s (clojure.string/replace-first #"^src/" ""))
          (some-> s (clojure.string/replace-first #"^resources/" ""))
          (some-> s (clojure.string/replace-first #"^config/" ""))
          (some-> s (clojure.string/replace-first #"^vendor/" ""))]
      (remove nil?)
      distinct)))

(defn- read-edn-source
  [path]
  (let [f (as-file path)]
    (cond
      (and f (.exists f))
      {:kind :file
       :path (.getPath f)
       :value (edn/read-string (slurp f))}

      :else
      (when-let [resource (some io/resource (resource-path-candidates path))]
        {:kind :resource
         :path (str resource)
         :value (edn/read-string (slurp resource))}))))

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
       (if-let [source (read-edn-source path)]
         (:value source)
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
       (if-let [source (read-edn-source path)]
         (:value source)
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
