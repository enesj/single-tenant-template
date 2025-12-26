(ns app.domain.frontend.expenses.admin.components.entity-actions
  (:require
    [app.domain.frontend.expenses.events.receipts :as receipts-events]
    [app.template.frontend.components.action-components :refer [view-details-icon]]
    [app.template.frontend.components.dropdown :as dropdown]
    [app.template.frontend.utils.id :as id-utils]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]))

(defn- open-detail-modal!
  [entity-segment item-id]
  (let [event-key (keyword (str "app.domain.frontend.expenses.events." entity-segment)
                    "open-detail-modal")]
    (rf/dispatch [event-key item-id])))

(defn- view-detail-actions
  [entity-segment item]
  (let [item-id (id-utils/extract-entity-id item)]
    [{:group-title "View"
      :items [{:id "view-details"
               :icon ($ view-details-icon)
               :label "View Details"
               :on-click (fn [e]
                           (.stopPropagation e)
                           (open-detail-modal! entity-segment item-id))}]}]))

(defn- render-actions-dropdown
  [entity-segment item]
  (let [item-id (id-utils/extract-entity-id item)]
    ($ dropdown/action-dropdown
      {:entity-id item-id
       :actions (view-detail-actions entity-segment item)
       :position :portal})))

(defui admin-suppliers-actions
  [{:keys [suppliers]}]
  (when suppliers
    (render-actions-dropdown "suppliers" suppliers)))

(defui admin-articles-actions
  [{:keys [articles]}]
  (when articles
    (render-actions-dropdown "articles" articles)))

(defui admin-payers-actions
  [{:keys [payers]}]
  (when payers
    (render-actions-dropdown "payers" payers)))

(defn- receipt-ocr-allowed?
  "Check if OCR action should be shown for a receipt.
  Returns true for statuses where OCR makes sense."
  [receipt]
  (let [status (or (:status receipt) (:receipts/status receipt))]
    (contains? #{"uploaded" "failed" "review_required" "extracted" "parsing" "parsed" "extracting"}
      status)))

(defn- receipts-actions
  "Action groups for receipts including View and OCR."
  [receipt]
  (let [item-id (id-utils/extract-entity-id receipt)
        ocr-allowed? (receipt-ocr-allowed? receipt)]
    (cond-> [{:group-title "View"
              :items [{:id "view-details"
                       :icon ($ view-details-icon)
                       :label "View Details"
                       :on-click (fn [e]
                                   (.stopPropagation e)
                                   (open-detail-modal! "receipts" item-id))}]}]
      ;; Add OCR group when allowed
      ocr-allowed?
      (conj {:group-title "OCR"
             :items [{:id "parse-ocr"
                      :icon "🔍"
                      :label "Parse (OCR)"
                      :tooltip "Run OCR to extract receipt data. Status will update asynchronously."
                      :on-click (fn [e]
                                  (.stopPropagation e)
                                  (rf/dispatch [::receipts-events/ocr-receipt item-id]))}]}))))

(defui admin-receipts-actions
  [{:keys [receipts]}]
  (when receipts
    (let [item-id (id-utils/extract-entity-id receipts)]
      ($ dropdown/action-dropdown
        {:entity-id item-id
         :actions (receipts-actions receipts)
         :position :portal}))))

(defui admin-article-aliases-actions
  [{:keys [article-aliases]}]
  (when article-aliases
    (render-actions-dropdown "article-aliases" article-aliases)))

(defui admin-price-observations-actions
  [{:keys [price-observations]}]
  (when price-observations
    (render-actions-dropdown "price-observations" price-observations)))

(defui admin-expense-items-actions
  [{:keys [expense-items]}]
  (when expense-items
    (render-actions-dropdown "expense-items" expense-items)))
