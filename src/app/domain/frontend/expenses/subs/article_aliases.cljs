(ns app.domain.frontend.expenses.subs.article-aliases
  (:require [re-frame.core :as rf]))

(def ^:private base-path [:admin :expenses :article-aliases])

(rf/reg-sub
  :expenses/article-aliases
  (fn [db _]
    (get-in db (conj base-path :items))))

(rf/reg-sub
  :expenses/article-aliases-error
  (fn [db _]
    (get-in db (conj base-path :error))))

(rf/reg-sub
  :expenses/article-alias
  (fn [db [_ alias-id]]
    (get-in db (conj base-path :by-id alias-id))))

(rf/reg-sub
  :expenses/article-alias-detail-loading?
  (fn [db _]
    (true? (get-in db (conj base-path :detail-loading?)))))

(rf/reg-sub
  :expenses/article-alias-detail-modal-open?
  (fn [db _]
    (true? (get-in db (conj base-path :detail-modal :open?)))))

(rf/reg-sub
  :expenses/article-alias-detail-modal-id
  (fn [db _]
    (get-in db (conj base-path :detail-modal :entity-id))))
