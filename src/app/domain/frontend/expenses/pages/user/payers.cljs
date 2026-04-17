(ns app.domain.frontend.expenses.pages.user.payers
  "User-facing payers list (shared catalog)."
  (:require
    [app.domain.frontend.expenses.authz :as authz]
    [app.domain.frontend.expenses.components.user-reference-forms :refer [user-payer-add-form-modal user-payer-edit-form-modal]]
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
  [edit-mode item {:keys [on-success on-cancel]}]
  (let [payer-id (id-utils/extract-entity-id item)
        initial-data (dissoc item :show-edit? :show-delete? :on-edit-click)]
    ($ user-payer-edit-form-modal
      {:payer-id payer-id
       :initial-data initial-data
       :edit-mode edit-mode
       :on-success on-success
       :on-cancel on-cancel})))

(defn- render-add-form
  [{:keys [on-success on-cancel]}]
  ($ user-payer-add-form-modal
    {:on-success on-success
     :on-cancel on-cancel}))

(def ^:private system-payer-row-border-class
  "[&>td]:border-y [&>td]:border-y-primary/20 [&>td:first-child]:border-l-4 [&>td:first-child]:border-l-primary [&>td:last-child]:border-r [&>td:last-child]:border-r-primary/20")

(defn- system-payer-row?
  [item]
  (= "system" (some-> (or (:payer-type item)
                        (:payer_type item)
                        (:type item))
                str)))

(defn- payer-related-expense-count
  [item]
  (let [raw (or (:related-expense-count item)
              (:related_expense_count item)
              0)
        parsed (cond
                 (number? raw) raw
                 (string? raw) (js/parseInt raw 10)
                 :else 0)]
    (if (js/isNaN parsed) 0 parsed)))

(defn- payer-linked-to-expenses?
  [item]
  (pos? (payer-related-expense-count item)))

(defn- payer-delete-disabled-reason
  [t item]
  (cond
    (payer-linked-to-expenses? item) (t :payers/delete-disabled-linked)
    (true? (:delete-disabled? item)) (t :payers/delete-disabled-generic)
    :else nil))

(defn- payer-row-class
  [item]
  (when (system-payer-row? item)
    system-payer-row-border-class))

(def ^:private payer-type-field-ids
  #{"payer-type"
    "payer-type-label"
    "payer_type"
    "payer_type_label"})

(defn- payer-type-options
  [t]
  [{:value "system" :label (t :payers/type-system)}
   {:value "custom" :label (t :payers/type-custom)}])

(defn- payer-type-field?
  [field]
  (contains? payer-type-field-ids (some-> (:id field) name)))

(def ^:private payer-boolean-field-ids
  #{"is-default"
    "is_default"})

(def ^:private payer-user-email-field-ids
  #{"user-email"
    "user_email"})

(def ^:private payer-user-name-field-ids
  #{"user-full-name"
    "user_full_name"})

(defn- payer-boolean-field?
  [field]
  (contains? payer-boolean-field-ids (some-> (:id field) name)))

(defn- payer-user-email-field?
  [field]
  (contains? payer-user-email-field-ids (some-> (:id field) name)))

(defn- payer-user-name-field?
  [field]
  (contains? payer-user-name-field-ids (some-> (:id field) name)))

(defn- payer-boolean-options
  [t]
  [{:value true :label (t :common/yes)}
   {:value false :label (t :common/no)}])

(defn- localize-payer-entity-spec
  [t entity-spec]
  (mapv (fn [field]
          (cond
            (payer-type-field? field)
            (assoc field
              :type "select"
              :options (payer-type-options t))

            (payer-boolean-field? field)
            (assoc field
              :type "select"
              :options (payer-boolean-options t))

            (payer-user-email-field? field)
            (assoc field :label (t :common/email))

            (payer-user-name-field? field)
            (assoc field :label (t :common/user))

            :else field))
    (or entity-spec [])))

(defn- payer-edit-mode
  [{:keys [can-manage? user-payer-id]} item]
  (let [payer-id-str (some-> (id-utils/extract-entity-id item) str)
        is-own-payer? (and (some? user-payer-id) (= payer-id-str user-payer-id))
        is-system-payer? (system-payer-row? item)]
    (cond
      (and can-manage? is-system-payer?) :label-and-default
      can-manage? :full
      is-own-payer? :label-only
      :else nil)))

(defn- show-edit-payer-action?
  [view-context item]
  (boolean
    (and (not (false? (:show-edit? item)))
      (payer-edit-mode view-context item))))

(defn- show-delete-payer-action?
  [{:keys [can-manage?]} item]
  (boolean
    (and can-manage?
      (not (false? (:show-delete? item)))
      (not (system-payer-row? item)))))

