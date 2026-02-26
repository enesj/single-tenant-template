(ns app.domain.frontend.expenses.subs.suppliers
  (:require
    [app.domain.frontend.expenses.subs.related-records-factory :as rr-subs]
    [re-frame.core :as rf]))

(def ^:private base-path [:admin :expenses :suppliers])

(rf/reg-sub
  :expenses/suppliers
  (fn [db _]
    (get-in db (conj base-path :items))))

;; Related records modal subs
(rr-subs/register-related-records-subs!
  {:entity-singular "supplier"
   :base-path base-path})
