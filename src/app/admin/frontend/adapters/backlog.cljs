(ns app.admin.frontend.adapters.backlog
  "Adapter for admin backlog entity to integrate with generic template CRUD/list flows."
  (:require
    [app.admin.frontend.adapters.core :as adapters.core]
    [app.admin.frontend.utils.http :as admin-http]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.shared.utils.db :as db-utils]
    [app.template.frontend.shared.utils.entity :as entity-utils]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(defn backlog->template-entity
  "Normalize backlog item for template entity store."
  [item]
  (let [number (:number item)]
    (cond-> item
      (some? number) (assoc :id number))))

(entity-utils/register-entity-spec-sub!
  {:entity-key :backlog})

(entity-utils/register-sync-event!
  {:event-id ::sync-backlog-to-template
   :entity-key :backlog
   :normalize-fn backlog->template-entity
   :log-prefix "[backlog] Syncing backlog items to template system:"})

(adapters.core/register-admin-crud-bridge!
  {:entity-key :backlog
   :context-pred (fn [_] true)
   :operations
   {:fetch {:request (fn [{:keys [db]} entity-type default-effect]
                       (if (adapters.core/admin-token db)
                         (assoc default-effect
                           :http-xhrio (admin-http/entity-request
                                         {:method :get
                                          :entity-type entity-type
                                          :params {:limit 500 :offset 0}
                                          :on-success [:app.template.frontend.events.list.crud/fetch-success entity-type]
                                          :on-failure [:app.template.frontend.events.list.crud/fetch-failure entity-type]}))
                         {:dispatch [:admin/redirect-to-login]}))}

    :batch-delete {:request (fn [{:keys [db]} entity-type ids default-effect]
                              (if (adapters.core/admin-token db)
                                (let [ids* (->> (or ids [])
                                             (remove nil?)
                                             (map str)
                                             distinct
                                             vec)]
                                  (assoc default-effect
                                    :http-xhrio (admin-http/admin-delete
                                                  {:uri "/admin/api/entities/backlog/batch"
                                                   :params {:ids ids*}
                                                   :on-success [:app.template.frontend.events.list.crud/batch-delete-success entity-type ids*]
                                                   :on-failure [:app.template.frontend.events.list.crud/batch-delete-failure entity-type ids*]})))
                                {:dispatch [:admin/redirect-to-login]}))
                   :on-success (fn [_cofx _entity-type _ids _response default-effect]
                                 (assoc default-effect
                                   :dispatch [:app.template.frontend.events.list.crud/fetch-entities :backlog]))}}})

(rf/reg-event-fx
  ::initialize-backlog-adapter-with-config
  (fn [{:keys [db]} _]
    (let [metadata-path (paths/entity-metadata :backlog)
          ui-state-path (paths/list-ui-state :backlog)
          selected-ids-path (paths/entity-selected-ids :backlog)
          db* (db-utils/assoc-paths db
                [[(conj metadata-path :sort) {:field :priority :direction :asc}]
                 [(conj metadata-path :filters) {}]
                 [ui-state-path {:sort {:field :priority :direction :asc}
                                 :pagination (merge {:current-page 1}
                                               (:pagination (get-in db ui-state-path)))}]
                 [selected-ids-path #{}]])
          fetch-config (db-utils/maybe-fetch-config db)]
      (cond-> {:db db*}
        fetch-config (assoc :dispatch-n [fetch-config])))))

(defn init-backlog-adapter!
  "Initialize admin backlog list UI defaults."
  []
  (rf/dispatch [::initialize-backlog-adapter-with-config])
  (log/info "Backlog adapter initialized."))
