(ns app.domain.frontend.expenses.pages.user.price-observations
  "Power-user view of price observations."
  (:require
    [app.domain.frontend.expenses.admin.components.detail-views.utils :as detail-utils]
    [app.domain.frontend.expenses.authz :as authz]
    [app.domain.frontend.expenses.components.page-guard :refer [expenses-page-guard]]
    [app.domain.frontend.expenses.components.user-power-forms :refer [user-price-observation-edit-form-modal]]
    [app.template.frontend.components.action-components :as action-components]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.confirm-dialog :as confirm-dialog]
    [app.template.frontend.components.dropdown.action :as dropdown]
    [app.template.frontend.components.icons :refer [delete-icon edit-icon]]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.components.modal-wrapper :refer [modal-wrapper]]
    [app.template.frontend.components.shared-utils :as shared]
    [app.template.frontend.subs.entity :as entity-subs]
    [app.template.frontend.utils.id :as id-utils]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-callback use-effect use-state]]
    [uix.re-frame :refer [use-subscribe]]
    app.domain.frontend.expenses.subs.user-expenses))

(defn- render-edit-form
  [item {:keys [on-success on-cancel]}]
  ($ user-price-observation-edit-form-modal
    {:item item
     :on-success on-success
     :on-cancel on-cancel}))

(defn- detail-header
  [{:keys [title subtitle icon]}]
  ($ shared/detail-modal-header
    {:title title
     :subtitle subtitle
     :icon icon
     :icon-bg "bg-primary/10"}))

(defui user-price-observation-detail-body
  [{:keys [observation observation-id]}]
  (let [obs (or observation {})
        observed-at (:observed-at obs)
        article-label (or (:article-canonical-name obs)
                        (:article-id obs)
                        "")
        supplier-label (or (:supplier-display-name obs)
                         (:supplier-id obs)
                         "")
        unit-price (:unit-price obs)
        qty (:qty obs)
        currency (:currency obs)
        id-value (or (:id obs) observation-id)]
    ($ :div {:class "space-y-3"}
      (cond
        (nil? observation)
        ($ :div {:class "ds-alert"}
          ($ :span "Price observation not found."))

        :else
        ($ :div {:class "space-y-3"}
          (detail-utils/label-value "Observed At" (shared/format-date observed-at))
          (detail-utils/label-value "Article" article-label)
          (detail-utils/label-value "Supplier" supplier-label)
          (detail-utils/label-value "Unit Price" unit-price)
          (detail-utils/label-value "Quantity" qty)
          (detail-utils/label-value "Currency" currency)
          (detail-utils/label-value "ID" (or id-value (some-> observation-id str))))))))

(comment
  "actions are defined inside `price-observations-page` so the View Details action can open the local details modal")

