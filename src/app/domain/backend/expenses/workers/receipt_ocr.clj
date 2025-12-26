(ns app.domain.backend.expenses.workers.receipt-ocr
  "Receipt OCR worker.

  This worker is intended to run out-of-band (cron / one-shot runner) and moves
  receipts through the existing status machine:

  uploaded → parsing → parsed → extracting → extracted|review_required

  Persistence uses existing functions in `app.domain.backend.expenses.services.receipts`."
  (:require
    [app.domain.backend.expenses.integrations.mistral-ocr :as mistral-ocr]
    [app.domain.backend.expenses.services.receipts :as receipts]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [malli.core :as m]
    [taoensso.timbre :as log])
  (:import
    [java.nio.file Files]
    [java.sql Timestamp]
    [java.time Instant LocalDate LocalDateTime OffsetDateTime ZoneId ZoneOffset]))

(def ^:private allowed-currencies #{"BAM" "EUR" "USD"})

(def ^:private ReceiptExtraction
  [:map {:closed false}
   [:merchant [:map {:closed false}
               [:name string?]
               [:address {:optional true} [:maybe string?]]
               [:tax_id {:optional true} [:maybe string?]]]]
   [:purchased_at {:optional true} [:maybe string?]]
   [:currency {:optional true} [:maybe string?]]
   [:totals [:map {:closed false}
             [:subtotal {:optional true} [:maybe [:or number? string?]]]
             [:tax {:optional true} [:maybe [:or number? string?]]]
             [:total [:or number? string?]]]]
   [:payment_hints {:optional true}
    [:maybe [:map {:closed false}
             [:method {:optional true} [:maybe string?]]
             [:card_last4 {:optional true} [:maybe string?]]]]]
   [:items [:sequential [:map {:closed false}
                         [:raw_label string?]
                         [:qty {:optional true} [:maybe [:or number? string?]]]
                         [:unit_price {:optional true} [:maybe [:or number? string?]]]
                         [:line_total [:or number? string?]]]]]])

(defn- safe-ex-data [e]
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

(defn- parse-instant [v]
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

(defn- normalize-currency
  ([currency] (normalize-currency currency "BAM"))
  ([currency default-currency]
   (let [default-currency (some-> default-currency str/trim str/upper-case)
         currency (some-> currency str/trim str/upper-case)]
     (cond
       (contains? allowed-currencies currency) currency
       (contains? allowed-currencies default-currency) default-currency
       :else nil))))

(defn- parse-money [v]
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

(defn- storage-key->file
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

(defn- read-receipt-bytes!
  [receipt {:keys [max-file-size-bytes] :or {max-file-size-bytes (* 10 1024 1024)} :as opts}]
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

(defn- extraction->guesses
  [{:keys [merchant totals currency purchased_at payment_hints items]} {:keys [default-currency]}]
  (let [supplier (some-> merchant :name str/trim not-empty)
        total (parse-money (some-> totals :total))
        currency* (normalize-currency currency (or default-currency "BAM"))
        purchased-at (some-> purchased_at parse-instant)
        purchased-at-ts (some-> purchased-at Timestamp/from)
        payment (when (map? payment_hints)
                  (select-keys payment_hints [:method :card_last4]))
        items-count (if (sequential? items) (count items) 0)]
    {:supplier_guess supplier
     :total_amount_guess total
     :currency_guess currency*
     :purchased_at_guess purchased-at-ts
     :payment_hints payment
     :items-count items-count}))

(defn- review-required?
  [{:keys [supplier_guess total_amount_guess currency_guess items-count]}]
  (or (nil? supplier_guess)
    (nil? total_amount_guess)
    (nil? currency_guess)
    (zero? (long (or items-count 0)))))

(defn- process-parse!
  [db ocr-cfg receipt opts]
  (let [receipt-id (:id receipt)]
    (if-not (seq (:api-key ocr-cfg))
      (do
        (log/warn "Receipt OCR parse skipped: missing Mistral API key" {:receipt-id receipt-id})
        {:receipt-id receipt-id :stage :parse :result :skipped :reason :missing-api-key})
      (if-let [_claimed (receipts/claim-for-parsing! db receipt-id {:lease-seconds (:lease-seconds opts)})]
        (try
          (let [{:keys [bytes]} (read-receipt-bytes! receipt opts)
                parse-result (mistral-ocr/ocr-parse! ocr-cfg {:bytes bytes
                                                              :filename (:original_filename receipt)
                                                              :content-type (:content_type receipt)})]
            (receipts/store-extraction-results! db receipt-id {:raw_parse_json (:raw parse-result)
                                                               :parsed_markdown (:parsed-markdown parse-result)})
            (receipts/update-status! db receipt-id "parsed" {:error_message nil :error_details nil})
            {:receipt-id receipt-id :stage :parse :result :ok})
          (catch Exception e
            (receipts/mark-failed! db receipt-id (or (.getMessage e) "Parse failed") (safe-ex-data e))
            {:receipt-id receipt-id :stage :parse :result :failed :error (.getMessage e)}))
        {:receipt-id receipt-id :stage :parse :result :skipped :reason :not-claimed}))))

(defn- process-extract!
  [db ocr-cfg receipt opts]
  (let [receipt-id (:id receipt)]
    (if-not (seq (:api-key ocr-cfg))
      (do
        (log/warn "Receipt OCR extract skipped: missing Mistral API key" {:receipt-id receipt-id})
        {:receipt-id receipt-id :stage :extract :result :skipped :reason :missing-api-key})
      (if-let [_claimed (receipts/claim-for-extracting! db receipt-id {:lease-seconds (:lease-seconds opts)})]
        (try
          (let [{:keys [bytes]} (read-receipt-bytes! receipt opts)
                extract-result (mistral-ocr/ocr-extract! ocr-cfg {:bytes bytes
                                                                  :content-type (:content_type receipt)})
                extraction (:extraction extract-result)
                valid-shape? (and (map? extraction) (m/validate ReceiptExtraction extraction))
                guesses (when (map? extraction) (extraction->guesses extraction opts))
                status (if (and valid-shape? guesses (not (review-required? guesses)))
                         "extracted"
                         "review_required")
                raw-extract-json {:provider "mistral"
                                  :received_at (:received-at extract-result)
                                  :model (:model extract-result)
                                  :response (:raw extract-result)
                                  :extraction extraction
                                  :valid_shape? valid-shape?}]
            (receipts/store-extraction-results!
              db
              receipt-id
              (merge {:raw_extract_json raw-extract-json
                      :parsed_markdown (:parsed-markdown extract-result)}
                (select-keys guesses [:supplier_guess
                                      :total_amount_guess
                                      :currency_guess
                                      :purchased_at_guess
                                      :payment_hints])))
            (receipts/update-status! db receipt-id status {:error_message nil :error_details nil})
            {:receipt-id receipt-id :stage :extract :result :ok :status status})
          (catch Exception e
            (receipts/mark-failed! db receipt-id (or (.getMessage e) "Extraction failed") (safe-ex-data e))
            {:receipt-id receipt-id :stage :extract :result :failed :error (.getMessage e)}))
        {:receipt-id receipt-id :stage :extract :result :skipped :reason :not-claimed}))))

(defn process-receipt!
  "Process a single receipt based on its current status.
  If status is 'uploaded', it will perform both parse and extract in one go (if supported).

  Returns a small result map describing what happened."
  [db ocr-cfg receipt opts]
  (case (:status receipt)
    "uploaded"
    ;; For Mistral, we can do both in one call.
    ;; We'll use process-extract! but it will also store the markdown.
    (process-extract! db ocr-cfg receipt opts)

    "parsing"
    (let [parse-res (process-parse! db ocr-cfg receipt opts)
          ;; Reload status for potential extraction in the same run.
          receipt* (receipts/get-receipt db (:id receipt))]
      (if (and (= :ok (:result parse-res)) (#{"parsed" "extracting"} (:status receipt*)))
        (merge parse-res (process-extract! db ocr-cfg receipt* opts))
        parse-res))

    ("parsed" "extracting")
    (process-extract! db ocr-cfg receipt opts)

    {:receipt-id (:id receipt) :result :skipped :reason :status :status (:status receipt)}))

(defn process-pending!
  "Process pending receipts.

  opts:
  - :max-receipts (default 25)
  - :lease-seconds (default 900)
  - :storage-base-dir (optional)
  - :max-file-size-bytes (default 10MB)
  - :default-currency (default BAM)

  Returns a summary map."
  ([db app-config]
   (process-pending! db app-config nil))
  ([db app-config {:keys [max-receipts] :as opts}]
   (let [ocr-cfg (mistral-ocr/build-config app-config)
         opts (merge {:max-receipts 25
                      :lease-seconds 900
                      :default-currency "BAM"}
                (or opts {}))]
     (if-not (:enabled? ocr-cfg)
       (do
         (log/info "Receipt OCR worker disabled" {})
         {:enabled? false
          :processed 0})
       (let [candidates (receipts/list-pending-for-processing db)
             candidates (take (long (or max-receipts 25)) candidates)
             results (mapv #(process-receipt! db ocr-cfg % opts) candidates)
             summary (->> results
                       (map :result)
                       (frequencies))]
         (log/info "Receipt OCR run complete" {:candidates (count candidates)
                                               :summary summary})
         {:enabled? true
          :candidates (count candidates)
          :summary summary
          :results results})))))
