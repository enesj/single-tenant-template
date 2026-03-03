(ns app.admin.frontend.events.users.template.messages
  "Admin message management - success/error messages and UI feedback"
  (:require

    [re-frame.core :as rf]))

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

;; ============================================================================
;; Message Subscriptions
;; ============================================================================

(rf/reg-sub
  :admin/success-message
  (fn [db _]
    (:admin/success-message db nil)))

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

(rf/reg-event-db
  :admin/clear-entity-error
  (fn [db [_ entity-name]]
    ;; Admin pages track entity-level errors as keywords like :admin/users-error.
    ;; The admin page wrapper needs a generic way to clear those.
    (if (some? entity-name)
      (dissoc db (keyword "admin" (str (name entity-name) "-error")))
      db)))

(rf/reg-sub
  :admin/error-message
  (fn [db _]
    (:admin/error-message db nil)))
