(ns app.domain.frontend.expenses.pages.user.payers
  "User-facing payers list (shared catalog)."
  (:require
    [app.domain.frontend.expenses.components.user-reference-forms :refer [user-payer-add-form-modal user-payer-edit-form-modal]]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.confirm-dialog :as confirm-dialog]
    [app.template.frontend.components.icons :refer [delete-icon edit-icon]]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.utils.id :as id-utils]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-callback use-effect]]
    [uix.re-frame :refer [use-subscribe]]
    app.domain.frontend.expenses.subs.user-expenses))

(defn- normalize-role
  [role]
  (cond
    (keyword? role) (name role)
    (string? role) role
    :else nil))

(defn- render-edit-form
  [item {:keys [on-success on-cancel]}]
  (let [payer-id (id-utils/extract-entity-id item)
        initial-data (dissoc item :show-edit? :show-delete? :on-edit-click)]
    ($ user-payer-edit-form-modal
      {:payer-id payer-id
       :initial-data initial-data
       :on-success on-success
       :on-cancel on-cancel})))

(defn- render-add-form
  [{:keys [on-success on-cancel]}]
  ($ user-payer-add-form-modal
    {:on-success on-success
     :on-cancel on-cancel}))

(defui payers-page []
  (let [role (normalize-role (use-subscribe [:user-role]))
        can-modify? (contains? #{"member" "admin"} role)
        entity-name :payers
        entity-spec (use-subscribe [:entity-specs/by-name entity-name])
        refresh-list (use-callback
                       (fn []
                         (rf/dispatch [:user-expenses/fetch-payers]))
                       [])]

    (use-effect
      (fn []
        (refresh-list)
        js/undefined)
      [refresh-list])

    (let [render-actions
          (fn [item]
            (let [payer-id (id-utils/extract-entity-id item)
                  payer-id-str (some-> payer-id str)
                  on-edit-click (:on-edit-click item)
                  show-edit? (and can-modify? (not (false? (:show-edit? item))))
                  show-delete? (and can-modify? (not (false? (:show-delete? item))))
                  item-data (dissoc item :show-edit? :show-delete? :on-edit-click)]
              ($ :div {:class "flex items-center justify-center gap-2"}
                (when show-edit?
                  ($ button
                    {:id (str "btn-edit-payers-" payer-id-str)
                     :btn-type :primary
                     :shape "circle"
                     :on-click (fn [e]
                                 (.stopPropagation e)
                                 (when on-edit-click
                                   (on-edit-click item-data)))}
                    ($ edit-icon)))

                (when show-delete?
                  ($ button
                    {:id (str "btn-delete-payers-" payer-id-str)
                     :btn-type :danger
                     :shape "circle"
                     :on-click (fn [e]
                                 (.stopPropagation e)
                                 (confirm-dialog/show-confirm
                                   {:title "Delete payer"
                                    :message "Do you want to delete this payer?"
                                    :on-confirm #(rf/dispatch [:user-expenses/delete-payer payer-id-str])
                                    :on-cancel nil}))}
                    ($ delete-icon))))))]

      ($ :div {:class "min-h-screen bg-base-100"}
        ($ :header {:class "bg-white border-b border-base-200"}
          ($ :div {:class "w-full px-4 py-4 sm:py-6"}
            ($ :div {:class "flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4"}
              ($ :div
                ($ :h1 {:class "text-xl sm:text-2xl font-bold text-base-content"} "Payers")
                ($ :p {:class "text-sm text-base-content/70"}
                  "Shared payment methods for your household"))
              ($ :div {:class "flex gap-2"}
                ($ button {:id "btn-back-expenses-dashboard-payers"
                           :btn-type :ghost
                           :on-click #(rf/dispatch [:navigate-to "/expenses"])}
                  "Dashboard")))))

        (when (= role "viewer")
          ($ :div {:class "w-full px-4 mt-4"}
            ($ :div {:class "ds-alert"}
              ($ :span "Read-only access. Ask a household member to update payers."))))

        ($ :main {:class "w-full px-4 py-6"}
          ($ list-view
            {:entity-name entity-name
             :entity-spec entity-spec
             :title "Payers"
             :form-display :modal
             :allow-add? can-modify?
             :render-add-form render-add-form
             :render-edit-form render-edit-form
             :render-actions render-actions}))))))
