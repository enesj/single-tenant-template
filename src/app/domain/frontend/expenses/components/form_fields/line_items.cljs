(ns app.domain.frontend.expenses.components.form-fields.line-items
  "Line items input component for expense form"
  (:require
    [app.domain.frontend.expenses.components.form-fields.helpers :as h]
    [app.template.frontend.components.common :as common]
    [uix.core :refer [$ defui]]))

(defui line-items-input
  [{:keys [value on-change error field-spec]}]
  (let [items (if (seq value) value [(h/new-line-item)])
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
    ($ :div {:class "space-y-2"}
      ($ :div {:class "flex items-center justify-between mb-2"}
        ($ :h2 {:class "text-lg font-semibold"} (:label field-spec))
        ($ :button {:id (str "btn-add-" field-key "-line-item")
                    :class "ds-btn ds-btn-ghost ds-btn-sm"
                    :type "button"
                    :on-click add-item}
          "Add line item"))
      (let [overflow-y-class (or (:overflow-y-class field-spec) "overflow-y-auto")
            stable-gutter? (true? (:scrollbar-gutter-stable? field-spec))
            style (merge {:maxHeight "300px"}
                    (:style field-spec)
                    (when stable-gutter? {:scrollbarGutter "stable"}))]
        ($ :div {:class (str "border border-base-300 rounded-lg bg-base-200/30 "
                          overflow-y-class " "
                          (:max-height-class field-spec))
                 :style style}
          ($ :div {:class "min-w-full"}
            ($ :div {:class "flex bg-base-200 sticky top-0 z-10 gap-2 px-2 py-2 border-b border-base-300"}
              (for [col columns
                    :let [label (:label col)
                          width (:width col)]]
                ($ :div {:key (:id col)
                         :class (str "font-bold text-sm " (or width ""))}
                  label))
              ($ :div {:class "w-8"}))
            ($ :div {:class "divide-y divide-base-200"}
              (for [item items
                    :let [item-id (:id item)]]
                ($ :div {:key item-id
                         :class "flex items-center gap-2 px-2 py-1"}
                  (for [col columns
                        :let [col-id (:id col)
                              val (get item col-id)
                              type (:type col)
                              placeholder (:placeholder col)
                              step (:step col)
                              min-val (:min col)
                              width (:width col)
                              is-number? (= type :number)]]
                    ($ :div {:key col-id
                             :class (or width "")}
                      ($ common/input
                        (cond-> {:id (input-id item-id col-id)
                                 :class (str "ds-input ds-input-bordered ds-input-sm w-full"
                                          (when is-number? " text-right font-mono"))
                                 :value (or val "")
                                 :type (name (or type :text))
                                 :on-change (handle-line-change item-id col-id)}
                          placeholder (assoc :placeholder placeholder)
                          step (assoc :step step)
                          min-val (assoc :min min-val)))))
                  ($ :div {:class "w-8 shrink-0 text-center"}
                    ($ :button {:id (str "btn-remove-" field-key "-line-item-" item-id)
                                :class "text-error hover:text-error/80 p-1"
                                :type "button"
                                :title "Remove line item"
                                :on-click #(remove-item item-id)}
                      ($ :svg {:class "w-4 h-4" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
                        ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                                  :d "M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"}))))))))))
      (when error
        ($ :div {:class "text-error text-sm mt-1"} error)))))
