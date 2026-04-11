(ns app.admin.frontend.pages.domain.expenses.receipts
  "Admin Receipts page.

  Renders an admin-native list backed by the expenses admin API."
  (:require
    [app.admin.frontend.components.receipt-approval-form :as receipt-approval-form]
    [app.admin.frontend.components.layout :as layout]
    app.admin.frontend.events.receipts-approval
    app.admin.frontend.events.receipts-detail
    app.admin.frontend.subs.receipts-detail
    [app.domain.frontend.expenses.components.receipt-detail-modal :as receipt-detail-ui]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.components.list.cells :as list-cells]
    [app.template.frontend.components.modal-wrapper :refer [modal-wrapper]]
    [app.template.frontend.components.shared-utils :as shared]
    [app.template.frontend.events.list.ui-state :as ui-state]
    [app.template.frontend.utils.id :as id-utils]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-callback use-effect use-state]]
    [uix.re-frame :refer [use-subscribe]]

    ;; Side-effect requires: register sync handlers, entity spec fallbacks, CRUD bridges,
    ;; and list-loading events.
    app.domain.frontend.expenses.admin.adapters.admin-crud
    app.domain.frontend.expenses.admin.adapters.specs
    app.domain.frontend.expenses.admin.adapters.sync
    [app.domain.frontend.expenses.events.receipts :as receipts-events]))

(defn dispatch-admin-receipts-refresh!
  [dispatch! dispatch-sync!]
  (dispatch-sync! [::ui-state/set-pagination-mode :receipts :server])
  (dispatch-sync! [::ui-state/set-refresh-event :receipts [::receipts-events/load-list]])
  (dispatch! [::receipts-events/load-list {:page 1}]))

(defn- build-receipt-detail-ctx
  [on-close]
  {:receipt-sub :admin/receipt-detail
   :receipt-detail-loading-sub :admin/receipt-detail-loading?
   :receipt-action-loading-sub :admin/receipt-action-loading?
   :receipts-error-sub :admin/receipt-detail-error
   :fetch-receipt-event :admin/fetch-receipt-detail
   :close-modal on-close
    :approve-form receipt-approval-form/receipt-approval-form
   :can-approve-sub :admin/authenticated?
   :approve-unavailable-message "Receipt editing is currently unavailable in the admin view."})

(defui admin-receipt-detail-modal
  [{:keys [open? receipt-id on-close]}]
  (let [receipt (use-subscribe [:admin/receipt-detail receipt-id])
        subtitle (or (:original-filename receipt)
                   (when receipt-id
                     (str "Receipt " receipt-id))
                   "Receipt details")
        header ($ shared/detail-modal-header
                 {:title "Receipt Details"
                  :subtitle subtitle
                  :icon "R"
                  :icon-bg "bg-primary/10"})]
    ($ modal-wrapper
      {:id "admin-receipt-detail-modal"
       :visible? (and open? (some? receipt-id))
       :on-close on-close
       :draggable? true
       :resizable? true
       :initial-position {:y 24}
       :width "960px"
       :backdrop-opacity 20
       :class "max-w-[95vw] max-h-[85vh] h-[85vh] flex flex-col"
       :header header
       :header-class "p-0 border-0 bg-transparent mb-3"
       :content-class "p-0 overflow-hidden min-h-0 flex flex-col"
       :close-button-id "btn-close-admin-receipt-detail-modal"}
      ($ :div {:class "flex flex-col flex-1 h-full min-h-0 overflow-hidden p-4"}
        ($ receipt-detail-ui/receipt-detail-body
          {:receipt-id receipt-id
           :ctx (build-receipt-detail-ctx on-close)})))))

(defn- render-receipt-actions
  [open-receipt-detail! receipt]
  ($ :div {:class "flex items-center gap-2"}
    (when (not (false? (:show-edit? receipt)))
      ($ list-cells/edit-button
        {:entity-name :receipts
         :item-id (id-utils/extract-entity-id receipt)
         :item receipt
         :disabled? (boolean (:edit-disabled? receipt))
         :on-edit-click open-receipt-detail!}))
    (when (not (false? (:show-delete? receipt)))
      ($ list-cells/delete-button
        {:entity-name :receipts
         :item-id (id-utils/extract-entity-id receipt)
         :disabled? (boolean (:delete-disabled? receipt))}))))

(defui admin-receipts-page
  "Admin route: /admin/receipts"
  []
  (let [entity-name :receipts
        entity-spec (use-subscribe [(keyword "entity-specs" (name entity-name))])
        [detail-open? set-detail-open!] (use-state false)
        [detail-receipt-id set-detail-receipt-id!] (use-state nil)
        refresh-list (use-callback
                       (fn []
                         (dispatch-admin-receipts-refresh! rf/dispatch rf/dispatch-sync))
                       [])
        open-receipt-detail! (use-callback
                               (fn [receipt]
                                 (when-let [receipt-id (some-> receipt
                                                         id-utils/extract-entity-id
                                                         str)]
                                   (set-detail-receipt-id! receipt-id)
                                   (set-detail-open! true)
                                   (rf/dispatch [:admin/fetch-receipt-detail receipt-id])))
                               [])
        close-receipt-detail! (use-callback
                                (fn []
                                  (set-detail-open! false)
                                  (set-detail-receipt-id! nil))
                                [])]
    (use-effect
      (fn []
        (refresh-list)
        js/undefined)
      [refresh-list])

    ($ layout/admin-layout
      ($ :div {:class "p-6 min-h-screen"}
        ($ list-view
          {:entity-name entity-name
           :entity-spec entity-spec
           :title "Receipts"
           :render-actions #(render-receipt-actions open-receipt-detail! %)})
        ($ admin-receipt-detail-modal
          {:open? detail-open?
           :receipt-id detail-receipt-id
           :on-close close-receipt-detail!})))))