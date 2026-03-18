(ns app.template.frontend.components.list.ui
  (:require
    [app.template.frontend.events.config :as config-events]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.form :refer [form]]
    [app.template.frontend.components.icons :refer [plus-icon]]
    [app.template.frontend.events.form :as form-events]
    [app.template.frontend.events.list.crud :as crud-events]
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
        entity-name-kw (keyword entity-name)]
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
           :button-text "Save"})))))

(defui header-section
  [{:keys [title show-add-form? set-show-add-form! set-editing! entity-name show-add-button? add-disabled?
           on-add-click]}]
  (let [plus-icon-el ($ plus-icon)
        entity-label (or (when title (str/lower-case (name title)))
                       (when entity-name (str/lower-case (name entity-name))))
        button-id (str "btn-add-" entity-label)
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
             :aria-label (str "Add " (or entity-label "item"))
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
