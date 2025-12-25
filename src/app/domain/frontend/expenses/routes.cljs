(ns app.domain.frontend.expenses.routes
  "Expense domain routes - merged into admin router"
  (:require
    [app.domain.frontend.expenses.events.expenses :as expenses-events]
    [app.domain.frontend.expenses.events.payers :as payers-events]
    [app.domain.frontend.expenses.events.receipts :as receipts-events]
    [app.domain.frontend.expenses.events.suppliers :as suppliers-events]
    [app.domain.frontend.expenses.events.articles :as articles-events]
    [app.domain.frontend.expenses.events.expense-items :as expense-items-events]
    [app.domain.frontend.expenses.events.article-aliases :as aliases-events]
    [app.domain.frontend.expenses.events.price-observations :as price-obs-events]
    [app.domain.frontend.expenses.pages.admin.expense-detail :as expense-detail]
    [app.domain.frontend.expenses.pages.admin.expense-list :as expense-list]
    [app.domain.frontend.expenses.pages.admin.expense-items :as expense-items]
    [app.domain.frontend.expenses.pages.admin.payers :as payers]
    [app.domain.frontend.expenses.pages.admin.payer-detail :as payer-detail]
    [app.domain.frontend.expenses.pages.admin.receipts :as receipts]
    [app.domain.frontend.expenses.pages.admin.receipt-detail :as receipt-detail]
    [app.domain.frontend.expenses.pages.admin.suppliers :as suppliers]
    [app.domain.frontend.expenses.pages.admin.supplier-detail :as supplier-detail]
    [app.domain.frontend.expenses.pages.admin.articles :as articles]
    [app.domain.frontend.expenses.pages.admin.article-detail :as article-detail]
    [app.domain.frontend.expenses.pages.admin.article-aliases :as article-aliases]
    [app.domain.frontend.expenses.pages.admin.article-alias-detail :as article-alias-detail]
    [app.domain.frontend.expenses.pages.admin.price-observations :as price-observations]
    [app.domain.frontend.expenses.pages.admin.price-observation-detail :as price-observation-detail]
    [re-frame.core :as rf]))

(defn- parse-int
  [v]
  (when v
    (let [n (js/parseInt v 10)]
      (when-not (js/isNaN n) n))))

(defn- normalize-query-params
  [query]
  (reduce-kv
    (fn [acc k v]
      (assoc acc (if (string? k) (keyword k) k) v))
    {}
    (or query {})))

(defn- list-params
  "Normalize query params and parse pagination fields for load-list events."
  [query]
  (let [q0 (normalize-query-params query)
        q (cond-> q0
            (:per_page q0) (-> (assoc :per-page (:per_page q0)) (dissoc :per_page)))
        page (parse-int (:page q))
        per-page (parse-int (:per-page q))
        limit (parse-int (:limit q))
        offset (parse-int (:offset q))]
    (cond-> q
      page (assoc :page page)
      per-page (assoc :per-page per-page)
      limit (assoc :limit limit)
      offset (assoc :offset offset))))

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
                                    (let [params (list-params query)]
                                      [[::expenses-events/load-list params]])))]}]
   ["/expenses/:id"
    {:name :admin-expense-detail
     :view expense-detail/admin-expense-detail-page
     :controllers [(guarded-start (fn [params]
                                    (when-let [entry-id (get-in params [:path-params :id])]
                                      [[::expenses-events/load-detail entry-id]])))]}]
   ;; Expense items
   ["/expense-items"
    {:name :admin-expense-items
     :view expense-items/admin-expense-items-page
     :controllers [(guarded-start (fn [{:keys [query]}]
                                    (let [params (list-params query)]
                                      [[::expense-items-events/load-list params]])))]}]
   ;; Receipts inbox
   ["/receipts"
    {:name :admin-receipts
     :view receipts/admin-receipts-page
     :controllers [(guarded-start (fn [{:keys [query]}]
                                    (let [params (list-params query)]
                                      [[::receipts-events/load-list params]])))]}]
   ["/receipts/:id"
    {:name :admin-receipt-detail
     :view receipt-detail/admin-receipt-detail-page
     :controllers [(guarded-start (fn [params]
                                    (when-let [receipt-id (get-in params [:path-params :id])]
                                      [[::receipts-events/load-detail receipt-id]])))]}]
   ;; Suppliers
   ["/suppliers"
    {:name :admin-suppliers
     :view suppliers/admin-suppliers-page
     :controllers [(guarded-start (fn [{:keys [query]}]
                                    (let [params (list-params query)]
                                      [[::suppliers-events/load-list params]])))]}]
   ["/suppliers/:id"
    {:name :admin-supplier-detail
     :view supplier-detail/admin-supplier-detail-page
     :controllers [(guarded-start (fn [params]
                                    (when-let [supplier-id (get-in params [:path-params :id])]
                                      [[::suppliers-events/load-detail supplier-id]])))]}]
   ;; Payers
   ["/payers"
    {:name :admin-payers
     :view payers/admin-payers-page
     :controllers [(guarded-start (fn [{:keys [query]}]
                                    (let [params (list-params query)]
                                      [[::payers-events/load-list params]])))]}]
   ["/payers/:id"
    {:name :admin-payer-detail
     :view payer-detail/admin-payer-detail-page
     :controllers [(guarded-start (fn [params]
                                    (when-let [payer-id (get-in params [:path-params :id])]
                                      [[::payers-events/load-detail payer-id]])))]}]
   ;; Articles
   ["/articles"
    {:name :admin-articles
     :view articles/admin-articles-page
     :controllers [(guarded-start (fn [{:keys [query]}]
                                    (let [params (list-params query)]
                                      [[::articles-events/load-list params]])))]}]
   ["/articles/:id"
    {:name :admin-article-detail
     :view article-detail/admin-article-detail-page
     :controllers [(guarded-start (fn [params]
                                    (when-let [article-id (get-in params [:path-params :id])]
                                      [[::articles-events/load-detail article-id]])))]}]
   ;; Article aliases
   ["/article-aliases"
    {:name :admin-article-aliases
     :view article-aliases/admin-article-aliases-page
     :controllers [(guarded-start (fn [{:keys [query]}]
                                    (let [params (list-params query)]
                                      [[::aliases-events/load-list params]])))]}]
   ["/article-aliases/:id"
    {:name :admin-article-alias-detail
     :view article-alias-detail/admin-article-alias-detail-page
     :controllers [(guarded-start (fn [params]
                                    (when-let [alias-id (get-in params [:path-params :id])]
                                      [[::aliases-events/load-detail alias-id]])))]}]
   ;; Price observations
   ["/price-observations"
    {:name :admin-price-observations
     :view price-observations/admin-price-observations-page
     :controllers [(guarded-start (fn [{:keys [query]}]
                                    (let [params (list-params query)]
                                      [[::price-obs-events/load-list params]])))]}]
   ["/price-observations/:id"
    {:name :admin-price-observation-detail
     :view price-observation-detail/admin-price-observation-detail-page
     :controllers [(guarded-start (fn [params]
                                    (when-let [obs-id (get-in params [:path-params :id])]
                                      [[::price-obs-events/load-detail obs-id]])))]}]])
