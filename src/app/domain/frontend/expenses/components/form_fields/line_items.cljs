(ns app.domain.frontend.expenses.components.form-fields.line-items
  "Line items input component for expense form"
  (:require
    [app.domain.frontend.expenses.components.form-fields.helpers :as h]
    [app.template.frontend.i18n :as i18n]
    [app.template.frontend.components.common :as common]
    [uix.core :refer [$ defui]]))

(defui line-items-input
  [{:keys [value on-change error field-spec]}]
  (let [t (i18n/use-t)
        items (if (seq value) value [(h/new-line-item)])
        columns (:columns field-spec)
        field-key (let [id (:id field-spec)]
                    (cond
                      (keyword? id) (name id)
                      (string? id) id
                      :else "items"))
        input-id (fn [item-id col-id]
                   (str field-key "-" item-id "-" (name col-id)))

        add-item (fn []
                   (on-change (conj items (h/new-line-item))))

        remove-item (fn [id]
                      (on-change (h/remove-line-item items id)))

        handle-line-change (fn [item-id key]
                             (fn [e]
                               (on-change
                                 (h/update-line-item items item-id key (.. e -target -value)))))]
    ($ :div {:class "space-y-4"}
      ($ :div {:class "flex items-center justify-between"}
        ($ :h2 {:class "text-lg font-semibold"} (:label field-spec))
        ($ :button {:id (str "btn-add-" field-key "-line-item")
                    :class "ds-btn ds-btn-ghost ds-btn-sm"
                    :type "button"
                    :on-click add-item}
          (t :common/add-line-item)))
      (let [overflow-y-class (or (:overflow-y-class field-spec) "overflow-y-auto")
            stable-gutter? (true? (:scrollbar-gutter-stable? field-spec))
            style (merge {:maxHeight "300px"}
                    (:style field-spec)
                    (when stable-gutter? {:scrollbarGutter "stable"}))]
        ($ :div {:class (str "overflow-x-auto " (:max-height-class field-spec) " " overflow-y-class)
                 :style style}
          ($ :table {:class "ds-table w-full"}
            ($ :thead
              ($ :tr
                (for [col columns
                      :let [label (:label col)
                            width (:width col)]]
                  ($ :th {:key (:id col)
                          :class (or width "")}
                    label))
                ($ :th "")))
            ($ :tbody
              (for [item items
                    :let [item-id (:id item)]]
                ($ :tr {:key item-id}
                  (for [col columns
                        :let [col-id (:id col)
                              val (get item col-id)
                              type (:type col)
                              placeholder (:placeholder col)
                              step (:step col)
                              min-val (:min col)]]
                    ($ :td {:key col-id}
                      ($ common/input
                        (cond-> {:id (input-id item-id col-id)
                                 :class (if (= type :number)
                                         "ds-input ds-input-bordered w-full [&::-webkit-inner-spin-button]:appearance-none [&::-webkit-outer-spin-button]:appearance-none [-moz-appearance:textfield] appearance-none"
                                         "ds-input ds-input-bordered w-full")
                                 :value (or val "")
                                 :type (name (or type :text))
                                 :on-change (handle-line-change item-id col-id)}
                          placeholder (assoc :placeholder placeholder)
                          step (assoc :step step)
                          min-val (assoc :min min-val)))))
                  ($ :td {:class "w-8 text-center"}
                    ($ :button {:id (str "btn-remove-" field-key "-line-item-" item-id)
                                :class "text-error hover:text-error/80 p-1"
                                :type "button"
                                :title (t :common/remove-line-item)
                                :on-click #(remove-item item-id)}
                      ($ :svg {:class "w-4 h-4" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
                        ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                                  :d "M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"}))))))))))
      (when error
        ($ :div {:class "text-error text-sm mt-1"} error)))))
