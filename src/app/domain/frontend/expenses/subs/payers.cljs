(ns app.domain.frontend.expenses.subs.payers
  (:require [re-frame.core :as rf]))

(def ^:private base-path [:admin :expenses :payers])

(rf/reg-sub
  :expenses/payers
  (fn [db _]
    (get-in db (conj base-path :items))))

#_(rf/reg-sub
  :expenses/payers-loading?
  (fn [db _]
    (true? (get-in db (conj base-path :loading?)))))

#_(rf/reg-sub
  :expenses/payers-error
  (fn [db _]
    (get-in db (conj base-path :error))))
