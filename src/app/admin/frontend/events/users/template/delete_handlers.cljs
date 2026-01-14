(ns app.admin.frontend.events.users.template.delete-handlers
  "Template delete operation overrides for admin context"
  (:require
    [re-frame.core :as rf]))

;; ============================================================================
;; Template Delete Success Override
;; ============================================================================

;; NOTE: Delete success handler moved to audit.cljs adapter
;; which provides comprehensive handling for all entity types including audit logs

;; ============================================================================
;; Delete Confirmation and UI State
;; ============================================================================

(rf/reg-event-db
  :admin/hide-delete-confirmation
  (fn [db _]
    (-> db
      (dissoc :admin/delete-confirmation-visible?)
      (dissoc :admin/delete-confirmation-entity))))

(rf/reg-sub
  :admin/delete-confirmation-visible?
  (fn [db _]
    (:admin/delete-confirmation-visible? db false)))

(rf/reg-sub
  :admin/delete-confirmation-entity
  (fn [db _]
    (:admin/delete-confirmation-entity db)))
