(ns app.domain.frontend.expenses.subs.related-records-factory
  "Factory for generating related records modal subscriptions for any entity."
  (:require [re-frame.core :as rf]))

(defn register-related-records-subs!
  "Registers related records modal subscriptions for an entity.

  Config:
  - :entity-singular - singular string, e.g. \"supplier\"
  - :base-path       - app-db path, e.g. [:admin :expenses :suppliers]

  Registers subs like:
  - :expenses/supplier-related-records-modal-open?
  - :expenses/supplier-related-records-modal-entity
  - :expenses/supplier-related-records-step
  - :expenses/supplier-related-records-type
  - :expenses/supplier-related-records
  - :expenses/supplier-related-record
  - :expenses/supplier-related-records-loading?
  - :expenses/supplier-related-records-error"
  [{:keys [entity-singular base-path]}]
  (let [modal-path (conj base-path :related-records-modal)
        s entity-singular]

    (rf/reg-sub
      (keyword "expenses" (str s "-related-records-modal-open?"))
      (fn [db _]
        (true? (get-in db (conj modal-path :open?)))))

    (rf/reg-sub
      (keyword "expenses" (str s "-related-records-modal-entity"))
      (fn [db _]
        (get-in db (conj modal-path :entity))))

    (rf/reg-sub
      (keyword "expenses" (str s "-related-records-step"))
      (fn [db _]
        (or (get-in db (conj modal-path :step)) 1)))

    (rf/reg-sub
      (keyword "expenses" (str s "-related-records-type"))
      (fn [db _]
        (get-in db (conj modal-path :selected-type))))

    (rf/reg-sub
      (keyword "expenses" (str s "-related-records"))
      (fn [db _]
        (vec (or (get-in db (conj modal-path :records)) []))))

    (rf/reg-sub
      (keyword "expenses" (str s "-related-record"))
      (fn [db _]
        (get-in db (conj modal-path :selected-record))))

    (rf/reg-sub
      (keyword "expenses" (str s "-related-records-loading?"))
      (fn [db _]
        (true? (get-in db (conj modal-path :loading?)))))

    (rf/reg-sub
      (keyword "expenses" (str s "-related-records-error"))
      (fn [db _]
        (get-in db (conj modal-path :error))))))
