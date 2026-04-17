(ns app.domain.frontend.expenses.events.user-expenses.crud.bridges
  "CRUD bridge registrations for user-facing expense entity endpoints."
  (:require
    [app.domain.frontend.expenses.admin.adapters.sync :as expenses-sync]
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]

    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.shared.bridges.crud :as crud-bridges]
    [clojure.string :as str]))

(defn- user-ui-context?
  "True only for the user-facing (non-admin) UI."
  [db]
  (let [route-name (get-in db (paths/current-route-name))
        admin-route? (and route-name (str/starts-with? (name route-name) "admin"))
        pathname (when (exists? js/window)
                   (some-> js/window .-location .-pathname))
        in-admin-path? (and pathname (str/includes? pathname "/admin"))]
    (not (or admin-route? in-admin-path?))))

(defn- lookup-uri
  [base-uri]
  (str base-uri "?limit=500&offset=0"))

(defn- normalize-id-list
  [ids]
  (->> (or ids [])
    (remove nil?)
    (map str)
    distinct
    vec))

;; ---------------------------------------------------------------------------
;; Expenses — batch-update + batch-delete
;; ---------------------------------------------------------------------------

(crud-bridges/register-crud-bridge!
  {:entity-key :expenses
   :bridge-id :expenses-user-expenses
   :priority 90
   :operations
   {:batch-update
    {:request
     (fn [{:keys [db]} entity-type request-params default-effect]
       (assoc default-effect
         :db (assoc-in db (paths/entity-loading? entity-type) true)
         :http-xhrio
         (x/xhrio db
           {:method :put
            :uri (str endpoints/list-endpoint "/batch")
            :params request-params
            :timeout 8000
            :on-success [:app.template.frontend.events.list.batch/batch-update-success entity-type]
            :on-failure [:app.template.frontend.events.list.batch/batch-update-failure entity-type]})))

     :on-success
     (fn [_cofx _entity-type response default-effect]
       (let [updated-records (:data response)
             updated-records (vec (or updated-records []))
             dispatches (or (:dispatch-n default-effect) [])
             dispatches* (->> dispatches
                           (remove (fn [dispatch]
                                     (= :app.template.frontend.events.list.crud/fetch-entities (first dispatch))))
                           vec)
             dispatches** (into [[:user-expenses/fetch-recent {:limit 25 :offset 0}]] dispatches*)
             dispatches*** (cond-> dispatches**
                             (seq updated-records) (conj [::expenses-sync/upsert-expenses updated-records]))]
         (assoc default-effect :dispatch-n dispatches***)))}

    :batch-delete
    {:request
     (fn [{:keys [db]} entity-type ids default-effect]
       (let [ids* (normalize-id-list ids)]
         (assoc default-effect
           :db (assoc-in db (paths/entity-loading? entity-type) true)
           :http-xhrio (x/xhrio db
                         {:method :delete
                          :uri (str endpoints/list-endpoint "/batch")
                          :params {:ids ids*}
                          :timeout 8000
                          :on-success [:app.template.frontend.events.list.crud/batch-delete-success entity-type ids*]
                          :on-failure [:app.template.frontend.events.list.crud/batch-delete-failure entity-type ids*]}))))}}})

;; ---------------------------------------------------------------------------
;; Lookup fetch bridges (user UI context only)
;; ---------------------------------------------------------------------------

(crud-bridges/register-crud-bridge!
  {:entity-key :payers
   :bridge-id :expenses-user-lookups
   :priority 90
   :context-pred user-ui-context?
   :operations
   {:fetch
    {:request
     (fn [{:keys [db]} entity-type default-effect]
       (assoc default-effect
         :db (assoc-in db (paths/entity-loading? entity-type) true)
         :http-xhrio
         (http/api-request
           {:method :get
            :uri (lookup-uri endpoints/payers-endpoint)
            :on-success [:app.template.frontend.events.list.crud/fetch-success entity-type]
            :on-failure [:app.template.frontend.events.list.crud/fetch-failure entity-type]})))}}})

(crud-bridges/register-crud-bridge!
  {:entity-key :suppliers
   :bridge-id :expenses-user-lookups
   :priority 90
   :context-pred user-ui-context?
   :operations
   {:fetch
    {:request
     (fn [{:keys [db]} entity-type default-effect]
       (assoc default-effect
         :db (assoc-in db (paths/entity-loading? entity-type) true)
         :http-xhrio
         (http/api-request
           {:method :get
            :uri (lookup-uri endpoints/suppliers-endpoint)
            :on-success [:app.template.frontend.events.list.crud/fetch-success entity-type]
            :on-failure [:app.template.frontend.events.list.crud/fetch-failure entity-type]})))}}})

