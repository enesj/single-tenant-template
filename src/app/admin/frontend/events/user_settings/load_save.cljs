(ns app.admin.frontend.events.user-settings.load-save
  (:require
    [app.admin.frontend.utils.http :as admin-http]
    [app.admin.frontend.events.user-settings.utils :as u]
    [app.admin.frontend.utils.navigation-config :as nav-config]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; =============================================================================
;; Load
;; =============================================================================

(rf/reg-event-fx
  :app.admin.frontend.events.user-settings/init
  (fn [{:keys [db]} _]
    (log/info "Init user-ui settings editor")
    {:db (-> db
             (assoc-in [:admin :user-settings :loading?] true)
             (assoc-in [:admin :user-settings :saving?] false)
             (assoc-in [:admin :user-settings :error] nil)
             (assoc-in [:admin :user-settings :tab] "view-options")
             (assoc-in [:admin :user-settings :last-saved] nil))
     :http-xhrio (admin-http/admin-get
                   {:uri u/user-ui-config-uri
                    :on-success [:app.admin.frontend.events.user-settings/load-success]
                    :on-failure [:app.admin.frontend.events.user-settings/load-failure]})}))

(rf/reg-event-db
  :app.admin.frontend.events.user-settings/load-success
  (fn [db [_ response]]
    (let [entities (u/normalize-entity-map (:entities response))
          view-options (u/normalize-user-view-options (:view-options response))
          form-fields (u/normalize-entity-map (:form-fields response))
          table-columns (u/normalize-entity-map (:table-columns response))
             navigation (nav-config/normalize-navigation (:navigation response))
          draft {:entities entities
                 :view-options view-options
                 :form-fields form-fields
               :table-columns table-columns
               :navigation navigation}]
      (log/info "Loaded user UI config" {:entities (count entities)
                                         :view-options (count view-options)
                        :table-columns (count table-columns)
                        :navigation-sections (count (:sections navigation))})
      (-> db
          ;; Make this config available for user routes (non-admin).
          (assoc-in [:domain :config :entities] entities)
          (assoc-in [:domain :config :view-options] view-options)
          (assoc-in [:domain :config :form-fields] form-fields)
          (assoc-in [:domain :config :table-columns] table-columns)
          (assoc-in [:domain :config :navigation] navigation)

          ;; Editor state
          (assoc-in [:admin :user-settings :draft] draft)
          (assoc-in [:admin :user-settings :saved] draft)
          (assoc-in [:admin :user-settings :loading?] false)
          (assoc-in [:admin :user-settings :saving?] false)
          (assoc-in [:admin :user-settings :error] nil)))))

(rf/reg-event-db
  :app.admin.frontend.events.user-settings/load-failure
  (fn [db [_ error]]
    (let [msg (admin-http/extract-error-message error)]
      (log/error "Failed to load user UI config" error)
      (-> db
          (assoc-in [:admin :user-settings :loading?] false)
          (assoc-in [:admin :user-settings :saving?] false)
          (assoc-in [:admin :user-settings :error] msg)))))

;; =============================================================================
;; Save / Discard
;; =============================================================================

(rf/reg-event-db
  :app.admin.frontend.events.user-settings/discard-draft
  (fn [db _]
    (assoc-in db [:admin :user-settings :draft] (u/saved-config db))))

(rf/reg-event-fx
  :app.admin.frontend.events.user-settings/save
  (fn [{:keys [db]} _]
    (let [draft (u/draft-config db)
          payload (select-keys draft [:entities :view-options :form-fields :table-columns :navigation])]
      (log/info "Saving user UI config" {:entities (count (:entities payload))
                                         :view-options (count (:view-options payload))
                                         :table-columns (count (:table-columns payload))
                                         :navigation-sections (count (get-in payload [:navigation :sections]))})
      {:db (-> db
               (assoc-in [:admin :user-settings :saving?] true)
               (assoc-in [:admin :user-settings :error] nil))
       :http-xhrio (admin-http/admin-put
                     {:uri u/user-ui-config-uri
                      :params payload
                      :on-success [:app.admin.frontend.events.user-settings/save-success]
                      :on-failure [:app.admin.frontend.events.user-settings/save-failure]})})))

(rf/reg-event-db
  :app.admin.frontend.events.user-settings/save-success
  (fn [db [_ response]]
    (let [entities (u/normalize-entity-map (:entities response))
          view-options (u/normalize-user-view-options (:view-options response))
          form-fields (u/normalize-entity-map (:form-fields response))
          table-columns (u/normalize-entity-map (:table-columns response))
             navigation (nav-config/normalize-navigation (:navigation response))
          saved {:entities entities
                 :view-options view-options
                 :form-fields form-fields
               :table-columns table-columns
               :navigation navigation}]
      (log/info "Saved user UI config")
      (-> db
          ;; Keep domain config in sync for user routes.
          (assoc-in [:domain :config :entities] entities)
          (assoc-in [:domain :config :view-options] view-options)
          (assoc-in [:domain :config :form-fields] form-fields)
          (assoc-in [:domain :config :table-columns] table-columns)
          (assoc-in [:domain :config :navigation] navigation)

          ;; Editor state
          (assoc-in [:admin :user-settings :saved] saved)
          (assoc-in [:admin :user-settings :draft] saved)
          (assoc-in [:admin :user-settings :saving?] false)
          (assoc-in [:admin :user-settings :error] nil)
          (assoc-in [:admin :user-settings :last-saved] (js/Date.now))))))

(rf/reg-event-db
  :app.admin.frontend.events.user-settings/save-failure
  (fn [db [_ error]]
    (let [msg (admin-http/extract-error-message error)]
      (log/error "Failed to save user UI config" error)
      (-> db
          (assoc-in [:admin :user-settings :saving?] false)
          (assoc-in [:admin :user-settings :error] msg)))))

