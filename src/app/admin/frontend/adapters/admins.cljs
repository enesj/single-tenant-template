(ns app.admin.frontend.adapters.admins
  "Data adapter layer bridging admin accounts to template entity system"
  (:require
    [app.admin.frontend.adapters.core :as adapters.core]
    [app.admin.frontend.utils.http :as admin-http]
    [app.template.frontend.db.paths :as paths]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(defn admin->template-entity
  "Normalize admin data for the template entity store using shared adapter helpers."
  [admin]
  (adapters.core/normalize-entity admin {:entity-ns :admins
                                         :id-keys [:admins/id :id]}))

(adapters.core/register-entity-spec-sub!
  {:entity-key :admins})

(adapters.core/register-sync-event!
  {:event-id ::sync-admins-to-template
   :entity-key :admins
   :normalize-fn admin->template-entity
   :log-prefix "🛡️ Syncing admin data to template system:"})

;; Admins have a dedicated /admin/api/admins endpoint
(defn- admins-request
  "Create HTTP request config for admin accounts API."
  [{:keys [method id params on-success on-failure]}]
  (let [base-uri "/admin/api/admins"
        uri (if id (str base-uri "/" id) base-uri)]
    (admin-http/admin-request {:method method
                               :uri uri
                               :params params
                               :on-success on-success
                               :on-failure on-failure})))

(adapters.core/register-admin-crud-bridge!
  {:entity-key :admins
   :context-pred (fn [_] true)
   :operations
   {:delete {:request (fn [{:keys [db]} entity-type id default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (admins-request
                                          {:method :delete
                                           :id id
                                           :on-success [:app.template.frontend.events.list.crud/delete-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/delete-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _id default-effect]
                           (assoc default-effect :dispatch [:admin/load-admins]))}
    :create {:request (fn [{:keys [db]} entity-type form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (admins-request
                                          {:method :post
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/create-success entity-type]
                                           :on-failure [:app.template.frontend.events.list.crud/create-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _response default-effect]
                           (assoc default-effect :dispatch [:admin/load-admins]))}
    :update {:request (fn [{:keys [db]} entity-type id form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (admins-request
                                          {:method :put
                                           :id id
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/update-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/update-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _id _response default-effect]
                           (assoc default-effect :dispatch [:admin/load-admins]))}}})

(rf/reg-event-fx
  ::initialize-admins-adapter-with-config
  (fn [{:keys [db]} _]
    (let [metadata-path (paths/entity-metadata :admins)
          ui-state-path (paths/list-ui-state :admins)
          selected-ids-path (paths/entity-selected-ids :admins)
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

(defn init-admins-adapter!
  "Initialize the admins adapter UI state. Only fetch config if not already loaded
  to avoid wiping currently loaded entities (which causes table flicker)."
  []
  ;; Dispatch the adapter initialization
  (rf/dispatch [::initialize-admins-adapter-with-config])
  (log/info "Admins adapter initialized."))
