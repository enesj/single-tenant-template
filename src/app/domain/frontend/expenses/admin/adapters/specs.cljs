(ns app.domain.frontend.expenses.admin.adapters.specs
  (:require
    [app.template.frontend.shared.utils.entity :as entity-utils]))

;; =============================================================================
;; Entity Specs (fallbacks when UI config not yet loaded)
;; =============================================================================

(def expenses-entity-spec
  {:id :expenses
   :fields [{:id :supplier-display-name :label "Supplier" :type :text}
            {:id :supplier-normalized-key :label "Supplier key" :type :text}
            {:id :payer-label :label "Payer" :type :text}
            {:id :payer-type :label "Payer type" :type :text}
            {:id :total-amount :label "Total" :type :number}
            {:id :currency :label "Currency" :type :text}
            {:id :purchased-at :label "Purchased at" :type :datetime}
            {:id :status :label "Status" :type :text}]})

(def expense-items-entity-spec
  {:id :expense-items
   :fields [{:id :expense-purchased-at :label "Expense purchased at" :type :datetime}
            {:id :article-canonical-name :label "Article" :type :text}
            {:id :raw-label :label "Raw label" :type :text}
            {:id :raw-label-id :label "Raw label ID" :type :text}
            {:id :qty :label "Qty" :type :number}
            {:id :unit-price :label "Unit price" :type :number}
            {:id :line-total :label "Line total" :type :number}
            {:id :created-at :label "Created at" :type :datetime}
            {:id :expense-id :label "Expense ID" :type :text}
            {:id :article-id :label "Article ID" :type :text}]})

(def receipts-entity-spec
  {:id :receipts
   :fields [{:id :original-filename :label "File" :type :text}
            {:id :status :label "Status" :type :text}
            {:id :supplier-guess :label "Supplier guess" :type :text}
            {:id :created-at :label "Created at" :type :datetime}]})

(def suppliers-entity-spec
  {:id :suppliers
   :fields [{:id :display-name :label "Name" :type :text}
            {:id :normalized-key :label "Normalized key" :type :text}
            {:id :address :label "Address" :type :text}
            {:id :created-at :label "Created at" :type :datetime}]})

(def payers-entity-spec
  {:id :payers
   :fields [{:id :label :label "Label" :type :text}
            {:id :payer-type-id :label "Payer type" :type :text}
            {:id :is-default :label "Default?" :type :boolean}]})

(def articles-entity-spec
  {:id :articles
   :fields [{:id :canonical-name :label "Name" :type :text}
            {:id :category :label "Category" :type :text}
            {:id :created-at :label "Created at" :type :datetime}]})

(def article-aliases-entity-spec
  {:id :article-aliases
   :fields [{:id :supplier-display-name :label "Supplier" :type :text}
            {:id :article-canonical-name :label "Article" :type :text}
            {:id :raw-label-normalized :label "Alias" :type :text}
            {:id :confidence :label "Confidence" :type :number}
            {:id :created-at :label "Created at" :type :datetime}]})

(def raw-labels-entity-spec
  {:id :raw-labels
   :fields [{:id :raw-label :label "Raw label" :type :text}
            {:id :normalized-key :label "Normalized key" :type :text}
            {:id :created-at :label "Created at" :type :datetime}
            {:id :updated-at :label "Updated at" :type :datetime}]})

(def price-observations-entity-spec
  {:id :price-observations
   :fields [{:id :article-canonical-name :label "Article" :type :text}
            {:id :supplier-display-name :label "Supplier" :type :text}
            {:id :observed-at :label "Observed at" :type :datetime}
            {:id :unit-price :label "Unit price" :type :number}
            {:id :line-total :label "Line total" :type :number}
            {:id :currency :label "Currency" :type :text}
            {:id :qty :label "Qty" :type :number}]})

;; Register entity spec subscriptions with fallback values
(entity-utils/register-entity-spec-sub!
  {:entity-key :expenses
   :value-fn (fn [spec _] (or spec expenses-entity-spec))})

(entity-utils/register-entity-spec-sub!
  {:entity-key :expense-items
   :value-fn (fn [spec _] (or spec expense-items-entity-spec))})

(entity-utils/register-entity-spec-sub!
  {:entity-key :receipts
   :value-fn (fn [spec _] (or spec receipts-entity-spec))})

(entity-utils/register-entity-spec-sub!
  {:entity-key :suppliers
   :value-fn (fn [spec _] (or spec suppliers-entity-spec))})

(entity-utils/register-entity-spec-sub!
  {:entity-key :payers
   :value-fn (fn [spec _] (or spec payers-entity-spec))})

(entity-utils/register-entity-spec-sub!
  {:entity-key :articles
   :value-fn (fn [spec _] (or spec articles-entity-spec))})

(entity-utils/register-entity-spec-sub!
  {:entity-key :article-aliases
   :value-fn (fn [spec _] (or spec article-aliases-entity-spec))})

(entity-utils/register-entity-spec-sub!
  {:entity-key :raw-labels
   :value-fn (fn [spec _] (or spec raw-labels-entity-spec))})

(entity-utils/register-entity-spec-sub!
  {:entity-key :price-observations
   :value-fn (fn [spec _] (or spec price-observations-entity-spec))})

;; Fallback spec for payer-types entity (admin/owner managed)

