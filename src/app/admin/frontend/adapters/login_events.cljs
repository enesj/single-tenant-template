(ns app.admin.frontend.adapters.login-events
  "Adapter for login events to work with the template system.
   
   This adapter is responsible for:
   - Data normalization (login-event->template-entity)
   - Template system sync (register-sync-event!)
   - UI state initialization
   - Deletion handling
   
   HTTP events are in app.admin.frontend.events.login-events"
  (:require
    [app.admin.frontend.adapters.core :as adapters.core]
    [app.template.frontend.db.paths :as paths]
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

(adapters.core/register-entity-spec-sub!
  {:entity-key :login-events})

(adapters.core/register-sync-event!
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
          existing-per-page (or (get-in db (paths/list-per-page :login-events))
                              (get-in db (conj ui-state-path :per-page))
                              (get-in db (conj ui-state-path :pagination :per-page)))
          existing-page (or (get-in db (paths/list-current-page :login-events))
                          (get-in db (conj ui-state-path :current-page))
                          (get-in db (conj ui-state-path :pagination :current-page)))
          per-page (or existing-per-page 20)
          page (or existing-page 1)
          db* (adapters.core/assoc-paths db
                [[(conj metadata-path :sort) {:field :created-at :direction :desc}]
                 [(conj metadata-path :pagination) {:page page :per-page per-page}]
                 [(conj metadata-path :filters) {}]
                 [ui-state-path {:sort {:field :created-at :direction :desc}
                                 :current-page page
                                 :per-page per-page
                                 :pagination {:current-page page :per-page per-page}}]
                 [(paths/list-per-page :login-events) per-page]
                 [(paths/list-current-page :login-events) page]
                 [selected-ids-path #{}]])
          fetch-config (adapters.core/maybe-fetch-config db)]
      (cond-> {:db db*}
        fetch-config (assoc :dispatch-n [fetch-config])
        :else (assoc :dispatch-n [])))))

(defn init-login-events-adapter!
  "Initialize the login events adapter for template system integration"
  []
  (rf/dispatch [::initialize-login-events-ui-state]))
