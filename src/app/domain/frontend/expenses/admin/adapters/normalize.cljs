(ns app.domain.frontend.expenses.admin.adapters.normalize
  "Normalization helpers for expenses entities.
   
   These functions convert raw API responses to normalized entities
   for the template entity store."
  (:require
    [app.admin.frontend.adapters.core :as adapters.core]))

;; =============================================================================
;; Normalization helpers
;; =============================================================================

(defn expense->template-entity
  [expense]
  (adapters.core/normalize-entity
    expense
    {:entity-ns :expenses
     :id-keys [:id]
     ;; Stringify foreign-key identifiers so they can be used as
     ;; stable string IDs in the shared template entity store.
     :stringify-keys [:supplier_id :payer_id :receipt_id :user_id]
     ;; Alias commonly used fields to their kebab-case counterparts so
     ;; vector-config / entity-specs (which are based on models.edn) can
     ;; resolve them by the expected IDs like :supplier-id, :payer-id, etc.
     :alias-keys {:supplier_display_name [:supplier-display-name]
                  :supplier_normalized_key [:supplier-normalized-key]
                  :payer_label [:payer-label]
                  :payer_type [:payer-type]
                  :total_amount [:total-amount]
                  :purchased_at [:purchased-at]
                  :is_posted [:is-posted]
                  :created_at [:created-at]
                  :updated_at [:updated-at]
                  ;; FK id aliases used by list-view generated specs
                  :supplier_id [:supplier-id]
                  :payer_id [:payer-id]
                  :user_id [:user-id]
                  :receipt_id [:receipt-id]}
     :post-transform (fn [m]
                       (let [posted? (get m :is-posted)]
                         (assoc m :status (if (true? posted?) "posted" "draft"))))}))

(defn receipt->template-entity
  [receipt]
  (adapters.core/normalize-entity
    receipt
    {:entity-ns :receipts
     :id-keys [:id]
     :alias-keys {:original_filename [:original-filename]
                  :supplier_guess [:supplier-guess]
                  :created_at [:created-at]}}))

(defn supplier->template-entity
  [supplier]
  (adapters.core/normalize-entity
    supplier
    {:entity-ns :suppliers
     :id-keys [:id]
     :alias-keys {:display_name [:display-name]
                  :normalized_key [:normalized-key]
                  :address [:address]
                  :tax_id [:tax-id]
                  :created_at [:created-at]}}))

(defn payer->template-entity
  [payer]
  (adapters.core/normalize-entity
    payer
    {:entity-ns :payers
     :id-keys [:id]
     :alias-keys {:is_default [:is-default]
                  :created_at [:created-at]}}))

(defn article->template-entity
  [article]
  (adapters.core/normalize-entity
    article
    {:entity-ns :articles
     :id-keys [:id]
     :alias-keys {:canonical_name [:canonical-name]
                  :normalized_key [:normalized-key]
                  :barcode [:barcode]
                  :category [:category]
                  :created_at [:created-at]
                  :updated_at [:updated-at]}}))

(defn article-alias->template-entity
  [alias]
  (adapters.core/normalize-entity
    alias
    {:entity-ns :article-aliases
     :id-keys [:id]
     :stringify-keys [:supplier_id :article_id]
     :alias-keys {:supplier_display_name [:supplier-display-name]
                  :article_canonical_name [:article-canonical-name]
                  :raw_label_normalized [:raw-label-normalized]
                  :confidence [:confidence]
                  :created_at [:created-at]}}))

(defn price-observation->template-entity
  [obs]
  (adapters.core/normalize-entity
    obs
    {:entity-ns :price-observations
     :id-keys [:id]
     :stringify-keys [:supplier_id :article_id :expense_item_id]
     :alias-keys {:article_canonical_name [:article-canonical-name]
                  :supplier_display_name [:supplier-display-name]
                  :observed_at [:observed-at]
                  :unit_price [:unit-price]
                  :line_total [:line-total]
                  :qty [:qty]
                  :currency [:currency]
                  :created_at [:created-at]}}))
