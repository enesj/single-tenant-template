(ns app.admin.frontend.events.users.template.messages
  "Admin message management - success/error messages and UI feedback"
  (:require
    [app.template.frontend.events.form :as form-events]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ============================================================================
;; Success Message Management
;; ============================================================================

(rf/reg-event-db
  :admin/show-success-message
  (fn [db [_ message]]
    (assoc db :admin/success-message message)))

(rf/reg-event-db
  :admin/clear-success-message
  (fn [db _]
    (dissoc db :admin/success-message)))

(rf/reg-event-fx
  :admin/hide-form-modal
  (fn [{:keys [db]} _]
    (let [entity-type (or (get-in db [:ui :entity-name]) :users)]
      (log/info "admin/hide-form-modal" {:entity-type entity-type
                                         :ui-entity-name (get-in db [:ui :entity-name])})
      {:dispatch [::form-events/cancel-form entity-type]})))

;; ============================================================================
;; Message Subscriptions
;; ============================================================================

(rf/reg-sub
  :admin/success-message
  (fn [db _]
    (:admin/success-message db nil)))

(rf/reg-sub
  :admin/has-success-message?
  (fn [db _]
    (some? (:admin/success-message db))))

;; ============================================================================
;; Error Message Management
;; ============================================================================

(rf/reg-event-db
  :admin/show-error-message
  (fn [db [_ message]]
    (assoc db :admin/error-message message)))

(rf/reg-event-db
  :admin/clear-error-message
  (fn [db _]
    (dissoc db :admin/error-message)))

(rf/reg-sub
  :admin/error-message
  (fn [db _]
    (:admin/error-message db nil)))

(rf/reg-sub
  :admin/has-error-message?
  (fn [db _]
    (some? (:admin/error-message db))))
