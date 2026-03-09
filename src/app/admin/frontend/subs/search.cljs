(ns app.admin.frontend.subs.search
  (:require
    [re-frame.core :as rf]))

(rf/reg-sub
  :admin/search-query
  (fn [db _]
    (get-in db [:admin :search :query])))

(rf/reg-sub
  :admin/search-loading?
  (fn [db _]
    (get-in db [:admin :search :loading?])))

(rf/reg-sub
  :admin/search-results
  (fn [db _]
    (get-in db [:admin :search :results])))

(rf/reg-sub
  :admin/search-selected
  (fn [db _]
    (get-in db [:admin :search :selected])))

(rf/reg-sub
  :admin/search-related
  (fn [db _]
    (get-in db [:admin :search :related])))

(rf/reg-sub
  :admin/search-related-loading?
  (fn [db _]
    (get-in db [:admin :search :related-loading?])))