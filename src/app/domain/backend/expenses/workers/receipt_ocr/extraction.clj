(ns app.domain.backend.expenses.workers.receipt-ocr.extraction
  "Extraction post-processing + persistence.

  The OCR provider returns a mix of:
  - structured extraction JSON (extraction)
  - markdown text

  We reconcile these into the shape expected by the receipts workflow and persist
  results + derived guesses."
  (:require
    [app.domain.backend.expenses.services.article-aliases :as aliases]
    [app.domain.backend.expenses.services.articles :as articles]
    [app.domain.backend.expenses.services.receipts.approval :as receipt-approval]
    [app.domain.backend.expenses.services.receipts.parsing :as receipt-parsing]
    [app.domain.backend.expenses.services.receipts.queries :as receipt-queries]
    [app.domain.backend.expenses.services.receipts.status :as receipt-status]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [app.domain.backend.expenses.services.user-expense-settings :as user-expense-settings]
    [app.domain.backend.expenses.workers.receipt-ocr.common :as common]
    [app.domain.backend.expenses.workers.receipt-ocr.markdown :as markdown]
    [clojure.string :as str]
    [malli.core :as m]
    [taoensso.timbre :as log])
  (:import
    [java.sql Timestamp]))

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
   [:items [:sequential [:map {:closed false}
                         [:raw_label string?]
                         [:qty {:optional true} [:maybe [:or number? string?]]]
                         [:unit_price {:optional true} [:maybe [:or number? string?]]]
                         [:line_total [:or number? string?]]]]]])

(defn extraction->guesses
  [{:keys [merchant totals currency purchased_at items]} {:keys [default-currency]}]
  (let [supplier (some-> merchant :name str/trim not-empty)
        total (common/parse-money (some-> totals :total))
        currency* (common/normalize-currency currency (or default-currency "BAM"))
        purchased-at (some-> purchased_at common/parse-instant)
        purchased-at-ts (some-> purchased-at Timestamp/from)
        items-count (if (sequential? items) (count items) 0)]
    {:supplier_guess supplier
     :total_amount_guess total
     :currency_guess currency*
     :purchased_at_guess purchased-at-ts
     :items-count items-count}))

(defn review-required?
  [{:keys [supplier_guess total_amount_guess currency_guess items-count]}]
  (or (nil? supplier_guess)
    (nil? total_amount_guess)
    (nil? currency_guess)
    (zero? (long (or items-count 0)))))

(defn- looks-like-json-schema? [m]
  (boolean
    (and (map? m)
      (contains? m :properties)
      (contains? m :type)
      (contains? m :required))))

(defn- abs-decimal-diff [a b]
  (when (and a b)
    (double (.abs (.subtract (bigdec a) (bigdec b))))))

(defn- normalize-line-item
  [item]
  (cond-> item
    (and (map? item)
      (contains? item :line_total)
      (not (contains? item :line-total)))
    (assoc :line-total (:line_total item))))

(defn- lines-total-mismatch?
  [items total-amount]
  (let [items* (mapv normalize-line-item (or items []))
        lines-total (receipt-parsing/lines-total items*)]
    (and (some? total-amount)
      (some? lines-total)
      (> (abs-decimal-diff lines-total total-amount) 0.01))))

