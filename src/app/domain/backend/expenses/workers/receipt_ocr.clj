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
   [:merchant {:optional true}
    [:maybe
     [:map {:closed false}
      [:name string?]
      [:address {:optional true} [:maybe string?]]
      [:tax_id {:optional true} [:maybe string?]]]]]
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

(defn- normalize-text [s]
  (some-> s
    str
    str/lower-case
    (str/replace #"\s+" " ")
    str/trim
    not-empty))

(defn- label-present-in-markdown? [markdown raw-label]
  (let [m (normalize-text markdown)
        l (normalize-text raw-label)]
    (boolean (and m l (str/includes? m l)))))

(defn- has-letter? [s]
  (boolean (and (string? s) (re-find #"[A-Za-zÀ-ÿ]" s))))

(defn- looks-like-json-schema? [m]
  (boolean
    (and (map? m)
      (contains? m :properties)
      (contains? m :type)
      (contains? m :required))))

(defn- normalize-item-label [raw-label]
  (let [raw-label (some-> raw-label
                    str
                    (str/replace #"\|" " ")
                    (str/replace #"\s+" " ")
                    str/trim
                    not-empty)]
    (when raw-label
      (if-let [[_ _ rest] (re-matches #"(?i)^([0-9]{4,}|[A-Z][0-9]{4,})[ \t]+(.+)$" raw-label)]
        (let [rest (str/trim rest)]
          (if (has-letter? rest)
            rest
            raw-label))
        raw-label))))

(def ^:private unit-prefixes
  #{"t/pc"})

(defn- unit-prefix? [s]
  (when (string? s)
    (contains? unit-prefixes (normalize-text s))))

(def ^:private supplier-ignore-prefixes
  ["jib"
   "pib"
   "tbfm"
   "bf"
   "fiskalni"
   "racun"
   "račun"
   "total"
   "ukupno"
   "pdv"
   "vat"
   "osn"
   "ve"
   "upl"
   "gotovina"
   "kartica"
   "povrat"])

(defn- markdown->supplier-guess [markdown]
  (when (string? markdown)
    (->> (str/split-lines markdown)
      (keep (fn [line0]
              (let [line (some-> line0 str str/trim not-empty)
                    norm (normalize-text line)]
                (when (and norm
                        (not (some #(str/starts-with? norm %) supplier-ignore-prefixes)))
                  line))))
      first)))

(defn- markdown->total-amount [markdown]
  (when (string? markdown)
    (->> (str/split-lines markdown)
      (keep (fn [line0]
              (let [line (some-> line0 str str/trim not-empty)
                    norm (normalize-text line)]
                (when (and norm
                        (or (str/starts-with? norm "total")
                          (str/starts-with? norm "ukupno")))
                  (parse-money line)))))
      last)))

(defn- markdown->pipe-line-items [markdown]
  (when (string? markdown)
    (->> (str/split-lines markdown)
      (map (fn [line]
             (->> (str/split (or line "") #"\|")
               (map str/trim)
               (remove str/blank?)
               vec)))
      (filter #(>= (count %) 4))
      (map (fn [cells]
             (let [raw-label (normalize-item-label (first cells))
                   qty (parse-money (nth cells 1 nil))
                   unit-price (parse-money (nth cells 2 nil))
                   line-total (parse-money (nth cells 3 nil))]
               (when (and (has-letter? raw-label)
                       line-total)
                 {:raw_label raw-label
                  :qty qty
                  :unit_price unit-price
                  :line_total line-total}))))
      (remove nil?)
      vec)))

(defn- line->qty-unit-total [line]
  (let [tokens (->> (str/split (str/trim (or line "")) #"\s+")
                 (remove str/blank?)
                 (remove #{"|" "¦" "│"})
                 vec)]
    (when (seq tokens)
      (or
        ;; qty token already contains "x", e.g. "1.000x"
        (some
          (fn [i]
            (let [t (get tokens i)
                  qty (when (and (string? t) (re-find #"(?i)x" t))
                        (parse-money t))]
              (when qty
                (let [unit-price (parse-money (get tokens (inc i)))
                      line-total (parse-money (get tokens (+ i 2)))]
                  (when unit-price
                    {:label-from-line (when (pos? i) (str/join " " (subvec tokens 0 i)))
                     :qty qty
                     :unit_price unit-price
                     :line_total line-total})))))
          (range (count tokens)))
        ;; separate "x" token, e.g. "1.000 x 2,10 2,10"
        (some
          (fn [i]
            (let [t (get tokens i)]
              (when (and (= "x" (str/lower-case t)) (pos? i))
                (let [qty (parse-money (get tokens (dec i)))
                      unit-price (parse-money (get tokens (inc i)))
                      line-total (parse-money (get tokens (+ i 2)))]
                  (when (and qty unit-price)
                    {:label-from-line (when (pos? (dec i)) (str/join " " (subvec tokens 0 (dec i))))
                     :qty qty
                     :unit_price unit-price
                     :line_total line-total})))))
          (range (count tokens)))))))

(def ^:private discount-line-re
  #"(?i)^\s*-?\s*([0-9]{1,3}(?:[\.,][0-9]{1,2})?)\s*%\s*:?:?\s*(-?[0-9]{1,9}[\.,][0-9]{2})\s*(?:e|km|bam|€)?\s*$")

(defn- line->discount [line]
  (when (string? line)
    (when-let [[_ pct amount] (re-matches discount-line-re (str/trim line))]
      (let [pct (parse-money pct)
            amount (parse-money amount)]
        (when (and pct amount (pos? (double pct)) (<= (double pct) 100.0))
          {:pct (.abs (bigdec pct))
           :amount (bigdec amount)})))))

(defn- apply-discount-to-item
  [{:keys [line_total qty] :as item} {:keys [pct amount]}]
  (let [base-total (parse-money line_total)
        qty (parse-money qty)]
    (if-not (and base-total pct amount)
      item
      (let [base-total (bigdec base-total)
            pct (.abs (bigdec pct))
            amount (bigdec amount)
            amount-abs (.abs amount)
            expected-discount (* base-total (/ pct 100M))
            expected-final (.subtract base-total expected-discount)
            treat-as-final?
            (and (not (neg? (.signum amount)))
              (let [d-discount (double (.abs (.subtract amount expected-discount)))
                    d-final (double (.abs (.subtract amount expected-final)))]
                (<= d-final d-discount)))
            new-total (cond
                        (neg? (.signum amount)) (.subtract base-total amount-abs)
                        treat-as-final? amount
                        :else (.subtract base-total amount))]
        (if (neg? (.signum (bigdec new-total)))
          item
          (let [new-unit (when (and qty (pos? (.signum (bigdec qty))))
                           (.divide (bigdec new-total) (bigdec qty) 2 java.math.RoundingMode/HALF_UP))]
            (cond-> (assoc item :line_total (bigdec new-total))
              new-unit (assoc :unit_price new-unit))))))))

(declare line->trailing-money item-ignore-prefixes)

(defn- markdown->qty-line-items [markdown]
  (when (string? markdown)
    (let [lines (str/split-lines markdown)]
      (loop [remaining lines
             pending []
             items []]
        (if-not (seq remaining)
          (vec items)
          (let [line0 (first remaining)
                line (some-> line0 str str/trim not-empty)
                norm (normalize-text line)]
            (cond
              (nil? line)
              (recur (rest remaining) [] items)

              (or (nil? norm)
                (some #(str/starts-with? norm %) item-ignore-prefixes))
              (recur (rest remaining) [] items)

              (and (seq items) (line->discount line))
              (let [discount (line->discount line)
                    last-item (peek items)
                    items (conj (pop items) (apply-discount-to-item last-item discount))]
                (recur (rest remaining) [] items))

              :else
              (if-let [{:keys [label-from-line qty unit_price line_total]} (line->qty-unit-total line)]
                (let [label-lines-raw (if-let [l (some-> label-from-line str/trim not-empty)]
                                        (if (has-letter? l) [l] pending)
                                        pending)
                      pending-total (->> label-lines-raw
                                      (keep (comp :money line->trailing-money))
                                      last)
                      label-lines (->> label-lines-raw
                                    (map (fn [l]
                                           (or (:prefix (line->trailing-money l)) l)))
                                    (remove str/blank?)
                                    vec)
                      label (->> label-lines
                              (map str/trim)
                              (remove str/blank?)
                              (str/join " ")
                              normalize-item-label)
                      line-total0 (or line_total pending-total)
                      unit-price0 (or unit_price
                                    (when (and qty line-total0 (pos? (.signum (bigdec qty))))
                                      (.divide (bigdec line-total0) (bigdec qty) 2 java.math.RoundingMode/HALF_UP)))
                      line-total1 (or line-total0
                                    (when (and qty unit-price0)
                                      (.setScale (* (bigdec qty) (bigdec unit-price0)) 2 java.math.RoundingMode/HALF_UP)))]
                  (if (and (has-letter? label) line-total1)
                    (recur (rest remaining)
                      []
                      (conj items {:raw_label label
                                   :qty qty
                                   :unit_price unit-price0
                                   :line_total line-total1}))
                    (recur (rest remaining) [] items)))
                (if-let [{:keys [prefix money]} (line->trailing-money line)]
                  (if (str/includes? line "|")
                    (let [pending (cond-> pending
                                    (has-letter? line) (conj line))
                          pending (if (> (count pending) 3)
                                    (subvec pending (- (count pending) 3))
                                    pending)]
                      (recur (rest remaining) pending items))
                    (let [label-lines-raw (cond-> pending
                                            (and prefix (has-letter? prefix) (not (unit-prefix? prefix)))
                                            (conj prefix))
                          label-lines (->> label-lines-raw
                                        (map (fn [l]
                                               (or (:prefix (line->trailing-money l)) l)))
                                        (remove str/blank?)
                                        vec)
                          label (->> label-lines
                                  (map str/trim)
                                  (remove str/blank?)
                                  (str/join " ")
                                  normalize-item-label)]
                      (if (and money (has-letter? label))
                        (recur (rest remaining)
                          []
                          (conj items {:raw_label label
                                       :qty 1M
                                       :unit_price money
                                       :line_total money}))
                        (recur (rest remaining) [] items))))
                  (let [pending (cond-> pending
                                  (has-letter? line) (conj line))
                        pending (if (> (count pending) 3)
                                  (subvec pending (- (count pending) 3))
                                  pending)]
                    (recur (rest remaining) pending items)))))))))))

(def ^:private trailing-money-re
  #"(?i)^(.*?)(\d{1,9}[\.,]\d{2})\s*(?:e|km|bam|€)?\s*$")

(defn- line->trailing-money [line]
  (when (string? line)
    (let [line (-> line
                 str
                 (str/replace #"\|" " ")
                 (str/replace #"\s+" " ")
                 str/trim)]
      (when-let [[_ prefix amount] (re-matches trailing-money-re line)]
        (let [money (parse-money amount)
              prefix (some-> prefix str/trim not-empty)]
          (when money
            {:prefix prefix
             :money money}))))))

(def ^:private item-ignore-prefixes
  (-> supplier-ignore-prefixes
    (conj "ibfm")
    (conj "ibem")
    (conj "tbfm")
    (conj "pdu")))

(defn- markdown->price-line-items [markdown]
  (when (string? markdown)
    (let [lines (str/split-lines markdown)]
      (loop [remaining lines
             pending []
             items []]
        (if-not (seq remaining)
          (vec items)
          (let [line0 (first remaining)
                line (some-> line0 str str/trim not-empty)
                norm (normalize-text line)]
            (cond
              (nil? line)
              (recur (rest remaining) [] items)

              (or (nil? norm)
                (some #(str/starts-with? norm %) item-ignore-prefixes))
              (recur (rest remaining) [] items)

              (and (seq items) (line->discount line))
              (let [discount (line->discount line)
                    last-item (peek items)
                    items (conj (pop items) (apply-discount-to-item last-item discount))]
                (recur (rest remaining) [] items))

              :else
              (if-let [{:keys [prefix money]} (line->trailing-money line)]
                (let [label-lines (cond-> pending
                                    (and prefix (has-letter? prefix) (not (unit-prefix? prefix)))
                                    (conj prefix))
                      label (->> label-lines
                              (map str/trim)
                              (remove str/blank?)
                              (str/join " ")
                              normalize-item-label)]
                  (if (and money (has-letter? label))
                    (recur (rest remaining)
                      []
                      (conj items {:raw_label label
                                   :qty 1M
                                   :unit_price money
                                   :line_total money}))
                    (recur (rest remaining) [] items)))
                (let [pending (cond-> pending
                                (has-letter? line) (conj line))
                      pending (if (> (count pending) 3)
                                (subvec pending (- (count pending) 3))
                                pending)]
                  (recur (rest remaining) pending items))))))))))

(defn- markdown->line-item-candidates [markdown]
  (when (string? markdown)
    (let [pipe-items (markdown->pipe-line-items markdown)]
      (if (seq pipe-items)
        pipe-items
        (let [qty-items (markdown->qty-line-items markdown)]
          (if (seq qty-items)
            qty-items
            (markdown->price-line-items markdown)))))))

(defn- abs-decimal-diff [a b]
  (when (and a b)
    (double (.abs (.subtract (bigdec a) (bigdec b))))))

(defn- best-markdown-item-match [markdown-items item]
  (let [item-total (parse-money (:line_total item))
        item-qty (parse-money (:qty item))
        item-unit (parse-money (:unit_price item))]
    (when (and item-total (seq markdown-items))
      (->> markdown-items
        (map (fn [cand]
               (let [d-total (abs-decimal-diff (:line_total cand) item-total)
                     d-unit (abs-decimal-diff (:unit_price cand) item-unit)
                     d-qty (abs-decimal-diff (:qty cand) item-qty)
                     score (+ (* 10 (or d-total 999.0))
                             (* 2 (or d-unit 1.0))
                             (* 1 (or d-qty 1.0)))]
                 {:cand cand
                  :d-total d-total
                  :score score})))
        ;; Ensure we match the correct row primarily by line total
        (filter #(<= (double (or (:d-total %) 999.0)) 0.05))
        (sort-by :score)
        first
        :cand))))

(defn- reconcile-extraction-with-markdown [extraction markdown]
  (if-not (and (map? extraction) (string? markdown) (sequential? (:items extraction)))
    {:extraction extraction
     :changed? false
     :changes []}
    (let [markdown-items (markdown->line-item-candidates markdown)
          {:keys [items changes]}
          (reduce
            (fn [{:keys [items changes] :as acc} item]
              (let [raw-label (:raw_label item)]
                (cond
                  (label-present-in-markdown? markdown raw-label)
                  (update acc :items conj item)

                  :else
                  (if-let [match (best-markdown-item-match markdown-items item)]
                    (-> acc
                      (update :items conj (merge item (select-keys match [:raw_label :qty :unit_price :line_total])))
                      (update :changes conj {:from raw-label
                                             :to (:raw_label match)
                                             :match :ocr-markdown}))
                    (update acc :items conj item)))))
            {:items [] :changes []}
            (:items extraction))
          changed? (boolean (seq changes))]
      {:extraction (assoc extraction :items items)
       :changed? changed?
       :changes changes})))

(defn- persist-extract-result!
  [db receipt-id extract-result opts]
  (let [markdown (:parsed-markdown extract-result)
        markdown-items (markdown->line-item-candidates markdown)
        markdown-supplier (markdown->supplier-guess markdown)
        markdown-total (markdown->total-amount markdown)
        extraction0 (or (:extraction extract-result) {})
        extraction0 (if (looks-like-json-schema? extraction0) {} extraction0)
        extraction0 (cond
                      (seq markdown-items) (assoc extraction0 :items markdown-items)
                      (sequential? (:items extraction0)) extraction0
                      :else (assoc extraction0 :items []))
        provider-total (parse-money (get-in extraction0 [:totals :total]))
        extraction0 (cond-> extraction0
                      (and markdown-total
                        (or (nil? provider-total)
                          (> (abs-decimal-diff markdown-total provider-total) 0.05)))
                      (assoc :totals {:total markdown-total})

                      (and (nil? (get-in extraction0 [:merchant :name])) markdown-supplier)
                      (assoc :merchant {:name markdown-supplier}))
        {:keys [extraction changed? changes]}
        (reconcile-extraction-with-markdown extraction0 markdown)
        valid-shape? (and (map? extraction) (m/validate ReceiptExtraction extraction))
        guesses (when (map? extraction)
                  (let [g (extraction->guesses extraction opts)]
                    (cond-> g
                      (and (nil? (:supplier_guess g)) markdown-supplier)
                      (assoc :supplier_guess markdown-supplier)

                      (and (nil? (:total_amount_guess g)) markdown-total)
                      (assoc :total_amount_guess markdown-total))))
        status (if (and valid-shape? guesses (not (review-required? guesses)))
                 "extracted"
                 "review_required")
        raw-extract-json (cond-> {:provider "mistral"
                                  :received_at (:received-at extract-result)
                                  :model (:model extract-result)
                                  :response (:raw extract-result)
                                  :extraction extraction
                                  :valid_shape? valid-shape?}
                           changed?
                           (assoc :reconciliation {:changes changes
                                                   :source :parsed_markdown}))]
    (receipts/store-extraction-results!
      db
      receipt-id
      (merge {:raw_extract_json raw-extract-json
              :parsed_markdown markdown}
        (select-keys guesses [:supplier_guess
                              :total_amount_guess
                              :currency_guess
                              :purchased_at_guess
                              :payment_hints])))
    (receipts/update-status! db receipt-id status {:error_message nil :error_details nil})
    {:receipt-id receipt-id :stage :extract :result :ok :status status}))

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
                                                                  :content-type (:content_type receipt)})]
            (persist-extract-result! db receipt-id extract-result opts))
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

(defn- process-receipts-by-ids-batch!
  [db ocr-cfg receipt-ids reset? opts]
  (let [prepared
        (mapv
          (fn [rid]
            (try
              (when reset?
                (receipts/reset-for-ocr! db rid))
              (let [receipt (receipts/get-receipt db rid)]
                (cond
                  (nil? receipt)
                  {:receipt-id rid :stage :extract :result :skipped :reason :not-found}

                  (not (seq (:api-key ocr-cfg)))
                  {:receipt-id rid :stage :extract :result :skipped :reason :missing-api-key}

                  :else
                  (if-let [_claimed (receipts/claim-for-extracting! db rid {:lease-seconds (:lease-seconds opts)})]
                    (try
                      (let [{:keys [bytes]} (read-receipt-bytes! receipt opts)]
                        {:receipt-id rid
                         :receipt receipt
                         :request {:custom-id (str rid)
                                   :bytes bytes
                                   :content-type (:content_type receipt)}})
                      (catch Exception e
                        (receipts/mark-failed! db rid (or (.getMessage e) "Extraction preparation failed") (safe-ex-data e))
                        {:receipt-id rid :stage :extract :result :failed :error (.getMessage e)}))
                    {:receipt-id rid :stage :extract :result :skipped :reason :not-claimed})))
              (catch Exception e
                (log/error e "Failed to prepare receipt for batch OCR" {:receipt-id rid})
                {:receipt-id rid :stage :extract :result :failed :error (.getMessage e)})))
          receipt-ids)
        batch-reqs (->> prepared (keep :request) vec)
        batch-ids (set (map :custom-id batch-reqs))]
    (if (empty? batch-reqs)
      (mapv (fn [m] (dissoc m :receipt :request)) prepared)
      (let [batch-res (try
                        (mistral-ocr/ocr-extract-batch! ocr-cfg batch-reqs)
                        (catch Exception e
                          {:exception e}))]
        (if-let [e (:exception batch-res)]
          (do
            (doseq [cid batch-ids]
              (let [rid (try (java.util.UUID/fromString cid) (catch Exception _ nil))]
                (when rid
                  (receipts/mark-failed! db rid (or (.getMessage e) "Batch extraction failed") (safe-ex-data e)))))
            (mapv
              (fn [{:keys [receipt-id request] :as m}]
                (if (seq (some-> request :custom-id))
                  {:receipt-id receipt-id :stage :extract :result :failed :error (or (.getMessage e) "Batch extraction failed")}
                  (dissoc m :receipt :request)))
              prepared))
          (mapv
            (fn [{:keys [receipt-id request] :as m}]
              (let [cid (some-> request :custom-id)
                    extract-result (when (seq cid) (get-in batch-res [:results cid]))
                    err (when (seq cid) (get-in batch-res [:errors cid]))]
                (cond
                  (nil? cid)
                  (dissoc m :receipt :request)

                  extract-result
                  (persist-extract-result! db receipt-id extract-result opts)

                  :else
                  (do
                    (receipts/mark-failed! db receipt-id "Batch extraction failed" (or err {:type :mistral/batch-unknown-error}))
                    {:receipt-id receipt-id :stage :extract :result :failed :error "Batch extraction failed"}))))
            prepared))))))

(defn process-receipts-by-ids!
  "Process receipts by explicit IDs (for UI-triggered OCR).

  Each receipt is first reset to 'uploaded' status (clearing OCR fields) and then
  processed through the OCR pipeline.

  opts:
  - :lease-seconds (default 900)
  - :storage-base-dir (optional, default upload/stripes)
  - :max-file-size-bytes (default 10MB)
  - :default-currency (default BAM)
  - :reset? (default true) - whether to reset receipt before processing

  Returns a summary map similar to process-pending!."
  ([db app-config receipt-ids]
   (process-receipts-by-ids! db app-config receipt-ids nil))
  ([db app-config receipt-ids {:keys [reset?] :or {reset? true} :as opts}]
   (let [ocr-cfg (mistral-ocr/build-config app-config)
         use-batch? (and reset? (:batch-enabled? ocr-cfg) (> (count receipt-ids) 1))
         opts (merge {:lease-seconds 900
                      :storage-base-dir "upload/stripes"
                      :default-currency "BAM"}
                (dissoc opts :reset?))]
     (if-not (:enabled? ocr-cfg)
       (do
         (log/info "Receipt OCR disabled (by-ids)" {:receipt-ids receipt-ids})
         {:enabled? false
          :processed 0
          :receipt-ids receipt-ids})
       (if-not (:api-key ocr-cfg)
         (do
           (log/warn "Receipt OCR skipped: missing API key" {:receipt-ids receipt-ids})
           {:enabled? true
            :error :missing-api-key
            :processed 0
            :receipt-ids receipt-ids})
         (let [results
               (if use-batch?
                 (do
                   (log/info "Receipt OCR by-ids using Mistral Batch API" {:receipt-ids receipt-ids})
                   (process-receipts-by-ids-batch! db ocr-cfg receipt-ids reset? opts))
                 (mapv
                   (fn [rid]
                     (try
                       (let [;; Optionally reset the receipt first
                             _ (when reset?
                                 (receipts/reset-for-ocr! db rid))
                             receipt (receipts/get-receipt db rid)]
                         (if receipt
                           (process-receipt! db ocr-cfg receipt opts)
                           {:receipt-id rid :result :skipped :reason :not-found}))
                       (catch Exception e
                         (log/error e "Failed to process receipt" {:receipt-id rid})
                         {:receipt-id rid :result :failed :error (.getMessage e)})))
                   receipt-ids))
               summary (->> results
                         (map :result)
                         (frequencies))]
           (log/info "Receipt OCR by-ids complete" {:receipt-ids receipt-ids
                                                    :summary summary
                                                    :batch? use-batch?})
           {:enabled? true
            :processed (count results)
            :summary summary
            :results results
            :receipt-ids receipt-ids}))))))
