(ns app.domain.frontend.expenses.subs.suppliers
  (:require [re-frame.core :as rf]))

(def ^:private base-path [:admin :expenses :suppliers])

(rf/reg-sub
  :expenses/suppliers
  (fn [db _]
    (get-in db (conj base-path :items))))

#_(rf/reg-sub
  :expenses/suppliers-loading?
  (fn [db _]
    (true? (get-in db (conj base-path :loading?)))))

#_(rf/reg-sub
  :expenses/suppliers-error
  (fn [db _]
    (get-in db (conj base-path :error))))
