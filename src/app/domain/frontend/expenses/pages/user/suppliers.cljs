(ns app.domain.frontend.expenses.pages.user.suppliers
  "User-facing suppliers list (shared catalog)."
  (:require
    [app.template.frontend.components.shared-utils :as shared]
    [app.domain.frontend.expenses.admin.components.detail-views.utils :as detail-utils]
    [app.domain.frontend.expenses.authz :as authz]
    [app.domain.frontend.expenses.components.user-reference-forms :refer [user-supplier-add-form-modal user-supplier-edit-form-modal]]
    [app.template.frontend.components.action-components :as action-components]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.confirm-dialog :as confirm-dialog]
    [app.template.frontend.components.dropdown.action :as dropdown]
    [app.template.frontend.components.icons :refer [delete-icon edit-icon]]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.components.modal :refer [modal]]
    [app.template.frontend.components.messages :refer [error-alert]]
    [app.template.frontend.utils.id :as id-utils]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-callback use-effect use-state]]
    [uix.re-frame :refer [use-subscribe]]
    app.domain.frontend.expenses.subs.user-expenses))

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

(defn- detail-header
  [{:keys [title subtitle icon]}]
  ($ shared/detail-modal-header
    {:title title
     :subtitle subtitle
     :icon icon
     :icon-bg "bg-primary/10"}))

