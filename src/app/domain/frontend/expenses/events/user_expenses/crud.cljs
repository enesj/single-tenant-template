(ns app.domain.frontend.expenses.events.user-expenses.crud
  "User expense CRUD events."
  (:require
    [app.domain.frontend.expenses.admin.adapters.sync :as expenses-sync]
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.shared.bridges.crud :as crud-bridges]
    [app.template.frontend.shared.crud.success :as crud-success]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ---------------------------------------------------------------------------
;; Template CRUD bridge overrides
;;
;; The shared template list-view selection/batch delete dispatches
;; :app.template.frontend.events.list.crud/delete-entity for the entity keyword
;; (e.g. :expenses). For expenses, those template CRUD events would normally hit
;; the generic entity API (/api/v1/entities/expenses/:id), which is blocked for
;; security. We override delete to use the user-scoped expenses API instead.
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
         (assoc default-effect :dispatch-n dispatches***)))}}})

(defn- user-ui-context?
  "True only for the user-facing (non-admin) UI.

  This prevents user-scoped expenses endpoints (e.g. /api/v1/expenses/*) from
  being used while browsing the admin UI (which relies on /admin/api/* and an
  admin token)."
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
;; Create expense
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/create-expense
  common-interceptors
  (fn [{:keys [db]} [expense-data]]
    {:db (-> db
           (assoc-in [:user-expenses :form :loading?] true)
           (assoc-in [:user-expenses :form :error] nil))
     :http-xhrio (x/xhrio db
                   {:method :post
                    :uri endpoints/list-endpoint

                    :params expense-data
                    :on-success [:user-expenses/create-expense-success]
                    :on-failure [:user-expenses/create-expense-failure]})}))

(rf/reg-event-fx
  :user-expenses/create-expense-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [expense-id (or (get-in response [:data :id])
                       (get-in response [:expense :id]))]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] false)
             (assoc-in [:user-expenses :form :error] nil))
       :dispatch-n [[:user-expenses/fetch-recent {:limit 25 :offset 0}]
                    [:navigate-to (str "/expenses/" expense-id)]]})))

(rf/reg-event-db
  :user-expenses/create-expense-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to create expense" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Create expense (modal)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/create-expense-modal
  common-interceptors
  (fn [{:keys [db]} [expense-data on-success]]
    {:db (-> db
           (assoc-in [:user-expenses :form :loading?] true)
           (assoc-in [:user-expenses :form :error] nil))
     :http-xhrio (x/xhrio db
                   {:method :post
                    :uri endpoints/list-endpoint

                    :params expense-data
                    :on-success [:user-expenses/create-expense-modal-success on-success]
                    :on-failure [:user-expenses/create-expense-modal-failure]})}))

(rf/reg-event-fx
  :user-expenses/create-expense-modal-success
  common-interceptors
  (fn [{:keys [db]} [on-success response]]
    (let [expense-id (or (get-in response [:data :id])
                       (get-in response [:expense :id]))
          highlight-id (some-> expense-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] false)
             (assoc-in [:user-expenses :form :error] nil)
             (cond-> highlight-id
               (crud-success/track-recently-created :expenses highlight-id)))
       :dispatch-n [[:user-expenses/fetch-recent {:limit 25 :offset 0}]]
       :fx [(when on-success
              [:dispatch-later {:ms 100
                                :dispatch [:user-expenses/call-modal-callback on-success]}])]})))

(rf/reg-event-db
  :user-expenses/create-expense-modal-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to create expense (modal)" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Update expense
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/update-expense
  common-interceptors
  (fn [{:keys [db]} [expense-id expense-data]]
    {:db (-> db
           (assoc-in [:user-expenses :form :loading?] true)
           (assoc-in [:user-expenses :form :error] nil))
     :http-xhrio (x/xhrio db
                   {:method :put
                    :uri (str endpoints/list-endpoint "/" expense-id)

                    :params expense-data
                    :on-success [:user-expenses/update-expense-success expense-id]
                    :on-failure [:user-expenses/update-expense-failure]})}))

(rf/reg-event-fx
  :user-expenses/update-expense-success
  common-interceptors
  (fn [{:keys [db]} [expense-id response]]
    (let [expense (:data response)]
      (cond-> {:db (-> db
                     (assoc-in [:user-expenses :form :loading?] false)
                     (assoc-in [:user-expenses :form :error] nil))
               :dispatch-n [[:user-expenses/fetch-expense expense-id]
                            [:user-expenses/fetch-recent {:limit 25 :offset 0}]]}
        expense (assoc :dispatch [::expenses-sync/upsert-expenses [expense]])))))

(rf/reg-event-db
  :user-expenses/update-expense-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to update expense" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Update expense (modal)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/update-expense-modal
  common-interceptors
  (fn [{:keys [db]} [expense-id expense-data on-success]]
    {:db (-> db
           (assoc-in [:user-expenses :form :loading?] true)
           (assoc-in [:user-expenses :form :error] nil))
     :http-xhrio (x/xhrio db
                   {:method :put
                    :uri (str endpoints/list-endpoint "/" expense-id)

                    :params expense-data
                    :on-success [:user-expenses/update-expense-modal-success expense-id on-success]
                    :on-failure [:user-expenses/update-expense-modal-failure]})}))

(rf/reg-event-fx
  :user-expenses/update-expense-modal-success
  common-interceptors
  (fn [{:keys [db]} [expense-id on-success response]]
    (let [expense (:data response)
          highlight-id (some-> expense-id str)]
      (cond-> {:db (-> db
                     (assoc-in [:user-expenses :form :loading?] false)
                     (assoc-in [:user-expenses :form :error] nil)
                     (cond-> highlight-id
                       (crud-success/track-recently-updated :expenses highlight-id)))
               :dispatch-n [[:user-expenses/fetch-recent {:limit 25 :offset 0}]
                            [:user-expenses/fetch-expense expense-id]]
               :fx [(when on-success
                      [:dispatch-later {:ms 100
                                        :dispatch [:user-expenses/call-modal-callback on-success]}])]}
        expense
        (assoc :dispatch [::expenses-sync/upsert-expenses [expense]])))))

(rf/reg-event-db
  :user-expenses/update-expense-modal-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to update expense (modal)" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

;; Helper event to call modal callback
(rf/reg-event-fx
  :user-expenses/call-modal-callback
  common-interceptors
  (fn [_cofx [callback & args]]
    (when (fn? callback)
      (if (seq args)
        (apply callback args)
        (callback)))
    {}))

;; ---------------------------------------------------------------------------
;; Delete expense
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/delete-expense
  common-interceptors
  (fn [{:keys [db]} [expense-id]]
    {:db (assoc-in db [:user-expenses :form :loading?] true)
     :http-xhrio (x/xhrio db
                   {:method :delete
                    :uri (str endpoints/list-endpoint "/batch")
                    :params {:ids [(str expense-id)]}
                    :on-success [:user-expenses/delete-expense-success]
                    :on-failure [:user-expenses/delete-expense-failure]})}))

(rf/reg-event-fx
  :user-expenses/delete-expense-success
  common-interceptors
  (fn [{:keys [db]} [_response]]
    {:db (assoc-in db [:user-expenses :form :loading?] false)
     :dispatch-n [[:user-expenses/refresh-expenses-list]
                  [:navigate-to "/expenses/list"]]}))

(rf/reg-event-db
  :user-expenses/delete-expense-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to delete expense" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Post expense (mark as posted)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/post-expense
  common-interceptors
  (fn [{:keys [db]} [expense-id]]
    {:db (assoc-in db [:user-expenses :form :loading?] true)
     :http-xhrio (x/xhrio db
                   {:method :put
                    :uri (str endpoints/list-endpoint "/" expense-id)

                    :params {:is_posted true}
                    :on-success [:user-expenses/post-expense-success expense-id]
                    :on-failure [:user-expenses/post-expense-failure]})}))

(rf/reg-event-fx
  :user-expenses/post-expense-success
  common-interceptors
  (fn [{:keys [db]} [expense-id _response]]
    {:db (assoc-in db [:user-expenses :form :loading?] false)
     :dispatch [:user-expenses/fetch-expense expense-id]}))

(rf/reg-event-db
  :user-expenses/post-expense-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to post expense" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Batch delete (true bulk endpoints)
;; ---------------------------------------------------------------------------

(defn- normalize-id-list
  [ids]
  (->> (or ids [])
    (remove nil?)
    (map str)
    distinct
    vec))

(crud-bridges/register-crud-bridge!
  {:entity-key :expenses
   :bridge-id :expenses-user-expenses
   :priority 90
   :operations
   {:batch-delete
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
  {:entity-key :payer-types
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
                          :uri (str endpoints/payer-types-endpoint "/batch")
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
