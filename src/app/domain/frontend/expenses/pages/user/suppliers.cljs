(ns app.domain.frontend.expenses.pages.user.suppliers
  "User-facing suppliers list (shared catalog)."
  (:require
    [app.template.frontend.components.shared-utils :as shared]
    [app.domain.frontend.expenses.admin.components.detail-views.utils :as detail-utils]
    [app.domain.frontend.expenses.authz :as authz]
    [app.domain.frontend.expenses.components.user-reference-forms :refer [user-supplier-add-form-modal user-supplier-edit-form-modal]]
    [app.domain.frontend.expenses.events.supplier-stores :as supplier-stores-events]
    [app.domain.frontend.expenses.subs.supplier-stores :as supplier-stores-subs]
    [app.template.frontend.components.action-components :as action-components]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.confirm-dialog :as confirm-dialog]
    [app.template.frontend.components.dropdown.action :as dropdown]
    [app.template.frontend.components.icons :refer [chevron-right-icon delete-icon edit-icon]]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.components.modal-wrapper :refer [modal-wrapper]]
    [app.template.frontend.components.messages :refer [error-alert]]
    [app.template.frontend.events.list.ui-state :as list-ui-state-events]
    [app.template.frontend.i18n :refer [use-t]]
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
  (let [t (use-t)
        supplier (use-subscribe [:user-expenses/supplier-detail supplier-id])
        loading? (use-subscribe [:user-expenses/supplier-detail-loading?])
        error (use-subscribe [:user-expenses/supplier-detail-error])
        expenses (use-subscribe [:user-expenses/supplier-detail-expenses])
        aliases (use-subscribe [:user-expenses/supplier-detail-article-aliases])]
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
          ($ :span (t :suppliers/not-found)))

        :else
        ($ :div {:class "space-y-6"}
          ($ :div {:class "grid gap-3 md:grid-cols-3"}
            (detail-utils/label-value (t :suppliers/col-name) (:display-name supplier))
            (detail-utils/label-value (t :suppliers/col-normalized-key) (:normalized-key supplier))
            (detail-utils/label-value (t :suppliers/col-address) (:address supplier))
            (detail-utils/label-value (t :suppliers/col-created) (shared/format-date (:created-at supplier)))
            (detail-utils/label-value (t :suppliers/col-id) (or (:id supplier) (some-> supplier-id str))))

          ($ :div {:class "grid gap-4 lg:grid-cols-3"}
            ($ detail-utils/related-table
              {:title (t :suppliers/recent-expenses-title)
               :rows expenses
               :columns [{:label (t :suppliers/col-purchased) :value-fn #(shared/format-date (:purchased-at %))}
                         {:label (t :suppliers/col-total) :value-fn #(detail-utils/format-money (:total-amount %) (or (:currency %) (:currency-code %)))}
                         {:label (t :suppliers/col-payer) :value-fn #(or (:payer-label %) (:payer %) (:payers/label %))}
                         {:label (t :suppliers/col-status) :value-fn #(or (:status %) (:receipt-status %) (:expense-status %))}]
               :empty-label (t :suppliers/recent-expenses-empty)
               :view-all-href nil
               :view-all-id (when supplier-id
                              (str "btn-view-expenses-supplier-" supplier-id))})
            ($ detail-utils/related-table
              {:title (t :suppliers/aliases-title)
               :rows aliases
               :columns [{:label (t :suppliers/col-raw-label) :value-fn #(:raw-label %)}
                         {:label (t :suppliers/col-article) :value-fn #(:article-canonical-name %)}
                         {:label (t :suppliers/col-normalized) :value-fn #(:raw-label-normalized %)}]
               :empty-label (t :suppliers/aliases-empty)
               :view-all-href nil
               :view-all-id (when supplier-id
                              (str "btn-view-article-aliases-supplier-" supplier-id))})))))))

;; =============================================================================
;; Expand row — readonly store list for a single supplier
;; =============================================================================

(defui supplier-stores-expand-row
  "Renders stores for a single supplier in readonly mode.
   Used as the row-expansion panel in the supplier list."
  [{:keys [supplier-id]}]
  (let [stores  (use-subscribe [::supplier-stores-subs/stores-for-supplier supplier-id])
        loading? (use-subscribe [::supplier-stores-subs/loading-for-supplier? supplier-id])]
    ($ :div {:class "px-6 py-3 bg-base-50 border-b border-base-200"}
      (if loading?
        ($ :div {:class "space-y-2 py-1"}
          (for [i (range 3)]
            ($ :div {:key i :class "h-4 bg-base-200 rounded animate-pulse"})))
        (if (empty? stores)
          ($ :div {:class "text-sm text-base-content/50 py-1"} "No stores")
          ($ :table {:class "w-full text-sm"}
            ($ :thead
              ($ :tr {:class "text-left text-xs text-base-content/50 uppercase border-b border-base-200"}
                ($ :th {:class "py-1.5 pr-4 font-medium"} "Store")
                ($ :th {:class "py-1.5 pr-4 font-medium"} "Address")
                ($ :th {:class "py-1.5 font-medium"} "City")))
            ($ :tbody
              (for [store stores]
                ($ :tr {:key (or (:id store) (str store))
                        :class "border-b border-base-100 last:border-0"}
                  ($ :td {:class "py-1.5 pr-4 text-base-content"}
                    (or (:display-name store) (:display_name store)
                      ($ :span {:class "text-base-content/30"} "—")))
                  ($ :td {:class "py-1.5 pr-4 text-base-content/70"}
                    (or (:address store)
                      ($ :span {:class "text-base-content/30"} "—")))
                  ($ :td {:class "py-1.5 text-base-content/70"}
                    (or (:city-name store) (:city_name store) (:city store)
                      ($ :span {:class "text-base-content/30"} "—"))))))))))))

(defui suppliers-page []
  (let [t (use-t)
        role (use-subscribe [:expenses/user-role])
        can-modify? (authz/can? role :expenses/reference.write)
        power-user? (boolean (use-subscribe [:expenses/power-user?]))
        form-error (use-subscribe [:user-expenses/form-error])
        entity-name :suppliers
        entity-spec (use-subscribe [:entity-specs/by-name entity-name])
        debug? (boolean (.-DEBUG js/goog))
        [instance-id] (use-state (random-uuid))
        [detail-supplier set-detail-supplier] (use-state nil)
        [expanded-ids set-expanded-ids!] (use-state #{})
        detail-supplier-id (some-> detail-supplier id-utils/extract-entity-id)
        detail-supplier-record (use-subscribe [:user-expenses/supplier-detail detail-supplier-id])
        refresh-list (use-callback
                       (fn []
                         (rf/dispatch [:user-expenses/refresh-suppliers-list]))
                       [])
        toggle-expand (use-callback
                        (fn [supplier-id]
                          (set-expanded-ids!
                            (fn [ids]
                              (if (contains? ids supplier-id)
                                (disj ids supplier-id)
                                (conj ids supplier-id))))
                          (rf/dispatch [::supplier-stores-events/fetch-stores-for-supplier supplier-id]))
                        [])]

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
        (rf/dispatch [::list-ui-state-events/set-pagination-mode entity-name :server])
        (rf/dispatch [::list-ui-state-events/set-refresh-event entity-name [:user-expenses/refresh-suppliers-list]])
        (refresh-list)
        js/undefined)
      [refresh-list])

    (let [render-actions
          (fn [item]
            (let [supplier-id (id-utils/extract-entity-id item)
                  supplier-id-str (some-> supplier-id str)
                  store-count (or (:store-count item) (:store_count item) 0)
                  expanded? (contains? expanded-ids supplier-id)
                  show-expand? (and power-user? (pos? store-count))
                  item-data (dissoc item :show-edit? :show-delete? :edit-disabled? :delete-disabled? :on-edit-click)

                  on-edit-click (:on-edit-click item)
                  edit-disabled? (true? (:edit-disabled? item))
                  delete-disabled? (true? (:delete-disabled? item))
                  show-edit? (not (false? (:show-edit? item)))
                  show-delete? (not (false? (:show-delete? item)))]
              ($ :div {:class "flex items-center justify-center gap-2"}
                ;; Always render the chevron slot when power-user so every row
                ;; has the same DOM structure and edit/delete stay aligned.
                (when power-user?
                  ($ :div {:class (when-not show-expand? "invisible")}
                    ($ button
                      {:id (str "btn-expand-suppliers-" supplier-id-str)
                       :btn-type :ghost
                       :shape "circle"
                       :title (if expanded? "Collapse stores" "Expand stores")
                       :on-click (fn [e]
                                   (.stopPropagation e)
                                   (when show-expand?
                                     (toggle-expand supplier-id)))}
                      ($ :span {:class (str "inline-flex transition-transform duration-150"
                                         (when expanded? " rotate-90"))}
                        ($ chevron-right-icon)))))
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

                (when show-delete?
                  ($ button
                    {:id (str "btn-delete-suppliers-" supplier-id-str)
                     :btn-type :danger
                     :shape "circle"
                     :disabled delete-disabled?
                     :on-click (fn [e]
                                 (.stopPropagation e)
                                 (when-not delete-disabled?
                                   (confirm-dialog/show-confirm
                                     {:title (t :suppliers/delete-title)
                                      :message (t :suppliers/delete-msg)
                                      :on-confirm #(rf/dispatch [:user-expenses/delete-supplier supplier-id-str])
                                      :on-cancel nil})))}
                    ($ delete-icon)))
                (when (seq supplier-id-str)
                  (let [actions [{:group-title (t :common/view)
                                  :items [{:id "view-details"
                                           :icon ($ action-components/view-details-icon)
                                           :label (t :expenses-list/view-details)
                                           :on-click (fn [e]
                                                       (.stopPropagation e)
                                                       (when debug?
                                                         (js/console.log "[user-suppliers] view-details click" (str instance-id) supplier-id-str))
                                                       (set-detail-supplier item-data))}]}]]
                    ($ dropdown/action-dropdown
                      {:entity-id supplier-id-str
                       :actions actions
                       :position :portal}))))))]

      ($ :div {:class "min-h-screen bg-base-100"}
        ($ :header {:class "bg-white border-b border-base-200"}
          ($ :div {:class "w-full px-4 py-4 sm:py-6"}
            ($ :div {:class "flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4"}
              ($ :div
                ($ :h1 {:class "text-xl sm:text-2xl font-bold text-base-content"} (t :suppliers/title))
                ($ :p {:class "text-sm text-base-content/70"}
                  (t :suppliers/subtitle)))
              ($ :div {:class "flex gap-2"}
                ($ button {:id "btn-back-expenses-dashboard-suppliers"
                           :btn-type :ghost
                           :on-click #(rf/dispatch [:navigate-to "/expenses"])}
                  (t :suppliers/btn-dashboard))))))

        (when (not can-modify?)
          ($ :div {:class "w-full px-4 mt-4"}
            ($ :div {:class "ds-alert"}
              ($ :span (t :suppliers/read-only-notice)))))

        (when form-error
          ($ :div {:class "w-full px-4 mt-4"}
            ($ error-alert {:error form-error
                            :on-close #(rf/dispatch [:user-expenses/clear-form-error])})))

        (when detail-supplier
          (let [supplier-id detail-supplier-id
                supplier-name (or (:display-name detail-supplier-record)
                                (:display-name detail-supplier))
                subtitle (or supplier-name
                           (when supplier-id (str "Supplier " supplier-id))
                           "Supplier details")
                close! (fn []
                         (rf/dispatch [:user-expenses/clear-supplier-detail])
                         (set-detail-supplier nil))]
            ($ :<>
              (when debug?
                ($ :div {:id "debug-user-supplier-detail-open" :class "hidden"}
                  "open"))
              ($ modal-wrapper
                {:id "user-supplier-detail-modal"
                 :visible? true
                 :title (t :suppliers/detail-title)
                 :size :extra-large
                 :draggable? true
                 :resizable? true
                 :on-close close!
                 :close-button-id "btn-close-user-supplier-detail-modal"}

                ;; Keep the rich header style inside the modal body.
                (detail-header {:title (t :suppliers/detail-title)
                                :subtitle subtitle
                                :icon "S"})

                ($ :div {:class "mt-4"}
                  ($ user-supplier-detail-body {:supplier-id supplier-id}))))))

        ($ :main {:class "w-full px-4 py-6"}
          ($ list-view
            {:entity-name entity-name
             :entity-spec entity-spec
             :render-add-form render-add-form
             :render-edit-form render-edit-form
             :render-actions render-actions
             :render-row-expansion (when power-user?
                                     (fn [item]
                                       (let [supplier-id (id-utils/extract-entity-id item)
                                             store-count (or (:store-count item) (:store_count item) 0)]
                                         (when (and (pos? store-count)
                                                 (contains? expanded-ids supplier-id))
                                           ($ supplier-stores-expand-row {:supplier-id supplier-id})))))}))))))
