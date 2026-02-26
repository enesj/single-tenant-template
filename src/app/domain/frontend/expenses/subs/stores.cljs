(ns app.domain.frontend.expenses.subs.stores
  (:require
    [app.domain.frontend.expenses.subs.related-records-factory :as rr-subs]))

(def ^:private base-path [:admin :expenses :stores])

;; Related records modal subs
(rr-subs/register-related-records-subs!
  {:entity-singular "store"
   :base-path base-path})
