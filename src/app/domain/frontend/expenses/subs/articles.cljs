(ns app.domain.frontend.expenses.subs.articles
  (:require [re-frame.core :as rf]))

(def ^:private base-path [:admin :expenses :articles])

(rf/reg-sub
  :expenses/articles
  (fn [db _]
    (get-in db (conj base-path :items))))

(rf/reg-sub
  :expenses/article
  (fn [db [_ article-id]]
    (get-in db (conj base-path :by-id article-id))))

(rf/reg-sub
  :expenses/article-detail-loading?
  (fn [db _]
    (true? (get-in db (conj base-path :detail-loading?)))))

(rf/reg-sub
  :expenses/articles-error
  (fn [db _]
    (get-in db (conj base-path :error))))

(rf/reg-sub
  :expenses/article-detail-modal-open?
  (fn [db _]
    (true? (get-in db (conj base-path :detail-modal :open?)))))

(rf/reg-sub
  :expenses/article-detail-modal-id
  (fn [db _]
    (get-in db (conj base-path :detail-modal :entity-id))))
