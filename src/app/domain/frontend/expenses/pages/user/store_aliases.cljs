(ns app.domain.frontend.expenses.pages.user.store-aliases
  "Power-user view of store aliases.

  Store aliases help normalize raw store labels into canonical Stores."
  (:require
    [app.domain.frontend.expenses.components.page-guard :refer [expenses-page-guard]]
    [app.domain.frontend.expenses.components.user-power-forms :refer [user-store-alias-edit-form-modal]]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.confirm-dialog :as confirm-dialog]
    [app.template.frontend.components.icons :refer [delete-icon edit-icon]]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.events.list.ui-state :as list-ui-state-events]
    [app.template.frontend.i18n :refer [use-t]]
    [app.template.frontend.utils.id :as id-utils]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-callback use-effect]]
    [uix.re-frame :refer [use-subscribe]]
    app.domain.frontend.expenses.subs.user-expenses))

(defn- render-edit-form
  [item {:keys [on-success on-cancel]}]
  ($ user-store-alias-edit-form-modal
    {:item item
     :on-success on-success
     :on-cancel on-cancel}))

(defn- render-actions
  [t item]
  (let [store-alias-id (id-utils/extract-entity-id item)
        store-alias-id-str (some-> store-alias-id str)
        on-edit-click (:on-edit-click item)
        show-edit? (not (false? (:show-edit? item)))
        show-delete? (not (false? (:show-delete? item)))
        edit-disabled? (true? (:edit-disabled? item))
        delete-disabled? (true? (:delete-disabled? item))
        item-data (dissoc item :show-edit? :show-delete? :edit-disabled? :delete-disabled? :on-edit-click)]
    ($ :div {:class "flex items-center justify-center gap-2"}
      (when show-edit?
        ($ button
          {:id (str "btn-edit-store-aliases-" store-alias-id-str)
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
          {:id (str "btn-delete-store-aliases-" store-alias-id-str)
           :btn-type :danger
           :shape "circle"
           :disabled delete-disabled?
           :on-click (fn [e]
                       (.stopPropagation e)
                       (when-not delete-disabled?
                         (confirm-dialog/show-confirm
                           {:title (t :store-aliases/delete-title)
                            :message (t :store-aliases/delete-msg)
                            :on-confirm #(rf/dispatch [:user-expenses/delete-store-alias store-alias-id-str])
                            :on-cancel nil})))}
          ($ delete-icon))))))

(defui store-aliases-page
  []
  (let [t (use-t)
        entity-name :store-aliases
        entity-spec (use-subscribe [:entity-specs/by-name entity-name])
        refresh-aliases (use-callback
                          (fn []
                            (rf/dispatch [:user-expenses/refresh-store-aliases-list]))
                          [])
        refresh-stores (use-callback
                         (fn []
                           (rf/dispatch [:user-expenses/fetch-stores]))
                         [])]
    (use-effect
      (fn []
        (rf/dispatch [::list-ui-state-events/set-pagination-mode entity-name :server])
        (rf/dispatch [::list-ui-state-events/set-refresh-event entity-name [:user-expenses/refresh-store-aliases-list]])
        (refresh-stores)
        (refresh-aliases)
        js/undefined)
      [refresh-stores refresh-aliases])

    ($ expenses-page-guard
      {:capability :expenses/store-aliases.manage
       :children
       ($ :div {:class "min-h-screen bg-base-100"}
         ($ :header {:class "bg-white border-b border-base-200"}
           ($ :div {:class "w-full px-4 py-4 sm:py-6"}
             ($ :div {:class "flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4"}
               ($ :div
                 ($ :h1 {:class "text-xl sm:text-2xl font-bold text-base-content"}
                   (t :store-aliases/title))
                 ($ :p {:class "text-sm text-base-content/70"}
                   (t :store-aliases/subtitle)))
               ($ :div {:class "flex gap-2 flex-wrap"}
                 ($ button {:id "btn-back-expenses-dashboard-store-aliases"
                            :btn-type :ghost
                            :on-click #(rf/dispatch [:navigate-to "/expenses"])}
                   (t :store-aliases/btn-dashboard))
                 ($ button {:id "btn-go-stores-store-aliases"
                            :btn-type :primary
                            :on-click #(rf/dispatch [:navigate-to "/stores"])}
                   (t :store-aliases/btn-stores))))))

         ($ :main {:class "w-full px-4 py-6"}
           ($ list-view
             {:entity-name entity-name
              :entity-spec entity-spec
              :render-edit-form render-edit-form
              :on-edit-success refresh-aliases
              :render-actions (fn [item] (render-actions t item))
              :on-batch-delete (fn [ids]
                                 (rf/dispatch [:user-expenses/batch-delete-store-aliases ids]))})))})))
