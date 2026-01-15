(ns app.domain.frontend.expenses.events.user-expenses.power-tools
  "User-facing power-user list fetches for Expenses reference entities.

  These pages are rendered in the app build (not the admin panel) and are
  role-gated in the UI + backend."
  (:require
    [app.domain.frontend.expenses.admin.adapters.sync :as expenses-sync]
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.shared.crud.success :as crud-success]
    [re-frame.core :as rf]))

(defn- begin-entity-load
  [db entity-type]
  (-> db
    (assoc-in (paths/entity-loading? entity-type) true)
    (assoc-in (paths/entity-error entity-type) nil)))

(defn- finish-entity-load
  [db entity-type error]
  (-> db
    (assoc-in (paths/entity-loading? entity-type) false)
    (assoc-in (paths/entity-error entity-type) (when error (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Articles (power-user only)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-articles
  common-interceptors
  (fn [{:keys [db]} [params]]
    (let [request-params (merge {:limit 200 :offset 0} (when (map? params) params))]
      {:db (begin-entity-load db :articles)
       :http-xhrio (x/xhrio db
                     {:method :get
                      :uri endpoints/articles-endpoint
                      :admin-uri endpoints/admin-articles-endpoint
                      :params request-params
                      :on-success [:user-expenses/fetch-articles-success]
                      :on-failure [:user-expenses/fetch-articles-failure]})})))

(rf/reg-event-fx
  :user-expenses/fetch-articles-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [articles (vec (or (:articles response) (:data response) []))]
      {:db (finish-entity-load db :articles nil)
       :dispatch [::expenses-sync/sync-articles articles]})))

(rf/reg-event-db
  :user-expenses/fetch-articles-failure
  common-interceptors
  (fn [db [error]]
    (finish-entity-load db :articles error)))

(rf/reg-event-fx
  :user-expenses/create-article-modal
  common-interceptors
  (fn [{:keys [db]} [{:keys [canonical_name canonical-name]} on-success]]
    (let [name* (or canonical_name canonical-name)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] true)
             (assoc-in [:user-expenses :form :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :post
                      :uri endpoints/articles-endpoint
                      :admin-uri endpoints/admin-articles-endpoint
                      :params {:canonical_name name*}
                      :on-success [:user-expenses/create-article-modal-success on-success]
                      :on-failure [:user-expenses/create-article-modal-failure]})})))

(rf/reg-event-fx
  :user-expenses/create-article-modal-success
  common-interceptors
  (fn [{:keys [db]} [on-success response]]
    (let [article (or (:article response) (:data response) response)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] false)
             (assoc-in [:user-expenses :form :error] nil))
       :dispatch-n [[:user-expenses/fetch-articles]]
       :fx [(when on-success
              [:dispatch-later {:ms 100
                                :dispatch [:user-expenses/call-modal-callback on-success article]}])]})))

(rf/reg-event-db
  :user-expenses/create-article-modal-failure
  common-interceptors
  (fn [db [error]]
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Expense items (power-user only; admin/owner)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-expense-items
  common-interceptors
  (fn [{:keys [db]} [params]]
    (let [request-params (merge {:limit 200 :offset 0} (when (map? params) params))]
      {:db (begin-entity-load db :expense-items)
       :http-xhrio (x/xhrio db
                     {:method :get
                      :uri endpoints/expense-items-endpoint
                      :admin-uri endpoints/admin-expense-items-endpoint
                      :params request-params
                      :on-success [:user-expenses/fetch-expense-items-success]
                      :on-failure [:user-expenses/fetch-expense-items-failure]})})))

(rf/reg-event-fx
  :user-expenses/fetch-expense-items-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [items (vec (or (:data response) (:expense-items response) []))]
      {:db (finish-entity-load db :expense-items nil)
       :dispatch [::expenses-sync/sync-expense-items items]})))

(rf/reg-event-db
  :user-expenses/fetch-expense-items-failure
  common-interceptors
  (fn [db [error]]
    (finish-entity-load db :expense-items error)))


(rf/reg-event-fx
  :user-expenses/update-expense-item-modal
  common-interceptors
  (fn [{:keys [db]} [expense-item-id expense-item-data on-success]]
    {:db (-> db
           (assoc-in [:user-expenses :form :loading?] true)
           (assoc-in [:user-expenses :form :error] nil))
     :http-xhrio (x/xhrio db
                   {:method :put
                    :uri (str endpoints/expense-items-endpoint "/" expense-item-id)
                    :admin-uri (str endpoints/admin-expense-items-endpoint "/" expense-item-id)
                    :params expense-item-data
                    :on-success [:user-expenses/update-expense-item-modal-success expense-item-id on-success]
                    :on-failure [:user-expenses/update-expense-item-modal-failure]})}))

(rf/reg-event-fx
  :user-expenses/update-expense-item-modal-success
  common-interceptors
  (fn [{:keys [db]} [expense-item-id on-success _response]]
    (let [highlight-id (some-> expense-item-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] false)
             (assoc-in [:user-expenses :form :error] nil)
             (cond-> highlight-id
               (crud-success/track-recently-updated :expense-items highlight-id)))
       :dispatch-n [[:user-expenses/fetch-expense-items]]
       :fx [(when on-success
              [:dispatch-later {:ms 100
                                :dispatch [:user-expenses/call-modal-callback on-success]}])]})))

(rf/reg-event-db
  :user-expenses/update-expense-item-modal-failure
  common-interceptors
  (fn [db [error]]
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))


