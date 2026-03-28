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
    [clojure.string :as str]
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

(defn- normalize-filter-value
  [value]
  (cond
    (map? value)
    (when (or (contains? value :value)
            (contains? value "value"))
      (or (normalize-filter-value (:value value))
        (normalize-filter-value (get value "value"))))

    (keyword? value) (name value)
    (string? value) (some-> value str/trim not-empty)
    (vector? value) (let [items (->> value
                                  (map normalize-filter-value)
                                  (remove nil?)
                                  vec)]
                      (when (seq items)
                        items))
    (sequential? value) (let [items (->> value
                                      (map normalize-filter-value)
                                      (remove nil?)
                                      vec)]
                          (when (seq items)
                            items))
    :else value))

(defn- normalize-filter-params
  [filters]
  (reduce-kv
    (fn [acc k v]
      (let [field-name (name k)]
        (cond
          (and (map? v)
            (or (contains? v :from)
              (contains? v :to)))
          (let [from* (normalize-filter-value (:from v))
                to* (normalize-filter-value (:to v))]
            (cond-> acc
              (some? from*) (assoc (keyword (str field-name "-from")) from*)
              (some? to*) (assoc (keyword (str field-name "-to")) to*)))

          (and (map? v)
            (or (contains? v :min)
              (contains? v :max)))
          (let [min* (normalize-filter-value (:min v))
                max* (normalize-filter-value (:max v))]
            (cond-> acc
              (some? min*) (assoc (keyword (str field-name "-min")) min*)
              (some? max*) (assoc (keyword (str field-name "-max")) max*)))

          :else
          (if-let [normalized (normalize-filter-value v)]
            (assoc acc k normalized)
            acc))))
    {}
    (or filters {})))

(defn- current-list-page-params
  [db entity-key default-limit]
  (let [per-page (paths/resolved-list-per-page db entity-key default-limit)
        current-page (paths/resolved-list-current-page db entity-key)
        sort-config (or (get-in db [:ui :lists entity-key :sort]) {})
        order-dir (let [direction (:direction sort-config)]
                    (when (contains? #{:asc :desc "asc" "desc"} direction)
                      (name (keyword direction))))
        order-field (when-let [f (:field sort-config)] (name f))
        filters (normalize-filter-params (get-in db [:ui :lists entity-key :filters]))]
    (cond-> (merge {:limit per-page
                    :offset (* (max 0 (dec current-page)) per-page)}
              filters)
      (some? order-dir) (assoc :order-dir order-dir)
      (some? order-field) (assoc :order-by order-field))))

(rf/reg-event-fx
  :user-expenses/refresh-articles-list
  common-interceptors
  (fn [{:keys [db]} [opts]]
    {:dispatch [:user-expenses/fetch-articles (merge (current-list-page-params db :articles 200)
                                                (when (map? opts) opts))]}))

(rf/reg-event-fx
  :user-expenses/refresh-expense-items-list
  common-interceptors
  (fn [{:keys [db]} [opts]]
    {:dispatch [:user-expenses/fetch-expense-items (merge (current-list-page-params db :expense-items 200)
                                                     (when (map? opts) opts))]}))

(rf/reg-event-fx
  :user-expenses/refresh-article-aliases-list
  common-interceptors
  (fn [{:keys [db]} [opts]]
    {:dispatch [:user-expenses/fetch-article-aliases (merge (current-list-page-params db :article-aliases 200)
                                                       (when (map? opts) opts))]}))

(rf/reg-event-fx
  :user-expenses/refresh-supplier-aliases-list
  common-interceptors
  (fn [{:keys [db]} [opts]]
    {:dispatch [:user-expenses/fetch-supplier-aliases (merge (current-list-page-params db :supplier-aliases 200)
                                                        (when (map? opts) opts))]}))

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

                      :params request-params
                      :on-success [:user-expenses/fetch-articles-success]
                      :on-failure [:user-expenses/fetch-articles-failure]})})))

(rf/reg-event-fx
  :user-expenses/fetch-articles-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [articles (vec (or (:data response) []))
          total (or (:total response) (count articles))
          date-highlights (:date-highlights response)]
      {:db (cond-> (-> (finish-entity-load db :articles nil)
                     (assoc-in (paths/list-total-items :articles) total))
             (map? date-highlights)
             (assoc-in (conj (paths/list-ui-state :articles) :date-highlights) date-highlights))
       :dispatch [::expenses-sync/sync-articles articles]})))

(rf/reg-event-db
  :user-expenses/fetch-articles-failure
  common-interceptors
  (fn [db [error]]
    (finish-entity-load db :articles error)))

(rf/reg-event-fx
  :user-expenses/create-article-modal
  common-interceptors
  (fn [{:keys [db]} [form-data on-success]]
    {:db (-> db
           (assoc-in [:user-expenses :form :loading?] true)
           (assoc-in [:user-expenses :form :error] nil))
     :http-xhrio (x/xhrio db
                   {:method :post
                    :uri endpoints/articles-endpoint
                    :params (or form-data {})
                    :on-success [:user-expenses/create-article-modal-success on-success]
                    :on-failure [:user-expenses/create-article-modal-failure]})}))

