(ns app.domain.frontend.expenses.subs.subcategories
  (:require
    [app.domain.frontend.expenses.subs.related-records-factory :as rr-subs]))

(def ^:private base-path [:admin :expenses :subcategories])

;; Related records modal subs
(rr-subs/register-related-records-subs!
  {:entity-singular "subcategory"
   :base-path base-path})