(crud-bridges/register-crud-bridge!
  {:entity-key :receipts
   :bridge-id :expenses-user-lookups
   :priority 90
   :context-pred user-ui-context?
   :operations
   {:fetch
    {:request
     (fn [{:keys [db]} entity-type default-effect]
       (assoc default-effect
         :db (assoc-in db (paths/entity-loading? entity-type) true)
         :http-xhrio
         (http/api-request
           {:method :get
            :uri (lookup-uri endpoints/receipts-endpoint)
            :on-success [:app.template.frontend.events.list.crud/fetch-success entity-type]
            :on-failure [:app.template.frontend.events.list.crud/fetch-failure entity-type]})))}}})

(crud-bridges/register-crud-bridge!
  {:entity-key :categories
   :bridge-id :expenses-user-lookups
   :priority 90
   :context-pred user-ui-context?
   :operations
   {:fetch
    {:request
     (fn [{:keys [db]} entity-type default-effect]
       (assoc default-effect
         :db (assoc-in db (paths/entity-loading? entity-type) true)
         :http-xhrio
         (http/api-request
           {:method :get
            :uri (lookup-uri endpoints/categories-endpoint)
            :on-success [:app.template.frontend.events.list.crud/fetch-success entity-type]
            :on-failure [:app.template.frontend.events.list.crud/fetch-failure entity-type]})))}}})

(crud-bridges/register-crud-bridge!
  {:entity-key :expense-categories
   :bridge-id :expenses-user-lookups
   :priority 90
   :context-pred user-ui-context?
   :operations
   {:fetch
    {:request
     (fn [{:keys [db]} entity-type default-effect]
       (assoc default-effect
         :db (assoc-in db (paths/entity-loading? entity-type) true)
         :http-xhrio
         (http/api-request
           {:method :get
            :uri (lookup-uri endpoints/expense-categories-endpoint)
            :on-success [:app.template.frontend.events.list.crud/fetch-success entity-type]
            :on-failure [:app.template.frontend.events.list.crud/fetch-failure entity-type]})))}}})

(crud-bridges/register-crud-bridge!
  {:entity-key :subcategories
   :bridge-id :expenses-user-lookups
   :priority 90
   :context-pred user-ui-context?
   :operations
   {:fetch
    {:request
     (fn [{:keys [db]} entity-type default-effect]
       (assoc default-effect
         :db (assoc-in db (paths/entity-loading? entity-type) true)
         :http-xhrio
         (http/api-request
           {:method :get
            :uri (lookup-uri endpoints/subcategories-endpoint)
            :on-success [:app.template.frontend.events.list.crud/fetch-success entity-type]
            :on-failure [:app.template.frontend.events.list.crud/fetch-failure entity-type]})))}}})

;; ---------------------------------------------------------------------------
;; Batch-delete bridges (user UI context only)
;; ---------------------------------------------------------------------------

(crud-bridges/register-crud-bridge!
  {:entity-key :stores
   :bridge-id :expenses-user-lookups
   :priority 90
   :context-pred user-ui-context?
   :operations
   {:batch-delete
    {:request
     (fn [{:keys [db]} entity-type ids default-effect]
       (let [ids* (normalize-id-list ids)]
         (assoc default-effect
           :db (assoc-in db (paths/entity-loading? entity-type) true)
           :http-xhrio (http/api-request
                         {:method :delete
                          :uri endpoints/stores-batch-endpoint
                          :params {:ids ids*}
                          :on-success [:app.template.frontend.events.list.crud/batch-delete-success entity-type ids*]
                          :on-failure [:app.template.frontend.events.list.crud/batch-delete-failure entity-type ids*]}))))}}})

(crud-bridges/register-crud-bridge!
  {:entity-key :payers
   :bridge-id :expenses-user-lookups
   :priority 90
   :context-pred user-ui-context?
   :operations
   {:batch-delete
    {:request
     (fn [{:keys [db]} entity-type ids default-effect]
       (let [ids* (normalize-id-list ids)]
         (assoc default-effect
           :db (assoc-in db (paths/entity-loading? entity-type) true)
           :http-xhrio (http/api-request
                         {:method :delete
                          :uri (str endpoints/payers-endpoint "/batch")
                          :params {:ids ids*}
                          :on-success [:app.template.frontend.events.list.crud/batch-delete-success entity-type ids*]
                          :on-failure [:app.template.frontend.events.list.crud/batch-delete-failure entity-type ids*]}))))}}})