(rf/reg-event-fx
  :user-expenses/delete-expense-item
  common-interceptors
  (fn [{:keys [db]} [expense-item-id]]
    {:db (-> db
           (assoc-in [:user-expenses :form :loading?] true)
           (assoc-in [:user-expenses :form :error] nil))
     :http-xhrio (x/xhrio db
                   {:method :delete
                    :uri (str endpoints/expense-items-endpoint "/" expense-item-id)
                    :admin-uri (str endpoints/admin-expense-items-endpoint "/" expense-item-id)
                    :on-success [:user-expenses/delete-expense-item-success]
                    :on-failure [:user-expenses/delete-expense-item-failure]})}))

(rf/reg-event-fx
  :user-expenses/delete-expense-item-success
  common-interceptors
  (fn [{:keys [db]} [_response]]
    {:db (assoc-in db [:user-expenses :form :loading?] false)
     :dispatch [:user-expenses/fetch-expense-items]}))

(rf/reg-event-db
  :user-expenses/delete-expense-item-failure
  common-interceptors
  (fn [db [error]]
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Article aliases (power-user list + basic edit/delete)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-article-aliases
  common-interceptors
  (fn [{:keys [db]} [params]]
    (let [request-params (merge {:limit 200 :offset 0} (when (map? params) params))]
      {:db (begin-entity-load db :article-aliases)
       :http-xhrio (x/xhrio db
                     {:method :get
                      :uri endpoints/article-aliases-endpoint
                      :admin-uri endpoints/admin-article-aliases-endpoint
                      :params request-params
                      :on-success [:user-expenses/fetch-article-aliases-success]
                      :on-failure [:user-expenses/fetch-article-aliases-failure]})})))

(rf/reg-event-fx
  :user-expenses/fetch-article-aliases-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [aliases (vec (or (:data response) (:article-aliases response) []))]
      {:db (finish-entity-load db :article-aliases nil)
       :dispatch [::expenses-sync/sync-article-aliases aliases]})))

(rf/reg-event-db
  :user-expenses/fetch-article-aliases-failure
  common-interceptors
  (fn [db [error]]
    (finish-entity-load db :article-aliases error)))

(rf/reg-event-fx
  :user-expenses/update-article-alias-modal
  common-interceptors
  (fn [{:keys [db]} [article-alias-id form-data on-success]]
    (let [article-alias-id-str (some-> article-alias-id str)
          {:keys [raw_label_normalized raw-label-normalized confidence]} (or form-data {})
          payload (cond-> {}
                    (some? raw_label_normalized) (assoc :raw_label_normalized raw_label_normalized)
                    (some? raw-label-normalized) (assoc :raw_label_normalized raw-label-normalized)
                    (some? confidence) (assoc :confidence confidence))
          payload* (if (seq payload) payload (or form-data {}))]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] true)
             (assoc-in [:user-expenses :form :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :put
                      :uri (str endpoints/article-aliases-endpoint "/" article-alias-id-str)
                      :admin-uri (str endpoints/admin-article-aliases-endpoint "/" article-alias-id-str)
                      :params payload*
                      :on-success [:user-expenses/update-article-alias-modal-success article-alias-id-str on-success]
                      :on-failure [:user-expenses/update-article-alias-modal-failure]})})))

(rf/reg-event-fx
  :user-expenses/update-article-alias-modal-success
  common-interceptors
  (fn [{:keys [db]} [article-alias-id on-success _response]]
    (let [highlight-id (some-> article-alias-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] false)
             (assoc-in [:user-expenses :form :error] nil)
             (cond-> highlight-id
               (crud-success/track-recently-updated :article-aliases highlight-id)))
       :dispatch-n [[:user-expenses/fetch-article-aliases]]
       :fx [(when on-success
              [:dispatch-later {:ms 100
                                :dispatch [:user-expenses/call-modal-callback on-success]}])]})))

(rf/reg-event-db
  :user-expenses/update-article-alias-modal-failure
  common-interceptors
  (fn [db [error]]
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

(rf/reg-event-fx
  :user-expenses/delete-article-alias
  common-interceptors
  (fn [{:keys [db]} [article-alias-id]]
    (let [article-alias-id-str (some-> article-alias-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] true)
             (assoc-in [:user-expenses :form :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :delete
                      :uri (str endpoints/article-aliases-endpoint "/" article-alias-id-str)
                      :admin-uri (str endpoints/admin-article-aliases-endpoint "/" article-alias-id-str)
                      :on-success [:user-expenses/delete-article-alias-success]
                      :on-failure [:user-expenses/delete-article-alias-failure]})})))

(rf/reg-event-fx
  :user-expenses/delete-article-alias-success
  common-interceptors
  (fn [{:keys [db]} [_response]]
    {:db (assoc-in db [:user-expenses :form :loading?] false)
     :dispatch [:user-expenses/fetch-article-aliases]}))

(rf/reg-event-db
  :user-expenses/delete-article-alias-failure
  common-interceptors
  (fn [db [error]]
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Price observations (read-only list for now; supplier detail also uses the same endpoint)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-price-observations
  common-interceptors
  (fn [{:keys [db]} [params]]
    (let [request-params (merge {:limit 200 :offset 0} (when (map? params) params))]
      {:db (begin-entity-load db :price-observations)
       :http-xhrio (x/xhrio db
                     {:method :get
                      :uri endpoints/price-observations-endpoint
                      :admin-uri endpoints/admin-price-observations-endpoint
                      :params request-params
                      :on-success [:user-expenses/fetch-price-observations-success]
                      :on-failure [:user-expenses/fetch-price-observations-failure]})})))

(rf/reg-event-fx
  :user-expenses/fetch-price-observations-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [observations (vec (or (:data response) (:price-observations response) []))]
      {:db (finish-entity-load db :price-observations nil)
       :dispatch [::expenses-sync/sync-price-observations observations]})))

(rf/reg-event-db
  :user-expenses/fetch-price-observations-failure
  common-interceptors
  (fn [db [error]]
    (finish-entity-load db :price-observations error)))
