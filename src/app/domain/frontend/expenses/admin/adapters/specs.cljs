(ns app.domain.frontend.expenses.admin.adapters.specs
  (:require
    [app.admin.frontend.adapters.core :as adapters.core]))

;; =============================================================================
;; Entity Specs (fallbacks when UI config not yet loaded)
;; =============================================================================

(def expenses-entity-spec
  {:id :expenses
   :fields [{:id :supplier-display-name :label "Supplier"}
            {:id :supplier-normalized-key :label "Supplier key"}
            {:id :payer-label :label "Payer"}
            {:id :payer-type :label "Payer type"}
            {:id :total-amount :label "Total"}
            {:id :currency :label "Currency"}
            {:id :purchased-at :label "Purchased at"}
            {:id :status :label "Status"}]})

(def receipts-entity-spec
  {:id :receipts
   :fields [{:id :original-filename :label "File"}
            {:id :status :label "Status"}
            {:id :supplier-guess :label "Supplier guess"}
            {:id :created-at :label "Created at"}]})

(def suppliers-entity-spec
  {:id :suppliers
   :fields [{:id :display-name :label "Name"}
            {:id :normalized-key :label "Normalized key"}
            {:id :address :label "Address"}
            {:id :tax-id :label "Tax ID"}
            {:id :created-at :label "Created at"}]})

(def payers-entity-spec
  {:id :payers
   :fields [{:id :label :label "Label"}
            {:id :type :label "Type"}
            {:id :is-default :label "Default?"}]})

(def articles-entity-spec
  {:id :articles
   :fields [{:id :canonical-name :label "Name"}
            {:id :barcode :label "Barcode"}
            {:id :category :label "Category"}
            {:id :created-at :label "Created at"}]})

(def article-aliases-entity-spec
  {:id :article-aliases
   :fields [{:id :supplier-display-name :label "Supplier"}
            {:id :article-canonical-name :label "Article"}
            {:id :raw-label-normalized :label "Alias"}
            {:id :confidence :label "Confidence"}
            {:id :created-at :label "Created at"}]})

(def price-observations-entity-spec
  {:id :price-observations
   :fields [{:id :article-canonical-name :label "Article"}
            {:id :supplier-display-name :label "Supplier"}
            {:id :observed-at :label "Observed at"}
            {:id :unit-price :label "Unit price"}
            {:id :line-total :label "Line total"}
            {:id :currency :label "Currency"}]})

;; Register entity spec subscriptions with fallback values
(adapters.core/register-entity-spec-sub!
  {:entity-key :expenses
   :value-fn (fn [spec _] (or spec expenses-entity-spec))})

(adapters.core/register-entity-spec-sub!
  {:entity-key :receipts
   :value-fn (fn [spec _] (or spec receipts-entity-spec))})

(adapters.core/register-entity-spec-sub!
  {:entity-key :suppliers
   :value-fn (fn [spec _] (or spec suppliers-entity-spec))})

(adapters.core/register-entity-spec-sub!
  {:entity-key :payers
   :value-fn (fn [spec _] (or spec payers-entity-spec))})

(adapters.core/register-entity-spec-sub!
  {:entity-key :articles
   :value-fn (fn [spec _] (or spec articles-entity-spec))})

(adapters.core/register-entity-spec-sub!
  {:entity-key :article-aliases
   :value-fn (fn [spec _] (or spec article-aliases-entity-spec))})

(adapters.core/register-entity-spec-sub!
  {:entity-key :price-observations
   :value-fn (fn [spec _] (or spec price-observations-entity-spec))})
