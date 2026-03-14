(ns app.domain.frontend.expenses.components.manual-expense-form.smart-input.constants
  "Plain data constants for the smart expense input.")

(def chip-styles
  {:supplier "bg-blue-100 text-blue-800 border-blue-200"
   :store    "bg-emerald-100 text-emerald-800 border-emerald-200"
   :category "bg-purple-100 text-purple-800 border-purple-200"
   :article  "bg-amber-100 text-amber-800 border-amber-200"})

(def type-button-styles
  {:supplier "bg-blue-50 hover:bg-blue-100 text-blue-700 border-blue-200"
   :store    "bg-emerald-50 hover:bg-emerald-100 text-emerald-700 border-emerald-200"
   :category "bg-purple-50 hover:bg-purple-100 text-purple-700 border-purple-200"
   :article  "bg-amber-50 hover:bg-amber-100 text-amber-700 border-amber-200"})

(def create-events
  {:supplier :user-expenses/create-supplier-modal
   :store    :user-expenses/create-store-modal
   :category :user-expenses/create-expense-category-modal
   :article  :user-expenses/create-article-modal})

(def create-field-names
  {:supplier :display_name
   :store    :display_name
   :category :name
   :article  :canonical_name})

(def currency-options
  [{:label "BAM" :value "BAM"}
   {:label "EUR" :value "EUR"}
   {:label "USD" :value "USD"}])
