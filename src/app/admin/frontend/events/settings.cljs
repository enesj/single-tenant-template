(ns app.admin.frontend.events.settings
  "Events for managing view-options, form-fields, and table-columns configs via backend API"
  (:require
    [app.admin.frontend.config.loader :as config-loader]
    [app.admin.frontend.utils.http :as admin-http]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(defn- unauthorized?
  "Return true when an XHR error represents a 401/unauthorized response."
  [error]
  (= 401 (or (:status error) (get-in error [:response :status]))))

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
             (assoc-in [:admin :settings :view-options] view-options)
             (assoc-in [:admin :settings :error] nil))})))

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

(rf/reg-event-fx
  ::update-entity-setting
  (fn [{:keys [db]} [_ entity-name setting-key new-value]]
    (let [entity-kw (if (keyword? entity-name) entity-name (keyword entity-name))
          setting-kw (if (keyword? setting-key) setting-key (keyword setting-key))]
      {:db (-> db
             (assoc-in [:admin :settings :saving?] true)
             ;; Optimistically update the local state
             (assoc-in [:admin :settings :view-options entity-kw setting-kw] new-value))
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
          updated-options (assoc-in current-options [entity-kw setting-kw] new-value)]
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

(rf/reg-event-fx
  ::remove-entity-setting
  (fn [{:keys [db]} [_ entity-name setting-key]]
    (let [entity-kw (if (keyword? entity-name) entity-name (keyword entity-name))
          setting-kw (if (keyword? setting-key) setting-key (keyword setting-key))]
      {:db (-> db
             (assoc-in [:admin :settings :saving?] true)
             ;; Optimistically remove from local state
             (update-in [:admin :settings :view-options entity-kw] dissoc setting-kw))
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
          updated-options (update current-options entity-kw dissoc setting-kw)]
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

(rf/reg-event-db
  ::toggle-editing
  (fn [db _]
    (let [current (get-in db [:admin :settings :editing?] false)
          new-val (not current)]
      (log/info "Toggle editing" {:current current :new-val new-val})
      (assoc-in db [:admin :settings :editing?] new-val))))

;; =============================================================================
;; Active Config Tab
;; =============================================================================

(rf/reg-event-db
  ::set-config-tab
  (fn [db [_ tab]]
    (assoc-in db [:admin :settings :config-tab] tab)))

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
