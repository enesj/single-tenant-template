(ns app.admin.frontend.events.config
  "Simplified events for vector-based column configuration"
  (:require
   [app.admin.frontend.config.loader :as config-loader]
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

;; Load all config from EDN files
(rf/reg-event-fx
  ::load-config
  (fn [{:keys [db]} _]
    (let [configs (config-loader/load-all-configs)]
      {:db (assoc-in db [:admin :config] configs)})))

;; Load saved config from localStorage
(rf/reg-event-db
  ::load-saved-column-config
  (fn [db [_ entity-name]]
    (let [stored-key (str "column-visibility-" entity-name)
          stored-value (js/localStorage.getItem stored-key)]
      (if stored-value
        (let [visible-columns (->> (js->clj (js/JSON.parse stored-value))
                                (keep (fn [col]
                                        (cond
                                          (keyword? col) col
                                          (string? col) (keyword col)
                                          (nil? col) nil
                                          :else (keyword (str col)))))
                                vec)]
          (assoc-in db [:admin :config :table-columns entity-name :visible-columns] visible-columns))
        db))))

;; =============================================================================
;; Column Visibility Events (Vector-based with Order Preservation)
;; =============================================================================

;; Toggle column visibility (preserves order!)
(rf/reg-event-fx
  ::toggle-column-visibility
  (fn [{:keys [db]} [_ entity-name column-name]]
    (let [normalize (fn [col]
                      (cond
                        (nil? col) nil
                        (keyword? col) col
                        (string? col) (keyword col)
                        :else (keyword (str col))))
          current-visible (->> (or (get-in db [:admin :config :table-columns entity-name :visible-columns])
                                 (get-in db [:admin :config :table-columns entity-name :default-visible-columns])
                                 [])
                            (keep normalize)
                            vec)
          all-columns (->> (get-in db [:admin :config :table-columns entity-name :available-columns] [])
                        (keep normalize)
                        vec)
          column-name (normalize column-name)
          always-visible (set (map normalize (get-in db [:admin :config :table-columns entity-name :always-visible] [])))]
      (if (nil? column-name)
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
                                   vec)]
          {:db (assoc-in db [:admin :config :table-columns entity-name :visible-columns] normalized-visible)
           :fx [[:dispatch [::save-column-config entity-name normalized-visible]]]})))))

;; Reorder columns
(rf/reg-event-fx
  ::reorder-columns
  (fn [{:keys [db]} [_ entity-name from-index to-index]]
    (let [visible-columns (vec (get-in db [:admin :config :table-columns entity-name :visible-columns] []))
          column-to-move (nth visible-columns from-index)
          without-column (vec (concat (subvec visible-columns 0 from-index)
                                (subvec visible-columns (inc from-index))))
          new-visible (vec (concat (subvec without-column 0 to-index)
                             [column-to-move]
                             (subvec without-column to-index)))]
      {:db (assoc-in db [:admin :config :table-columns entity-name :visible-columns] new-visible)
       :fx [[:dispatch [::save-column-config entity-name new-visible]]]})))

;; Reset to default columns
(rf/reg-event-fx
  ::reset-columns-to-default
  (fn [{:keys [db]} [_ entity-name]]
    (let [default-columns (get-in db [:admin :config :table-columns entity-name :default-visible-columns] [])]
      {:db (assoc-in db [:admin :config :table-columns entity-name :visible-columns] default-columns)
       :fx [[:dispatch [::clear-saved-column-config entity-name]]]})))

;; =============================================================================
;; LocalStorage Persistence
;; =============================================================================

;; Save column config to localStorage
(rf/reg-event-fx
  ::save-column-config
  (fn [_ [_ entity-name visible-columns]]
    {::save-to-local-storage {:key (str "column-visibility-" entity-name)
                              :value visible-columns}}))

;; Clear saved config
(rf/reg-event-fx
  ::clear-saved-column-config
  (fn [_ [_ entity-name]]
    {::remove-from-local-storage {:key (str "column-visibility-" entity-name)}}))

;; Effect handlers for localStorage
(rf/reg-fx
 ::save-to-local-storage
  (fn [{:keys [key value]}]
    (js/localStorage.setItem key (js/JSON.stringify (clj->js value)))))

(rf/reg-fx
 ::remove-from-local-storage
  (fn [{:keys [key]}]
    (js/localStorage.removeItem key)))

;; =============================================================================
;; Fixed Configuration Loading Events
;; =============================================================================

(rf/reg-event-fx
  :admin/load-ui-configs
  (fn [{:keys [db]} _]
    (let [admin-path? (str/starts-with? (or (.-pathname js/window.location) "") "/admin")
          preloaded-configs (config-loader/load-all-configs)
          bootstrap (get-in db [:admin :config :bootstrap])
          last-started (:last-started-at bootstrap 0)
          inflight? (:in-flight? bootstrap)
          now-ts (now)
          within-window? (< (- now-ts last-started) bootstrap-throttle-ms)]
      (if-not admin-path?
        {:db db}
        (if (and inflight? within-window?)
          {:db (assoc-in db [:admin :config :bootstrap :last-requested-at] now-ts)}
          {:db (-> db
                 (assoc-in [:admin :config] preloaded-configs)
                 (assoc :admin/config-loaded? true)
                 (assoc-in [:admin :config :bootstrap]
                   (-> bootstrap
                     (assoc :in-flight? true)
                     (assoc :last-started-at now-ts)
                     (assoc :last-requested-at now-ts)
                     (dissoc :last-error))))
           :fx [[:dispatch [::async-load-configs]]
                [:dispatch [::load-entity-configs]]]})))))

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
             (assoc-in [:admin :config :bootstrap :in-flight?] false)
             (assoc-in [:admin :config :bootstrap :last-completed-at] now-ts)
             (dissoc :admin/config-load-error))})))

(rf/reg-event-db
  ::config-bootstrap-failed
  (fn [db [_ error]]
    (-> db
      (assoc-in [:admin :config :bootstrap :in-flight?] false)
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
