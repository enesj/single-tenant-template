(ns app.domain.frontend.expenses.components.manual-expense-form.pick-or-create
  "Generic pick-or-create component for reference fields.

  Shows a select dropdown with an inline 'New' button.
  Clicking 'New' reveals an inline text input + save/cancel.
  On save, dispatches a create event and selects the new entity."
  (:require
    [app.template.frontend.components.common :as common]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-state]]))

(defui pick-or-create-field
  "Reusable select field with inline creation.

  Props:
  - :id            - field DOM id
  - :label         - field label text
  - :required      - boolean
  - :value         - currently selected id (string)
  - :on-change     - callback with new id (string)
  - :options       - vec of {:value id :label name}
  - :placeholder   - select placeholder
  - :create-label  - e.g. 'New supplier'
  - :create-event  - re-frame event key for creation, e.g. :user-expenses/create-supplier-modal
  - :create-field  - field name sent to create event, e.g. :display_name or :name
  - :create-extra-params - extra map merged into create event params (e.g. {:supplier_id ...})
  - :id-key        - key to extract id from created entity (default :id)
  - :disabled?     - boolean
  - :error         - error message
  - :inline        - boolean (inline layout)"
  [{:keys [id label required value on-change options placeholder
           create-label create-event create-field create-extra-params id-key
           disabled? error inline]}]
  (let [[creating? set-creating!] (use-state false)
        [input-value set-input-value!] (use-state "")
        [saving? set-saving!] (use-state false)
        id-key (or id-key :id)
        select-id (or id "pick-or-create-select")
        input-id (str select-id "-create-input")
        label-class (str "ds-label" (when inline " mb-0 min-w-[150px] text-left"))
        wrapper-class (str "mb-4" (if inline " flex flex-row items-start gap-4"
                                    " flex flex-col items-start gap-4"))

        start-create (fn []
                       (set-input-value! "")
                       (set-creating! true))
        cancel-create (fn []
                        (set-creating! false)
                        (set-input-value! ""))
        do-save (fn []
                  (let [name* (str/trim (str input-value))]
                    (when (and (not (str/blank? name*))
                            (not saving?)
                            create-event)
                      (set-saving! true)
                      (rf/dispatch [create-event
                                    (merge {create-field name*} create-extra-params)
                                    (fn [entity]
                                      (set-saving! false)
                                      (set-creating! false)
                                      (set-input-value! "")
                                      (when (and (map? entity) (fn? on-change))
                                        (when-let [new-id (get entity id-key)]
                                          (on-change (str new-id)))))]))))]

    ($ :div {:class wrapper-class}
      ($ common/label {:text (or label "Field")
                       :class label-class
                       :for select-id
                       :required required})
      ($ :div {:class (when inline "flex-1 w-full text-left")}
        (if creating?
          ;; Inline create mode
          ($ :div {:class "flex items-center gap-2"}
            ($ :input {:id input-id
                       :class "ds-input ds-input-bordered ds-input-sm flex-1"
                       :type "text"
                       :value input-value
                       :auto-focus true
                       :placeholder (str "Enter " (str/lower-case (or label "name")))
                       :on-key-down (fn [e]
                                      (case (.-key e)
                                        "Enter" (do (.preventDefault e) (do-save))
                                        "Escape" (do (.preventDefault e) (cancel-create))
                                        nil))
                       :on-change (fn [e] (set-input-value! (.. e -target -value)))})
            ($ :button {:type "button"
                        :class "ds-btn ds-btn-primary ds-btn-sm"
                        :disabled (or saving? (str/blank? (str/trim (str input-value))))
                        :on-click (fn [e] (.preventDefault e) (do-save))}
              (if saving? "..." "Save"))
            ($ :button {:type "button"
                        :class "ds-btn ds-btn-ghost ds-btn-sm"
                        :disabled saving?
                        :on-click (fn [e] (.preventDefault e) (cancel-create))}
              "Cancel"))

          ;; Normal select mode
          ($ :div {:class "flex items-start gap-2"}
            ($ :div {:class "flex-1"}
              ($ common/select {:id select-id
                                :value value
                                :options (vec (or options []))
                                :on-change on-change
                                :disabled disabled?
                                :placeholder placeholder}))
            (when create-event
              ($ :button {:id (str select-id "-new-btn")
                          :type "button"
                          :class "ds-btn ds-btn-ghost ds-btn-sm"
                          :disabled disabled?
                          :on-click start-create}
                (or create-label "New")))))

        (when error
          ($ :div {:class "text-error text-sm mt-1"} error))))))
