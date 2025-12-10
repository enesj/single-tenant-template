(ns app.admin.frontend.components.admin-actions
  "Admin account management actions using shared components"
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

(defn create-admin-action-handlers
  "Factory function to create all action handlers for an admin"
  [admin-id admin-email]
  {:view-details (fn [e]
                   (.stopPropagation e)
                   (log/info "Viewing admin details" admin-id admin-email)
                   (rf/dispatch [:admin/view-admin-details admin-id]))

   :update-status-active (fn []
                           (log/info "Activating admin" admin-id admin-email)
                           (rf/dispatch [:admin/update-admin-status admin-id "active"]))

   :update-status-suspended (fn []
                              (log/info "Suspending admin" admin-id admin-email)
                              (rf/dispatch [:admin/update-admin-status admin-id "suspended"]))

   :delete-admin (fn []
                   (log/info "Deleting admin" admin-id admin-email)
                   (rf/dispatch [:admin/delete-admin admin-id]))

   :update-role (fn [new-role]
                  (log/info "Updating admin role" admin-id admin-email new-role)
                  (rf/dispatch [:admin/update-admin-role admin-id (name new-role)]))})

(defn create-admin-confirmation-handlers
  "Factory function to create confirmation handlers that wrap action handlers"
  [handlers admin-email]
  {:view-details (:view-details handlers)

   :update-status-active (fn [e]
                           (.stopPropagation e)
                           (confirm-dialog/show-confirm
                             {:message (str "Activate admin " admin-email "?")
                              :title "Confirm Admin Activation"
                              :on-confirm (:update-status-active handlers)}))

   :update-status-suspended (fn [e]
                              (.stopPropagation e)
                              (confirm-dialog/show-confirm
                                {:message (str "Suspend admin " admin-email "? They will lose access to the admin panel.")
                                 :title "Confirm Admin Suspension"
                                 :danger? true
                                 :on-confirm (:update-status-suspended handlers)}))

   :delete-admin (fn [e]
                   (.stopPropagation e)
                   (confirm-dialog/show-confirm
                     {:message (str "Permanently delete admin " admin-email "? This action cannot be undone.")
                      :title "Confirm Admin Deletion"
                      :danger? true
                      :on-confirm (:delete-admin handlers)}))

   :update-role (:update-role handlers)})

;; =============================================================================
;; Main Admin Actions Dropdown Component
;; =============================================================================

(defui admin-admin-actions
  "Admin account management actions using the reusable template dropdown component"
  [{:keys [admin]}]
  (let [admin-id (id-utils/extract-entity-id admin)
        ;; Extract admin data
        admin-status (or (:admins/status admin) (:status admin))
        admin-email (or (:admins/email admin) (:email admin))
        admin-role (or (:admins/role admin) (:role admin))

        ;; Get current logged-in admin
        current-admin (use-subscribe [:admin/current-admin])
        current-admin-id (or (:id current-admin) (:admins/id current-admin))
        current-admin-role (or (:role current-admin) (:admins/role current-admin))
        is-current-owner? (= (str current-admin-role) "owner")

        ;; Determine if this is self
        is-self? (= (str admin-id) (str current-admin-id))

        ;; Get owners count for protection checks
        owners-count (use-subscribe [:admin/owners-count])
        is-last-owner? (and (= (str admin-role) "owner") (= owners-count 1))

        ;; Enum options for role
        models-data (use-subscribe [:models-data])
        role-options (->> (or (when models-data
                                (field-meta/get-enum-choices models-data :admins :role))
                            ["admin" "support" "owner"])
                        (keep #(when % (keyword (str %))))
                        vec)
        role-key (cond
                   (keyword? admin-role) admin-role
                   (string? admin-role) (keyword admin-role)
                   :else nil)

        ;; Loading states
        updating-admin? (use-subscribe [:admin/updating-admin?])

        ;; Create action handlers
        action-handlers (create-admin-action-handlers admin-id admin-email)
        confirmation-handlers (create-admin-confirmation-handlers action-handlers admin-email)

        ;; Determine which actions should be disabled
        can-change-role? (and is-current-owner? (not is-self?) (not is-last-owner?))
        _can-change-status? (and is-current-owner? (not is-self?) (not is-last-owner?))
        can-delete? (and is-current-owner? (not is-self?) (not is-last-owner?))

        ;; Define action groups for the dropdown
        action-groups (-> []
                        ;; View actions group
                        (conj {:group-title "View"
                               :items [{:id "view-details"
                                        :icon "👁️"
                                        :label "View Details"
                                        :on-click (:view-details confirmation-handlers)}]})

                        ;; Status actions group (only show if owner and not self)
                        (cond-> is-current-owner?
                          (conj {:group-title "Status"
                                 :items (if (= (str admin-status) "active")
                                          [{:id "suspend"
                                            :icon "🚫"
                                            :label "Suspend Admin"
                                            :variant :warning
                                            :loading-key :updating-admin?
                                            :disabled? (or is-self? is-last-owner?)
                                            :tooltip (cond
                                                       is-self? "Cannot suspend yourself"
                                                       is-last-owner? "Cannot suspend the last owner"
                                                       :else "Suspend this admin")
                                            :on-click (:update-status-suspended confirmation-handlers)}]
                                          [{:id "activate"
                                            :icon "✅"
                                            :label "Activate Admin"
                                            :variant :success
                                            :loading-key :updating-admin?
                                            :on-click (:update-status-active confirmation-handlers)}])}))

                        ;; Role actions group (only show if owner)
                        (cond-> is-current-owner?
                          (conj {:group-title "Role"
                                 :items [{:id "change-role"
                                          :type :select
                                          :icon "👔"
                                          :label "Change Role"
                                          :current-value role-key
                                          :options role-options
                                          :loading-key :updating-admin?
                                          :disabled? (not can-change-role?)
                                          :tooltip (cond
                                                     is-self? "Cannot change your own role"
                                                     is-last-owner? "Cannot demote the last owner"
                                                     (not is-current-owner?) "Only owners can change roles"
                                                     :else "Change admin role")
                                          :on-change (:update-role confirmation-handlers)}]}))

                        ;; Dangerous actions group (only show if owner)
                        (cond-> is-current-owner?
                          (conj {:group-title "Danger Zone"
                                 :items [{:id "delete-admin"
                                          :icon "🗑️"
                                          :label "Delete Admin"
                                          :variant :error
                                          :loading-key :updating-admin?
                                          :disabled? (not can-delete?)
                                          :tooltip (cond
                                                     is-self? "Cannot delete yourself"
                                                     is-last-owner? "Cannot delete the last owner"
                                                     :else "Delete this admin")
                                          :on-click (:delete-admin confirmation-handlers)}]}))

                        ;; Filter out empty groups
                        (shared-actions/filter-empty-groups))

        ;; Loading states map
        loading-states {:updating-admin? updating-admin?}]

    ;; Return the action dropdown component
    ($ :div {:class "flex items-center"}
      ($ dropdown/action-dropdown {:entity-id admin-id
                                   :actions action-groups
                                   :loading-states loading-states
                                   :position :portal}))))
