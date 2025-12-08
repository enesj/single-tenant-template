(ns app.admin.frontend.subs.expenses
  "Subscriptions for expenses domain entities in admin UI."
  (:require [re-frame.core :as rf]))

;; Expenses
(rf/reg-sub
  :admin/expenses-loading?
  (fn [db _]
    (get-in db [:admin :expenses :loading?] false)))

(rf/reg-sub
  :admin/expenses-error
  (fn [db _]
    (get-in db [:admin :expenses :error])))

;; Receipts
(rf/reg-sub
  :admin/receipts-loading?
  (fn [db _]
    (get-in db [:admin :receipts :loading?] false)))

(rf/reg-sub
  :admin/receipts-error
  (fn [db _]
    (get-in db [:admin :receipts :error])))

;; Suppliers
(rf/reg-sub
  :admin/suppliers-loading?
  (fn [db _]
    (get-in db [:admin :suppliers :loading?] false)))

(rf/reg-sub
  :admin/suppliers-error
  (fn [db _]
    (get-in db [:admin :suppliers :error])))

;; Payers
(rf/reg-sub
  :admin/payers-loading?
  (fn [db _]
    (get-in db [:admin :payers :loading?] false)))

(rf/reg-sub
  :admin/payers-error
  (fn [db _]
    (get-in db [:admin :payers :error])))

;; Articles
(rf/reg-sub
  :admin/articles-loading?
  (fn [db _]
    (get-in db [:admin :articles :loading?] false)))

(rf/reg-sub
  :admin/articles-error
  (fn [db _]
    (get-in db [:admin :articles :error])))

;; Article aliases
(rf/reg-sub
  :admin/article-aliases-loading?
  (fn [db _]
    (get-in db [:admin :article-aliases :loading?] false)))

(rf/reg-sub
  :admin/article-aliases-error
  (fn [db _]
    (get-in db [:admin :article-aliases :error])))

;; Price observations
(rf/reg-sub
  :admin/price-observations-loading?
  (fn [db _]
    (get-in db [:admin :price-observations :loading?] false)))

(rf/reg-sub
  :admin/price-observations-error
  (fn [db _]
    (get-in db [:admin :price-observations :error])))
