(ns app.domain.frontend.expenses.events.user-expenses.subcategories
  "User-facing subcategories list + CRUD (admin/owner only).

  These events call /api/v1/expenses/subcategories and sync results into the
  shared template entity store so list-view can render them."
  (:require
    [app.domain.frontend.expenses.admin.adapters.sync :as expenses-sync]
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.shared.crud.success :as crud-success]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

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

(defn- parse-pos-int
  [value]
  (cond
    (number? value) (when (pos? value) (long value))
    (string? value) (let [n (js/parseInt value 10)]
                      (when (and (number? n) (not (js/isNaN n)) (pos? n))
                        (long n)))
    :else nil))

(defn- normalize-filter-value
  [value]
  (cond
    (map? value) (or (normalize-filter-value (:value value))
                   (normalize-filter-value (get value "value")))
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
      (let [normalized (normalize-filter-value v)]
        (if (nil? normalized)
          acc
          (assoc acc k normalized))))
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
  :user-expenses/refresh-subcategories-list
  common-interceptors
  (fn [{:keys [db]} _]
    {:dispatch [:user-expenses/fetch-subcategories (current-list-page-params db :subcategories 500)]}))

;; ---------------------------------------------------------------------------
;; Subcategories
;; ---------------------------------------------------------------------------

(rf/reg-event-fx
  :user-expenses/fetch-subcategories
  common-interceptors
  (fn [{:keys [db]} [params]]
    (let [request-params (merge {:limit 500 :offset 0} (when (map? params) params))]
      {:db (begin-entity-load db :subcategories)
       :http-xhrio (x/xhrio db
                     {:method :get
                      :uri endpoints/subcategories-endpoint
                      :params request-params
                      :on-success [:user-expenses/fetch-subcategories-success]
                      :on-failure [:user-expenses/fetch-subcategories-failure]})})))

(rf/reg-event-fx
  :user-expenses/fetch-subcategories-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [subcategories (vec (or (:data response) []))
          total (or (:total response) (count subcategories))]
      {:db (-> (finish-entity-load db :subcategories nil)
             (assoc-in (paths/list-total-items :subcategories) total))
       :dispatch [::expenses-sync/sync-subcategories subcategories]})))

(rf/reg-event-db
  :user-expenses/fetch-subcategories-failure
  common-interceptors
  (fn [db [error]]
    (finish-entity-load db :subcategories error)))

(rf/reg-event-fx
  :user-expenses/create-subcategory-modal
  common-interceptors
  (fn [{:keys [db]} [form-data on-success]]
    {:db (-> db
           (assoc-in [:user-expenses :form :loading?] true)
           (assoc-in [:user-expenses :form :error] nil))
     :http-xhrio (x/xhrio db
                   {:method :post
                    :uri endpoints/subcategories-endpoint
                    :params (or form-data {})
                    :on-success [:user-expenses/create-subcategory-modal-success on-success]
                    :on-failure [:user-expenses/create-subcategory-modal-failure]})}))

(rf/reg-event-fx
  :user-expenses/create-subcategory-modal-success
  common-interceptors
  (fn [{:keys [db]} [on-success response]]
    (let [subcategory (:data response)
          subcategory-id (:id subcategory)
          highlight-id (some-> subcategory-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] false)
             (assoc-in [:user-expenses :form :error] nil)
             (cond-> highlight-id
               (crud-success/track-recently-created :subcategories highlight-id)))
       :dispatch-n [[:user-expenses/refresh-subcategories-list]]
       :fx [(when on-success
              [:dispatch-later {:ms 100
                                :dispatch [:user-expenses/call-modal-callback on-success subcategory]}])]})))

(rf/reg-event-db
  :user-expenses/create-subcategory-modal-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to create subcategory" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

(rf/reg-event-fx
  :user-expenses/update-subcategory-modal
  common-interceptors
  (fn [{:keys [db]} [subcategory-id form-data on-success]]
    (let [subcategory-id-str (some-> subcategory-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] true)
             (assoc-in [:user-expenses :form :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :put
                      :uri (str endpoints/subcategories-endpoint "/" subcategory-id-str)
                      :params (or form-data {})
                      :on-success [:user-expenses/update-subcategory-modal-success subcategory-id-str on-success]
                      :on-failure [:user-expenses/update-subcategory-modal-failure]})})))

(rf/reg-event-fx
  :user-expenses/update-subcategory-modal-success
  common-interceptors
  (fn [{:keys [db]} [subcategory-id on-success _response]]
    (let [highlight-id (some-> subcategory-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] false)
             (assoc-in [:user-expenses :form :error] nil)
             (cond-> (seq highlight-id)
               (crud-success/track-recently-updated :subcategories highlight-id)))
       :dispatch-n [[:user-expenses/refresh-subcategories-list]]
       :fx [(when on-success
              [:dispatch-later {:ms 100
                                :dispatch [:user-expenses/call-modal-callback on-success]}])]})))

(rf/reg-event-db
  :user-expenses/update-subcategory-modal-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to update subcategory" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))

(rf/reg-event-fx
  :user-expenses/delete-subcategory
  common-interceptors
  (fn [{:keys [db]} [subcategory-id]]
    (let [subcategory-id-str (some-> subcategory-id str)]
      {:db (-> db
             (assoc-in [:user-expenses :form :loading?] true)
             (assoc-in [:user-expenses :form :error] nil))
       :http-xhrio (x/xhrio db
                     {:method :delete
                      :uri (str endpoints/subcategories-endpoint "/batch")
                      :params {:ids [subcategory-id-str]}
                      :on-success [:user-expenses/delete-subcategory-success]
                      :on-failure [:user-expenses/delete-subcategory-failure]})})))

(rf/reg-event-fx
  :user-expenses/delete-subcategory-success
  common-interceptors
  (fn [{:keys [db]} [_response]]
    {:db (assoc-in db [:user-expenses :form :loading?] false)
     :dispatch [:user-expenses/refresh-subcategories-list]}))

(rf/reg-event-db
  :user-expenses/delete-subcategory-failure
  common-interceptors
  (fn [db [error]]
    (log/warn "Failed to delete subcategory" {:error error})
    (-> db
      (assoc-in [:user-expenses :form :loading?] false)
      (assoc-in [:user-expenses :form :error] (http/extract-error-message error)))))
