(ns app.domain.expenses.frontend.routes
  "Expense domain routes - merged into admin router"
  (:require
    [app.domain.expenses.frontend.events.expenses :as expenses-events]
    [app.domain.expenses.frontend.events.payers :as payers-events]
    [app.domain.expenses.frontend.events.receipts :as receipts-events]
    [app.domain.expenses.frontend.events.suppliers :as suppliers-events]
    [app.domain.expenses.frontend.events.articles :as articles-events]
    [app.domain.expenses.frontend.events.article-aliases :as aliases-events]
    [app.domain.expenses.frontend.events.price-observations :as price-obs-events]
    [app.domain.expenses.frontend.pages.expense-detail :as expense-detail]
    [app.domain.expenses.frontend.pages.expense-form :as expense-form]
    [app.domain.expenses.frontend.pages.expense-list :as expense-list]
    [app.domain.expenses.frontend.pages.payers :as payers]
    [app.domain.expenses.frontend.pages.receipts :as receipts]
    [app.domain.expenses.frontend.pages.suppliers :as suppliers]
    [app.domain.expenses.frontend.pages.articles :as articles]
    [app.domain.expenses.frontend.pages.article-aliases :as article-aliases]
    [app.domain.expenses.frontend.pages.price-observations :as price-observations]
    [re-frame.core :as rf]))

(defn- parse-int
  [v]
  (when v
    (let [n (js/parseInt v 10)]
      (when-not (js/isNaN n) n))))

(defn- guarded-start
  "Creates a controller start fn that runs events after admin auth is confirmed.

   Accepts:
   - A single event vector
   - A vector of event vectors
   - A function of params returning a vector of event vectors"
  [events-or-fn]
  {:start (fn [params]
            (let [events (cond
                           (fn? events-or-fn) (or (events-or-fn params) [])
                           (and (sequential? events-or-fn)
                             (sequential? (first events-or-fn))) events-or-fn
                           (sequential? events-or-fn) [events-or-fn]
                           (nil? events-or-fn) []
                           :else [events-or-fn])]
              (rf/dispatch [:admin/check-auth-protected events])))})

(defn routes
  "Routes under /admin for expenses domain."
  []
  [;; Expenses list
   ["/expenses"
    {:name :admin-expenses
     :view expense-list/admin-expense-list-page
     :controllers [(guarded-start (fn [{:keys [query]}]
                                    (let [page (parse-int (or (:page query) (get query "page")))
                                          per-page (parse-int (or (:per-page query) (get query "per-page")))]
                                      [[::expenses-events/load-list {:page page :per-page per-page}]])))]}]
   ["/expenses/new"
    {:name :admin-expense-new
     :view expense-form/admin-expense-form-page
     :controllers [(guarded-start nil)]}]
   ["/expenses/:id"
    {:name :admin-expense-detail
     :view expense-detail/admin-expense-detail-page
     :controllers [(guarded-start (fn [params]
                                    (when-let [entry-id (get-in params [:path-params :id])]
                                      [[::expenses-events/load-detail entry-id]])))]}]
   ;; Receipts inbox
   ["/receipts"
    {:name :admin-receipts
     :view receipts/admin-receipts-page
     :controllers [(guarded-start (fn [{:keys [query]}]
                                    (let [page (parse-int (or (:page query) (get query "page")))
                                          per-page (parse-int (or (:per-page query) (get query "per-page")))]
                                      [[::receipts-events/load-list {:page page :per-page per-page}]])))]}]
   ;; Suppliers
   ["/suppliers"
    {:name :admin-suppliers
     :view suppliers/admin-suppliers-page
     :controllers [(guarded-start (fn [{:keys [query]}]
                                    (let [page (parse-int (or (:page query) (get query "page")))
                                          per-page (parse-int (or (:per-page query) (get query "per-page")))]
                                      [[::suppliers-events/load {:page page :per-page per-page}]])))]}]
   ;; Payers
   ["/payers"
    {:name :admin-payers
     :view payers/admin-payers-page
     :controllers [(guarded-start (fn [{:keys [query]}]
                                    (let [page (parse-int (or (:page query) (get query "page")))
                                          per-page (parse-int (or (:per-page query) (get query "per-page")))]
                                      [[::payers-events/load {:page page :per-page per-page}]])))]}]
   ;; Articles
   ["/articles"
    {:name :admin-articles
     :view articles/admin-articles-page
     :controllers [(guarded-start (fn [{:keys [query]}]
                                    (let [page (parse-int (or (:page query) (get query "page")))
                                          per-page (parse-int (or (:per-page query) (get query "per-page")))]
                                      [[::articles-events/load {:page page :per-page per-page}]])))]}]
   ;; Article aliases
   ["/article-aliases"
    {:name :admin-article-aliases
     :view article-aliases/admin-article-aliases-page
     :controllers [(guarded-start (fn [{:keys [query]}]
                                    (let [page (parse-int (or (:page query) (get query "page")))
                                          per-page (parse-int (or (:per-page query) (get query "per-page")))]
                                      [[::aliases-events/load {:page page :per-page per-page}]])))]}]
   ;; Price observations
   ["/price-observations"
    {:name :admin-price-observations
     :view price-observations/admin-price-observations-page
     :controllers [(guarded-start (fn [{:keys [query]}]
                                    (let [page (parse-int (or (:page query) (get query "page")))
                                          per-page (parse-int (or (:per-page query) (get query "per-page")))]
                                      [[::price-obs-events/load {:page page :per-page per-page}]])))]}]])