(defui user-supplier-detail-body
  [{:keys [supplier-id]}]
  (let [supplier (use-subscribe [:user-expenses/supplier-detail supplier-id])
        loading? (use-subscribe [:user-expenses/supplier-detail-loading?])
        error (use-subscribe [:user-expenses/supplier-detail-error])
        expenses (use-subscribe [:user-expenses/supplier-detail-expenses])
        aliases (use-subscribe [:user-expenses/supplier-detail-article-aliases])
        observations (use-subscribe [:user-expenses/supplier-detail-price-observations])
        archived-at (or (some-> supplier :archived_at)
                      (some-> supplier :suppliers/archived_at)
                      (some-> supplier :archived-at))
        archived? (some? archived-at)]
    (use-effect
      (fn []
        (when supplier-id
          (rf/dispatch [:user-expenses/init-supplier-detail supplier-id]))
        js/undefined)
      [supplier-id])

    ($ :div {:class "space-y-6"}
      (when error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span (str error))))

      (cond
        loading?
        ($ :div {:class "ds-loading ds-loading-spinner text-primary"})

        (nil? supplier)
        ($ :div {:class "ds-alert"}
          ($ :span "Supplier not found."))

        :else
        ($ :div {:class "space-y-6"}
          ($ :div {:class "grid gap-3 md:grid-cols-3"}
            (detail-utils/label-value "Name" (or (:display-name supplier) (:display_name supplier)))
            (detail-utils/label-value "Normalized Key" (or (:normalized-key supplier) (:normalized_key supplier)))
            (detail-utils/label-value "Address" (:address supplier))
            (detail-utils/label-value "Tax ID" (or (:tax-id supplier) (:tax_id supplier)))
            (detail-utils/label-value "Created At" (shared/format-date (or (:created-at supplier) (:created_at supplier))))
            (detail-utils/label-value "Archived At" (when archived-at (shared/format-date archived-at)))
            (detail-utils/label-value "ID" (or (:id supplier) (some-> supplier-id str))))

          ($ :div {:class "flex flex-wrap items-center gap-2"}
            (when archived?
              ($ :span {:class "text-xs text-base-content/60"}
                "Archived")))

          ($ :div {:class "grid gap-4 lg:grid-cols-3"}
            ($ detail-utils/related-table
              {:title "Recent Expenses"
               :rows expenses
               :columns [{:label "Purchased" :value-fn #(shared/format-date (or (:purchased-at %) (:purchased_at %)))}
                         {:label "Total" :value-fn #(detail-utils/format-money (or (:total-amount %) (:total_amount %)) (or (:currency %) (:currency_code %)))}
                         {:label "Payer" :value-fn #(or (:payer-label %) (:payer_label %) (:payer %) (:payers/label %))}
                         {:label "Status" :value-fn #(or (:status %) (:receipt_status %) (:expense_status %))}]
               :empty-label "No expenses for this supplier yet."
               :view-all-href nil
               :view-all-id (when supplier-id
                              (str "btn-view-expenses-supplier-" supplier-id))})
            ($ detail-utils/related-table
              {:title "Article Aliases"
               :rows aliases
               :columns [{:label "Alias" :value-fn #(or (:raw-label-normalized %) (:raw_label_normalized %))}
                         {:label "Article" :value-fn #(or (:article-canonical-name %) (:article_canonical_name %))}
                         {:label "Confidence" :value-fn #(or (:confidence %) (:confidence_score %))}]
               :empty-label "No article aliases for this supplier."
               :view-all-href nil
               :view-all-id (when supplier-id
                              (str "btn-view-article-aliases-supplier-" supplier-id))})
            ($ detail-utils/related-table
              {:title "Price Observations"
               :rows observations
               :columns [{:label "Observed" :value-fn #(shared/format-date (or (:observed-at %) (:observed_at %)))}
                         {:label "Article" :value-fn #(or (:article-canonical-name %) (:article_canonical_name %))}
                         {:label "Unit Price" :value-fn #(or (:unit-price %) (:unit_price %))}
                         {:label "Currency" :value-fn #(or (:currency %) (:currency_code %))}]
               :empty-label "No price observations for this supplier."
               :view-all-href nil
               :view-all-id (when supplier-id
                              (str "btn-view-price-observations-supplier-" supplier-id))})))))))

(defui suppliers-page []
  (let [role (use-subscribe [:expenses/user-role])
        can-modify? (authz/can? role :expenses/reference.write)
        can-purge? (authz/can? role :expenses/reference.purge)
        form-error (use-subscribe [:user-expenses/form-error])
        include-archived? (true? (use-subscribe [:user-expenses/suppliers-include-archived?]))
        entity-name :suppliers
        entity-spec (use-subscribe [:entity-specs/by-name entity-name])
        debug? (boolean (.-DEBUG js/goog))
        [instance-id] (use-state (random-uuid))
        [detail-supplier set-detail-supplier] (use-state nil)
        detail-supplier-id (some-> detail-supplier id-utils/extract-entity-id)
        detail-supplier-record (use-subscribe [:user-expenses/supplier-detail detail-supplier-id])
        refresh-list (use-callback
                       (fn []
                         (rf/dispatch [:user-expenses/fetch-suppliers]))
                       [include-archived?])]

    (use-effect
      (fn []
        (when debug?
          (js/console.log "[user-suppliers] mount" (str instance-id)))
        (fn []
          (when debug?
            (js/console.log "[user-suppliers] unmount" (str instance-id)))))
      [instance-id debug?])

    (use-effect
      (fn []
        (when debug?
          (js/console.log "[user-suppliers] detail-supplier" (str instance-id)
            (if detail-supplier "set" "nil")
            (some-> detail-supplier id-utils/extract-entity-id str)))
        js/undefined)
      [instance-id debug? detail-supplier])

    (use-effect
      (fn []
        (refresh-list)
        js/undefined)
      [refresh-list])

    (let [render-actions
          (fn [item]
            (let [supplier-id (id-utils/extract-entity-id item)
                  supplier-id-str (some-> supplier-id str)
                  item-data (dissoc item :show-edit? :show-delete? :edit-disabled? :delete-disabled? :on-edit-click)

                  archived? (some? (or (:archived_at item)
                                     (:suppliers/archived_at item)
                                     (:archived-at item)))
                  active-expenses (long (or (:active_expenses_count item)
                                          (:active-expenses-count item)
                                          (:active_expenses item)
                                          (:active-expenses item)
                                          0))
                  purge-disabled? (pos? active-expenses)
                  purge-tooltip (when purge-disabled?
                                  (str "Cannot purge: supplier has " active-expenses " active expense(s)."))

                  on-edit-click (:on-edit-click item)
                  edit-disabled? (true? (:edit-disabled? item))
                  delete-disabled? (true? (:delete-disabled? item))
                  show-edit? (and (not archived?)
                               (not (false? (:show-edit? item))))
                  show-archive? (and (not archived?)
                                  (not (false? (:show-delete? item))))]
              ($ :div {:class "flex items-center justify-center gap-2"}
                (when show-edit?
                  ($ button
                    {:id (str "btn-edit-suppliers-" supplier-id-str)
                     :btn-type :primary
                     :shape "circle"
                     :disabled edit-disabled?
                     :on-click (fn [e]
                                 (.stopPropagation e)
                                 (when (and (not edit-disabled?) on-edit-click)
                                   (on-edit-click item-data)))}
                    ($ edit-icon)))

                (when archived?
                  ($ :span {:class "text-xs text-base-content/60"}
                    "Archived"))

                (when show-archive?
                  ($ button
                    {:id (str "btn-archive-suppliers-" supplier-id-str)
                     :btn-type :danger
                     :shape "circle"
                     :disabled delete-disabled?
                     :on-click (fn [e]
                                 (.stopPropagation e)
                                 (when-not delete-disabled?
                                   (confirm-dialog/show-confirm
                                     {:title "Archive supplier"
                                      :message "Do you want to archive this supplier?"
                                      :on-confirm #(rf/dispatch [:user-expenses/delete-supplier supplier-id-str])
                                      :on-cancel nil})))}
                    ($ delete-icon)))

                (when (and can-purge? (seq supplier-id-str))
                  (let [actions (cond->
                                  [{:group-title "View"
                                    :items [{:id "view-details"
                                             :icon ($ action-components/view-details-icon)
                                             :label "View Details"
                                             :on-click (fn [e]
                                                         (.stopPropagation e)
                                                         (when debug?
                                                           (js/console.log "[user-suppliers] view-details click" (str instance-id) supplier-id-str))
                                                         (set-detail-supplier item-data))}]}]
                                  archived?
                                  (conj {:group-title "Danger"
                                         :items [{:id "purge-permanently"
                                                  :icon ($ action-components/delete-icon)
                                                  :label "Purge permanently"
                                                  :variant :error
                                                  :disabled? purge-disabled?
                                                  :tooltip purge-tooltip
                                                  :on-click (fn [e]
                                                              (.stopPropagation e)
                                                              (rf/dispatch [:user-expenses/open-purge-supplier-confirm supplier-id-str]))}]}))]
                    ($ dropdown/action-dropdown
                      {:entity-id supplier-id-str
                       :actions actions
                       :position :portal}))))))]

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

        (when (not can-modify?)
          ($ :div {:class "w-full px-4 mt-4"}
            ($ :div {:class "ds-alert"}
              ($ :span "Read-only access. Ask a household member to update suppliers."))))

        (when form-error
          ($ :div {:class "w-full px-4 mt-4"}
            ($ error-alert {:error form-error
                            :on-close #(rf/dispatch [:user-expenses/clear-form-error])})))

        (when detail-supplier
          (let [supplier-id detail-supplier-id
                supplier-name (or (:display-name detail-supplier-record)
                                (:display_name detail-supplier-record)
                                (:display-name detail-supplier)
                                (:display_name detail-supplier))
                subtitle (or supplier-name
                           (when supplier-id (str "Supplier " supplier-id))
                           "Supplier details")
                header (detail-header {:title "Supplier Details"
                                       :subtitle subtitle
                                       :icon "S"})
                close! (fn []
                         (rf/dispatch [:user-expenses/clear-supplier-detail])
                         (set-detail-supplier nil))]
            ($ :<>
              (when debug?
                ($ :div {:id "debug-user-supplier-detail-open" :class "hidden"}
                  "open"))
              ($ modal
                {:id "user-supplier-detail-modal"
                 :on-close close!
                 :draggable? true
                 :width "960px"
                 :class "max-w-[95vw] h-[85vh] flex flex-col"
                 :header header
                 :header-class "p-0 border-0 bg-transparent mb-3"}
                ($ :div {:class "flex-1 overflow-y-auto p-4"}
                  ($ user-supplier-detail-body {:supplier-id supplier-id}))))))

        ($ :main {:class "w-full px-4 py-6"}
          ($ :label {:class "mb-4 flex items-center gap-2 cursor-pointer select-none"}
            ($ :span {:class "text-sm text-base-content/70"} "Show archived")
            ($ :input {:id "toggle-show-archived-suppliers-user"
                       :type "checkbox"
                       :class "ds-toggle ds-toggle-sm"
                       :checked (true? include-archived?)
                       :on-change (fn [e]
                                    (rf/dispatch [:user-expenses/set-suppliers-include-archived (.. e -target -checked)]))})
            (when include-archived?
              ($ :span {:class "text-xs text-base-content/50"}
                "Archived suppliers are read-only")))
          ($ list-view
            {:entity-name entity-name
             :entity-spec entity-spec
             :title "Suppliers"
             :form-display :modal
             :disallowed-action-mode :disable
             :allow-add? can-modify?
             :allow-edit? can-modify?
             :allow-delete? can-modify?
             :render-add-form render-add-form
             :render-edit-form render-edit-form
             :render-actions render-actions}))))))