(defui price-observations-page []
  (let [role (use-subscribe [:expenses/user-role])
        can-manage? (authz/can? role :expenses/articles.manage)
        entity-name :price-observations
        entity-spec (use-subscribe [:entity-specs/by-name entity-name])
        [detail-observation set-detail-observation] (use-state nil)
        detail-observation-id (some-> detail-observation id-utils/extract-entity-id str)
        entities-by-id (use-subscribe [::entity-subs/entity-data entity-name])
        detail-observation-record (or (get entities-by-id detail-observation-id)
                                    detail-observation)
        refresh-list (use-callback
                       (fn []
                         (rf/dispatch [:user-expenses/fetch-price-observations]))
                       [])]
    (use-effect
      (fn []
        (refresh-list)
        js/undefined)
      [refresh-list])

    (let [render-actions
          (fn [item]
            (let [price-observation-id (id-utils/extract-entity-id item)
                  price-observation-id-str (some-> price-observation-id str)
                  on-edit-click (:on-edit-click item)
                  show-edit? (not (false? (:show-edit? item)))
                  show-delete? (not (false? (:show-delete? item)))
                  edit-disabled? (true? (:edit-disabled? item))
                  delete-disabled? (true? (:delete-disabled? item))
                  item-data (dissoc item :show-edit? :show-delete? :edit-disabled? :delete-disabled? :on-edit-click)]
              ($ :div {:class "flex items-center justify-center gap-2 flex-nowrap"}
                (when show-edit?
                  ($ button
                    {:id (str "btn-edit-price-observations-" price-observation-id-str)
                     :btn-type :primary
                     :shape "circle"
                     :disabled edit-disabled?
                     :on-click (fn [e]
                                 (.stopPropagation e)
                                 (when-not edit-disabled?
                                   (when on-edit-click
                                     (on-edit-click item-data))))}
                    ($ edit-icon)))

                (when show-delete?
                  ($ button
                    {:id (str "btn-delete-price-observations-" price-observation-id-str)
                     :btn-type :danger
                     :shape "circle"
                     :disabled delete-disabled?
                     :on-click (fn [e]
                                 (.stopPropagation e)
                                 (when-not delete-disabled?
                                   (confirm-dialog/show-confirm
                                     {:title "Delete price observation"
                                      :message "Do you want to delete this price observation?"
                                      :on-confirm #(rf/dispatch [:user-expenses/delete-price-observation price-observation-id-str])
                                      :on-cancel nil})))}
                    ($ delete-icon)))

                (when (seq price-observation-id-str)
                  (let [actions [{:group-title "View"
                                  :items [{:id "view-details"
                                           :icon ($ action-components/view-details-icon)
                                           :label "View Details"
                                           :on-click (fn [e]
                                                       (.stopPropagation e)
                                                       (set-detail-observation item-data))}]}]]
                    ($ dropdown/action-dropdown
                      {:entity-id price-observation-id-str
                       :actions actions
                       :position :portal}))))))]

      ($ expenses-page-guard
        {:capability :expenses/articles.manage
         :children
         ($ :div {:class "min-h-screen bg-base-100"}
           ($ :header {:class "bg-white border-b border-base-200"}
             ($ :div {:class "w-full px-4 py-4 sm:py-6"}
               ($ :div {:class "flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4"}
                 ($ :div
                   ($ :h1 {:class "text-xl sm:text-2xl font-bold text-base-content"} "Price Observations")
                   ($ :p {:class "text-sm text-base-content/70"}
                     "Power-user price history/observations"))
                 ($ :div {:class "flex gap-2 flex-wrap"}
                   ($ button {:id "btn-back-expenses-dashboard-price-observations"
                              :btn-type :ghost
                              :on-click #(rf/dispatch [:navigate-to "/expenses"])}
                     "Dashboard")))))

           (when detail-observation
             (let [observation-id detail-observation-id
                   article-label (or (:article-canonical-name detail-observation-record)
                                   (:article-id detail-observation-record))
                   observed-at (:observed-at detail-observation-record)
                   subtitle (or (when (and article-label observed-at)
                                  (str article-label " · " (shared/format-date observed-at)))
                              (when observation-id (str "Observation " observation-id))
                              "Price observation details")
                   close! (fn []
                            (set-detail-observation nil))]
               ($ modal-wrapper
                 {:id "user-price-observation-detail-modal"
                  :visible? true
                  :title "Price Observation Details"
                  :size :large
                  :width "640px"
                  :draggable? true
                  :resizable? true
                  :on-close close!
                  :close-button-id "btn-close-user-price-observation-detail-modal"}

                 (detail-header {:title "Price Observation Details"
                                 :subtitle subtitle
                                 :icon "P"})

                 ($ :div {:class "mt-4"}
                   ($ user-price-observation-detail-body
                     {:observation detail-observation-record
                      :observation-id observation-id})))))

           ($ :main {:class "w-full px-4 py-6"}
             ($ list-view
               {:entity-name entity-name
                :entity-spec entity-spec
                :title "Price Observations"
                :form-display :modal
                :disallowed-action-mode :disable
                :allow-add? false
                :allow-edit? can-manage?
                :allow-delete? can-manage?
                :render-edit-form render-edit-form
                :on-edit-success refresh-list
                :render-actions render-actions})))}))))