(rf/reg-event-fx
  :user-expenses/create-article-modal-success
  common-interceptors
  (fn [{:keys [db]} [on-success response]]
    (let [article (:data response)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] false)
             (assoc-in [:user-expenses :form :error] nil))
       :dispatch-n [[:user-expenses/refresh-articles-list]]
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

(rf/reg-event-fx
  :user-expenses/update-article-modal
  common-interceptors
  (fn [{:keys [db]} [article-id form-data on-success]]
    (let [article-id-str (some-> article-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] true)
             (assoc-in [:user-expenses :form :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :put
                      :uri (str endpoints/articles-endpoint "/" article-id-str)

                      :params (or form-data {})
                      :on-success [:user-expenses/update-article-modal-success article-id-str on-success]
                      :on-failure [:user-expenses/update-article-modal-failure]})})))

(rf/reg-event-fx
  :user-expenses/update-article-modal-success
  common-interceptors
  (fn [{:keys [db]} [article-id on-success _response]]
    {:db (-> db
           (assoc-in [:user-expenses :form :loading?] false)
           (assoc-in [:user-expenses :form :error] nil)
           (cond-> (seq article-id)
             (crud-success/track-recently-updated :articles article-id)))
     :dispatch-n [[:user-expenses/refresh-articles-list]]
     :fx [(when on-success
            [:dispatch-later {:ms 100
                              :dispatch [:user-expenses/call-modal-callback on-success]}])]}))

(rf/reg-event-db
  :user-expenses/update-article-modal-failure
  common-interceptors
  (fn [db [error]]
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

(rf/reg-event-fx
  :user-expenses/delete-article
  common-interceptors
  (fn [{:keys [db]} [article-id]]
    (let [article-id-str (some-> article-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] true)
             (assoc-in [:user-expenses :form :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :delete
                      :uri (str endpoints/articles-endpoint "/batch")
                      :params {:ids [article-id-str]}
                      :on-success [:user-expenses/delete-article-success]
                      :on-failure [:user-expenses/delete-article-failure]})})))

(rf/reg-event-fx
  :user-expenses/delete-article-success
  common-interceptors
  (fn [{:keys [db]} [_response]]
    {:db (assoc-in db [:user-expenses :form :loading?] false)
     :dispatch [:user-expenses/refresh-articles-list]}))

(rf/reg-event-db
  :user-expenses/delete-article-failure
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

                      :params request-params
                      :on-success [:user-expenses/fetch-expense-items-success]
                      :on-failure [:user-expenses/fetch-expense-items-failure]})})))

(rf/reg-event-fx
  :user-expenses/fetch-expense-items-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [items (vec (or (:data response) []))
          total (or (:total response) (count items))
          date-highlights (:date-highlights response)]
      {:db (cond-> (-> (finish-entity-load db :expense-items nil)
                     (assoc-in (paths/list-total-items :expense-items) total))
             (map? date-highlights)
             (assoc-in (conj (paths/list-ui-state :expense-items) :date-highlights) date-highlights))
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
       :dispatch-n [[:user-expenses/refresh-expense-items-list]]
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
                    :uri (str endpoints/expense-items-endpoint "/batch")
                    :params {:ids [(some-> expense-item-id str)]}
                    :on-success [:user-expenses/delete-expense-item-success]
                    :on-failure [:user-expenses/delete-expense-item-failure]})}))

(rf/reg-event-fx
  :user-expenses/delete-expense-item-success
  common-interceptors
  (fn [{:keys [db]} [_response]]
    {:db (assoc-in db [:user-expenses :form :loading?] false)
     :dispatch [:user-expenses/refresh-expense-items-list]}))

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

                      :params request-params
                      :on-success [:user-expenses/fetch-article-aliases-success]
                      :on-failure [:user-expenses/fetch-article-aliases-failure]})})))

(rf/reg-event-fx
  :user-expenses/fetch-article-aliases-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [aliases (vec (or (:data response) []))
          total (or (:total response) (count aliases))
          date-highlights (:date-highlights response)]
      {:db (cond-> (-> (finish-entity-load db :article-aliases nil)
                     (assoc-in (paths/list-total-items :article-aliases) total))
             (map? date-highlights)
             (assoc-in (conj (paths/list-ui-state :article-aliases) :date-highlights) date-highlights))
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
          {:keys [raw-label raw-label-normalized article-id]} (or form-data {})
          payload (cond-> {}
                    (some? raw-label) (assoc :raw-label raw-label)
                    (some? raw-label-normalized) (assoc :raw-label-normalized raw-label-normalized)
                    (some? article-id) (assoc :article-id article-id))]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] true)
             (assoc-in [:user-expenses :form :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :put
                      :uri (str endpoints/article-aliases-endpoint "/" article-alias-id-str)

                      :params payload
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
       :dispatch-n [[:user-expenses/refresh-article-aliases-list]]
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
                      :uri (str endpoints/article-aliases-endpoint "/batch")
                      :params {:ids [article-alias-id-str]}
                      :on-success [:user-expenses/delete-article-alias-success]
                      :on-failure [:user-expenses/delete-article-alias-failure]})})))

