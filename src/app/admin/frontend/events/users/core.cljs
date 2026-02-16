(ns app.admin.frontend.events.users.core
  "Core CRUD operations for user management (single-tenant)"
  (:require
    [app.admin.frontend.events.users.utils :as utils]
    [app.admin.frontend.utils.http :as admin-http]
    [app.template.frontend.utils.state :as state-utils]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.state.normalize :as normalize]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ============================================================================
;; Load Users Events
;; ============================================================================

(rf/reg-event-fx
  :admin/load-users
  (fn [{:keys [db]} [_ params]]
    (let [entity-key :users
          params* (or params {})
          pagination* (:pagination params*)
          parse-pos-int (fn [v]
                          (cond
                            (number? v) (when (pos? v) (long v))
                            (string? v) (let [n (js/parseInt v 10)]
                                          (when (and (number? n) (not (js/isNaN n)) (pos? n))
                                            (long n)))
                            :else nil))
          parse-non-neg-int (fn [v]
                              (cond
                                (number? v) (when (>= v 0) (long v))
                                (string? v) (let [n (js/parseInt v 10)]
                                              (when (and (number? n) (not (js/isNaN n)) (>= n 0))
                                                (long n)))
                                :else nil))
          template-per-page (or (parse-pos-int (get-in db (paths/list-per-page entity-key)))
                              (parse-pos-int (get-in db (conj (paths/list-ui-state entity-key) :per-page)))
                              (parse-pos-int (get-in db (conj (paths/list-ui-state entity-key) :pagination :per-page)))
                              25)
          template-page (or (parse-pos-int (get-in db (paths/list-current-page entity-key)))
                          (parse-pos-int (get-in db (conj (paths/list-ui-state entity-key) :current-page)))
                          (parse-pos-int (get-in db (conj (paths/list-ui-state entity-key) :pagination :current-page)))
                          1)
          limit (or (parse-pos-int (:limit params*))
                  (parse-pos-int (:limit pagination*))
                  (parse-pos-int (:per-page params*))
                  (parse-pos-int (:per-page pagination*))
                  template-per-page)
          page (or (parse-pos-int (:page params*))
                 (parse-pos-int (:current-page params*))
                 (parse-pos-int (:page pagination*))
                 template-page)
          offset (or (parse-non-neg-int (:offset params*))
                   (parse-non-neg-int (:offset pagination*))
                   (* (dec page) limit))
          ui-filters (get-in db (paths/list-filters entity-key) {})
          param-filters (if (map? (:filters params*))
                          (:filters params*)
                          {})
          request-params (-> (merge ui-filters
                               param-filters
                               (dissoc params* :filters :pagination :page :per-page :current-page))
                           (assoc :limit limit
                             :offset offset))]
      (utils/log-user-operation "Loading users with params" request-params)
      {:db (state-utils/start-api-request db {:loading-key :admin/users-loading?
                                              :error-key :admin/users-error
                                              :entity-type :users})
       :http-xhrio (admin-http/admin-get
                     {:uri "/admin/api/users"
                      :params request-params
                      :on-success [:admin/load-users-success]
                      :on-failure [:admin/load-users-failure]})})))

(rf/reg-event-fx
  :admin/load-users-success
  (fn [{:keys [db]} [_ response]]
    (let [response* (state-utils/safe-js->clj response)
          total-items (or (:total response*)
                        (count (or (:users response*) [])))
          effect (state-utils/handle-entity-api-success
                   db
                   :users
                   response
                   {:loading-key :admin/users-loading?
                    :admin-key :admin/users
                    :sync-event [:app.admin.frontend.adapters.users/sync-users-to-template]})]
      (update effect :db
        #(assoc-in % (paths/list-total-items :users) total-items)))))

(rf/reg-event-db
  :admin/load-users-failure
  (fn [db [_ error]]
    (utils/handle-user-api-error db error :admin/users-loading? :admin/users-error "load users")
    (state-utils/handle-api-error db {:loading-key :admin/users-loading?
                                      :error-key :admin/users-error
                                      :entity-type :users
                                      :error-message error})))

;; ============================================================================
;; View User Details Events
;; ============================================================================

(rf/reg-event-fx
  :admin/view-user-details
  (fn [{:keys [db]} [_ user-id]]
    (utils/log-user-operation "Loading user details for" user-id)
    {:db (-> (utils/create-loading-db-state db :admin/loading-user-details)
           (assoc :admin/user-details-modal-open? true)
           (dissoc :admin/user-details-error)
           (assoc :admin/current-user-details nil))
     :http-xhrio (admin-http/admin-get
                   {:uri (str "/admin/api/users/" user-id)
                    :on-success [:admin/view-user-details-success]
                    :on-failure [:admin/view-user-details-failure]})}))

(rf/reg-event-db
  :admin/view-user-details-success
  (fn [db [_ response]]
    (utils/log-user-operation "User details loaded successfully" (:user response))
    (-> (utils/clear-loading-db-state db :admin/loading-user-details)
      (assoc :admin/user-details-modal-open? true)
      (assoc :admin/current-user-details (:user response)))))

(rf/reg-event-db
  :admin/view-user-details-failure
  (fn [db [_ error]]
    (let [db' (utils/handle-user-api-error db error :admin/loading-user-details :admin/user-details-error "load user details")]
      (assoc db' :admin/user-details-modal-open? true))))

(rf/reg-event-db
  :admin/hide-user-details
  (fn [db _]
    (-> (utils/clear-loading-db-state db :admin/loading-user-details)
      (assoc :admin/user-details-modal-open? false)
      (dissoc :admin/current-user-details)
      (dissoc :admin/user-details-error))))

;; ============================================================================
;; Core Subscriptions
;; ============================================================================

(rf/reg-sub
  :admin/users
  (fn [db _]
    (:admin/users db [])))

(rf/reg-sub
  :admin/users-loading?
  (fn [db _]
    (:admin/users-loading? db false)))

(rf/reg-sub
  :admin/users-error
  (fn [db _]
    (:admin/users-error db nil)))

;; Admin-specific fetch-entities event, single-tenant variant (users only)
(rf/reg-event-fx
  :admin/fetch-entities
  (fn [{:keys [db]} [_ entity-type]]
    (when (= entity-type :users)
      {:db (assoc-in db (paths/entity-loading? entity-type) true)
       :http-xhrio (admin-http/admin-get
                     {:uri "/admin/api/users"
                      :on-success [:admin/fetch-entities-success entity-type]
                      :on-failure [:admin/fetch-entities-failure entity-type]})})))

(rf/reg-event-fx
  :admin/fetch-entities-success
  (fn [{:keys [db]} [_ entity-type response]]
    (let [clj-response (if (object? response) (js->clj response :keywordize-keys true) response)
          entities (or (get clj-response :users) clj-response)
          clj-entities (if (and (coll? entities) (object? (first entities)))
                         (map #(js->clj % :keywordize-keys true) entities)
                         entities)
          normalized-data (normalize/normalize-entities clj-entities)
          {:keys [data ids]} normalized-data]
      {:db (-> db
             (assoc-in (paths/entity-data entity-type) data)
             (assoc-in (paths/entity-ids entity-type) ids)
             (assoc-in (paths/entity-loading? entity-type) false)
             (assoc :admin/users clj-entities))
       :dispatch-n [[:app.admin.frontend.adapters.users/sync-users-to-template clj-entities]]})))

(rf/reg-event-db
  :admin/fetch-entities-failure
  (fn [db [_ entity-type error]]
    (-> db
      (assoc-in (paths/entity-loading? entity-type) false)
      (assoc-in (paths/entity-error entity-type) (str "Failed to load " entity-type ": " error)))))

;; ============================================================================
;; Individual User Deletion
;; ============================================================================

(rf/reg-event-fx
  :admin/delete-user
  (fn [{:keys [db]} [_ user-id]]
    (utils/log-user-operation "Deleting user" user-id)
    {:db (-> (utils/create-loading-db-state db :admin/updating-user)
           (dissoc :admin/user-update-error))
     :http-xhrio (admin-http/admin-request
                   {:method :delete
                    :uri "/admin/api/users/batch"
                    :params {:ids [(str user-id)]}
                    :on-success [:admin/delete-user-success user-id]
                    :on-failure [:admin/delete-user-failure user-id]})}))

(rf/reg-event-fx
  :admin/delete-user-success
  (fn [{:keys [db]} [_ user-id _response]]
    (utils/log-user-operation "User deleted successfully" user-id)
    (let [db' (-> (utils/clear-loading-db-state db :admin/updating-user)
                (update :admin/users
                  (fn [users]
                    (->> users
                      (remove (fn [u]
                                (= (or (:users/id u) (:id u)) user-id)))
                      vec)))
                (update-in (paths/entity-data :users)
                  (fn [data] (dissoc (or data {}) user-id)))
                (update-in (paths/entity-ids :users)
                  (fn [ids] (->> (or ids [])
                              (remove #(= % user-id))
                              vec)))
                (update-in (paths/entity-selected-ids :users)
                  (fn [selected] (when selected (disj selected user-id))))
                (dissoc :admin/user-update-error :admin/error-message))]
      {:db db'
       :dispatch-n [[:admin/hide-delete-confirmation]
                    [:admin/show-success-message "User deleted successfully"]
                    [:admin/load-users]]})))

(rf/reg-event-fx
  :admin/delete-user-failure
  (fn [{:keys [db]} [_ user-id error]]
    (log/error "Failed to delete user" {:user-id user-id :error error})
    (let [db' (utils/handle-user-api-error db error :admin/updating-user :admin/user-update-error "delete user")
          message (or (admin-http/extract-error-message error) "Failed to delete user")]
      {:db db'
       :dispatch [:admin/show-error-message message]})))
