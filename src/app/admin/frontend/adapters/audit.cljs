(ns app.admin.frontend.adapters.audit
  "Adapter for audit logs to work with the template system.

   This adapter is responsible for:
   - Data normalization (audit-log->template-entity)
   - Template system sync (register-sync-event!)
   - Bridge registration for CRUD operations
   - UI state initialization

   HTTP events are in app.admin.frontend.events.audit"
  (:require
    [app.admin.frontend.adapters.core :as adapters.core]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.interceptors.persistence :as persistence]
    [app.template.frontend.shared.utils.db :as db-utils]
    [app.template.frontend.shared.utils.entity :as entity-utils]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(defn- present-string
  [value]
  (let [value* (some-> value str str/trim)]
    (when (seq value*)
      value*)))

(defn- truncate-summary
  [value]
  (when-let [value* (present-string value)]
    (if (> (count value*) 96)
      (str (subs value* 0 93) "...")
      value*)))

(defn- actor-type-label
  [actor-type]
  (case (some-> actor-type present-string str/lower-case)
    "admin" "Admin"
    "user" "User"
    "system" "System"
    (some-> actor-type
      present-string
      (str/replace #"[_-]+" " ")
      str/capitalize)))

(defn- audit-changes
  [log]
  (let [candidate (or (:changes log) (:metadata log))]
    (if (map? candidate)
      candidate
      {})))

(defn- derive-actor-display-name
  [log]
  (or (present-string (:actor-display-name log))
    (present-string (:admin-name log))
    (present-string (:admin-ref log))
    (actor-type-label (:actor-type log))))

(defn- derive-entity-name
  [log changes]
  (or (present-string (:entity-name log))
    (present-string (:api-name changes))
    (present-string (:triggering-user-name changes))
    (present-string (:target-type log))
    (present-string (:entity-type changes))))

(defn- derive-context-summary
  [log changes]
  (let [summary-parts (->> [(present-string (:api-name changes))
                            (present-string (:operation changes))
                            (when-let [http-status (:http-status changes)]
                              (str "HTTP " http-status))
                            (when-let [member-count (:member-ids-count changes)]
                              (str member-count " members"))
                            (when (and (nil? (:api-name changes))
                                    (nil? (:operation changes))
                                    (nil? (:http-status changes))
                                    (nil? (:member-ids-count changes)))
                              (or (present-string (:entity-type changes))
                                (present-string (:target-type log))
                                (truncate-summary (:error-message changes))))]
                        (remove nil?)
                        distinct
                        vec)]
    (when (seq summary-parts)
      (str/join " • " summary-parts))))

;; Transform namespaced keys to simple keys for template system
(defn audit-log->template-entity
  "Normalize audit log data for the template entity store.
   Keep only plain keys without namespacing to avoid duplicate columns in the table.
   Also promote the most useful audit details to top-level fields so the shared
   list/detail UI can render meaningful summaries without bespoke row logic."
  [log]
  (let [changes (audit-changes log)
        normalized-log (-> log
                         (update :id #(when % (str %)))
                         (update :audit-log-id #(when % (str %)))
                         (update :entity-id #(when % (str %)))
                         (update :actor-id #(when % (str %)))
                         (update :target-id #(when % (str %)))
                         (update :user-id #(when % (str %)))
                         (update :admin-id #(when % (str %)))
                         ;; Ensure we have an :id field for the template system
                         (as-> log*
                           (if (and (not (:id log*)) (:audit-log-id log*))
                             (assoc log* :id (:audit-log-id log*))
                             log*)))
        entity-type (or (:entity-type normalized-log) (:target-type normalized-log))
        entity-id (or (:entity-id normalized-log) (:target-id normalized-log))
        actor-display-name (derive-actor-display-name normalized-log)
        entity-name (derive-entity-name normalized-log changes)
        context-summary (derive-context-summary normalized-log changes)]
    (cond-> normalized-log
      entity-type (assoc :entity-type (str entity-type))
      entity-id (assoc :entity-id (str entity-id))
      actor-display-name (assoc :actor-display-name actor-display-name)
      entity-name (assoc :entity-name entity-name)
      context-summary (assoc :context-summary context-summary)
      (contains? changes :api-name) (assoc :api-name (present-string (:api-name changes)))
      (contains? changes :operation) (assoc :operation (present-string (:operation changes)))
      (contains? changes :severity) (assoc :severity (present-string (:severity changes)))
      (contains? changes :http-status) (assoc :http-status (:http-status changes))
      (contains? changes :error-type) (assoc :error-type (present-string (:error-type changes)))
      (contains? changes :error-message) (assoc :error-message (present-string (:error-message changes)))
      (contains? changes :request-url) (assoc :request-url (present-string (:request-url changes)))
      (contains? changes :retry-attempted) (assoc :retry-attempted (:retry-attempted changes))
      (contains? changes :retry-succeeded) (assoc :retry-succeeded (:retry-succeeded changes))
      (contains? changes :triggering-user-name)
      (assoc :triggering-user-name (present-string (:triggering-user-name changes)))
      (contains? changes :triggering-user-id)
      (assoc :triggering-user-id (some-> (:triggering-user-id changes) str)))))

;; Sync normalized audit logs into template entity store (data + ids)
(entity-utils/register-entity-spec-sub!
  {:entity-key :audit-logs})

(entity-utils/register-sync-event!
  {:event-id ::sync-audit-logs-to-template
   :entity-key :audit-logs
   :normalize-fn audit-log->template-entity
   :log-prefix "[audit] Syncing audit logs to template system:"})

(adapters.core/register-admin-crud-bridge!
  {:entity-key :audit-logs
   :operations
   {:delete {:request (fn [_ _ id default-effect]
                        (log/info "[audit] Routing delete through admin API" id)
                        (-> default-effect
                          (dissoc :http-xhrio)
                          (assoc :dispatch [:admin/delete-audit-log id])))
             :on-success (fn [_ _ _ default-effect]
                           (assoc default-effect :dispatch [:admin/load-audit-logs]))}}})

(rf/reg-event-fx
  ::initialize-audit-ui-state
  [persistence/persist-entity-prefs]
  (fn [{:keys [db]} _]
    (let [metadata-path (paths/entity-metadata :audit-logs)
          ui-state-path (paths/list-ui-state :audit-logs)
          selected-ids-path (paths/entity-selected-ids :audit-logs)
          ;; Seed only current-page and preserve existing pagination (including per-page) if present.
          ;; Per-page defaults are seeded by list-view from entities.edn (:display-settings :per-page).
            db* (db-utils/assoc-paths db
              [[(conj metadata-path :sort) {:field :created-at :direction :desc}]
               [(conj metadata-path :filters) {}]
               [ui-state-path {:sort {:field :created-at :direction :desc}
                       :pagination-mode :server
                       :refresh-event [:admin/load-audit-logs]
                       :pagination (-> (merge {:current-page 1}
                                 (:pagination (get-in db ui-state-path)))
                               (assoc :mode :server))}]
               [selected-ids-path #{}]])
          fetch-config (db-utils/maybe-fetch-config db)]
      (cond-> {:db db*}
        fetch-config (assoc :dispatch-n [fetch-config])))))

;; Handle successful audit log deletion from main events
(rf/reg-event-db
  ::audit-log-deleted
  (fn [db [_ audit-id]]
    (let [data-path (paths/entity-data :audit-logs)
          ids-path (paths/entity-ids :audit-logs)
          id-str (str audit-id)]

      ;; Remove from template store
      (-> db
        (update-in data-path (fn [m] (dissoc m id-str)))
        (update-in ids-path #(filterv (fn [id] (not= (str id) id-str)) %))))))

(defn init-audit-adapter!
  "Initialize the audit logs adapter for template system integration"
  []
  (rf/dispatch [::initialize-audit-ui-state]))