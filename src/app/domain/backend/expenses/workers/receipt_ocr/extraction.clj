(ns app.domain.backend.expenses.workers.receipt-ocr.extraction
  "Extraction post-processing + persistence."
  (:require
    [app.domain.backend.expenses.services.article-aliases :as aliases]
    [app.domain.backend.expenses.services.receipts.queries :as receipt-queries]
    [app.domain.backend.expenses.services.receipts.status :as receipt-status]
    [app.domain.backend.expenses.workers.receipt-ocr.common :as common]
    [app.domain.backend.expenses.workers.receipt-ocr.extraction.hybrid-items :as hybrid-items]
    [app.domain.backend.expenses.workers.receipt-ocr.extraction.item-aliases :as item-aliases]
    [app.domain.backend.expenses.workers.receipt-ocr.extraction.items :as extraction-items]
    [app.domain.backend.expenses.workers.receipt-ocr.extraction.merchant :as extraction-merchant]
    [app.domain.backend.expenses.workers.receipt-ocr.extraction.reconcile :as reconcile]
    [app.domain.backend.expenses.workers.receipt-ocr.extraction.review :as review]
    [app.domain.backend.expenses.workers.receipt-ocr.extraction.shape :as shape]
    [app.domain.backend.expenses.workers.receipt-ocr.extraction.supplier-store :as supplier-store]
    [app.domain.backend.expenses.workers.receipt-ocr.markdown.header :as markdown-header]
    [app.domain.backend.expenses.workers.receipt-ocr.markdown.items :as markdown-items]

    [app.domain.backend.expenses.workers.receipt-ocr.markdown.totals :as markdown-totals]
    [clojure.string :as str]
    [malli.core :as m]
    [taoensso.timbre :as log]))

(def ^:private ReceiptExtraction
  shape/ReceiptExtraction)

(defn extraction->guesses
  [extraction opts]
  (shape/extraction->guesses extraction opts))

(defn review-required?
  [guesses]
  (shape/review-required? guesses))

(defn- looks-like-json-schema?
  [m]
  (shape/looks-like-json-schema? m))

(defn- lines-total-mismatch?
  ([items total-amount]
   (shape/lines-total-mismatch? items total-amount))
  ([items total-amount provider-confidence]
   (shape/lines-total-mismatch? items total-amount provider-confidence)))

^{:clj-kondo/ignore [:unused-private-var]
  :clojure-lsp/ignore [:unused-private-var]}
(defn- non-item-reason
  [ctx item]
  (extraction-items/non-item-reason ctx item))

(defn- resolve-user-region
  [db receipt opts]
  (supplier-store/resolve-user-region db receipt opts))

(defn- resolve-supplier-and-alias
  [db supplier-guess extraction opts]
  (supplier-store/resolve-supplier-and-alias db supplier-guess extraction opts))

(defn- resolve-store-and-alias
  [db supplier-id extraction opts]
  (supplier-store/resolve-store-and-alias db supplier-id extraction opts))

(defn- auto-create-aliases!
  [db supplier-id extraction opts]
  (item-aliases/auto-create-aliases! db supplier-id extraction opts))

(defn- auto-approve-extracted-receipt!
  [db receipt-id extraction supplier-id store-id opts]
  (review/auto-approve-extracted-receipt! db receipt-id extraction supplier-id store-id opts))

(defn reconcile-extraction-with-markdown
  [extraction markdown-content]
  (reconcile/reconcile-extraction-with-markdown extraction markdown-content))

(defn- merge-markdown-merchant-header
  [merchant markdown-merchant-header]
  (extraction-merchant/merge-markdown-merchant-header merchant markdown-merchant-header))

(defn- post-process-extraction
  [extraction]
  (let [extraction (extraction-merchant/post-process-merchant extraction)]
    (if-not (and (map? extraction) (sequential? (:items extraction)))
      {:extraction extraction
       :post-processing nil}
      (let [items (:items extraction)
            ctx {:items-count (count items)
                 :grand-total (common/parse-money (get-in extraction [:totals :total]))}
            {:keys [items post-processing]} (extraction-items/clean-extraction-items items ctx)]
        {:extraction (assoc extraction :items items)
         :post-processing post-processing}))))

