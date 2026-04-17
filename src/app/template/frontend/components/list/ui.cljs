(ns app.template.frontend.components.list.ui
  (:require
    [app.shared.keywords :as kw]
    [app.template.frontend.events.config :as config-events]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.form :refer [form]]
    [app.template.frontend.components.icons :refer [plus-icon]]
    [app.template.frontend.i18n :refer [use-t]]
    [app.template.frontend.events.form :as form-events]
    [app.template.frontend.events.list.crud :as crud-events]
    [app.template.frontend.events.list.ui-state :as list-ui-state]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defui add-item-section
  [{:keys [entity-name entity-spec form-entity-spec set-show-add-form!] :as props}]
  (let [props-map (js->clj props :keywordize-keys true)
        effective-entity-spec (or form-entity-spec entity-spec)
        default-values (reduce (fn [acc field]
                                 (if-let [default-value (:default-value field)]
                                   (assoc acc (keyword (:id field)) default-value)
                                   acc))
                         {}
                         effective-entity-spec)
        entity-name-kw (keyword entity-name)
        t (use-t)]
    ($ :div {:class "w-full mt-4"}
      ($ form
        (merge (dissoc props-map
                 :show-add-form?
                 :entity-spec
                 :form-entity-spec
                 :form-entity-spec-edit
                 :set-show-add-form!)
          {:initial-values default-values
           :entity-spec effective-entity-spec
           :on-cancel #(do
                         (rf/dispatch [::crud-events/clear-error entity-name-kw])
                         (rf/dispatch [::form-events/clear-form-errors entity-name-kw])
                         (set-show-add-form! false))
           :button-text (t :common/save)})))))

(defui header-section
  [{:keys [title show-add-form? set-show-add-form! set-editing! entity-name show-add-button? add-disabled?
           on-add-click add-button-label]}]
  (let [plus-icon-el ($ plus-icon)
        t (use-t)
        entity-label (or (when title (str/lower-case (name title)))
                       (when entity-name (str/lower-case (name entity-name))))
        button-id (str "btn-add-" entity-label)
        resolved-add-button-label (or add-button-label
                                    (if entity-label
                                      (str (t :common/add) " " entity-label)
                                      (t :common/add)))
        ;; Session + metadata to determine if adding is allowed
        current-tenant (use-subscribe [:current-tenant])
        models-data (use-subscribe [:models-data])
        entity-key (if (keyword? entity-name) entity-name (keyword entity-name))
        fields (get-in models-data [entity-key :fields])
        has-tenant-id? (some (fn [f]
                               (let [fname (first f)
                                     fname-kw (if (keyword? fname) fname (keyword fname))]
                                 (= :tenant-id fname-kw)))
                         fields)
        can-add? (or (not has-tenant-id?) current-tenant)
        show-add-button? (if (nil? show-add-button?) true show-add-button?)
        add-disabled? (boolean add-disabled?)]
    ($ :div {:class "flex justify-between items-center mb-4"}
      (when title
        ($ :h2 {:class "text-2xl font-bold"} ($ :span (str title))))
      ($ :div {:class "flex items-center space-x-2 ml-auto"}
        (when (and (not show-add-form?) can-add? show-add-button?)
          ($ button
            {:shape "circle"
             :id button-id
             :aria-label resolved-add-button-label
             :disabled add-disabled?
             :on-click (if on-add-click
                         ;; Use custom handler if provided (for modal mode)
                         on-add-click
                         ;; Default inline behavior
                         #(do
                            ;; Clear any errors
                            (rf/dispatch [::crud-events/clear-error (keyword entity-name)])
                            (rf/dispatch [::form-events/clear-form-errors (keyword entity-name)])

                            ;; Set UI state for add form
                            (set-show-add-form! true)
                            (rf/dispatch [::config-events/set-show-add-form true])

                            ;; Reset editing state
                            (set-editing! nil)))
             :children plus-icon-el}))))))

(defui active-sort-controls
  [{:keys [entity-name sorts field-labels]}]
  (let [t (use-t)
        entity-id (kw/ensure-name entity-name)
        sort-count (count sorts)
        toggle-direction! (fn [field]
                            (rf/dispatch [::list-ui-state/set-sort-field entity-name field {:append? true}]))
        icon-button-class (fn [base-class disabled?]
                            (str "cursor-pointer text-sm font-bold leading-none transition-colors "
                              base-class
                              (when disabled? " cursor-not-allowed opacity-40")))]
    (when (seq sorts)
      ($ :div {:id (str "active-sorts-" entity-id)
               :class "mb-3 rounded-lg border border-blue-200 bg-blue-50 px-3 py-2"}
        ($ :div {:class "flex flex-wrap items-center gap-2"}
          ($ :span {:class "mr-2 text-xs font-medium text-blue-700"}
            (str (or (t :common/sort) "Sorting") " (" sort-count "):"))
          (doall
            (map-indexed
              (fn [idx {:keys [field direction]}]
                (let [field-id (kw/ensure-name field)
                      label (or (get field-labels field)
                              (str/replace field-id #"-" " "))
                      direction-label (case direction
                                        :desc "↓"
                                        :asc "↑"
                                        "↕")
                      left-disabled? (zero? idx)
                      right-disabled? (= idx (dec sort-count))]
                  ($ :div {:id (str "sort-chip-" entity-id "-" field-id)
                           :key (str field-id "-" idx)
                           :class "inline-flex items-center gap-1 rounded-full border border-blue-300 bg-white px-2 py-1 text-xs shadow-sm"}
                    ($ :span {:class "text-xs font-semibold text-warning"} (str (inc idx)))
                    ($ :span {:class "mr-1 font-medium text-gray-700"} label)
                    ($ :button {:id (str "btn-sort-direction-" entity-id "-" field-id)
                                :type "button"
                                :class (icon-button-class "text-base-content/60 hover:text-base-content" false)
                                :title "Toggle sort direction"
                                :on-click #(toggle-direction! field)}
                      direction-label)
                    ($ :button {:id (str "btn-sort-left-" entity-id "-" field-id)
                                :type "button"
                                :class (icon-button-class "text-base-content/50 hover:text-base-content" left-disabled?)
                                :disabled left-disabled?
                                :title "Move sort left"
                                :on-click #(rf/dispatch [::list-ui-state/move-sort-field-left entity-name field])}
                      "←")
                    ($ :button {:id (str "btn-sort-right-" entity-id "-" field-id)
                                :type "button"
                                :class (icon-button-class "text-base-content/50 hover:text-base-content" right-disabled?)
                                :disabled right-disabled?
                                :title "Move sort right"
                                :on-click #(rf/dispatch [::list-ui-state/move-sort-field-right entity-name field])}
                      "→")
                    ($ :button {:id (str "btn-sort-remove-" entity-id "-" field-id)
                                :type "button"
                                :class (icon-button-class "text-red-500 hover:text-red-600" false)
                                :title "Remove sort"
                                :on-click #(rf/dispatch [::list-ui-state/remove-sort-field entity-name field])}
                      "×"))))
              sorts))
          (when (> sort-count 1)
            ($ :button {:id (str "btn-clear-sorts-" entity-id)
                        :type "button"
                        :class "inline-flex items-center rounded-full border border-red-300 bg-red-100 px-2 py-1 text-xs text-red-700 cursor-pointer hover:bg-red-200"
                        :title "Clear all sorts"
                        :on-click #(rf/dispatch [::list-ui-state/clear-sorts entity-name])}
              (or (t :common/clear-all) "Clear all"))))))))
