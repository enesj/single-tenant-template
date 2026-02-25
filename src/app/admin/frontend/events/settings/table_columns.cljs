(ns app.admin.frontend.events.settings.table-columns
  (:require
    [app.admin.frontend.config.loader :as config-loader]
    [app.admin.frontend.events.settings.utils :as utils]
    [app.admin.frontend.utils.http :as admin-http]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; =============================================================================
;; Load Table Columns Config
;; =============================================================================

(rf/reg-event-fx
  :app.admin.frontend.events.settings/load-table-columns
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:admin :settings :table-columns-loading?] true)
     :http-xhrio (admin-http/admin-get
                   {:uri "/admin/api/settings/table-columns"
                    :on-success [:app.admin.frontend.events.settings/load-table-columns-success]
                    :on-failure [:app.admin.frontend.events.settings/load-table-columns-failure]})}))

(rf/reg-event-fx
  :app.admin.frontend.events.settings/load-table-columns-success
  (fn [{:keys [db]} [_ response]]
    (let [table-columns (:table-columns response)]
      (log/info "Loaded table columns from backend" {:count (count table-columns)})
      {:db (-> db
             (assoc-in [:admin :settings :table-columns-loading?] false)
             (assoc-in [:admin :settings :table-columns] table-columns)
             (assoc-in [:admin :config :table-columns] table-columns)
             (assoc-in [:admin :settings :error] nil))})))

(rf/reg-event-fx
  :app.admin.frontend.events.settings/load-table-columns-failure
  (fn [{:keys [db]} [_ error]]
    (log/error "Failed to load table columns" error)
    (utils/load-failure-effect db :table-columns-loading? "Failed to load table columns" error)))

;; =============================================================================
;; Update Table Columns Entity Config
;; =============================================================================

(rf/reg-event-fx
  :app.admin.frontend.events.settings/update-table-columns-entity
  (fn [{:keys [db]} [_ entity-name entity-config]]
    (let [entity-kw (if (keyword? entity-name) entity-name (keyword entity-name))]
      {:db (-> db
             (assoc-in [:admin :settings :saving?] true)
             ;; Optimistically update
             (assoc-in [:admin :settings :table-columns entity-kw] entity-config)
             (assoc-in [:admin :config :table-columns entity-kw] entity-config))
       :http-xhrio (admin-http/admin-patch
                     {:uri "/admin/api/settings/table-columns/entity"
                      :params {:entity-name (name entity-kw)
                               :entity-config entity-config}
                      :on-success [:app.admin.frontend.events.settings/update-table-columns-success entity-kw entity-config]
                      :on-failure [:app.admin.frontend.events.settings/update-table-columns-failure entity-kw]})})))

(rf/reg-event-fx
  :app.admin.frontend.events.settings/update-table-columns-success
  (fn [{:keys [db]} [_ entity-kw entity-config _response]]
    (log/info "Table columns updated successfully" {:entity entity-kw})
    ;; Update config-loader cache
    (config-loader/register-preloaded-config! :table-columns entity-kw entity-config)
    {:db (-> db
           (assoc-in [:admin :settings :saving?] false)
           (assoc-in [:admin :settings :last-saved] (js/Date.now))
           (assoc-in [:admin :config :table-columns entity-kw] entity-config)
           (assoc-in [:admin :settings :error] nil))}))

(rf/reg-event-fx
  :app.admin.frontend.events.settings/update-table-columns-failure
  (fn [{:keys [db]} [_ entity-kw error]]
    (log/error "Failed to update table columns" {:entity entity-kw :error error})
    (utils/update-failure-effect db error "Failed to save table columns"
      [:app.admin.frontend.events.settings/load-table-columns])))

