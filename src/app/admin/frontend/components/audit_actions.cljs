(ns app.admin.frontend.components.audit-actions
  "Refactored audit log admin actions using shared components"
  (:require
    [app.template.frontend.components.action-components :as shared-actions]
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

(defn create-audit-action-handlers
  "Factory function to create all action handlers for an audit log"
  [audit-log]
  (let [{:keys [id admin-email action entity-type created-at]} audit-log]
    {:view-details (fn [e]
                     (.stopPropagation e)
                     (log/info "Viewing audit log details:" id)
                     (rf/dispatch [:admin/show-audit-details audit-log]))

     :filter-by-admin (fn [e]
                        (.stopPropagation e)
                        (log/info "Filtering audit logs by admin:" admin-email)
                        (rf/dispatch [:admin/apply-audit-filter :admin-email admin-email]))

     :filter-by-action (fn [e]
                         (.stopPropagation e)
                         (log/info "Filtering audit logs by action:" action)
                         (rf/dispatch [:admin/apply-audit-filter :action action]))

     :filter-by-entity (fn [e]
                         (.stopPropagation e)
                         (log/info "Filtering audit logs by entity type:" entity-type)
                         (rf/dispatch [:admin/apply-audit-filter :entity-type entity-type]))

     :export-single (fn [e]
                      (.stopPropagation e)
                      (log/info "Exporting single audit log:" id)
                      (rf/dispatch [:admin/export-single-audit-log audit-log]))

     :delete-log (fn [e]
                   (.stopPropagation e)
                   (log/info "Delete audit log action triggered:" id)
                   (confirm-dialog/show-confirm
                     {:message (str "Are you sure you want to delete this audit log entry?\n\n"
                                 "Admin: " admin-email "\n"
                                 "Action: " action "\n"
                                 "Date: " (when created-at (.toLocaleString (js/Date. created-at))) "\n\n"
                                 "This action cannot be undone and may affect compliance requirements.")
                      :title "Confirm Delete Audit Log"
                      :confirm-text "Delete"
                      :cancel-text "Cancel"
                      :on-confirm #(rf/dispatch [:admin/delete-audit-log id])}))}))

;; =============================================================================
;; Main Audit Actions Dropdown Component
;; =============================================================================

(defui admin-audit-actions
  "Enhanced admin audit actions using shared components"
  [{:keys [audit-log]}]
  (let [audit-id (:id audit-log)
        admin-email (:admin-email audit-log)
        action (:action audit-log)
        entity-type (:entity-type audit-log)

        ;; Subscribe to loading states
        loading-audit-details? (use-subscribe [:admin/loading-audit-details?])
        exporting-audit? (use-subscribe [:admin/exporting-audit?])
        deleting-audit? (use-subscribe [:admin/deleting-audit?])

        ;; Create action handlers
        action-handlers (create-audit-action-handlers audit-log)

        ;; Define action groups for the dropdown using shared components
        action-groups (-> []
                           ;; View & Details group
                        (conj {:group-title "View & Details"
                               :items [{:id "view-details"
                                        :icon ($ shared-actions/view-details-icon)
                                        :label "View Details"
                                        :description "See full audit log information"
                                        :loading-key :loading-audit-details?
                                        :on-click (:view-details action-handlers)}]})

                           ;; Filters group
                        (conj (when (or admin-email action entity-type)
                                {:group-title "Filters"
                                 :items (cond-> []
                                          admin-email
                                          (conj {:id "filter-by-admin"
                                                 :icon ($ shared-actions/filter-icon)
                                                 :label "Filter by Admin"
                                                 :description (str "Show logs from " admin-email)
                                                 :on-click (:filter-by-admin action-handlers)})

                                          action
                                          (conj {:id "filter-by-action"
                                                 :icon ($ shared-actions/filter-icon)
                                                 :label "Filter by Action"
                                                 :description (str "Show " action " actions")
                                                 :on-click (:filter-by-action action-handlers)})

                                          entity-type
                                          (conj {:id "filter-by-entity"
                                                 :icon ($ shared-actions/filter-icon)
                                                 :label "Filter by Entity"
                                                 :description (str "Show " entity-type " actions")
                                                 :on-click (:filter-by-entity action-handlers)}))}))

                           ;; Export group
                        (conj {:group-title "Export"
                               :items [{:id "export-single"
                                        :icon ($ shared-actions/export-icon)
                                        :label "Export Entry"
                                        :description "Download as JSON"
                                        :loading-key :exporting-audit?
                                        :on-click (:export-single action-handlers)}]})

                           ;; Dangerous Actions group
                        (conj (shared-actions/create-dangerous-action-group
                                [{:id "delete-log"
                                  :icon ($ shared-actions/delete-icon)
                                  :label "Delete Entry"
                                  :description "Permanently remove this audit log"
                                  :loading-key :deleting-audit?
                                  :on-click (:delete-log action-handlers)}]))

                           ;; Filter out empty groups
                        (shared-actions/filter-empty-groups))

        ;; Loading states map
        loading-states {:loading-audit-details? loading-audit-details?
                        :exporting-audit? exporting-audit?
                        :deleting-audit? deleting-audit?}]

    ;; Return the action dropdown component
    ($ :div {:class "flex items-center justify-center"}
      ($ dropdown/action-dropdown {:entity-id audit-id
                                   :trigger-label "⋯"
                                   :actions action-groups
                                   :loading-states loading-states
                                   :position :portal}))))
