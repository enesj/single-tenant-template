(ns app.domain.frontend.expenses.pages.user.article-aliases
  "Power-user view of article aliases.

  NOTE: Row action buttons are controlled by the table settings panel
  (Edit/Delete toggles)."
  (:require
    [app.domain.frontend.expenses.components.page-guard :refer [expenses-page-guard]]
    [app.domain.frontend.expenses.components.user-power-forms :refer [user-article-alias-edit-form-modal]]
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
  ($ user-article-alias-edit-form-modal
    {:item item
     :on-success on-success
     :on-cancel on-cancel}))

(defn- render-actions
  [t item]
  (let [article-alias-id (id-utils/extract-entity-id item)
        article-alias-id-str (some-> article-alias-id str)
        on-edit-click (:on-edit-click item)
        show-edit? (not (false? (:show-edit? item)))
        show-delete? (not (false? (:show-delete? item)))]
    ($ :div {:class "flex items-center justify-center gap-2"}
      (when show-edit?
        ($ button
          {:id (str "btn-edit-article-aliases-" article-alias-id-str)
           :btn-type :primary
           :shape "circle"
           :on-click (fn [e]
                       (.stopPropagation e)
                       (when on-edit-click
                         (on-edit-click (dissoc item :show-edit? :show-delete? :on-edit-click))))}
          ($ edit-icon)))

      (when show-delete?
        ($ button
          {:id (str "btn-delete-article-aliases-" article-alias-id-str)
           :btn-type :danger
           :shape "circle"
           :on-click (fn [e]
                       (.stopPropagation e)
                       (confirm-dialog/show-confirm
                         {:title (t :article-aliases/delete-title)
                          :message (t :article-aliases/delete-msg)
                          :on-confirm #(rf/dispatch [:user-expenses/delete-article-alias article-alias-id-str])
                          :on-cancel nil}))}
          ($ delete-icon))))))

(defui article-aliases-page []
  (let [t (use-t)
        entity-name :article-aliases
        entity-spec (use-subscribe [:entity-specs/by-name entity-name])
        refresh-list (use-callback
                       (fn []
                         (rf/dispatch [:user-expenses/refresh-article-aliases-list]))
                       [])]
    (use-effect
      (fn []
        (rf/dispatch [::list-ui-state-events/set-pagination-mode entity-name :server])
        (rf/dispatch [::list-ui-state-events/set-refresh-event entity-name [:user-expenses/refresh-article-aliases-list]])
        (refresh-list)
        js/undefined)
      [refresh-list])

    ($ expenses-page-guard
      {:capability :expenses/articles.manage
       :children
       ($ :div {:class "min-h-screen bg-base-100"}
         ($ :header {:class "bg-white border-b border-base-200"}
           ($ :div {:class "w-full px-4 py-4 sm:py-6"}
             ($ :div {:class "flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4"}
               ($ :div
                 ($ :h1 {:class "text-xl sm:text-2xl font-bold text-base-content"} (t :article-aliases/title))
                 ($ :p {:class "text-sm text-base-content/70"}
                   (t :article-aliases/subtitle)))
               ($ :div {:class "flex gap-2 flex-wrap"}
                 ($ button {:id "btn-back-expenses-dashboard-article-aliases"
                            :btn-type :ghost
                            :on-click #(rf/dispatch [:navigate-to "/expenses"])}
                   (t :article-aliases/btn-dashboard))
                 ($ button {:id "btn-go-unmapped-items-article-aliases"
                            :btn-type :primary
                            :on-click #(rf/dispatch [:navigate-to "/unmapped-items"])}
                   (t :article-aliases/btn-unmapped))))))

         ($ :main {:class "w-full px-4 py-6"}
           ($ list-view
             {:entity-name entity-name
              :entity-spec entity-spec
              :render-edit-form render-edit-form
              :render-actions (fn [item] (render-actions t item))})))})))

