(ns app.admin.frontend.events.config
  "Simplified events for vector-based column configuration.
   
   COLUMN VISIBILITY PERSISTENCE:
   ==============================
   Column visibility is now persisted via the unified ui-entity-prefs system:
   - [:ui :entity-prefs <entity> :columns :visible-order]  Vector for ordering
  - [:ui :entity-prefs <entity> :columns :visible]        Map for quick lookup"
  (:require
    [app.admin.frontend.auth.persistence :as auth-persist]
    [app.admin.frontend.config.loader :as config-loader]
    [app.shared.keywords :as kw]
    [app.shared.model-naming :as model-naming]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.interceptors.persistence :as persistence]
    [clojure.string :as str]
    [re-frame.core :as rf]))

(def ^:private bootstrap-throttle-ms 300)

(defn- now []
  (js/Date.now))

(defn- failure-info [error]
  {:timestamp (now)
   :message (or (some-> error .-message)
              (some-> error :message)
              (str error))})

;; =============================================================================
;; Configuration Loading Events
;; =============================================================================

;; =============================================================================
;; Column Visibility Events (Vector-based with Order Preservation)
;; =============================================================================

;; Toggle column visibility (preserves order!)
;; Now persists to unified entity-prefs in addition to admin config
(rf/reg-event-fx
  ::toggle-column-visibility
  [persistence/persist-entity-prefs]
  (fn [{:keys [db]} [_ entity-name column-name]]
    (let [normalize (fn [v]
                      (model-naming/ensure-app-keyword (kw/ensure-name v)))
          entity-key (model-naming/ensure-app-keyword entity-name)
          prefs-key (paths/entity-prefs-key db entity-key)
          policy-locks (merge
                         (get-in db [:admin :config :view-options entity-key :column-locks])
                         (get-in db [:admin :settings :view-options entity-key :column-locks]))
          locked? (and (map? policy-locks)
                    (contains? (into #{} (keep normalize (keys policy-locks)))
                      (normalize column-name)))
          current-visible (->> (or (get-in db [:admin :config :table-columns entity-key :visible-columns])
                                 (get-in db [:admin :config :table-columns entity-key :default-visible-columns])
                                 [])
                            (keep normalize)
                            vec)
          all-columns (->> (get-in db [:admin :config :table-columns entity-key :available-columns] [])
                        (keep normalize)
                        vec)
          column-name (normalize column-name)
          always-visible (->> (get-in db [:admin :config :table-columns entity-key :always-visible] [])
                           (keep normalize)
                           set)]
      (if (or (nil? entity-key)
            (nil? column-name)
            locked?)
        {:db db}
        (let [removing? (some #{column-name} current-visible)
            ;; Do not allow hiding always-visible columns
              _ (when (and removing? (contains? always-visible column-name))
                  nil)
              new-visible (if (and removing? (not (contains? always-visible column-name)))
                          ;; Remove column while preserving order
                            (vec (remove #{column-name} current-visible))
                          ;; Add column in its proper position based on all-columns order
                            (let [col-index (.indexOf all-columns column-name)
                                  insert-at (count (filter #(< (.indexOf all-columns %) col-index)
                                                     current-visible))]
                              (vec (concat (take insert-at current-visible)
                                     [column-name]
                                     (drop insert-at current-visible)))))
              normalized-visible (->> new-visible
                                   (keep normalize)
                                    ;; Ensure always-visible columns are present
                                   (into (vec always-visible))
                                   distinct
                                   vec)
              ;; Build a complete visibility map (true/false for ALL available columns).
              ;;
              ;; IMPORTANT:
              ;; The unified list rendering treats missing keys as visible.
              ;; If we only store `{col true}` entries, hidden columns disappear from the map and
              ;; will incorrectly render as visible. So we must write explicit `false` entries
              ;; for hidden columns.
              visible-set (set normalized-visible)
              visibility-map (cond
                               (seq all-columns)
                               (into {}
                                 (map (fn [col]
                                        [col (contains? visible-set col)]))
                                 all-columns)

                               :else
                               (into {} (map (fn [col] [col true]) normalized-visible)))]
          {:db (-> db
                 (assoc-in [:admin :config :table-columns entity-key :visible-columns] normalized-visible)
                   ;; Also store in unified prefs for persistence
                 (assoc-in (paths/entity-prefs-columns-visible-order prefs-key) normalized-visible)
                 (assoc-in (paths/entity-prefs-columns-visible prefs-key) visibility-map))})))))

;; Reorder columns

(rf/reg-event-db
  ::reorder-columns
  [persistence/persist-entity-prefs]
  (fn [db [_ entity-name column-order]]
    (if entity-name
      (let [normalize (fn [v]
                        (model-naming/ensure-app-keyword (kw/ensure-name v)))
            entity-key (model-naming/ensure-app-keyword entity-name)
            prefs-key (paths/entity-prefs-key db entity-key)
            normalized (->> (or column-order []) (keep normalize) vec)]
        (assoc-in db (paths/entity-prefs-columns-order prefs-key) normalized))
      db)))

;; Reset to default columns

;; =============================================================================
;; Fixed Configuration Loading Events
;; =============================================================================

(rf/reg-event-fx
  :admin/load-ui-configs
  (fn [{:keys [db]} [_ opts]]
    (let [admin-path? (str/starts-with? (or (.-pathname js/window.location) "") "/admin")
          token (or (:admin/token db)
                  (auth-persist/get-persisted-token))
          force? (boolean (:force? opts))
          preloaded-configs (config-loader/load-all-configs)
          bootstrap (get-in db [:admin :config :bootstrap])
          last-started (:last-started-at bootstrap 0)
          inflight? (:in-flight? bootstrap)
          now-ts (now)
          within-window? (< (- now-ts last-started) bootstrap-throttle-ms)]
      (cond
        (not admin-path?)
        {:db db}

        (and (not force?) (boolean (:admin/config-loaded? db)))
        {:db db}

        (nil? token)
        {:db (-> db
               (assoc-in [:admin :config] preloaded-configs)
               ;; Only mark config as loaded when we actually have config data.
               ;; This flag is used to switch list/table rendering into vector-config mode.
               (assoc :admin/config-loaded? (boolean (seq (get preloaded-configs :table-columns))))
               (assoc :admin/config-loading? false)
               (assoc-in [:admin :config :bootstrap]
                 (-> bootstrap
                   (assoc :awaiting-auth? true)
                   (assoc :in-flight? false)
                   (assoc :last-requested-at now-ts)
                   (dissoc :last-error))))}

        (and inflight? within-window?)
        {:db (assoc-in db [:admin :config :bootstrap :last-requested-at] now-ts)}

        :else
        {:db (-> db
               (assoc-in [:admin :config] preloaded-configs)
               ;; Only mark config as loaded when we actually have config data.
               ;; This flag is used to switch list/table rendering into vector-config mode.
               (assoc :admin/config-loaded? (boolean (seq (get preloaded-configs :table-columns))))
               (assoc :admin/config-loading? true)
               (assoc-in [:admin :config :bootstrap]
                 (-> bootstrap
                   (assoc :awaiting-auth? false)
                   (assoc :in-flight? true)
                   (assoc :last-started-at now-ts)
                   (assoc :last-requested-at now-ts)
                   (dissoc :last-error))))
         :fx [[:dispatch [::async-load-configs]]
              [:dispatch [::load-entity-configs]]]}))))

(rf/reg-event-fx
  ::load-entity-configs
  (fn [_ _]
    ;; Entity configs are preloaded via app.admin.frontend.config.preload
    ;; so this event is now a no-op kept for backward compatibility.
    {}))

(rf/reg-event-fx
  ::async-load-configs
  (fn [_ _]
    (-> (config-loader/load-all-configs!)
      (.then (fn [_]
               (rf/dispatch [::update-configs-from-cache])))
      (.catch (fn [error]
                (js/console.error "Failed to refresh admin configs" error)
                (rf/dispatch [::config-bootstrap-failed error]))))
    {}))

(rf/reg-event-fx
  ::update-configs-from-cache
  (fn [{:keys [db]} _]
    (let [updated-configs (config-loader/load-all-configs)
          now-ts (now)]
      {:db (-> db
             (assoc-in [:admin :config] updated-configs)
             (assoc :admin/config-loaded? true)
             (assoc :admin/config-loading? false)
             (assoc-in [:admin :config :bootstrap :in-flight?] false)
             (assoc-in [:admin :config :bootstrap :last-completed-at] now-ts)
             (dissoc :admin/config-load-error))})))

(rf/reg-event-db
  ::config-bootstrap-failed
  (fn [db [_ error]]
    (-> db
      (assoc-in [:admin :config :bootstrap :in-flight?] false)
      (assoc :admin/config-loading? false)
      (assoc-in [:admin :config :bootstrap :last-error] (failure-info error)))))

;; =============================================================================
;; Legacy Compatibility Events
;; =============================================================================

;; Backward compatibility with old naming
(rf/reg-event-fx
  :admin/toggle-column-visibility
  (fn [_ [_ entity-keyword column-key]]
    (rf/dispatch [::toggle-column-visibility entity-keyword column-key])
    {}))

(rf/reg-event-fx
  :admin/reorder-columns
  (fn [_ [_ entity-keyword column-order]]
    (rf/dispatch [::reorder-columns entity-keyword column-order])
    {}))
