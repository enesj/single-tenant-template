(ns app.domain.frontend.expenses.pages.user.manufacturer-aliases
  "Power-user view of manufacturer aliases.

  NOTE: Row action buttons are controlled by the table settings panel
  (Edit/Delete toggles)."
  (:require
    [app.domain.frontend.expenses.components.page-guard :refer [expenses-page-guard]]
    [app.domain.frontend.expenses.components.user-power-forms :refer [user-manufacturer-alias-edit-form-modal]]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.confirm-dialog :as confirm-dialog]
    [app.template.frontend.components.icons :refer [delete-icon edit-icon]]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.utils.id :as id-utils]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-callback use-effect]]
    [uix.re-frame :refer [use-subscribe]]
    app.domain.frontend.expenses.subs.user-expenses))

(defn- render-edit-form
  [item {:keys [on-success on-cancel]}]
  ($ user-manufacturer-alias-edit-form-modal
    {:item item
     :on-success on-success
     :on-cancel on-cancel}))

(defn- render-actions
  [item]
  (let [alias-id (id-utils/extract-entity-id item)
        alias-id-str (some-> alias-id str)
        on-edit-click (:on-edit-click item)
        show-edit? (not (false? (:show-edit? item)))
        show-delete? (not (false? (:show-delete? item)))]
    ($ :div {:class "flex items-center justify-center gap-2"}
      (when show-edit?
        ($ button
          {:id (str "btn-edit-manufacturer-aliases-" alias-id-str)
           :btn-type :primary
           :shape "circle"
           :on-click (fn [e]
                       (.stopPropagation e)
                       (when on-edit-click
                         (on-edit-click (dissoc item :show-edit? :show-delete? :on-edit-click))))}
          ($ edit-icon)))

      (when show-delete?
        ($ button
          {:id (str "btn-delete-manufacturer-aliases-" alias-id-str)
           :btn-type :danger
           :shape "circle"
           :on-click (fn [e]
                       (.stopPropagation e)
                       (confirm-dialog/show-confirm
                         {:title "Delete manufacturer alias"
                          :message "Do you want to delete this manufacturer alias?"
                          :on-confirm #(rf/dispatch [:user-expenses/delete-manufacturer-alias alias-id-str])
                          :on-cancel nil}))}
          ($ delete-icon))))))

(defui manufacturer-aliases-page []
  (let [entity-name :manufacturer-aliases
        entity-spec (use-subscribe [:entity-specs/by-name entity-name])
        refresh-list (use-callback
                       (fn []
                         (rf/dispatch [:user-expenses/fetch-manufacturer-aliases]))
                       [])]
    (use-effect
      (fn []
        (refresh-list)
        js/undefined)
      [refresh-list])

    ($ expenses-page-guard
      {:capability :expenses/manufacturer-aliases.manage
       :children
       ($ :div {:class "min-h-screen bg-base-100"}
         ($ :header {:class "bg-white border-b border-base-200"}
           ($ :div {:class "w-full px-4 py-4 sm:py-6"}
             ($ :div {:class "flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4"}
               ($ :div
                 ($ :h1 {:class "text-xl sm:text-2xl font-bold text-base-content"} "Manufacturer Aliases")
                 ($ :p {:class "text-sm text-base-content/70"}
                   "Power-user alias catalog for manufacturer normalization"))
               ($ :div {:class "flex gap-2 flex-wrap"}
                 ($ button {:id "btn-back-expenses-dashboard-manufacturer-aliases"
                            :btn-type :ghost
                            :on-click #(rf/dispatch [:navigate-to "/expenses"])}
                   "Dashboard")
                 ($ button {:id "btn-go-manufacturers-manufacturer-aliases"
                            :btn-type :primary
                            :on-click #(rf/dispatch [:navigate-to "/manufacturers"])}
                   "Manufacturers")))))

         ($ :main {:class "w-full px-4 py-6"}
           ($ list-view
             {:entity-name entity-name
              :entity-spec entity-spec
              :title "Manufacturer Aliases"
              :form-display :modal
              :allow-add? false
              :render-edit-form render-edit-form
              :render-actions render-actions})))})))