(defn- best-markdown-item-match [markdown-items item]
  (let [item-total (common/parse-money (:line_total item))
        item-qty (common/parse-money (:qty item))
        item-unit (common/parse-money (:unit_price item))]
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

(defn- valid-alias-label?
  [raw-label]
  (let [raw-label* (some-> raw-label str str/trim)
        normalized (articles/normalize-alias-label raw-label*)]
    (and (not (str/blank? raw-label*))
      (not (str/blank? normalized)))))

(defn- resolve-supplier-id
  [db supplier-guess]
  (if (str/blank? (some-> supplier-guess str))
    (aliases/get-unknown-supplier-id db)
    (let [{:keys [supplier]} (suppliers/find-or-create-supplier! db (str/trim (str supplier-guess)))]
      (:id supplier))))

(defn- auto-create-aliases!
  [db supplier-guess extraction]
  (when (and (map? extraction) (sequential? (:items extraction)))
    (let [supplier-id (resolve-supplier-id db supplier-guess)]
      (doseq [{:keys [raw_label] :as _item} (:items extraction)]
        (when (valid-alias-label? raw_label)
          (aliases/find-or-create-alias! db supplier-id (str/trim (str raw_label))))))))

(defn- resolve-payer-id
  [db {:keys [payer_id user_id]}]
  (or payer_id
    (when user_id
      (-> (user-expense-settings/get-user-expense-settings db user_id)
        user-expense-settings/effective-settings
        :default-payer-id))))

(defn- resolve-purchased-at
  [{:keys [purchased_at_guess created_at]}]
  (or purchased_at_guess created_at))

(defn- build-review-data
  [supplier-id payer-id receipt extraction {:keys [default-currency]}]
  (let [purchased-at (resolve-purchased-at receipt)
        total-amount (:total_amount_guess receipt)
        currency (or (:currency_guess receipt) default-currency "BAM")
        items (vec (or (:items extraction) []))
        notes (str "Extracted from receipt: "
                (or (:original_filename receipt)
                  (:storage_key receipt)
                  "receipt"))]
    {:supplier_id supplier-id
     :payer_id payer-id
     :purchased_at purchased-at
     :total_amount total-amount
     :currency currency
     :notes notes
     :items items}))

(defn- mark-review-required!
  [db receipt-id message]
  (receipt-status/update-status!
    db
    receipt-id
    "review_required"
    {:error_message message
     :error_details nil}))

(defn- auto-approve-extracted-receipt!
  [db receipt-id extraction opts]
  (when-let [receipt (receipt-queries/get-receipt db receipt-id)]
    (when (and (= "extracted" (:status receipt))
            (nil? (:expense_id receipt)))
      (let [supplier-id (resolve-supplier-id db (:supplier_guess receipt))
            payer-id (resolve-payer-id db receipt)
            review-data (build-review-data supplier-id payer-id receipt extraction opts)
            {:keys [purchased_at total_amount items]} review-data]
        (cond
          (nil? payer-id)
          {:status "review_required"
           :error (mark-review-required! db receipt-id "Payer is required to auto-post receipt.")}

          (nil? purchased_at)
          {:status "review_required"
           :error (mark-review-required! db receipt-id "Purchase date is required to auto-post receipt.")}

          (nil? total_amount)
          {:status "review_required"
           :error (mark-review-required! db receipt-id "Total amount is required to auto-post receipt.")}

          (empty? items)
          {:status "review_required"
           :error (mark-review-required! db receipt-id "Line items are required to auto-post receipt.")}

          :else
          (try
            (if-let [user-id (:user_id receipt)]
              (receipt-approval/approve-and-post-for-user! db user-id receipt-id review-data)
              (receipt-approval/approve-and-post! db receipt-id review-data))
            (log/info "Auto-posted extracted receipt" {:receipt-id receipt-id})
            {:status "posted"}
            (catch Exception e
              (log/warn e "Failed to auto-post extracted receipt" {:receipt-id receipt-id})
              {:status "review_required"
               :error (mark-review-required! db receipt-id (or (.getMessage e) "Auto-post failed"))})))))))

(defn reconcile-extraction-with-markdown
  "If provider extraction items don't match the OCR markdown labels, reconcile
  labels and numeric fields by finding the best markdown match.

  Returns {:extraction .. :changed? .. :changes ..}."
  [extraction markdown]
  (if-not (and (map? extraction) (string? markdown) (sequential? (:items extraction)))
    {:extraction extraction
     :changed? false
     :changes []}
    (let [markdown-items (markdown/markdown->line-item-candidates markdown)
          {:keys [items changes]}
          (reduce
            (fn [acc item]
              (let [raw-label (:raw_label item)]
                (cond
                  (markdown/label-present-in-markdown? markdown raw-label)
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

(defn persist-extract-result!
  "Persist a provider extract result, enriched with markdown-derived guesses.

  Returns {:receipt-id .. :stage :extract :result :ok :status extracted|review_required}."
  [db receipt-id extract-result opts]
  (let [markdown (:parsed-markdown extract-result)
        markdown-items (markdown/markdown->line-item-candidates markdown)
        markdown-merchant-header (markdown/markdown->merchant-header markdown)
        markdown-merchant-name (some-> (:merchant_name markdown-merchant-header) str/trim not-empty)
        markdown-supplier (if markdown-merchant-name
                            markdown-merchant-name
                            (markdown/markdown->supplier-guess markdown))
        markdown-total (markdown/markdown->total-amount markdown)
        extraction0 (or (:extraction extract-result) {})
        extraction0 (if (looks-like-json-schema? extraction0) {} extraction0)
        extraction0 (cond
                      (seq (:items extraction0)) extraction0
                      (seq markdown-items) (assoc extraction0 :items markdown-items)
                      (sequential? (:items extraction0)) extraction0
                      :else (assoc extraction0 :items []))
        provider-total (common/parse-money (get-in extraction0 [:totals :total]))
        extraction0 (cond-> extraction0
                      (and markdown-total
                        (or (nil? provider-total)
                          (> (abs-decimal-diff markdown-total provider-total) 0.05)))
                      (assoc :totals {:total markdown-total})

                      (and (nil? (get-in extraction0 [:merchant :name])) markdown-supplier)
                      (assoc :merchant (cond-> {:name markdown-supplier}
                                         (:store_name markdown-merchant-header)
                                         (assoc :store_name (:store_name markdown-merchant-header))
                                         (:address markdown-merchant-header)
                                         (assoc :address (:address markdown-merchant-header)))))
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
        supplier-guess (or (:supplier_guess guesses) markdown-supplier)
        status (if (and valid-shape? guesses (not (review-required? guesses)))
                 "extracted"
                 "review_required")
        lines-total-mismatch (and (= status "extracted")
                               (lines-total-mismatch? (:items extraction) (:total_amount_guess guesses)))
        llm-refine (:llm_refine extract-result)
        raw-extract-json (cond-> {:provider "mistral"
                                  :received_at (:received-at extract-result)
                                  :model (:model extract-result)
                                  :response (:raw extract-result)
                                  :extraction extraction
                                  :valid_shape? valid-shape?}
                           (map? llm-refine)
                           (assoc :llm_refine (dissoc llm-refine :extraction))
                           changed?
                           (assoc :reconciliation {:changes changes
                                                   :source :parsed_markdown}))]
    (receipt-status/store-extraction-results!
      db
      receipt-id
      (merge {:raw_extract_json raw-extract-json
              :parsed_markdown markdown}
        (select-keys guesses [:supplier_guess
                              :total_amount_guess
                              :currency_guess
                              :purchased_at_guess])))
    (receipt-status/update-status! db receipt-id status {:error_message nil :error_details nil})
    (try
      (auto-create-aliases! db supplier-guess extraction)
      (catch Exception e
        (log/warn e "Failed to auto-create aliases from receipt extraction" {:receipt-id receipt-id})))
     (let [auto-post? (get opts :auto-post-after-upload? true)
          auto-res (when (and (= status "extracted") auto-post?)
                     (try
                       (auto-approve-extracted-receipt! db receipt-id extraction opts)
                       (catch Exception e
                         (log/warn e "Failed during auto-approve flow" {:receipt-id receipt-id})
                         nil)))
          _ (when (and (= status "extracted") (not auto-post?))
              (log/info "Auto-post after upload disabled; leaving receipt for review" {:receipt-id receipt-id}))
          final-status (or (:status auto-res) status)
          review-required? (and (not= "posted" final-status)
                             (or (= status "review_required")
                               lines-total-mismatch))
          effective-status (if (and (= final-status "extracted") lines-total-mismatch)
                             "review_required"
                             final-status)
          refine-pending? (and (:defer-refine? opts) review-required?)
          _ (when refine-pending?
              (receipt-status/store-extraction-results!
                db
                receipt-id
                {:raw_extract_json (assoc raw-extract-json :refine_pending true)}))]
      {:receipt-id receipt-id
       :stage :extract
       :result :ok
       :status final-status
       :effective-status effective-status
       :review-required? review-required?})))