(defn persist-extract-result!
  "Persist a provider extract result, enriched with markdown-derived guesses.

  Returns {:receipt-id .. :stage :extract :result :ok :status extracted|review_required}."
  [db receipt-id extract-result opts]
  (let [receipt (receipt-queries/get-receipt db receipt-id)
        user-region (resolve-user-region db receipt opts)
        opts (cond-> opts
               user-region (assoc :user-region user-region))
        markdown-content (:parsed-markdown extract-result)
        markdown-items (markdown-items/candidates markdown-content)
        markdown-merchant-header (markdown-header/merchant-header markdown-content)
        markdown-merchant-name (some-> (:merchant_name markdown-merchant-header) str/trim not-empty)
        markdown-supplier (if markdown-merchant-name
                            markdown-merchant-name
                            (markdown-header/supplier-guess markdown-content))
        markdown-total (markdown-totals/total-amount markdown-content)
        markdown-purchased-at (markdown-totals/purchased-at markdown-content)
        extraction0 (or (:extraction extract-result) {})
        extraction0 (if (looks-like-json-schema? extraction0) {} extraction0)
        extraction0 (cond
                      (seq (:items extraction0)) extraction0
                      (seq markdown-items) (assoc extraction0 :items markdown-items)
                      (sequential? (:items extraction0)) extraction0
                      :else (assoc extraction0 :items []))
        structured-merge (hybrid-items/merge-structured-items (:items extraction0) (:raw extract-result))
        structured-merge-meta (:meta structured-merge)
        extraction0 (assoc extraction0 :items (:items structured-merge))
        provider-total0 (common/parse-money (get-in extraction0 [:totals :total]))
        extraction0 (cond-> extraction0
                      (shape/prefer-markdown-total? provider-total0 markdown-total (:items extraction0))
                      (assoc :totals {:total markdown-total})

                      (and (nil? (:purchased_at extraction0)) markdown-purchased-at)
                      (assoc :purchased_at markdown-purchased-at)

                      (and (nil? (get-in extraction0 [:merchant :name])) markdown-supplier)
                      (assoc :merchant {:name markdown-supplier})

                      (map? markdown-merchant-header)
                      (update :merchant merge-markdown-merchant-header markdown-merchant-header))
        final-total (common/parse-money (get-in extraction0 [:totals :total]))
        extraction0 (cond-> extraction0
                      (shape/prefer-markdown-items? (:items extraction0) markdown-items final-total)
                      (assoc :items markdown-items))
        {:keys [extraction changed? changes]}
        (reconcile-extraction-with-markdown extraction0 markdown-content)
        {:keys [extraction post-processing]}
        (post-process-extraction extraction)
        valid-shape? (and (map? extraction) (m/validate ReceiptExtraction extraction))
        guesses (when (map? extraction)
                  (let [g (extraction->guesses extraction opts)]
                    (cond-> g
                      (and (nil? (:supplier_guess g)) markdown-supplier)
                      (assoc :supplier_guess markdown-supplier)

                      (and (nil? (:total_amount_guess g)) markdown-total)
                      (assoc :total_amount_guess markdown-total))))
        supplier-guess (or (:supplier_guess guesses) markdown-supplier)
        {:keys [supplier-id supplier-alias-id source]}
        (try
          (resolve-supplier-and-alias db supplier-guess extraction opts)
          (catch Exception e
            (log/error e "Failed to resolve supplier from supplier_guess" {:receipt-id receipt-id})
            {:supplier-id nil
             :supplier-alias-id nil
             :source :unknown}))
        unknown-supplier-id (try
                              (aliases/find-unknown-supplier-id db)
                              (catch Exception _
                                nil))
        undefined-supplier? (or (nil? supplier-id)
                              (= :unknown source)
                              (and unknown-supplier-id
                                (= unknown-supplier-id supplier-id)))
        store-res
        (if (= :unknown source)
          {:store-id nil
           :store-alias-id nil
           :store-guess nil
           :source :unknown}
          (try
            (resolve-store-and-alias db supplier-id extraction
              (cond-> opts
                (seq markdown-content) (assoc :receipt-markdown markdown-content)))
            (catch Exception e
              (log/warn e "Failed to resolve store from merchant" {:receipt-id receipt-id})
              {:store-id nil
               :store-alias-id nil
               :store-guess nil
               :source :unknown})))
        {:keys [store-id store-alias-id store-guess] :as store-res*}
        store-res
        store-source (:source store-res*)
        status (if (and valid-shape?
                     guesses
                     (not (review-required? guesses))
                     (not undefined-supplier?))
                 "extracted"
                 "review_required")
        provider-confidence (:provider_confidence extraction)
        lines-total-mismatch (and (= status "extracted")
                               (lines-total-mismatch? (:items extraction)
                                 (:total_amount_guess guesses)
                                 provider-confidence))
        db-status (if lines-total-mismatch "review_required" status)
        llm-refine (:llm_refine extract-result)
        item-aliases-snapshot (if (= :unknown source)
                                (when (and (map? extraction) (sequential? (:items extraction)))
                                  (mapv (fn [{:keys [raw_label] :as _item}]
                                          {:raw_label (some-> raw_label str str/trim)
                                           :article_alias_id nil})
                                    (:items extraction)))
                                (try
                                  (auto-create-aliases! db supplier-id extraction opts)
                                  (catch Exception e
                                    (log/warn e "Failed to auto-create aliases from receipt extraction" {:receipt-id receipt-id})
                                    nil)))
        resolution-snapshot {:supplier {:supplier_id supplier-id
                                        :supplier_alias_id supplier-alias-id
                                        :supplier_guess supplier-guess
                                        :source source}
                             :store {:store_id store-id
                                     :store_alias_id store-alias-id
                                     :store_guess store-guess
                                     :source store-source}
                             :items item-aliases-snapshot}
        provider-name (or (some-> (:provider extract-result) str str/trim not-empty)
                        "mistral")
        raw-extract-json (cond-> {:provider provider-name
                                  :received_at (:received-at extract-result)
                                  :model (:model extract-result)
                                  :response (:raw extract-result)
                                  :extraction extraction
                                  :valid_shape? valid-shape?
                                  :resolution_snapshot resolution-snapshot}
                                      (map? provider-confidence)
                                      (assoc :provider_confidence provider-confidence)

                           (map? llm-refine)
                           (assoc :llm_refine (dissoc llm-refine :extraction))

                           changed?
                           (assoc :reconciliation {:changes changes
                                                   :source :parsed_markdown})

                           (map? structured-merge-meta)
                           (assoc :structured_response_merge structured-merge-meta)

                           (map? post-processing)
                           (assoc :post_processing post-processing))]
    (receipt-status/store-extraction-results!
      db
      receipt-id
      (merge {:raw_extract_json raw-extract-json
              :parsed_markdown markdown-content}
        (select-keys guesses [:supplier_guess
                              :total_amount_guess
                              :currency_guess
                              :purchased_at_guess])
        (when supplier-alias-id {:supplier_alias_id supplier-alias-id})
        (when store-guess {:store_guess store-guess})
        (when store-alias-id {:store_alias_id store-alias-id})))
    (receipt-status/update-status! db receipt-id db-status {:error_message nil :error_details nil})

    (let [auto-post? (and (not (:defer-refine? opts))
                       (get opts :auto-post-after-upload? true))
          auto-res (when (and (= status "extracted") auto-post?)
                     (try
                       (auto-approve-extracted-receipt! db receipt-id extraction supplier-id store-id opts)
                       (catch Exception e
                         (log/warn e "Failed during auto-approve flow" {:receipt-id receipt-id})
                         nil)))
          _ (when (and (= status "extracted") (not auto-post?))
              (log/info "Auto-post after upload skipped"
                {:receipt-id receipt-id
                 :reason (if (:defer-refine? opts) :defer-refine :disabled)}))
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
