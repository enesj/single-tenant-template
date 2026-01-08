(ns app.domain.backend.expenses.workers.receipt-ocr.extraction
  "Extraction post-processing + persistence.

  The OCR provider returns a mix of:
  - structured extraction JSON (extraction)
  - markdown text

  We reconcile these into the shape expected by the receipts workflow and persist
  results + derived guesses."
  (:require
    [app.domain.backend.expenses.services.receipts :as receipts]
    [app.domain.backend.expenses.workers.receipt-ocr.common :as common]
    [app.domain.backend.expenses.workers.receipt-ocr.markdown :as markdown]
    [clojure.string :as str]
    [malli.core :as m])
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
            (fn [{:keys [items changes] :as acc} item]
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
        markdown-supplier (markdown/markdown->supplier-guess markdown)
        markdown-total (markdown/markdown->total-amount markdown)
        extraction0 (or (:extraction extract-result) {})
        extraction0 (if (looks-like-json-schema? extraction0) {} extraction0)
        extraction0 (cond
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
                              :purchased_at_guess])))
    (receipts/update-status! db receipt-id status {:error_message nil :error_details nil})
    {:receipt-id receipt-id :stage :extract :result :ok :status status}))
