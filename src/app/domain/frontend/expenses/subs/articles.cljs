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

(def ^:private related-records-modal-path (conj base-path :related-records-modal))

(rf/reg-sub
  :expenses/article-related-records-modal-open?
  (fn [db _]
    (true? (get-in db (conj related-records-modal-path :open?)))))

(rf/reg-sub
  :expenses/article-related-records-modal-article
  (fn [db _]
    (get-in db (conj related-records-modal-path :article))))

(rf/reg-sub
  :expenses/article-related-records-step
  (fn [db _]
    (or (get-in db (conj related-records-modal-path :step)) 1)))

(rf/reg-sub
  :expenses/article-related-records-type
  (fn [db _]
    (get-in db (conj related-records-modal-path :selected-type))))

(rf/reg-sub
  :expenses/article-related-records
  (fn [db _]
    (vec (or (get-in db (conj related-records-modal-path :records)) []))))

(rf/reg-sub
  :expenses/article-related-record
  (fn [db _]
    (get-in db (conj related-records-modal-path :selected-record))))

(rf/reg-sub
  :expenses/article-related-records-loading?
  (fn [db _]
    (true? (get-in db (conj related-records-modal-path :loading?)))))

(rf/reg-sub
  :expenses/article-related-records-error
  (fn [db _]
    (get-in db (conj related-records-modal-path :error))))
