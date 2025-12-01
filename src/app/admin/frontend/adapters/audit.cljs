(ns app.admin.frontend.adapters.audit
  "Adapter for audit logs to work with the template system"
  (:require
    [app.admin.frontend.adapters.core :as adapters.core]
    [app.admin.frontend.utils.http :as admin-http]

    [app.template.frontend.db.paths :as paths]
    [clojure.string :as str]
    [day8.re-frame.http-fx]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; The audit table now uses the unified vector config system like Users  tables

(rf/reg-event-fx
  ::load-audit-logs-direct
  (fn [{:keys [db]} [_ {:as params}]]
    (let [metadata-path (paths/entity-metadata :audit-logs)]
      (if (adapters.core/admin-token db)
        {:db (assoc-in db (conj metadata-path :loading?) true)
         :http-xhrio (admin-http/admin-get
                       {:uri "/admin/api/audit"
                        :params params
                        :on-success [::audit-logs-loaded]
                        :on-failure [::audit-logs-load-failed]})}
        {:db (-> db
               (assoc-in (conj metadata-path :loading?) false)
               (assoc-in (conj metadata-path :error) "Authentication required"))}))))

;; Admin load event is defined in events/audit.cljs and delegates here.

;; Transform namespaced keys to simple keys for template system
(defn audit-log->template-entity
  "Normalize audit log data for the template entity store.
   Keep only plain keys without namespacing to avoid duplicate columns in the table."
  [log]
  ;; Simply ensure IDs are strings, don't create namespaced duplicates
  (-> log
    (update :id #(when % (str %)))
    (update :audit-log-id #(when % (str %)))
    (update :entity-id #(when % (str %)))
    (update :user-id #(when % (str %)))
    (update :admin-id #(when % (str %)))
      ;; Ensure we have an :id field for the template system
    (as-> log*
      (if (and (not (:id log*)) (:audit-log-id log*))
        (assoc log* :id (:audit-log-id log*))
        log*))))

;; Sync normalized audit logs into template entity store (data + ids)
(adapters.core/register-entity-spec-sub!
  {:entity-key :audit-logs})

(adapters.core/register-sync-event!
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
  ::audit-logs-loaded
  (fn [{:keys [db]} [_ response]]
    (let [raw-logs (get response :logs [])
          metadata-path (paths/entity-metadata :audit-logs)]
      {:db (-> db
             (assoc-in (conj metadata-path :loading?) false)
             (assoc-in (conj metadata-path :error) nil))
       :dispatch-n [[::sync-audit-logs-to-template raw-logs]
                    [:admin/audit-logs-loaded]]})))

;; Handle failed audit logs loading
(rf/reg-event-fx
  ::audit-logs-load-failed
  (fn [{:keys [db]} [_ error]]
    (let [error-msg "Failed to load audit logs"
          path (paths/entity-metadata :audit-logs)]
      (log/error "Audit logs load failed:" error)
      {:db (-> db
             (assoc-in (conj path :loading?) false)
             (assoc-in (conj path :error) error-msg))
       :dispatch [:admin/audit-logs-load-failed error]})))

(rf/reg-event-fx
  ::initialize-audit-ui-state
  (fn [{:keys [db]} _]
    (let [metadata-path (paths/entity-metadata :audit-logs)
          ui-state-path (paths/list-ui-state :audit-logs)
          selected-ids-path (paths/entity-selected-ids :audit-logs)
          ;; Determine existing pagination if already present to avoid overriding user choice
          existing-per-page (or (get-in db (paths/list-per-page :audit-logs))
                              (get-in db (conj ui-state-path :per-page))
                              (get-in db (conj ui-state-path :pagination :per-page)))
          existing-page (or (get-in db (paths/list-current-page :audit-logs))
                          (get-in db (conj ui-state-path :current-page))
                          (get-in db (conj ui-state-path :pagination :current-page)))

          per-page (or existing-per-page 10)
          page (or existing-page 1)

          ;; Initialize ALL relevant pagination/sort paths; but use existing values when present
          db* (adapters.core/assoc-paths db
                [[(conj metadata-path :sort) {:field :timestamp :direction :desc}]
                 [(conj metadata-path :pagination) {:page page :per-page per-page}]
                 [(conj metadata-path :filters) {}]
                 [ui-state-path {:sort {:field :timestamp :direction :desc}
                                 :current-page page
                                 :per-page per-page
                                 :pagination {:current-page page :per-page per-page}}]
                 [(paths/list-per-page :audit-logs) per-page]
                 [(paths/list-current-page :audit-logs) page]
                 [selected-ids-path #{}]])
          fetch-config (adapters.core/maybe-fetch-config db)]
      (cond-> {:db db*}
        fetch-config (assoc :dispatch-n [fetch-config])
        :else (assoc :dispatch-n [])))))

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

(rf/reg-event-db
  ::audit-log-delete-failed
  (fn [db [_ error]]
    (log/error "Failed to delete audit log:" error)
    (assoc-in db [:admin :error] "Failed to delete audit log")))

(defn init-audit-adapter!
  "Initialize the audit logs adapter for template system integration"
  []
  (rf/dispatch [::initialize-audit-ui-state]))

(defn format-timestamp
  [timestamp]
  (when timestamp
    (let [date (js/Date. timestamp)]
      (.toLocaleString date))))

(defn format-changes
  [changes]
  (when (seq changes)
    (clojure.string/join ", "
      (for [[k v] changes]
        (str (name k) ": " v)))))

(defn get-action-badge-class
  [action]
  (case (keyword action)
    :create "bg-green-100 text-green-800"
    :update "bg-yellow-100 text-yellow-800"
    :delete "bg-red-100 text-red-800"
    "bg-gray-100 text-gray-800"))

(defn get-entity-icon
  [entity-type]
  (case (keyword entity-type)
    :user [:icon {:name "user" :class "w-4 h-4 text-gray-500"}]
    :subscription [:icon {:name "credit-card" :class "w-4 h-4 text-gray-500"}]
    [:icon {:name "question-mark-circle" :class "w-4 h-4 text-gray-500"}]))
