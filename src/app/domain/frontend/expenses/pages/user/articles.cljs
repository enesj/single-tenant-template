(ns app.domain.frontend.expenses.pages.user.articles
  "Power-user articles list (used for mapping/aliases workflows)."
  (:require
    [app.domain.frontend.expenses.admin.components.detail-views.utils :as detail-utils]
    [app.domain.frontend.expenses.authz :as authz]
    [app.domain.frontend.expenses.components.page-guard :refer [expenses-page-guard]]
    [app.domain.frontend.expenses.components.user-power-forms :refer [user-article-add-form-modal
                                                                      user-article-edit-form-modal]]
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

(defn- render-add-form
  [{:keys [on-success on-cancel]}]
  ($ user-article-add-form-modal
    {:on-success on-success
     :on-cancel on-cancel}))

(defn- render-edit-form
  [item {:keys [on-success on-cancel]}]
  ($ user-article-edit-form-modal
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

(defui user-article-detail-body
  [{:keys [article]}]
  (let [canonical-name (:canonical-name article)
        normalized-key (:normalized-key article)
        manufacturer (:manufacturer article)
        link (:link article)
        created-at (:created-at article)
        updated-at (:updated-at article)
        id-value (:id article)]
    ($ :div {:class "space-y-3"}
      (cond
        (nil? article)
        ($ :div {:class "ds-alert"}
          ($ :span "Article not found."))

        :else
        ($ :div {:class "space-y-3"}
          (detail-utils/label-value "Canonical Name" canonical-name)
          (detail-utils/label-value "Normalized Key" normalized-key)
          (detail-utils/label-value "Manufacturer" manufacturer)
          (detail-utils/label-value "Link" link)
          (detail-utils/label-value "Created At" (shared/format-date created-at))
          (detail-utils/label-value "Updated At" (shared/format-date updated-at))
          (detail-utils/label-value "ID" id-value))))))

(comment
  "actions are defined inside `articles-page` so the View Details action can open the local details modal")

(defui articles-page []
  (let [role (use-subscribe [:expenses/user-role])
        can-manage? (authz/can? role :expenses/articles.manage)
        entity-name :articles
        entity-spec (use-subscribe [:entity-specs/by-name entity-name])
        [detail-article set-detail-article] (use-state nil)
        detail-article-id (some-> detail-article id-utils/extract-entity-id)
        entities-by-id (use-subscribe [::entity-subs/entity-data entity-name])
        detail-article-record (get entities-by-id detail-article-id)
        refresh-list (use-callback
                       (fn []
                         (rf/dispatch [:user-expenses/fetch-articles]))
                       [])]
    (use-effect
      (fn []
        (refresh-list)
        js/undefined)
      [refresh-list])

    (let [render-actions
          (fn [item]
            (let [article-id (id-utils/extract-entity-id item)
                  article-id-str (some-> article-id str)
                  on-edit-click (:on-edit-click item)
                  show-edit? (not (false? (:show-edit? item)))
                  show-delete? (not (false? (:show-delete? item)))
                  edit-disabled? (true? (:edit-disabled? item))
                  delete-disabled? (true? (:delete-disabled? item))
                  item-data (dissoc item :show-edit? :show-delete? :edit-disabled? :delete-disabled? :on-edit-click)]
              ($ :div {:class "flex items-center justify-center gap-2 flex-nowrap"}
                (when show-edit?
                  ($ button
                    {:id (str "btn-edit-articles-" article-id-str)
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
                    {:id (str "btn-delete-articles-" article-id-str)
                     :btn-type :danger
                     :shape "circle"
                     :disabled delete-disabled?
                     :on-click (fn [e]
                                 (.stopPropagation e)
                                 (when-not delete-disabled?
                                   (confirm-dialog/show-confirm
                                     {:title "Delete article"
                                      :message "Do you want to delete this article?"
                                      :on-confirm #(rf/dispatch [:user-expenses/delete-article article-id-str])
                                      :on-cancel nil})))}
                    ($ delete-icon)))

                (when (seq article-id-str)
                  (let [actions [{:group-title "View"
                                  :items [{:id "view-details"
                                           :icon ($ action-components/view-details-icon)
                                           :label "View Details"
                                           :on-click (fn [e]
                                                       (.stopPropagation e)
                                                       (set-detail-article item-data))}]}]]
                    ($ dropdown/action-dropdown
                      {:entity-id article-id-str
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
                   ($ :h1 {:class "text-xl sm:text-2xl font-bold text-base-content"} "Articles")
                   ($ :p {:class "text-sm text-base-content/70"}
                     "Power-user article catalog (used for mapping and aliases)"))
                 ($ :div {:class "flex gap-2 flex-wrap"}
                   ($ button {:id "btn-back-expenses-dashboard-articles"
                              :btn-type :ghost
                              :on-click #(rf/dispatch [:navigate-to "/expenses"])}
                     "Dashboard")
                   ($ button {:id "btn-go-unmapped-items-articles"
                              :btn-type :primary
                              :on-click #(rf/dispatch [:navigate-to "/unmapped-items"])}
                     "Unmapped Aliases")))))

           (when detail-article
             (let [article-id detail-article-id
                   canonical-name (:canonical-name detail-article-record)
                   subtitle (or canonical-name
                              (when article-id (str "Article " article-id))
                              "Article details")
                   close! (fn []
                            (set-detail-article nil))]
               ($ modal-wrapper
                 {:id "user-article-detail-modal"
                  :visible? true
                  :title "Article Details"
                  :size :large
                  :width "640px"
                  :draggable? true
                  :resizable? true
                  :on-close close!
                  :close-button-id "btn-close-user-article-detail-modal"}

                 (detail-header {:title "Article Details"
                                 :subtitle subtitle
                                 :icon "A"})

                 ($ :div {:class "mt-4"}
                   ($ user-article-detail-body
                     {:article detail-article-record})))))

           ($ :main {:class "w-full px-4 py-6"}
             ($ list-view
               {:entity-name entity-name
                :entity-spec entity-spec
                :title "Articles"
                :form-display :modal
                :disallowed-action-mode :disable
                :allow-add? can-manage?
                :allow-edit? can-manage?
                :allow-delete? can-manage?
                :render-add-form render-add-form
                :render-edit-form render-edit-form
                :on-add-success refresh-list
                :on-edit-success refresh-list
                :render-actions render-actions})))}))))
