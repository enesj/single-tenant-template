(ns app.admin.frontend.components.user-actions
  "Refactored admin user management actions using shared components"
  (:require
    [app.template.frontend.components.action-components :as shared-actions]
    [app.template.frontend.components.confirm-dialog :as confirm-dialog]
    [app.template.frontend.components.dropdown.action :as dropdown]
    [app.template.frontend.events.config :as config-events]
    [app.template.frontend.events.form :as form-events]
    [app.template.frontend.events.list.crud :as crud-events]
    [app.template.frontend.utils.id :as id-utils]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

;; =============================================================================
;; Action Handlers
;; =============================================================================

;; =============================================================================
;; Action Handlers
;; =============================================================================

(defn create-user-action-handlers
  "Factory function to create all action handlers for a user"
  [user user-id user-label]
  (merge
    (shared-actions/create-status-action-handlers user-id user-label "user" :admin/update-user-status)
    {:edit-user (fn [e]
                  (.stopPropagation e)
                  (log/info "Editing user" user-id user-label)
                  (rf/dispatch [::crud-events/clear-error :users])
                  (rf/dispatch [::form-events/clear-form-errors :users])
                  (if-let [on-edit-click (:on-edit-click user)]
                    (on-edit-click user)
                    (rf/dispatch [::config-events/set-editing user-id])))

     :view-details (fn [e]
                     (.stopPropagation e)
                     (log/info "Viewing user details" user-id user-label)
                     (rf/dispatch [:admin/view-user-details user-id]))

     :view-activity (fn [e]
                      (.stopPropagation e)
                      (log/info "Viewing user activity" user-id user-label)
                      (rf/dispatch [:admin/view-user-activity user-id]))

     :reset-password (fn []
                       (log/info "Resetting user password" user-id user-label)
                       (rf/dispatch [:admin/reset-user-password user-id]))

     :verify-email (fn []
                     (log/info "Force verifying email" user-id user-label)
                     (rf/dispatch [:admin/force-verify-email user-id]))

     :delete-user (fn []
                    (log/info "Deleting user" user-id user-label)
                    (rf/dispatch [:admin/delete-user user-id]))}))

(defn create-user-confirmation-handlers
  "Factory function to create confirmation handlers that wrap action handlers"
  [handlers user-label]
  (merge
    (shared-actions/create-confirmation-handlers handlers user-label "user")
    {:reset-password (fn [e]
                       (.stopPropagation e)
                       (confirm-dialog/show-confirm
                         {:message (str "Reset password for " user-label "? They will receive a reset email.")
                          :title "Confirm Password Reset"
                          :on-confirm (:reset-password handlers)}))

     :verify-email (fn [e]
                     (.stopPropagation e)
                     (confirm-dialog/show-confirm
                       {:message (str "Force verify email for " user-label "?")
                        :title "Confirm Email Verification"
                        :on-confirm (:verify-email handlers)}))

     :delete-user (fn [e]
                    (.stopPropagation e)
                    (confirm-dialog/show-confirm
                      {:message (str "Permanently delete user " user-label "? This action cannot be undone and will remove all associated data.")
                       :title "Confirm User Deletion"
                       :danger? true
                       :on-confirm (:delete-user handlers)}))}))

;; =============================================================================
;; Main User Actions Dropdown Component
;; =============================================================================

(defui admin-user-actions
  "Enhanced admin user actions using the reusable template dropdown component"
  [{:keys [user]}]
  (let [user-id (id-utils/extract-entity-id user)
        ;; Use shared utilities to extract entity data
        user-status (shared-actions/get-entity-status user :user)
        user-label (or (:full-name user)
                     (:full_name user)
                     (:user-display-name user)
                     (:user_display_name user)
                     (:user-ref user)
                     (:user_ref user)
                     (:email-masked user)
                     (:email_masked user)
                     "this user")
        email-verified (or (:users/email-verified user) (:email-verified user))

        ;; Subscribe to loading states
        updating-user? (use-subscribe [:admin/updating-user?])
        loading-user-details? (use-subscribe [:admin/loading-user-details?])

        ;; Create action handlers
        action-handlers (create-user-action-handlers user user-id user-label)
        confirmation-handlers (create-user-confirmation-handlers action-handlers user-label)

        ;; Deletion constraints are now checked in batch at the page level

        ;; Define action groups for the dropdown using shared components
        action-groups (-> []
                           ;; View actions group
                        (conj (shared-actions/create-view-action-group
                                (:view-details action-handlers)
                                [{:id "edit-user"
                                  :icon "✏️"
                                  :label "Edit User"
                                  :on-click (:edit-user action-handlers)}
                                 {:id "view-activity"
                                  :icon "📊"
                                  :label "View Activity"
                                  :on-click (:view-activity action-handlers)}]))

                           ;; Status actions group
                        (conj (shared-actions/create-status-action-group
                                user-status
                                confirmation-handlers
                                :updating-user?))

                           ;; Verification group (only if email not verified)
                        (cond-> (not email-verified)
                          (conj {:group-title "Verification"
                                 :items [{:id "verify-email"
                                          :icon "✉️"
                                          :label "Force Verify Email"
                                          :loading-key :updating-user?
                                          :on-click (:verify-email confirmation-handlers)}]}))

                           ;; Advanced actions group
                        (conj (shared-actions/create-dangerous-action-group
                                (cond-> []
                                  true
                                  (conj {:id "reset-password"
                                         :icon "🔑"
                                         :label "Reset Password"
                                         :loading-key :updating-user?
                                         :on-click (:reset-password confirmation-handlers)})

                                  ;; Add delete action with constraint-based disabling
                                  true
                                  (conj {:id "delete-user"
                                         :icon "🗑️"
                                         :label "Delete User"
                                         :variant :error
                                         :loading-key :updating-user?
                                         :tooltip "Delete this user"
                                         :on-click (:delete-user confirmation-handlers)}))))

                           ;; Filter out empty groups
                        (shared-actions/filter-empty-groups))

        ;; Loading states map
        loading-states {:updating-user? updating-user?
                        :loading-user-details? loading-user-details?}]

    ;; Return the action dropdown component - aligned to start like other buttons
    ($ :div {:class "flex items-center"}
      ($ dropdown/action-dropdown {:entity-id user-id
                                   :actions action-groups
                                   :loading-states loading-states
                                   :position :portal}))))
