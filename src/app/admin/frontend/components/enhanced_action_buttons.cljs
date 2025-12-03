(ns app.admin.frontend.components.enhanced-action-buttons
  "Simplified action buttons for single-tenant admin tables."
  (:require
    [app.frontend.utils.id :as id-utils]
    [app.shared.frontend.components.action-buttons :refer [action-buttons]]
    [app.shared.keywords :as kw]
    [app.template.frontend.components.confirm-dialog :as confirm-dialog]
    [app.template.frontend.components.icons :refer [delete-icon edit-icon]]
    [app.template.frontend.events.form :as form-events]
    [app.template.frontend.events.list.crud :as crud-events]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]))

(defn- protected-admin? [entity-name role status]
  (and (= entity-name :users)
       (= "admin" (str role))
       (= "active" (str status))))

(defui enhanced-action-buttons
  "Action buttons with only local protections (no remote deletion constraints)."
  [{:keys [entity-name item show-edit? show-delete? custom-actions]
    :or {show-edit? true show-delete? true}}]
  (let [edit-icon-el ($ edit-icon)
        delete-icon-el ($ delete-icon)
        entity-name-kw (kw/ensure-keyword entity-name)
        entity-name-lower (or (kw/lower-name entity-name) "entity")
        item-id (id-utils/extract-entity-id item)
        role (or (:users/role item) (:role item))
        status (or (:users/status item) (:status item))
        local-admin-protection? (protected-admin? entity-name role status)
        delete-disabled? local-admin-protection?
        delete-tooltip (or (when local-admin-protection?
                             "Cannot delete active admin user")
                           "Delete this record")
        ;; Route user deletions through the dedicated admin endpoint to ensure
        ;; admin authentication and audit logging are applied correctly.
        ;; Route admin deletions through the admin-specific endpoint.
        handle-delete-confirm #(cond
                                 (= entity-name-kw :users)
                                 (rf/dispatch [:admin/delete-user item-id])
                                 (= entity-name-kw :admins)
                                 (rf/dispatch [:admin/delete-admin item-id])
                                 :else
                                 (rf/dispatch [::crud-events/delete-entity entity-name-kw item-id]))
        handle-delete-click (fn [e]
                              (.stopPropagation e)
                              (if delete-disabled?
                                (confirm-dialog/show-confirm
                                  {:message delete-tooltip
                                   :title "Cannot Delete"
                                   :show-cancel? false
                                   :confirm-text "OK"})
                                (confirm-dialog/show-confirm
                                  {:message "Do you want to delete this record?"
                                   :title "Confirm Delete"
                                   :on-confirm handle-delete-confirm
                                   :on-cancel nil})))
        handle-edit-click (fn [e]
                            (.stopPropagation e)
                            (when entity-name-kw
                              (rf/dispatch [::crud-events/clear-error entity-name-kw])
                              (rf/dispatch [::form-events/clear-form-errors entity-name-kw]))
                            (rf/dispatch [:app.template.frontend.events.config/set-editing item-id]))]
    ($ action-buttons
      {:item item
       :custom-actions custom-actions
       :edit (when show-edit?
               {:id (str "btn-edit-" entity-name-lower "-" item-id)
                :shape "circle"
                :icon edit-icon-el
                :on-click handle-edit-click})
       :delete (when show-delete?
                 {:id (str "btn-delete-" entity-name-lower "-" item-id)
                  :class (str "ds-btn-circle "
                              (when delete-disabled?
                                "opacity-50 cursor-not-allowed pointer-events-none"))
                  :disabled? delete-disabled?
                  :aria-disabled? delete-disabled?
                  :tooltip delete-tooltip
                  :icon delete-icon-el
                  :on-click handle-delete-click})})))
