(ns app.admin.frontend.events.settings
  "Events for managing view-options, form-fields, and table-columns configs via backend API"
  (:require
    [app.admin.frontend.config.loader :as config-loader]
    [app.admin.frontend.utils.http :as admin-http]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(defn- safe-map
  "Normalize nil to empty map for comparisons/merges."
  [x]
  (if (map? x) x {}))

(defn- unauthorized?
  "Return true when an XHR error represents a 401/unauthorized response."
  [error]
  (= 401 (or (:status error) (get-in error [:response :status]))))

(defn- display-setting-key?
  "True when the key is one of the list-view display toggles.

  In the new schema, these live under :display-defaults / :display-locks.
  (Historically they were top-level keys in view-options.edn.)"
  [k]
  (and (keyword? k)
    (re-matches #"show-.*\?" (name k))))

(defn- normalize-kw
  [x]
  (cond
    (nil? x) nil
    (keyword? x) x
    (string? x) (keyword x)
    :else (keyword (str x))))

(defn- normalize-kws
  [xs]
  (->> (or xs [])
    (keep normalize-kw)
    vec))

;; =============================================================================
;; Load View Options from Backend
;; =============================================================================

(rf/reg-event-fx
  ::load-view-options
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:admin :settings :loading?] true)
     :http-xhrio (admin-http/admin-get
                   {:uri "/admin/api/settings"
                    :on-success [::load-view-options-success]
                    :on-failure [::load-view-options-failure]})}))

(rf/reg-event-fx
  ::load-view-options-success
  (fn [{:keys [db]} [_ response]]
    (let [view-options (:view-options response)]
      (log/info "Loaded view options from backend" {:count (count view-options)})
      {:db (-> db
             (assoc-in [:admin :settings :loading?] false)
             ;; Keep both the persisted (saved) copy and the editable draft.
             ;; Draft is what the UI edits; saved is used for diffing / discard.
             (assoc-in [:admin :settings :view-options-saved] view-options)
             (assoc-in [:admin :settings :view-options] view-options)
             ;; Also keep the global admin config in sync so list pages pick up locks
             ;; even if they aren't reading from :admin/:settings.
             (assoc-in [:admin :config :view-options] view-options)
             (assoc-in [:admin :settings :error] nil))})))

;; =============================================================================
;; View Options Draft Editing (staged changes)
;; =============================================================================

(rf/reg-event-db
  ::set-view-option-draft
  (fn [db [_ entity-name setting-key new-value]]
    (let [entity-kw (if (keyword? entity-name) entity-name (keyword entity-name))
          setting-kw (if (keyword? setting-key) setting-key (keyword setting-key))
          display? (display-setting-key? setting-kw)
          base-path [:admin :settings :view-options entity-kw]]
      (cond
        (and display? (nil? new-value))
        (update-in db (conj base-path :display-locks) (fnil dissoc {}) setting-kw)

        display?
        (assoc-in db (conj base-path :display-locks setting-kw) new-value)

        (nil? new-value)
        (update-in db base-path dissoc setting-kw)

        :else
        (assoc-in db (conj base-path setting-kw) new-value)))))

;; =============================================================================
;; Display toggles draft editing (explicit defaults vs locks)
;; =============================================================================

;; new-state schema:
;; - {:kind :inherit}
;; - {:kind :default :value boolean}
;; - {:kind :lock :value boolean}
(rf/reg-event-db
  ::set-display-setting-draft
  (fn [db [_ entity-name setting-key new-state]]
    (let [entity-kw (if (keyword? entity-name) entity-name (keyword entity-name))
          setting-kw (if (keyword? setting-key) setting-key (keyword setting-key))
          kind (:kind new-state)
          value (:value new-state)
          defaults-path [:admin :settings :view-options entity-kw :display-defaults]
          locks-path [:admin :settings :view-options entity-kw :display-locks]]
      (cond
        (or (nil? entity-kw) (nil? setting-kw) (not (display-setting-key? setting-kw)))
        db

        (= kind :inherit)
        (-> db
          (update-in defaults-path (fnil dissoc {}) setting-kw)
          (update-in locks-path (fnil dissoc {}) setting-kw))

        (and (= kind :default) (boolean? value))
        (-> db
          ;; A default only applies when not locked.
          (update-in locks-path (fnil dissoc {}) setting-kw)
          (assoc-in (conj defaults-path setting-kw) value))

        (and (= kind :lock) (boolean? value))
        (-> db
          ;; A lock supersedes any default.
          (update-in defaults-path (fnil dissoc {}) setting-kw)
          (assoc-in (conj locks-path setting-kw) value))

        :else
        db))))

;; =============================================================================
;; Column visibility draft editing (explicit defaults vs locks)
;; =============================================================================

;; new-state schema:
;; - {:kind :inherit}
;; - {:kind :default :value boolean}
;; - {:kind :lock :value boolean}
(rf/reg-event-db
  ::set-column-visibility-setting-draft
  (fn [db [_ entity-name column-key new-state]]
    (let [entity-kw (if (keyword? entity-name) entity-name (keyword entity-name))
          column-kw (if (keyword? column-key) column-key (keyword column-key))
          kind (:kind new-state)
          value (:value new-state)
          defaults-path [:admin :settings :view-options entity-kw :column-defaults]
          locks-path [:admin :settings :view-options entity-kw :column-locks]]
      (cond
        (or (nil? entity-kw) (nil? column-kw))
        db

        (= kind :inherit)
        (-> db
          (update-in defaults-path (fnil dissoc {}) column-kw)
          (update-in locks-path (fnil dissoc {}) column-kw))

        (and (= kind :default) (boolean? value))
        (-> db
          ;; A default only applies when not locked.
          (update-in locks-path (fnil dissoc {}) column-kw)
          (assoc-in (conj defaults-path column-kw) value))

        (and (= kind :lock) (boolean? value))
        (-> db
          ;; A lock supersedes any default.
          (update-in defaults-path (fnil dissoc {}) column-kw)
          (assoc-in (conj locks-path column-kw) value))

        :else
        db))))

;; =============================================================================
;; Bulk helpers: column visibility defaults
;; =============================================================================

(rf/reg-event-db
  ::set-column-defaults-bulk
  (fn [db [_ entity-name column-keys value]]
    (let [entity-kw (normalize-kw entity-name)
          cols (normalize-kws column-keys)
          value (boolean value)
          defaults-path [:admin :settings :view-options entity-kw :column-defaults]
          locks-path [:admin :settings :view-options entity-kw :column-locks]]
      (cond
        (or (nil? entity-kw) (empty? cols))
        db

        :else
        (reduce (fn [db' col]
                  (assoc-in db' (conj defaults-path col) value))
          (update-in db locks-path (fnil (fn [m] (apply dissoc m cols)) {}))
          cols)))))

;; =============================================================================
;; Bulk helpers: apply tristate to many display settings / columns
;; =============================================================================

(rf/reg-event-db
  ::set-display-settings-bulk
  (fn [db [_ entity-name setting-keys new-state]]
    (let [entity-kw (if (keyword? entity-name) entity-name (keyword entity-name))
          keys (->> (or setting-keys [])
                 (map (fn [k] (if (keyword? k) k (keyword k))))
                 vec)
          kind (:kind new-state)
          value (:value new-state)
          defaults-path [:admin :settings :view-options entity-kw :display-defaults]
          locks-path [:admin :settings :view-options entity-kw :display-locks]]
      (cond
        (or (nil? entity-kw) (empty? keys))
        db

        (= kind :inherit)
        (-> db
          (update-in defaults-path (fnil (fn [m] (apply dissoc m keys)) {}))
          (update-in locks-path (fnil (fn [m] (apply dissoc m keys)) {})))

        (and (= kind :default) (boolean? value))
        (reduce (fn [db' k]
            (assoc-in db' (conj defaults-path k) value))
          (update-in db locks-path (fnil (fn [m] (apply dissoc m keys)) {}))
          keys)

        (and (= kind :lock) (boolean? value))
        (reduce (fn [db' k]
            (assoc-in db' (conj locks-path k) value))
          (update-in db defaults-path (fnil (fn [m] (apply dissoc m keys)) {}))
          keys)

        :else
        db))))

(rf/reg-event-db
  ::set-column-visibility-bulk
  (fn [db [_ entity-name column-keys new-state]]
    (let [entity-kw (normalize-kw entity-name)
          cols (normalize-kws column-keys)
          kind (:kind new-state)
          value (:value new-state)
          defaults-path [:admin :settings :view-options entity-kw :column-defaults]
          locks-path [:admin :settings :view-options entity-kw :column-locks]]
      (log/info "Bulk column visibility change"
        {:entity entity-kw
         :columns-count (count cols)
         :state new-state})
      (cond
        (or (nil? entity-kw) (empty? cols))
        db

        (= kind :inherit)
        (-> db
          (update-in defaults-path (fnil (fn [m] (apply dissoc m cols)) {}))
          (update-in locks-path (fnil (fn [m] (apply dissoc m cols)) {})))

        (and (= kind :default) (boolean? value))
        (reduce (fn [db' c]
            (assoc-in db' (conj defaults-path c) value))
          (update-in db locks-path (fnil (fn [m] (apply dissoc m cols)) {}))
          cols)

        (and (= kind :lock) (boolean? value))
        (reduce (fn [db' c]
            (assoc-in db' (conj locks-path c) value))
          (update-in db defaults-path (fnil (fn [m] (apply dissoc m cols)) {}))
          cols)

        :else
        db))))

(rf/reg-event-db
  ::reset-view-options-draft
  (fn [db _]
    (let [saved (safe-map (get-in db [:admin :settings :view-options-saved]))]
      (assoc-in db [:admin :settings :view-options] saved))))

(rf/reg-event-fx
  ::save-view-options
  (fn [{:keys [db]} _]
    (let [draft (safe-map (get-in db [:admin :settings :view-options]))]
      {:db (-> db
             (assoc-in [:admin :settings :saving?] true)
             (assoc-in [:admin :settings :error] nil))
       :http-xhrio (admin-http/admin-put
                     {:uri "/admin/api/settings"
                      :params {:view-options draft}
                      :on-success [::save-view-options-success]
                      :on-failure [::save-view-options-failure]})})))

(rf/reg-event-fx
  ::save-view-options-success
  (fn [{:keys [db]} [_ response]]
    (let [saved (safe-map (:view-options response))]
      (log/info "View options saved successfully" {:count (count saved)})
      ;; Update the config-loader cache so admin/template list pages pick up the change.
      (config-loader/register-preloaded-config! :view-options saved)
      {:db (-> db
             (assoc-in [:admin :settings :saving?] false)
             (assoc-in [:admin :settings :last-saved] (js/Date.now))
             (assoc-in [:admin :settings :error] nil)
             (assoc-in [:admin :settings :view-options-saved] saved)
             (assoc-in [:admin :settings :view-options] saved)
             (assoc-in [:admin :config :view-options] saved))})))

(rf/reg-event-fx
  ::save-view-options-failure
  (fn [{:keys [db]} [_ error]]
    (log/error "Failed to save view options" error)
    (cond-> {:db (-> db
                   (assoc-in [:admin :settings :saving?] false)
                   (assoc-in [:admin :settings :error] "Failed to save view options"))}
      (unauthorized? error) (assoc :dispatch [:admin/auth-invalid]))))

(rf/reg-event-fx
  ::load-view-options-failure
  (fn [{:keys [db]} [_ error]]
    (log/error "Failed to load view options" error)
    (cond-> {:db (-> db
                   (assoc-in [:admin :settings :loading?] false)
                   (assoc-in [:admin :settings :error] "Failed to load settings"))}
      (unauthorized? error) (assoc :dispatch [:admin/auth-invalid]))))

;; =============================================================================
;; Update Single View Option Setting
;; =============================================================================

#_(rf/reg-event-fx
  ::update-entity-setting
  (fn [{:keys [db]} [_ entity-name setting-key new-value]]
    (let [entity-kw (if (keyword? entity-name) entity-name (keyword entity-name))
          setting-kw (if (keyword? setting-key) setting-key (keyword setting-key))
          display? (display-setting-key? setting-kw)
          base-path [:admin :settings :view-options entity-kw]
          db' (cond
                display?
                (assoc-in db (conj base-path :display-locks setting-kw) new-value)

                :else
                (assoc-in db (conj base-path setting-kw) new-value))]
      {:db (assoc-in db' [:admin :settings :saving?] true)
       :http-xhrio (admin-http/admin-patch
                     {:uri "/admin/api/settings/entity"
                      :params {:entity-name (name entity-kw)
                               :setting-key (name setting-kw)
                               :setting-value new-value}
                      :on-success [::update-setting-success entity-kw setting-kw new-value]
                      :on-failure [::update-setting-failure entity-kw setting-kw]})})))

(rf/reg-event-fx
  ::update-setting-success
  (fn [{:keys [db]} [_ entity-kw setting-kw new-value _response]]
    (log/info "Setting updated successfully" {:entity entity-kw :setting setting-kw :value new-value})
    ;; Also update the config-loader cache so components pick up the change
    (let [current-options (config-loader/get-all-view-options)
          display? (display-setting-key? setting-kw)
          path (if display?
                 [entity-kw :display-locks setting-kw]
                 [entity-kw setting-kw])
          updated-options (assoc-in current-options path new-value)]
      (config-loader/register-preloaded-config! :view-options updated-options))
    {:db (-> db
           (assoc-in [:admin :settings :saving?] false)
           (assoc-in [:admin :settings :last-saved] (js/Date.now))
           (assoc-in [:admin :settings :error] nil))}))

(rf/reg-event-fx
  ::update-setting-failure
  (fn [{:keys [db]} [_ entity-kw setting-kw error]]
    (log/error "Failed to update setting" {:entity entity-kw :setting setting-kw :error error})
    ;; Revert the optimistic update by reloading from backend
    (cond-> {:db (-> db
                   (assoc-in [:admin :settings :saving?] false)
                   (assoc-in [:admin :settings :error] "Failed to save setting"))
             :fx [[:dispatch [::load-view-options]]]}
      (unauthorized? error) (assoc :dispatch [:admin/auth-invalid]))))

;; =============================================================================
;; Remove Setting (make user-configurable)
;; =============================================================================

#_(rf/reg-event-fx
  ::remove-entity-setting
  (fn [{:keys [db]} [_ entity-name setting-key]]
    (let [entity-kw (if (keyword? entity-name) entity-name (keyword entity-name))
          setting-kw (if (keyword? setting-key) setting-key (keyword setting-key))
          display? (display-setting-key? setting-kw)
          base-path [:admin :settings :view-options entity-kw]
          db' (cond
                display?
                (update-in db (conj base-path :display-locks) (fnil dissoc {}) setting-kw)

                :else
                (update-in db base-path dissoc setting-kw))]
      {:db (assoc-in db' [:admin :settings :saving?] true)
       :http-xhrio (admin-http/admin-delete
                     {:uri "/admin/api/settings/entity"
                      :params {:entity-name (name entity-kw)
                               :setting-key (name setting-kw)}
                      :on-success [::remove-setting-success entity-kw setting-kw]
                      :on-failure [::remove-setting-failure entity-kw setting-kw]})})))

(rf/reg-event-fx
  ::remove-setting-success
  (fn [{:keys [db]} [_ entity-kw setting-kw _response]]
    (log/info "Setting removed successfully" {:entity entity-kw :setting setting-kw})
    ;; Also update the config-loader cache
    (let [current-options (config-loader/get-all-view-options)
          display? (display-setting-key? setting-kw)
          updated-options (if display?
                            (update-in current-options [entity-kw :display-locks] (fnil dissoc {}) setting-kw)
                            (update current-options entity-kw dissoc setting-kw))]
      (config-loader/register-preloaded-config! :view-options updated-options))
    {:db (-> db
           (assoc-in [:admin :settings :saving?] false)
           (assoc-in [:admin :settings :last-saved] (js/Date.now))
           (assoc-in [:admin :settings :error] nil))}))

(rf/reg-event-fx
  ::remove-setting-failure
  (fn [{:keys [db]} [_ entity-kw setting-kw error]]
    (log/error "Failed to remove setting" {:entity entity-kw :setting setting-kw :error error})
    (cond-> {:db (-> db
                   (assoc-in [:admin :settings :saving?] false)
                   (assoc-in [:admin :settings :error] "Failed to remove setting"))
             :fx [[:dispatch [::load-view-options]]]}
      (unauthorized? error) (assoc :dispatch [:admin/auth-invalid]))))

;; =============================================================================
;; Load Form Fields Config
;; =============================================================================

(rf/reg-event-fx
  ::load-form-fields
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:admin :settings :form-fields-loading?] true)
     :http-xhrio (admin-http/admin-get
                   {:uri "/admin/api/settings/form-fields"
                    :on-success [::load-form-fields-success]
                    :on-failure [::load-form-fields-failure]})}))

(rf/reg-event-fx
  ::load-form-fields-success
  (fn [{:keys [db]} [_ response]]
    (let [form-fields (:form-fields response)]
      (log/info "Loaded form fields from backend" {:count (count form-fields)})
      {:db (-> db
             (assoc-in [:admin :settings :form-fields-loading?] false)
             (assoc-in [:admin :settings :form-fields] form-fields)
             (assoc-in [:admin :settings :error] nil))})))

(rf/reg-event-fx
  ::load-form-fields-failure
  (fn [{:keys [db]} [_ error]]
    (log/error "Failed to load form fields" error)
    (let [db' (-> db
                (assoc-in [:admin :settings :form-fields-loading?] false)
                (assoc-in [:admin :settings :error] "Failed to load form fields"))]
      (if (unauthorized? error)
        {:db db'
         :dispatch [:admin/auth-invalid]}
        {:db db'}))))

;; =============================================================================
;; Update Form Fields Entity Config
;; =============================================================================

(rf/reg-event-fx
  ::update-form-fields-entity
  (fn [{:keys [db]} [_ entity-name entity-config]]
    (let [entity-kw (if (keyword? entity-name) entity-name (keyword entity-name))]
      {:db (-> db
             (assoc-in [:admin :settings :saving?] true)
             ;; Optimistically update
             (assoc-in [:admin :settings :form-fields entity-kw] entity-config))
       :http-xhrio (admin-http/admin-patch
                     {:uri "/admin/api/settings/form-fields/entity"
                      :params {:entity-name (name entity-kw)
                               :entity-config entity-config}
                      :on-success [::update-form-fields-success entity-kw entity-config]
                      :on-failure [::update-form-fields-failure entity-kw]})})))

(rf/reg-event-fx
  ::update-form-fields-success
  (fn [{:keys [db]} [_ entity-kw entity-config _response]]
    (log/info "Form fields updated successfully" {:entity entity-kw})
    ;; Update config-loader cache
    (config-loader/register-preloaded-config! :form-fields entity-kw entity-config)
    {:db (-> db
           (assoc-in [:admin :settings :saving?] false)
           (assoc-in [:admin :settings :last-saved] (js/Date.now))
           (assoc-in [:admin :settings :error] nil))}))

(rf/reg-event-fx
  ::update-form-fields-failure
  (fn [{:keys [db]} [_ entity-kw error]]
    (log/error "Failed to update form fields" {:entity entity-kw :error error})
    (cond-> {:db (-> db
                   (assoc-in [:admin :settings :saving?] false)
                   (assoc-in [:admin :settings :error] "Failed to save form fields"))
             :fx [[:dispatch [::load-form-fields]]]}
      (unauthorized? error) (assoc :dispatch [:admin/auth-invalid]))))

;; =============================================================================
;; Load Table Columns Config
;; =============================================================================

(rf/reg-event-fx
  ::load-table-columns
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:admin :settings :table-columns-loading?] true)
     :http-xhrio (admin-http/admin-get
                   {:uri "/admin/api/settings/table-columns"
                    :on-success [::load-table-columns-success]
                    :on-failure [::load-table-columns-failure]})}))

(rf/reg-event-fx
  ::load-table-columns-success
  (fn [{:keys [db]} [_ response]]
    (let [table-columns (:table-columns response)]
      (log/info "Loaded table columns from backend" {:count (count table-columns)})
      {:db (-> db
             (assoc-in [:admin :settings :table-columns-loading?] false)
             (assoc-in [:admin :settings :table-columns] table-columns)
             (assoc-in [:admin :settings :error] nil))})))

(rf/reg-event-fx
  ::load-table-columns-failure
  (fn [{:keys [db]} [_ error]]
    (log/error "Failed to load table columns" error)
    (let [db' (-> db
                (assoc-in [:admin :settings :table-columns-loading?] false)
                (assoc-in [:admin :settings :error] "Failed to load table columns"))]
      (if (unauthorized? error)
        {:db db'
         :dispatch [:admin/auth-invalid]}
        {:db db'}))))

;; =============================================================================
;; Update Table Columns Entity Config
;; =============================================================================

(rf/reg-event-fx
  ::update-table-columns-entity
  (fn [{:keys [db]} [_ entity-name entity-config]]
    (let [entity-kw (if (keyword? entity-name) entity-name (keyword entity-name))]
      {:db (-> db
             (assoc-in [:admin :settings :saving?] true)
             ;; Optimistically update
             (assoc-in [:admin :settings :table-columns entity-kw] entity-config))
       :http-xhrio (admin-http/admin-patch
                     {:uri "/admin/api/settings/table-columns/entity"
                      :params {:entity-name (name entity-kw)
                               :entity-config entity-config}
                      :on-success [::update-table-columns-success entity-kw entity-config]
                      :on-failure [::update-table-columns-failure entity-kw]})})))

(rf/reg-event-fx
  ::update-table-columns-success
  (fn [{:keys [db]} [_ entity-kw entity-config _response]]
    (log/info "Table columns updated successfully" {:entity entity-kw})
    ;; Update config-loader cache
    (config-loader/register-preloaded-config! :table-columns entity-kw entity-config)
    {:db (-> db
           (assoc-in [:admin :settings :saving?] false)
           (assoc-in [:admin :settings :last-saved] (js/Date.now))
           (assoc-in [:admin :settings :error] nil))}))

(rf/reg-event-fx
  ::update-table-columns-failure
  (fn [{:keys [db]} [_ entity-kw error]]
    (log/error "Failed to update table columns" {:entity entity-kw :error error})
    (cond-> {:db (-> db
                   (assoc-in [:admin :settings :saving?] false)
                   (assoc-in [:admin :settings :error] "Failed to save table columns"))
             :fx [[:dispatch [::load-table-columns]]]}
      (unauthorized? error) (assoc :dispatch [:admin/auth-invalid]))))

;; =============================================================================
;; Toggle Editing Mode
;; =============================================================================

(rf/reg-event-fx
  ::toggle-editing
  (fn [{:keys [db]} _]
    (let [current (get-in db [:admin :settings :editing?] false)
          new-val (not current)
          ;; When leaving edit mode, discard any staged view-options changes.
          db' (if (false? new-val)
                (let [saved (safe-map (get-in db [:admin :settings :view-options-saved]))]
                  (assoc-in db [:admin :settings :view-options] saved))
                db)]
      (log/info "Toggle editing" {:current current :new-val new-val})
      {:db (assoc-in db' [:admin :settings :editing?] new-val)})))

;; =============================================================================
;; Active Config Tab
;; =============================================================================

(rf/reg-event-fx
  ::set-config-tab
  (fn [{:keys [db]} [_ tab]]
    {:db (assoc-in db [:admin :settings :config-tab] tab)}))

;; =============================================================================
;; Active Domain Tab
;; =============================================================================

(rf/reg-event-fx
  ::set-domain-tab
  (fn [{:keys [db]} [_ tab]]
    {:db (assoc-in db [:admin :settings :domain-tab] tab)}))

;; =============================================================================
;; Derived View Options State
;; =============================================================================

(rf/reg-sub
  ::view-options-dirty?
  (fn [db _]
    (not= (safe-map (get-in db [:admin :settings :view-options]))
      (safe-map (get-in db [:admin :settings :view-options-saved])))))

;; =============================================================================
;; Subscriptions
;; =============================================================================

(rf/reg-sub
  ::loading?
  (fn [db _]
    (get-in db [:admin :settings :loading?] false)))

(rf/reg-sub
  ::saving?
  (fn [db _]
    (get-in db [:admin :settings :saving?] false)))

(rf/reg-sub
  ::error
  (fn [db _]
    (get-in db [:admin :settings :error])))

(rf/reg-sub
  ::editing?
  (fn [db _]
    (get-in db [:admin :settings :editing?] false)))

(rf/reg-sub
  ::editable-view-options
  (fn [db _]
    (get-in db [:admin :settings :view-options] {})))

(rf/reg-sub
  ::form-fields
  (fn [db _]
    (get-in db [:admin :settings :form-fields] {})))

(rf/reg-sub
  ::form-fields-loading?
  (fn [db _]
    (get-in db [:admin :settings :form-fields-loading?] false)))

(rf/reg-sub
  ::table-columns
  (fn [db _]
    (get-in db [:admin :settings :table-columns] {})))

(rf/reg-sub
  ::table-columns-loading?
  (fn [db _]
    (get-in db [:admin :settings :table-columns-loading?] false)))

(rf/reg-sub
  ::config-tab
  (fn [db _]
    (get-in db [:admin :settings :config-tab] "view-options")))

(rf/reg-sub
  ::domain-tab
  (fn [db _]
    (get-in db [:admin :settings :domain-tab] "system")))
