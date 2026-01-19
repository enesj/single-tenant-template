(ns app.domain.frontend.expenses.admin.adapters.normalize
  "Normalization helpers for expenses entities.

   These functions convert raw API responses to normalized entities
   for the template entity store."
  (:require
    [app.template.frontend.shared.utils.entity :as entity-utils]
    [clojure.string :as str]))

;; =============================================================================
;; Normalization helpers
;; =============================================================================

(defn- ->number
  "Best-effort coercion to number for comparing amounts from API payloads.

  Accepts numbers and strings (supports comma decimals). Returns nil when
  coercion fails."
  [value]
  (cond
    (number? value) value
    (string? value) (let [n (js/parseFloat (str/replace value "," "."))]
                      (when-not (js/isNaN n) n))
    :else nil))

(defn- fmt-amount
  "Render an amount for table display.

  Keeps trailing zeros with 2 decimals when the amount is numeric; otherwise
  falls back to stringification."
  [amount]
  (cond
    (nil? amount) nil
    (number? amount) (.toFixed (js/Number. amount) 2)
    (string? amount) (let [n (->number amount)]
                       (if (some? n)
                         (.toFixed (js/Number. n) 2)
                         amount))
    :else (str amount)))

(defn- amounts-different?
  "Compare amounts with a small tolerance to avoid float noise."
  [a b]
  (let [a* (->number a)
        b* (->number b)]
    (and (some? a*)
      (some? b*)
      (> (js/Math.abs (- a* b*)) 0.009))))

(defn expense->template-entity
  [expense]
  (entity-utils/normalize-entity
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

(defn expense-item->template-entity
  [item]
  (entity-utils/normalize-entity
    item
    {:entity-ns :expense-items
     :id-keys [:id]
     :stringify-keys [:expense_id :article_id]
     :alias-keys {:expense_id [:expense-id]
                  :article_id [:article-id]
                  :raw_label [:raw-label]
                  :raw_label_normalized [:raw-label-normalized]
                  :qty [:qty]
                  :unit_price [:unit-price]
                  :line_total [:line-total]
                  :expense_purchased_at [:expense-purchased-at]
                  :supplier_display_name [:supplier-display-name]
                  :payer_label [:payer-label]
                  :article_canonical_name [:article-canonical-name]
                  :created_at [:created-at]
                  :updated_at [:updated-at]}}))

(defn receipt->template-entity
  [receipt]
  (entity-utils/normalize-entity
    receipt
    {:entity-ns :receipts
     :id-keys [:id]
     :alias-keys {:original_filename [:original-filename]
                  :supplier_guess [:supplier-guess]
                  :created_at [:created-at]}
     :post-transform (fn [m]
                       (let [total (:total-amount-guess m)
                             lines-total (:lines-total-amount-guess m)
                             currency (:currency-guess m)
                             total-str (fmt-amount total)
                             lines-str (fmt-amount lines-total)
                             currency-str (when (and (string? currency) (not (str/blank? currency))) currency)
                             suffix (when currency-str (str " " currency-str))
                             total-display (cond
                                             (nil? total-str) nil
                                             (and (some? lines-str)
                                               (amounts-different? total lines-total))
                                             (str total-str suffix " (lines " lines-str ")")
                                             :else
                                             (str total-str suffix))]
                         (cond-> m
                           (some? total-display) (assoc :total-display total-display))))}))

(defn supplier->template-entity
  [supplier]
  (entity-utils/normalize-entity
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
  (entity-utils/normalize-entity
    payer
    {:entity-ns :payers
     :id-keys [:id]
     :stringify-keys [:payer_type_id]
     :alias-keys {:payer_type_id [:payer-type-id :payers/payer-type-id]
                  :payer_type_label [:payer-type]
                  :is_default [:is-default]
                  :created_at [:created-at]}}))

(defn payer-type->template-entity
  [payer-type]
  (entity-utils/normalize-entity
    payer-type
    {:entity-ns :payer-types
     :id-keys [:id]
     :alias-keys {:is_default [:is-default]
                  :created_at [:created-at]
                  :updated_at [:updated-at]}}))

(defn article->template-entity
  [article]
  (entity-utils/normalize-entity
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
  (entity-utils/normalize-entity
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
  (entity-utils/normalize-entity
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
