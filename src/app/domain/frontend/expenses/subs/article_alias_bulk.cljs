(ns app.domain.frontend.expenses.subs.article-alias-bulk
  (:require
    [re-frame.core :as rf]))

(def ^:private base-path [:admin :expenses :articles :add-aliases-modal])

(rf/reg-sub
  ::open?
  (fn [db _]
    (get-in db (conj base-path :open?) false)))

(rf/reg-sub
  ::article-id
  (fn [db _]
    (get-in db (conj base-path :article-id))))

(rf/reg-sub
  ::working?
  (fn [db _]
    (get-in db (conj base-path :working?) false)))

(rf/reg-sub
  ::error
  (fn [db _]
    (get-in db (conj base-path :error))))

(rf/reg-sub
  ::result
  (fn [db _]
    (get-in db (conj base-path :result))))
