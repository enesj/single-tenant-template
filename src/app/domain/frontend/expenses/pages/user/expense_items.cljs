(ns app.domain.frontend.expenses.pages.user.expense-items
  "Power-user expense items list (line items across expenses)."
  (:require
    [app.domain.frontend.expenses.authz :as authz]
    [app.domain.frontend.expenses.components.page-guard :refer [expenses-page-guard]]
    [app.shared.adapters.normalization :as normalization]
    [app.shared.model-naming :as model-naming]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.confirm-dialog :as confirm-dialog]
    [app.template.frontend.components.form :refer [form]]
    [app.template.frontend.components.icons :refer [delete-icon edit-icon]]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.events.list.ui-state :as list-ui-state-events]
    [app.template.frontend.utils.id :as id-utils]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-callback use-effect]]
    [uix.re-frame :refer [use-subscribe]]
    app.domain.frontend.expenses.subs.user-expenses))

(defn- spec-field-keys
  "Return the set of field keys (as keywords) described by a form spec.

  Handles either a seq of {:id ...} maps or a map keyed by field id."
  [spec]
  (->> (cond
         (map? spec) (vals spec)
         (sequential? spec) spec
         :else [])
    (keep :id)
    (map keyword)
    set))

(defn- initial-values-for
  "Build initial-values that work with both:

  - enhanced/dynamic specs (kebab-case keys like :raw-label)
  - fallback hardcoded specs (snake_case keys like :raw_label)

  by merging app keys plus their db-key equivalents."
  [app-values]
  (merge (model-naming/app-map-keys->db app-values) app-values))

(def ^:private expense-item-form-spec
  [{:id :raw_label
    :type :text
    :label "Raw label"
    :required true
    :placeholder "Line item label"}
   {:id :article_id
    :type :text
    :label "Article ID"
    :required false
    :placeholder "Article ID (UUID, optional)"}
   {:id :qty
    :type :number
    :label "Qty"
    :required false
    :step 0.001}
   {:id :unit
    :type :select
    :label "Unit"
    :required false
    :options ["kom" "kg" "g" "l" "pak"]}
   {:id :unit_price
    :type :number
    :label "Unit price"
    :required false
    :step 0.01}
   {:id :line_total
    :type :number
    :label "Line total"
    :required true
    :step 0.01}])

(defui user-expense-item-edit-form-modal
  [{:keys [expense-item-id initial-data on-success on-cancel]}]
  (let [form-error (use-subscribe [:user-expenses/form-error])
        ;; Get dynamic form spec from user-settings config
        dynamic-spec (use-subscribe [:form-entity-specs/by-name :expense-items true])
        spec-to-use (if (seq dynamic-spec) dynamic-spec expense-item-form-spec)
        ;; Build initial values from data.
        initial-values (initial-values-for
                         (-> (normalization/convert-db-keys->app-keys (or initial-data {}))
                           (select-keys [:raw-label :article-id :qty :unit :unit-price :line-total
                                         :id :expense-id :alias-id])))]
    ($ :div {:class "space-y-4"}
      (when form-error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span form-error)))

      ($ form
        {:entity-name "expense-items"
         ;; Only use hardcoded spec as fallback if dynamic config not available
         :entity-spec (when-not (seq dynamic-spec) expense-item-form-spec)
         :editing true
         :initial-values initial-values
         :on-cancel on-cancel
         :on-submit (fn [{:keys [values]}]
                      (let [payload (select-keys values (spec-field-keys spec-to-use))]
                        (rf/dispatch [:user-expenses/update-expense-item-modal
                                      expense-item-id
                                      (model-naming/app-map-keys->db payload)
                                      on-success])))
         :button-text "Update Expense Item"}))))

(defn- render-edit-form
  [item {:keys [on-success on-cancel]}]
  (let [expense-item-id (id-utils/extract-entity-id item)
        initial-data (dissoc item :show-edit? :show-delete? :on-edit-click)]
    ($ user-expense-item-edit-form-modal
      {:expense-item-id expense-item-id
       :initial-data initial-data
       :on-success on-success
       :on-cancel on-cancel})))

(defui expense-items-page []
  (let [entity-name :expense-items
        role (use-subscribe [:expenses/user-role])
        can-manage? (authz/can? role :expenses/expense-items.manage)
        entity-spec (use-subscribe [:entity-specs/by-name entity-name])
        refresh-list (use-callback
                       (fn []
                         (rf/dispatch [:user-expenses/refresh-expense-items-list]))
                       [])]
    (use-effect
      (fn []
        (rf/dispatch [::list-ui-state-events/set-pagination-mode entity-name :server])
        (rf/dispatch [::list-ui-state-events/set-refresh-event entity-name [:user-expenses/refresh-expense-items-list]])
        (refresh-list)
        js/undefined)
      [refresh-list])

    ($ expenses-page-guard
      {:capability :expenses/expense-items.manage
       :children
       ($ :div {:class "min-h-screen bg-base-100"}
         ($ :header {:class "bg-white border-b border-base-200"}
           ($ :div {:class "w-full px-4 py-4 sm:py-6"}
             ($ :div {:class "flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4"}
               ($ :div
                 ($ :h1 {:class "text-xl sm:text-2xl font-bold text-base-content"} "Expense Items")
                 ($ :p {:class "text-sm text-base-content/70"}
                   "Power-user line items across your expenses"))
               ($ :div {:class "flex gap-2 flex-wrap"}
                 ($ button {:id "btn-back-expenses-dashboard-expense-items"
                            :btn-type :ghost
                            :on-click #(rf/dispatch [:navigate-to "/expenses"])}
                   "Dashboard")
                 ($ button {:id "btn-go-expenses-list-expense-items"
                            :btn-type :primary
                            :on-click #(rf/dispatch [:navigate-to "/expenses/list"])}
                   "Expenses")))))

         ($ :main {:class "w-full px-4 py-6"}
           ($ list-view
             {:entity-name entity-name
              :entity-spec entity-spec
              :render-edit-form render-edit-form
              :render-actions (fn [item]
                                (let [expense-item-id (id-utils/extract-entity-id item)
                                      expense-item-id-str (some-> expense-item-id str)
                                      on-edit-click (:on-edit-click item)
                                      show-edit? (and can-manage? (not (false? (:show-edit? item))))
                                      show-delete? (and can-manage? (not (false? (:show-delete? item))))]
                                  ($ :div {:class "flex items-center justify-center gap-2"}
                                    (when show-edit?
                                      ($ button
                                        {:id (str "btn-edit-expense-items-" expense-item-id-str)
                                         :btn-type :primary
                                         :shape "circle"
                                         :on-click (fn [e]
                                                     (.stopPropagation e)
                                                     (when on-edit-click
                                                       (on-edit-click (dissoc item :show-edit? :show-delete? :on-edit-click))))}
                                        ($ edit-icon)))

                                    (when show-delete?
                                      ($ button
                                        {:id (str "btn-delete-expense-items-" expense-item-id-str)
                                         :btn-type :danger
                                         :shape "circle"
                                         :on-click (fn [e]
                                                     (.stopPropagation e)
                                                     (confirm-dialog/show-confirm
                                                       {:title "Delete expense item"
                                                        :message "Do you want to delete this expense item?"
                                                        :on-confirm #(rf/dispatch [:user-expenses/delete-expense-item expense-item-id-str])
                                                        :on-cancel nil}))}
                                        ($ delete-icon))))))})))})))
