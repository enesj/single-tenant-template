(ns app.admin.frontend.events.user-settings.subs
  (:require
    [app.admin.frontend.events.user-settings.utils :as u]
    [re-frame.core :as rf]))

;; =============================================================================
;; Subscriptions
;; =============================================================================

(rf/reg-sub
  :app.admin.frontend.events.user-settings/draft
  (fn [db _]
    (u/draft-config db)))

#_(rf/reg-sub
    :app.admin.frontend.events.user-settings/saved
    (fn [db _]
      (u/saved-config db)))

(rf/reg-sub
  :app.admin.frontend.events.user-settings/dirty?
  (fn [db _]
    (not= (u/draft-config db) (u/saved-config db))))

(rf/reg-sub
  :app.admin.frontend.events.user-settings/tab
  (fn [db _]
    (get-in db [:admin :user-settings :tab] "view-options")))

(rf/reg-sub
  :app.admin.frontend.events.user-settings/loading?
  (fn [db _]
    (get-in db [:admin :user-settings :loading?] false)))

(rf/reg-sub
  :app.admin.frontend.events.user-settings/saving?
  (fn [db _]
    (get-in db [:admin :user-settings :saving?] false)))

(rf/reg-sub
  :app.admin.frontend.events.user-settings/error
  (fn [db _]
    (get-in db [:admin :user-settings :error])))

(rf/reg-sub
  :app.admin.frontend.events.user-settings/last-saved
  (fn [db _]
    (get-in db [:admin :user-settings :last-saved])))

(rf/reg-sub
  :app.admin.frontend.events.user-settings/table-columns-config
  (fn [db _]
    (u/safe-map (get-in db [:admin :user-settings :draft :table-columns]))))

;; =============================================================================
;; Additional subscriptions for config editing
;; =============================================================================

#_(rf/reg-sub
    :app.admin.frontend.events.user-settings/entities-config
    (fn [db _]
      (u/safe-map (get-in db [:admin :user-settings :draft :entities]))))

#_(rf/reg-sub
    :app.admin.frontend.events.user-settings/form-fields-config
    (fn [db _]
      (u/safe-map (get-in db [:admin :user-settings :draft :form-fields]))))

