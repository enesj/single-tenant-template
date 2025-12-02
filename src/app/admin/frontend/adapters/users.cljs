(ns app.admin.frontend.adapters.users
  "Data adapter layer bridging admin users to template entity system"
  (:require
    [app.admin.frontend.adapters.core :as adapters.core]
    [app.admin.frontend.utils.http :as admin-http]
    [app.template.frontend.db.paths :as paths]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(defn user->template-entity
  "Normalize user data for the template entity store using shared adapter helpers."
  [user]
  (adapters.core/normalize-entity user {:entity-ns :users
                                        :id-keys [:users/id :id]}))

(adapters.core/register-entity-spec-sub!
  {:entity-key :users})

(adapters.core/register-sync-event!
  {:event-id ::sync-users-to-template
   :entity-key :users
   :normalize-fn user->template-entity
   :log-prefix "👤 Syncing user data to template system:"})

;; Users have a dedicated /admin/api/users endpoint, not /admin/api/entities/users
(defn- users-request
  "Create HTTP request config for admin users API.
   Users have dedicated routes at /admin/api/users, not /admin/api/entities/users."
  [{:keys [method id params on-success on-failure]}]
  (let [base-uri "/admin/api/users"
        uri (if id (str base-uri "/" id) base-uri)]
    (admin-http/admin-request {:method method
                               :uri uri
                               :params params
                               :on-success on-success
                               :on-failure on-failure})))

(adapters.core/register-admin-crud-bridge!
  {:entity-key :users
   :context-pred (fn [_] true)
   :operations
   ;; Handler signatures for on-success:
   ;; - delete: (fn [cofx entity-type id default-effect]) - 4 args
   ;; - create: (fn [cofx entity-type response default-effect]) - 4 args
   ;; - update: (fn [cofx entity-type id response default-effect]) - 5 args
   {:delete {:request (fn [{:keys [db]} entity-type id default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (users-request
                                          {:method :delete
                                           :id id
                                           :on-success [:app.template.frontend.events.list.crud/delete-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/delete-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _id default-effect]
                           (assoc default-effect :dispatch [:admin/load-users]))}
    :create {:request (fn [{:keys [db]} entity-type form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (users-request
                                          {:method :post
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/create-success entity-type]
                                           :on-failure [:app.template.frontend.events.list.crud/create-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _response default-effect]
                           (assoc default-effect :dispatch [:admin/load-users]))}
    :update {:request (fn [{:keys [db]} entity-type id form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (users-request
                                          {:method :put
                                           :id id
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/update-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/update-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             ;; Note: update on-success receives 5 args: cofx, entity-type, id, response, default-effect
             :on-success (fn [_cofx _entity-type _id _response default-effect]
                           (assoc default-effect :dispatch [:admin/load-users]))}}})

(rf/reg-event-fx
  ::initialize-users-adapter-with-config
  (fn [{:keys [db]} _]
    (let [metadata-path (paths/entity-metadata :users)
          ui-state-path (paths/list-ui-state :users)
          selected-ids-path (paths/entity-selected-ids :users)
          db* (adapters.core/assoc-paths db
                [[(conj metadata-path :sort) {:field :created_at :direction :desc}]
                 [(conj metadata-path :filters) {}]
                 [ui-state-path {:sort {:field :created_at :direction :desc}
                                 :pagination (merge {:current-page 1 :per-page 10}
                                               (:pagination (get-in db ui-state-path)))}]
                 [selected-ids-path #{}]])
          fetch-config (adapters.core/maybe-fetch-config db)]
      (cond-> {:db db*}
        fetch-config (assoc :dispatch-n [fetch-config])))))

(defn init-users-adapter!
  "Initialize the users adapter UI state. Only fetch config if not already loaded
  to avoid wiping currently loaded entities (which causes table flicker)."
  []
  ;; Dispatch the adapter initialization
  (rf/dispatch [::initialize-users-adapter-with-config])

  (log/info "Users adapter initialized."))
