(ns app.domain.frontend.expenses.routes
  "Expense domain routes - merged into admin router"
  (:require
    [app.domain.frontend.expenses.events.expenses :as expenses-events]
    [app.domain.frontend.expenses.events.payers :as payers-events]
    [app.domain.frontend.expenses.events.receipts :as receipts-events]
    [app.domain.frontend.expenses.events.suppliers :as suppliers-events]
    [app.domain.frontend.expenses.events.articles :as articles-events]
    [app.domain.frontend.expenses.events.article-aliases :as aliases-events]
    [app.domain.frontend.expenses.events.price-observations :as price-obs-events]
    [app.domain.frontend.expenses.pages.expense-detail :as expense-detail]
    [app.domain.frontend.expenses.pages.expense-form :as expense-form]
    [app.domain.frontend.expenses.pages.expense-list :as expense-list]
    [app.domain.frontend.expenses.pages.payers :as payers]
    [app.domain.frontend.expenses.pages.receipts :as receipts]
    [app.domain.frontend.expenses.pages.suppliers :as suppliers]
    [app.domain.frontend.expenses.pages.articles :as articles]
    [app.domain.frontend.expenses.pages.article-aliases :as article-aliases]
    [app.domain.frontend.expenses.pages.price-observations :as price-observations]
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
