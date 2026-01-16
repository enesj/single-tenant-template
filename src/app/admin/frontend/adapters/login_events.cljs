(ns app.admin.frontend.adapters.login-events
  "Adapter for login events to work with the template system.
   
   This adapter is responsible for:
   - Data normalization (login-event->template-entity)
   - Template system sync (register-sync-event!)
   - UI state initialization
   - Deletion handling
   
   HTTP events are in app.admin.frontend.events.login-events"
  (:require
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.shared.utils.db :as db-utils]
    [app.template.frontend.shared.utils.entity :as entity-utils]
    [re-frame.core :as rf]))

;; =============================================================================
;; Data Normalization
;; =============================================================================

(defn login-event->template-entity
  "Normalize login event data for the template entity store.
   Ensure IDs are strings and avoid namespaced duplicates."
  [event]
  (-> event
    (update :id #(when % (str %)))
    (update :principal-id #(when % (str %)))))

;; =============================================================================
;; Template System Integration
;; =============================================================================

(entity-utils/register-entity-spec-sub!
  {:entity-key :login-events})

(entity-utils/register-sync-event!
  {:event-id ::sync-login-events-to-template
   :entity-key :login-events
   :normalize-fn login-event->template-entity
   :log-prefix "[login-events] Syncing login events to template system:"})

;; =============================================================================
;; Deletion Handling
;; =============================================================================

(rf/reg-event-fx
  ::login-event-deleted
  (fn [{:keys [db]} [_ event-id]]
    (let [entity-path (paths/entity-data :login-events)
          ids-path (paths/entity-ids :login-events)
          selected-path (paths/entity-selected-ids :login-events)
          event-id-str (str event-id)
          ;; Remove from entity data map
          new-db (-> db
                   (update-in entity-path dissoc event-id-str)
                   ;; Remove from IDs list
                   (update-in ids-path #(filterv (fn [id] (not= (str id) event-id-str)) (or % [])))
                   ;; Remove from selected IDs
                   (update-in selected-path disj event-id-str))]
      {:db new-db})))

;; =============================================================================
;; UI State Initialization
;; =============================================================================

(rf/reg-event-fx
  ::initialize-login-events-ui-state
  (fn [{:keys [db]} _]
    (let [metadata-path (paths/entity-metadata :login-events)
          ui-state-path (paths/list-ui-state :login-events)
          selected-ids-path (paths/entity-selected-ids :login-events)
          db* (db-utils/assoc-paths db
                [[(conj metadata-path :sort) {:field :created-at :direction :desc}]
                 [(conj metadata-path :filters) {}]
                 [ui-state-path {:sort {:field :created-at :direction :desc}
                                 :pagination (merge {:current-page 1}
                                               (:pagination (get-in db ui-state-path)))}]
                 [selected-ids-path #{}]])
          fetch-config (db-utils/maybe-fetch-config db)]
      (cond-> {:db db*}
        fetch-config (assoc :dispatch-n [fetch-config])))))

(defn init-login-events-adapter!
  "Initialize the login events adapter for template system integration"
  []
  (rf/dispatch [::initialize-login-events-ui-state]))