(defui payers-page []
  (let [t (use-t)
        role (use-subscribe [:expenses/user-role])
        can-modify? (authz/can? role :expenses/reference.write)
        can-manage? (authz/power-user? role)
        user-payer-id (use-subscribe [:user-expenses/user-payer-id])
        entity-name :payers
        entity-spec-raw (use-subscribe [:entity-specs/by-name entity-name])
        entity-spec (localize-payer-entity-spec t entity-spec-raw)
        refresh-list (use-callback
                       (fn []
                         (rf/dispatch [:user-expenses/refresh-payers-list]))
                       [])]

    (use-effect
      (fn []
        (rf/dispatch [::list-ui-state-events/set-pagination-mode entity-name :client])
        (refresh-list)
        js/undefined)
      [refresh-list])

    (let [view-context {:can-manage? can-manage?
                        :user-payer-id user-payer-id}
          render-actions
          (fn [item]
            (let [payer-id (id-utils/extract-entity-id item)
                  payer-id-str (some-> payer-id str)
                  on-edit-click (:on-edit-click item)
                  is-active? (let [v (if (contains? item :is-active) (:is-active item) (get item :is_active true))]
                               (not (false? v)))
                  show-edit? (show-edit-payer-action? view-context item)
                  show-delete? (show-delete-payer-action? view-context item)
                  edit-disabled? (true? (:edit-disabled? item))
                  delete-disabled-reason (payer-delete-disabled-reason t item)
                  delete-disabled? (boolean delete-disabled-reason)
                  item-data (dissoc item :show-edit? :show-delete? :edit-disabled? :delete-disabled? :on-edit-click)
                  delete-button ($ button
                                  {:id (str "btn-delete-payers-" payer-id-str)
                                   :btn-type :danger
                                   :shape "circle"
                                   :class (when delete-disabled? "opacity-50 cursor-not-allowed")
                                   :disabled delete-disabled?
                                   :on-click (fn [e]
                                               (.stopPropagation e)
                                               (when-not delete-disabled?
                                                 (confirm-dialog/show-confirm
                                                   {:title (t :payers/delete-title)
                                                    :message (t :payers/delete-msg)
                                                    :on-confirm #(rf/dispatch [:user-expenses/delete-payer payer-id-str])
                                                    :on-cancel nil})))}
                                  ($ delete-icon))]
              ($ :div {:class "flex items-center justify-center gap-2"}
                (when-not is-active?
                  ($ :span {:class "ds-badge ds-badge-sm ds-badge-warning"} (t :common/inactive)))
                (when show-edit?
                  ($ button
                    {:id (str "btn-edit-payers-" payer-id-str)
                     :btn-type :primary
                     :shape "circle"
                     :disabled edit-disabled?
                     :on-click (fn [e]
                                 (.stopPropagation e)
                                 (when (and (not edit-disabled?) on-edit-click)
                                   (on-edit-click item-data)))}
                    ($ edit-icon)))

                (when show-delete?
                  (if delete-disabled-reason
                    ($ :div {:class "ds-tooltip ds-tooltip-top"
                             :data-tip delete-disabled-reason
                             :title delete-disabled-reason}
                      delete-button)
                    delete-button)))))]

      ($ :div {:class "min-h-screen bg-base-100"}
        ($ :header {:class "bg-white border-b border-base-200"}
          ($ :div {:class "w-full px-4 py-4 sm:py-6"}
            ($ :div {:class "flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4"}
              ($ :div
                ($ :h1 {:class "text-xl sm:text-2xl font-bold text-base-content"} (t :payers/title))
                ($ :p {:class "text-sm text-base-content/70"}
                  (t :payers/subtitle)))
              ($ :div {:class "flex gap-2"}
                ($ button {:id "btn-back-expenses-dashboard-payers"
                           :btn-type :ghost
                           :on-click #(rf/dispatch [:navigate-to "/expenses"])}
                  (t :payers/btn-dashboard))))))

        (when (not can-modify?)
          ($ :div {:class "w-full px-4 mt-4"}
            ($ :div {:class "ds-alert"}
              ($ :span (t :payers/read-only-notice)))))

        ($ :main {:class "w-full px-4 py-6"}
          ($ list-view
            {:entity-name entity-name
             :entity-spec entity-spec
             :add-button-label (t :payers/form-add-title)
             :add-modal-title (t :payers/form-add-title)
             :edit-modal-title (t :payers/form-edit-title)
             ;; only admin/owner can add new payers
             :render-add-form (when can-manage? render-add-form)
             ;; members get label-only edits; admins/owners keep full edits for regular payers and label/default edits for system payers
             :render-edit-form (fn [item modal-opts]
                                 (when-let [edit-mode (payer-edit-mode view-context item)]
                                   (render-edit-form edit-mode item modal-opts)))
             :render-actions render-actions
             :row-class-fn payer-row-class}))))))
