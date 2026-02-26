(ns app.domain.frontend.expenses.subs.manufacturers
  (:require
    [app.domain.frontend.expenses.subs.related-records-factory :as rr-subs]))

(def ^:private base-path [:admin :expenses :manufacturers])

;; Related records modal subs
(rr-subs/register-related-records-subs!
  {:entity-singular "manufacturer"
   :base-path base-path})
