(ns app.domain.frontend.expenses.components.manual-expense-form.smart-input.constants
  "Plain data constants for the smart expense input.")

(def chip-styles
  {:supplier "bg-blue-100 text-blue-800 border-blue-200"
   :store    "bg-emerald-100 text-emerald-800 border-emerald-200"
   :category "bg-purple-100 text-purple-800 border-purple-200"
   :article  "bg-amber-100 text-amber-800 border-amber-200"})

;; Per-supplier color palette: each entry pairs a stronger shade for the
;; supplier chip with a lighter shade in the same hue for its store chips,
;; so the user can visually link a store to its supplier at a glance.
;;
;; Tailwind only picks up class names that appear as literal strings in
;; source, so every variant is spelled out below — do not interpolate.
(def supplier-color-palette
  [{:supplier "bg-blue-200 text-blue-900 border-blue-400"
    :store    "bg-blue-50 text-blue-800 border-blue-300"}
   {:supplier "bg-emerald-200 text-emerald-900 border-emerald-400"
    :store    "bg-emerald-50 text-emerald-800 border-emerald-300"}
   {:supplier "bg-amber-200 text-amber-900 border-amber-400"
    :store    "bg-amber-50 text-amber-800 border-amber-300"}
   {:supplier "bg-rose-200 text-rose-900 border-rose-400"
    :store    "bg-rose-50 text-rose-800 border-rose-300"}
   {:supplier "bg-violet-200 text-violet-900 border-violet-400"
    :store    "bg-violet-50 text-violet-800 border-violet-300"}
   {:supplier "bg-cyan-200 text-cyan-900 border-cyan-400"
    :store    "bg-cyan-50 text-cyan-800 border-cyan-300"}
   {:supplier "bg-orange-200 text-orange-900 border-orange-400"
    :store    "bg-orange-50 text-orange-800 border-orange-300"}
   {:supplier "bg-teal-200 text-teal-900 border-teal-400"
    :store    "bg-teal-50 text-teal-800 border-teal-300"}])

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
