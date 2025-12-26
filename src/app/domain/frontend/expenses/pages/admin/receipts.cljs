(ns app.domain.frontend.expenses.pages.admin.receipts
  (:require
    [app.admin.frontend.components.generic-admin-entity-page :refer [generic-admin-entity-page]]
    [app.domain.frontend.expenses.events.receipts :as receipts-events]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.subs.list :as list-subs]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defui batch-parse-button
  "Batch parse button shown when receipts are selected."
  []
  (let [selected-ids (use-subscribe [::list-subs/selected-ids :receipts])
        action-loading? (use-subscribe [:expenses/receipt-action-loading?])
        has-selection? (and (seq selected-ids) (pos? (count selected-ids)))]
    (when has-selection?
      ($ :div {:class "flex items-center gap-2"}
        ($ button
          {:id "btn-batch-parse-admin-receipts"
           :btn-type :primary
           :class "ds-btn-sm"
           :disabled action-loading?
           :on-click (fn [e]
                       (.stopPropagation e)
                       (rf/dispatch [::receipts-events/ocr-selected selected-ids]))}
          (if action-loading?
            ($ :span {:class "ds-loading ds-loading-spinner ds-loading-xs"})
            "🔍 Batch Parse (OCR)")
          (when-not action-loading?
            ($ :span {:class "ds-badge ds-badge-sm ml-1"}
              (count selected-ids))))))))

(defui admin-receipts-page []
  ($ generic-admin-entity-page
    {:children :receipts
     :components {:custom-header batch-parse-button}}))
