(ns app.admin.frontend.events.settings.form-fields
  (:require
    [app.admin.frontend.config.loader :as config-loader]
    [app.admin.frontend.events.settings.utils :as utils]
    [app.admin.frontend.utils.http :as admin-http]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; =============================================================================
;; Load Form Fields Config
;; =============================================================================

(rf/reg-event-fx
  :app.admin.frontend.events.settings/load-form-fields
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:admin :settings :form-fields-loading?] true)
     :http-xhrio (admin-http/admin-get
                   {:uri "/admin/api/settings/form-fields"
                    :on-success [:app.admin.frontend.events.settings/load-form-fields-success]
                    :on-failure [:app.admin.frontend.events.settings/load-form-fields-failure]})}))

(rf/reg-event-fx
  :app.admin.frontend.events.settings/load-form-fields-success
  (fn [{:keys [db]} [_ response]]
    (let [form-fields (:form-fields response)]
      (log/info "Loaded form fields from backend" {:count (count form-fields)})
      {:db (-> db
             (assoc-in [:admin :settings :form-fields-loading?] false)
             (assoc-in [:admin :settings :form-fields] form-fields)
             (assoc-in [:admin :config :form-fields] form-fields)
             (assoc-in [:admin :settings :error] nil))})))

(rf/reg-event-fx
  :app.admin.frontend.events.settings/load-form-fields-failure
  (fn [{:keys [db]} [_ error]]
    (log/error "Failed to load form fields" error)
    (utils/load-failure-effect db :form-fields-loading? "Failed to load form fields" error)))

;; =============================================================================
;; Update Form Fields Entity Config
;; =============================================================================

(rf/reg-event-fx
  :app.admin.frontend.events.settings/update-form-fields-entity
  (fn [{:keys [db]} [_ entity-name entity-config]]
    (let [entity-kw (if (keyword? entity-name) entity-name (keyword entity-name))]
      {:db (-> db
             (assoc-in [:admin :settings :saving?] true)
             ;; Optimistically update
             (assoc-in [:admin :settings :form-fields entity-kw] entity-config)
             (assoc-in [:admin :config :form-fields entity-kw] entity-config))
       :http-xhrio (admin-http/admin-patch
                     {:uri "/admin/api/settings/form-fields/entity"
                      :params {:entity-name (name entity-kw)
                               :entity-config entity-config}
                      :on-success [:app.admin.frontend.events.settings/update-form-fields-success entity-kw entity-config]
                      :on-failure [:app.admin.frontend.events.settings/update-form-fields-failure entity-kw]})})))

(rf/reg-event-fx
  :app.admin.frontend.events.settings/update-form-fields-success
  (fn [{:keys [db]} [_ entity-kw entity-config _response]]
    (log/info "Form fields updated successfully" {:entity entity-kw})
    ;; Update config-loader cache
    (config-loader/register-preloaded-config! :form-fields entity-kw entity-config)
    {:db (-> db
           (assoc-in [:admin :settings :saving?] false)
           (assoc-in [:admin :settings :last-saved] (js/Date.now))
           (assoc-in [:admin :config :form-fields entity-kw] entity-config)
           (assoc-in [:admin :settings :error] nil))}))

(rf/reg-event-fx
  :app.admin.frontend.events.settings/update-form-fields-failure
  (fn [{:keys [db]} [_ entity-kw error]]
    (log/error "Failed to update form fields" {:entity entity-kw :error error})
    (utils/update-failure-effect db error "Failed to save form fields"
      [:app.admin.frontend.events.settings/load-form-fields])))

