(ns app.domain.frontend.expenses.pages.user.suppliers
  "User-facing suppliers list (shared catalog)."
  (:require
    [app.domain.frontend.expenses.components.user-reference-forms :refer [user-supplier-add-form-modal user-supplier-edit-form-modal]]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.confirm-dialog :as confirm-dialog]
    [app.template.frontend.components.icons :refer [delete-icon edit-icon]]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.components.messages :refer [error-alert]]
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
  (let [supplier-id (id-utils/extract-entity-id item)
        initial-data (dissoc item :show-edit? :show-delete? :on-edit-click)]
    ($ user-supplier-edit-form-modal
      {:supplier-id supplier-id
       :initial-data initial-data
       :on-success on-success
       :on-cancel on-cancel})))

(defn- render-add-form
  [{:keys [on-success on-cancel]}]
  ($ user-supplier-add-form-modal
    {:on-success on-success
     :on-cancel on-cancel}))

(defui suppliers-page []
  (let [role (normalize-role (use-subscribe [:user-role]))
        can-modify? (contains? #{"member" "admin"} role)
        form-error (use-subscribe [:user-expenses/form-error])
        entity-name :suppliers
        entity-spec (use-subscribe [:entity-specs/by-name entity-name])
        refresh-list (use-callback
                       (fn []
                         (rf/dispatch [:user-expenses/fetch-suppliers]))
                       [])]

    (use-effect
      (fn []
        (refresh-list)
        js/undefined)
      [refresh-list])

    (let [render-actions
          (fn [item]
            (let [supplier-id (id-utils/extract-entity-id item)
                  supplier-id-str (some-> supplier-id str)
                  on-edit-click (:on-edit-click item)
                  show-edit? (and can-modify? (not (false? (:show-edit? item))))
                  show-delete? (and can-modify? (not (false? (:show-delete? item))))
                  item-data (dissoc item :show-edit? :show-delete? :on-edit-click)]
              ($ :div {:class "flex items-center justify-center gap-2"}
                (when show-edit?
                  ($ button
                    {:id (str "btn-edit-suppliers-" supplier-id-str)
                     :btn-type :primary
                     :shape "circle"
                     :on-click (fn [e]
                                 (.stopPropagation e)
                                 (when on-edit-click
                                   (on-edit-click item-data)))}
                    ($ edit-icon)))

                (when show-delete?
                  ($ button
                    {:id (str "btn-delete-suppliers-" supplier-id-str)
                     :btn-type :danger
                     :shape "circle"
                     :on-click (fn [e]
                                 (.stopPropagation e)
                                 (confirm-dialog/show-confirm
                                   {:title "Delete supplier"
                                    :message "Do you want to delete this supplier?"
                                    :on-confirm #(rf/dispatch [:user-expenses/delete-supplier supplier-id-str])
                                    :on-cancel nil}))}
                    ($ delete-icon))))))]

      ($ :div {:class "min-h-screen bg-base-100"}
        ($ :header {:class "bg-white border-b border-base-200"}
          ($ :div {:class "w-full px-4 py-4 sm:py-6"}
            ($ :div {:class "flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4"}
              ($ :div
                ($ :h1 {:class "text-xl sm:text-2xl font-bold text-base-content"} "Suppliers")
                ($ :p {:class "text-sm text-base-content/70"}
                  "Shared supplier catalog for your household"))
              ($ :div {:class "flex gap-2"}
                ($ button {:id "btn-back-expenses-dashboard-suppliers"
                           :btn-type :ghost
                           :on-click #(rf/dispatch [:navigate-to "/expenses"])}
                  "Dashboard")))))

        (when (= role "viewer")
          ($ :div {:class "w-full px-4 mt-4"}
            ($ :div {:class "ds-alert"}
              ($ :span "Read-only access. Ask a household member to update suppliers."))))

        (when form-error
          ($ :div {:class "w-full px-4 mt-4"}
            ($ error-alert {:error form-error
                            :on-close #(rf/dispatch [:user-expenses/clear-form-error])})))

        ($ :main {:class "w-full px-4 py-6"}
          ($ list-view
            {:entity-name entity-name
             :entity-spec entity-spec
             :title "Suppliers"
             :form-display :modal
             :allow-add? can-modify?
             :render-add-form render-add-form
             :render-edit-form render-edit-form
             :render-actions render-actions}))))))