(crud-bridges/register-crud-bridge!
  {:entity-key :suppliers
   :bridge-id :expenses-user-lookups
   :priority 90
   :context-pred user-ui-context?
   :operations
   {:batch-delete
    {:request
     (fn [{:keys [db]} entity-type ids default-effect]
       (let [ids* (normalize-id-list ids)]
         (assoc default-effect
           :db (assoc-in db (paths/entity-loading? entity-type) true)
           :http-xhrio (http/api-request
                         {:method :delete
                          :uri (str endpoints/suppliers-endpoint "/batch")
                          :params {:ids ids*}
                          :on-success [:app.template.frontend.events.list.crud/batch-delete-success entity-type ids*]
                          :on-failure [:app.template.frontend.events.list.crud/batch-delete-failure entity-type ids*]}))))}}})

(crud-bridges/register-crud-bridge!
  {:entity-key :expense-categories
   :bridge-id :expenses-user-lookups
   :priority 90
   :context-pred user-ui-context?
   :operations
   {:batch-delete
    {:request
     (fn [{:keys [db]} entity-type ids default-effect]
       (let [ids* (normalize-id-list ids)]
         (assoc default-effect
           :db (assoc-in db (paths/entity-loading? entity-type) true)
           :http-xhrio (http/api-request
                         {:method :delete
                          :uri (str endpoints/expense-categories-endpoint "/batch")
                          :params {:ids ids*}
                          :on-success [:app.template.frontend.events.list.crud/batch-delete-success entity-type ids*]
                          :on-failure [:app.template.frontend.events.list.crud/batch-delete-failure entity-type ids*]}))))}}})

(crud-bridges/register-crud-bridge!
  {:entity-key :manufacturers
   :bridge-id :expenses-user-power-tools
   :priority 90
   :context-pred user-ui-context?
   :operations
   {:batch-delete
    {:request
     (fn [{:keys [db]} entity-type ids default-effect]
       (let [ids* (normalize-id-list ids)]
         (assoc default-effect
           :db (assoc-in db (paths/entity-loading? entity-type) true)
           :http-xhrio (http/api-request
                         {:method :delete
                          :uri (str endpoints/manufacturers-endpoint "/batch")
                          :params {:ids ids*}
                          :on-success [:app.template.frontend.events.list.crud/batch-delete-success entity-type ids*]
                          :on-failure [:app.template.frontend.events.list.crud/batch-delete-failure entity-type ids*]}))))}}})

(crud-bridges/register-crud-bridge!
  {:entity-key :articles
   :bridge-id :expenses-user-power-tools
   :priority 90
   :context-pred user-ui-context?
   :operations
   {:batch-delete
    {:request
     (fn [{:keys [db]} entity-type ids default-effect]
       (let [ids* (normalize-id-list ids)]
         (assoc default-effect
           :db (assoc-in db (paths/entity-loading? entity-type) true)
           :http-xhrio (http/api-request
                         {:method :delete
                          :uri (str endpoints/articles-endpoint "/batch")
                          :params {:ids ids*}
                          :on-success [:app.template.frontend.events.list.crud/batch-delete-success entity-type ids*]
                          :on-failure [:app.template.frontend.events.list.crud/batch-delete-failure entity-type ids*]}))))}}})

(crud-bridges/register-crud-bridge!
  {:entity-key :article-aliases
   :bridge-id :expenses-user-power-tools
   :priority 90
   :context-pred user-ui-context?
   :operations
   {:batch-delete
    {:request
     (fn [{:keys [db]} entity-type ids default-effect]
       (let [ids* (normalize-id-list ids)]
         (assoc default-effect
           :db (assoc-in db (paths/entity-loading? entity-type) true)
           :http-xhrio (http/api-request
                         {:method :delete
                          :uri (str endpoints/article-aliases-endpoint "/batch")
                          :params {:ids ids*}
                          :on-success [:app.template.frontend.events.list.crud/batch-delete-success entity-type ids*]
                          :on-failure [:app.template.frontend.events.list.crud/batch-delete-failure entity-type ids*]}))))}}})

(crud-bridges/register-crud-bridge!
  {:entity-key :supplier-aliases
   :bridge-id :expenses-user-power-tools
   :priority 90
   :context-pred user-ui-context?
   :operations
   {:batch-delete
    {:request
     (fn [{:keys [db]} entity-type ids default-effect]
       (let [ids* (normalize-id-list ids)]
         (assoc default-effect
           :db (assoc-in db (paths/entity-loading? entity-type) true)
           :http-xhrio (http/api-request
                         {:method :delete
                          :uri (str endpoints/supplier-aliases-endpoint "/batch")
                          :params {:ids ids*}
                          :on-success [:app.template.frontend.events.list.crud/batch-delete-success entity-type ids*]
                          :on-failure [:app.template.frontend.events.list.crud/batch-delete-failure entity-type ids*]}))))}}})
