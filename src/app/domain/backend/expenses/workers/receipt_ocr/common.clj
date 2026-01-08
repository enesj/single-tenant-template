(ns app.domain.backend.expenses.workers.receipt-ocr.common
  "Shared helper fns used by the receipt OCR worker. Kept separate so the worker
  orchestration code stays readable and the parsing helpers can be unit tested."
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str])
  (:import
    [java.nio.file Files]
    [java.time Instant LocalDate LocalDateTime OffsetDateTime ZoneId ZoneOffset]))

(def ^:private allowed-currencies
  "Currencies we accept for receipt guesses."
  #{"BAM" "EUR" "USD"})

(defn safe-ex-data
  "Sanitize (ex-data e) for safe storage/logging."
  [e]
  (let [data (ex-data e)]
    (when (map? data)
      (let [clean (-> data
                    (dissoc :bytes :body :request :http-client)
                    (select-keys [:type :status :body-snippet :response :error :message]))]
        (cond-> clean
          (map? (:response clean))
          (update :response select-keys [:status :headers :reason-phrase])

          (map? (:request clean))
          (update :request select-keys [:method :url :query-string]))))))

(defn parse-instant
  "Best-effort parse of multiple timestamp formats to java.time.Instant."
  [v]
  (let [v (some-> v str str/trim not-empty)]
    (when v
      (or
        (try
          (Instant/parse v)
          (catch Exception _ nil))
        (try
          (-> (OffsetDateTime/parse v) .toInstant)
          (catch Exception _ nil))
        (try
          (-> (LocalDateTime/parse v)
            (.atZone (ZoneId/systemDefault))
            .toInstant)
          (catch Exception _ nil))
        (try
          (-> (LocalDate/parse v)
            (.atStartOfDay ZoneOffset/UTC)
            .toInstant)
          (catch Exception _ nil))))))

(defn normalize-currency
  "Normalize a currency code (upper case) and apply a default.

  Returns nil if both currency and default are outside allowed-currencies."
  ([currency] (normalize-currency currency "BAM"))
  ([currency default-currency]
   (let [default-currency (some-> default-currency str/trim str/upper-case)
         currency (some-> currency str/trim str/upper-case)]
     (cond
       (contains? allowed-currencies currency) currency
       (contains? allowed-currencies default-currency) default-currency
       :else nil))))

(defn parse-money
  "Parse a money-ish value into a BigDecimal.

  Accepts numbers, BigDecimal, or strings with optional currency symbols and
  either '.' or ',' decimal separators."
  [v]
  (cond
    (nil? v) nil
    (instance? java.math.BigDecimal v) v
    (number? v) (bigdec v)
    (string? v)
    (let [s (-> v
              str/trim
              ;; keep digits/decimal separators/minus
              (str/replace #"[^0-9,\.\-]" ""))
          s (cond
              ;; both separators present → treat commas as thousands
              (and (str/includes? s ",") (str/includes? s ".")) (str/replace s "," "")
              ;; only comma present → treat comma as decimal separator
              (str/includes? s ",") (str/replace s "," ".")
              :else s)]
      (try
        (bigdec s)
        (catch Exception _ nil)))
    :else nil))

(defn storage-key->file
  "Resolve a receipt's storage_key to a local java.io.File.

  The worker only supports local filesystem paths. A non-absolute storage_key is
  optionally joined with :storage-base-dir."
  [{:keys [storage_key]} {:keys [storage-base-dir]}]
  (let [k (some-> storage_key str/trim not-empty)]
    (when-not k
      (throw (ex-info "storage_key missing" {:type :receipt/missing-storage-key})))
    (when (or (str/starts-with? k "s3://")
            (str/starts-with? k "http://")
            (str/starts-with? k "https://"))
      (throw (ex-info "storage_key scheme not supported by worker" {:type :receipt/unsupported-storage-key
                                                                    :storage_key k})))
    (let [f (io/file k)
          f (if (and storage-base-dir (not (.isAbsolute f)))
              (io/file storage-base-dir k)
              f)]
      f)))

(defn read-receipt-bytes!
  "Read the receipt bytes from local storage.

  opts:
  - :storage-base-dir
  - :max-file-size-bytes (default 10MB)"
  [receipt {:keys [max-file-size-bytes]
            :or {max-file-size-bytes (* 10 1024 1024)}
            :as opts}]
  (let [file (storage-key->file receipt opts)]
    (when-not (.exists file)
      (throw (ex-info "receipt file not found" {:type :receipt/file-not-found
                                                :path (.getPath file)})))
    (let [size (.length file)]
      (when (and max-file-size-bytes (pos? max-file-size-bytes) (> size max-file-size-bytes))
        (throw (ex-info "receipt file too large" {:type :receipt/file-too-large
                                                  :path (.getPath file)
                                                  :size-bytes size
                                                  :max-bytes max-file-size-bytes})))
      {:bytes (Files/readAllBytes (.toPath file))
       :file-size-bytes size
       :path (.getPath file)})))
