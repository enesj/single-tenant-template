(ns app.admin.frontend.components.user-actions
  "Refactored admin user management actions using shared components"
  (:require
    [app.template.frontend.components.action-components :as shared-actions]
    [app.shared.field-metadata :as field-meta]
    [app.frontend.utils.id :as id-utils]
    [app.template.frontend.components.confirm-dialog :as confirm-dialog]
    [app.template.frontend.components.dropdown :as dropdown]
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
  [user-id user-email]
  (merge
    (shared-actions/create-status-action-handlers user-id user-email "user" :admin/update-user-status)
    {:view-details (fn [e]
                     (.stopPropagation e)
                     (log/info "Viewing user details" user-id user-email)
                     (rf/dispatch [:admin/view-user-details user-id]))

     :view-activity (fn [e]
                      (.stopPropagation e)
                      (log/info "Viewing user activity" user-id user-email)
                      (rf/dispatch [:admin/view-user-activity user-id]))

     :impersonate (fn []
                    (log/info "Impersonating user" user-id user-email)
                    (rf/dispatch [:admin/impersonate-user user-id]))

     :reset-password (fn []
                       (log/info "Resetting user password" user-id user-email)
                       (rf/dispatch [:admin/reset-user-password user-id]))

     :verify-email (fn []
                     (log/info "Force verifying email" user-id user-email)
                     (rf/dispatch [:admin/force-verify-email user-id]))

     :delete-user (fn []
                    (log/info "Deleting user" user-id user-email)
                    (rf/dispatch [:admin/delete-user user-id]))

     :update-role (shared-actions/create-update-handlers user-id user-email "user" :admin/update-user-role "role")}))

(defn create-user-confirmation-handlers
  "Factory function to create confirmation handlers that wrap action handlers"
  [handlers user-email]
  (merge
    (shared-actions/create-confirmation-handlers handlers user-email "user")
    {:impersonate (fn [e]
                    (.stopPropagation e)
                    (confirm-dialog/show-confirm
                      {:message (str "Impersonate user " user-email "? This will log you in as this user.")
                       :title "Confirm User Impersonation"
                       :on-confirm (:impersonate handlers)}))

     :reset-password (fn [e]
                       (.stopPropagation e)
                       (confirm-dialog/show-confirm
                         {:message (str "Reset password for " user-email "? They will receive a reset email.")
                          :title "Confirm Password Reset"
                          :on-confirm (:reset-password handlers)}))

     :verify-email (fn [e]
                     (.stopPropagation e)
                     (confirm-dialog/show-confirm
                       {:message (str "Force verify email for " user-email "?")
                        :title "Confirm Email Verification"
                        :on-confirm (:verify-email handlers)}))

     :delete-user (fn [e]
                    (.stopPropagation e)
                    (confirm-dialog/show-confirm
                      {:message (str "Permanently delete user " user-email "? This action cannot be undone and will remove all associated data.")
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
        user-email (shared-actions/get-entity-name user :user)
        user-role (or (:users/role user) (:role user))
        email-verified (or (:users/email-verified user) (:email-verified user))
        ;; Enum options from models-data (fallback keeps previous hardcoded values)
        models-data (use-subscribe [:models-data])
        role-options (->> (or (when models-data
                                (field-meta/get-enum-choices models-data :users :role))
                              ["admin" "member" "viewer" "unassigned"])
                          (keep #(when % (keyword (str %))))
                          vec)
        role-key (cond
                   (keyword? user-role) user-role
                   (string? user-role) (keyword user-role)
                   :else nil)

        ;; Local guard: immediately block delete for active admin
        role-str (cond
                   (keyword? user-role) (name user-role)
                   (string? user-role) user-role
                   :else (str user-role))
        status-str (cond
                     (keyword? user-status) (name user-status)
                     (string? user-status) user-status
                     :else (str user-status))
        local-admin-protection? (and (some? role-str)
                                  (some? status-str)
                                  (= "admin" role-str)
                                  (= "active" status-str))

        ;; Subscribe to loading states
        updating-user? (use-subscribe [:admin/updating-user?])
        impersonating-user? (use-subscribe [:admin/impersonating-user?])
        loading-user-details? (use-subscribe [:admin/loading-user-details?])

        ;; Create action handlers
        action-handlers (create-user-action-handlers user-id user-email)
        confirmation-handlers (create-user-confirmation-handlers action-handlers user-email)

        ;; Deletion constraints are now checked in batch at the page level

        ;; Define action groups for the dropdown using shared components
        action-groups (-> []
                           ;; View actions group
                        (conj (shared-actions/create-view-action-group
                                (:view-details action-handlers)
                                [{:id "view-activity"
                                  :icon "📊"
                                  :label "View Activity"
                                  :on-click (:view-activity action-handlers)}]))

                           ;; Status actions group
                        (conj (shared-actions/create-status-action-group
                                user-status
                                confirmation-handlers
                                :updating-user?))

                           ;; Role actions group
                        (conj (shared-actions/create-property-action-group
                                "Role"
                                role-key
                                role-options
                                (:update-role action-handlers)
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
                                  (= user-status "active")
                                  (conj {:id "impersonate-user"
                                         :icon "👤"
                                         :label "Impersonate User"
                                         :variant :info
                                         :loading-key :impersonating-user?
                                         :on-click (:impersonate confirmation-handlers)})

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
                                         :disabled? local-admin-protection?
                                         :tooltip (or (when local-admin-protection?
                                                        "Cannot delete active admin user")
                                                    "Delete this user")
                                         :on-click (:delete-user confirmation-handlers)}))))

                           ;; Filter out empty groups
                        (shared-actions/filter-empty-groups))

        ;; Loading states map
        loading-states {:updating-user? updating-user?
                        :impersonating-user? impersonating-user?
                        :loading-user-details? loading-user-details?}]

    ;; Return the action dropdown component - aligned to start like other buttons
    ($ :div {:class "flex items-center"}
      ($ dropdown/action-dropdown {:entity-id user-id
                                   :actions action-groups
                                   :loading-states loading-states
                                   :position :portal}))))
