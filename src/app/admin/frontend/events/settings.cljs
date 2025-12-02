(ns app.admin.frontend.events.settings
  "Events for managing view-options.edn settings via backend API"
  (:require
    [app.admin.frontend.config.loader :as config-loader]
    [app.admin.frontend.utils.http :as admin-http]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; =============================================================================
;; Load Settings from Backend
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

(rf/reg-event-db
  ::load-view-options-failure
  (fn [db [_ error]]
    (log/error "Failed to load view options" error)
    (-> db
      (assoc-in [:admin :settings :loading?] false)
      (assoc-in [:admin :settings :error] "Failed to load settings"))))

;; =============================================================================
;; Update Single Setting
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
    {:db (-> db
           (assoc-in [:admin :settings :saving?] false)
           (assoc-in [:admin :settings :error] "Failed to save setting"))
     :fx [[:dispatch [::load-view-options]]]}))

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
    {:db (-> db
           (assoc-in [:admin :settings :saving?] false)
           (assoc-in [:admin :settings :error] "Failed to remove setting"))
     :fx [[:dispatch [::load-view-options]]]}))

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