(rf/reg-event-fx
  :user-expenses/delete-article-alias-success
  common-interceptors
  (fn [{:keys [db]} [_response]]
    {:db (assoc-in db [:user-expenses :form :loading?] false)
     :dispatch [:user-expenses/refresh-article-aliases-list]}))

(rf/reg-event-db
  :user-expenses/delete-article-alias-failure
  common-interceptors
  (fn [db [error]]
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

;; ---------------------------------------------------------------------------
;; Supplier aliases (power-user list + basic edit/delete)
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-supplier-aliases
  common-interceptors
  (fn [{:keys [db]} [params]]
    (let [request-params (merge {:limit 200 :offset 0} (when (map? params) params))]
      {:db (begin-entity-load db :supplier-aliases)
       :http-xhrio (x/xhrio db
                     {:method :get
                      :uri endpoints/supplier-aliases-endpoint

                      :params request-params
                      :on-success [:user-expenses/fetch-supplier-aliases-success]
                      :on-failure [:user-expenses/fetch-supplier-aliases-failure]})})))

(rf/reg-event-fx
  :user-expenses/fetch-supplier-aliases-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [aliases (vec (or (:data response) []))
          total (or (:total response) (count aliases))
          date-highlights (:date-highlights response)]
      {:db (cond-> (-> (finish-entity-load db :supplier-aliases nil)
                     (assoc-in (paths/list-total-items :supplier-aliases) total))
             (map? date-highlights)
             (assoc-in (conj (paths/list-ui-state :supplier-aliases) :date-highlights) date-highlights))
       :dispatch [::expenses-sync/sync-supplier-aliases aliases]})))

(rf/reg-event-db
  :user-expenses/fetch-supplier-aliases-failure
  common-interceptors
  (fn [db [error]]
    (finish-entity-load db :supplier-aliases error)))

(rf/reg-event-fx
  :user-expenses/update-supplier-alias-modal
  common-interceptors
  (fn [{:keys [db]} [supplier-alias-id form-data on-success]]
    (let [supplier-alias-id-str (some-> supplier-alias-id str)
          {:keys [raw-label raw-label-normalized]} (or form-data {})
          payload (cond-> {}
                    (some? raw-label) (assoc :raw-label raw-label)
                    (some? raw-label-normalized) (assoc :raw-label-normalized raw-label-normalized))]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] true)
             (assoc-in [:user-expenses :form :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :put
                      :uri (str endpoints/supplier-aliases-endpoint "/" supplier-alias-id-str)

                      :params payload
                      :on-success [:user-expenses/update-supplier-alias-modal-success supplier-alias-id-str on-success]
                      :on-failure [:user-expenses/update-supplier-alias-modal-failure]})})))

(rf/reg-event-fx
  :user-expenses/update-supplier-alias-modal-success
  common-interceptors
  (fn [{:keys [db]} [supplier-alias-id on-success _response]]
    (let [highlight-id (some-> supplier-alias-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] false)
             (assoc-in [:user-expenses :form :error] nil)
             (cond-> highlight-id
               (crud-success/track-recently-updated :supplier-aliases highlight-id)))
       :dispatch-n [[:user-expenses/refresh-supplier-aliases-list]]
       :fx [(when on-success
              [:dispatch-later {:ms 100
                                :dispatch [:user-expenses/call-modal-callback on-success]}])]})))

(rf/reg-event-db
  :user-expenses/update-supplier-alias-modal-failure
  common-interceptors
  (fn [db [error]]
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

(rf/reg-event-fx
  :user-expenses/delete-supplier-alias
  common-interceptors
  (fn [{:keys [db]} [supplier-alias-id]]
    (let [supplier-alias-id-str (some-> supplier-alias-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] true)
             (assoc-in [:user-expenses :form :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :delete
                      :uri (str endpoints/supplier-aliases-endpoint "/batch")
                      :params {:ids [supplier-alias-id-str]}
                      :on-success [:user-expenses/delete-supplier-alias-success]
                      :on-failure [:user-expenses/delete-supplier-alias-failure]})})))

(rf/reg-event-fx
  :user-expenses/delete-supplier-alias-success
  common-interceptors
  (fn [{:keys [db]} [_response]]
    {:db (assoc-in db [:user-expenses :form :loading?] false)
     :dispatch [:user-expenses/refresh-supplier-aliases-list]}))

(rf/reg-event-db
  :user-expenses/delete-supplier-alias-failure
  common-interceptors
  (fn [db [error]]
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))




