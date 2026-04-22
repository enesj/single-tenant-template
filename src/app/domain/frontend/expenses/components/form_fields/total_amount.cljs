(ns app.domain.frontend.expenses.components.form-fields.total-amount
  "Total amount input component for expense form"
  (:require
    [app.domain.frontend.expenses.components.form-fields.helpers :as h]
    [app.template.frontend.i18n :as i18n]
    [uix.core :refer [$ defui use-effect use-state]]))

(def ^:private amount-tolerance 0.01)

(defui total-amount-input
  [{:keys [id value on-change values error field-spec]}]
  (let [t (i18n/use-t)
        field-key (let [id* id]
                    (cond
                      (keyword? id*) (name id*)
                      (string? id*) id*
                      :else "total_amount"))
        input-id (str "expense-" field-key)
        items (:items values)
        computed-total (h/line-items-total items)
        parsed-total (h/safe-parse-number value)
        [auto-total? set-auto-total!] (use-state true)
        show-use-total? (not (false? (:show-use-total? field-spec)))

        receipt-total-guess (some-> (:receipt-total-guess field-spec) h/safe-parse-number)
        totals-match? (let [diff (when (and (number? receipt-total-guess)
                                         (number? computed-total))
                                   (js/Math.abs (- receipt-total-guess computed-total)))]
                        (and (some? diff) (<= diff amount-tolerance)))

        total-diff (when (and (number? parsed-total) (number? computed-total) (pos? computed-total))
                     (js/Math.abs (- parsed-total computed-total)))
        total-mismatch? (and total-diff (> total-diff amount-tolerance))]
    (use-effect
      (fn []
        (when (and auto-total? (pos? computed-total) (not= parsed-total computed-total))
          (on-change computed-total))
        js/undefined)
      [auto-total? computed-total parsed-total on-change])
    ($ :div {:class "space-y-1"}
      ($ :div {:class "flex gap-2"}
        ($ :input {:id input-id
                   :class "ds-input ds-input-bordered w-full"
                   :type "number"
                   :step "0.01"
                   :value (or value "")
                   :on-change (fn [e]
                                (set-auto-total! false)
                                (on-change (h/safe-parse-number (.. e -target -value))))})
        (when (and show-use-total? (pos? computed-total))
          ($ :button {:id (str "btn-use-total-" input-id)
                      :class "ds-btn ds-btn-ghost ds-btn-xs"
                      :type "button"
                      :on-click (fn []
                                  (set-auto-total! true)
                                  (on-change computed-total))}
            (t :common/use-total))))
      (when (pos? computed-total)
        ($ :div {:class "flex items-center justify-between gap-3 text-xs text-base-content/60"}
          ($ :div {:class (str "truncate " (when totals-match? "text-success"))}
            (str (t :common/line-items-total) ": " (or (h/format-decimal computed-total) "0.00"))
            (when total-mismatch?
              ($ :span {:class "text-error ml-2"}
                (str "(" (t :common/does-not-match-total) ")"))))
          (when (number? receipt-total-guess)
            ($ :div {:class (str "truncate " (when totals-match? "text-success"))}
              (str (t :common/total-guess) ": " (h/format-decimal receipt-total-guess))))))
      (when error
        ($ :div {:id (str input-id "-error")
                 :class "text-error text-sm mt-1"}
          error)))))

(defui totals-display
  "Display-only component for receipt approval showing Line items total and editable Total guess.
   The actual total_amount is auto-set from computed total but is not editable directly."
  [{:keys [id value on-change values error field-spec inline]}]
  (let [t (i18n/use-t)
        field-key (cond
                    (keyword? id) (name id)
                    (string? id) id
                    :else "total_amount")
        input-id (str "expense-" field-key)
        items (:items values)
        computed-total (h/line-items-total items)
        receipt-total-guess (some-> (:receipt-total-guess field-spec) h/safe-parse-number)
        [local-guess set-local-guess!] (use-state (or receipt-total-guess ""))
        parsed-guess (h/safe-parse-number local-guess)
        totals-match? (let [diff (when (and (number? parsed-guess)
                                         (number? computed-total))
                                   (js/Math.abs (- parsed-guess computed-total)))]
                        (and (some? diff) (<= diff amount-tolerance)))]
    ;; Auto-sync computed total to form value
    (use-effect
      (fn []
        (when (and (pos? computed-total) (not= (h/safe-parse-number value) computed-total))
          (on-change computed-total))
        js/undefined)
      [computed-total value on-change])
    (let [inline? (not (false? inline))
          row-class (str "mb-4" (if inline?
                                  " flex flex-row items-start gap-4"
                                  " flex flex-col items-start gap-2"))
          label-class (str "ds-label mb-0" (when inline? " min-w-[150px] text-left"))
          value-wrap-class (when inline? "flex-1 w-full text-left")
          label-text-class (str "font-bold" (when totals-match? " text-success"))
          value-text-class (str "text-lg font-semibold font-mono" (when totals-match? " text-success"))]
      ($ :div
        ;; Line items total (read-only, calculated from items)
        ($ :div {:class row-class}
          ($ :label {:class label-class}
            ($ :span {:class label-text-class} (str (t :common/line-items-total) ":")))
          ($ :div {:class value-wrap-class}
            ($ :div {:class value-text-class}
              (or (h/format-decimal computed-total) "0.00"))))

        ;; Total guess (editable)
        ($ :div {:class row-class}
          ($ :label {:class label-class}
            ($ :span {:class label-text-class} (str (t :common/total-guess) ":")))
          ($ :div {:class value-wrap-class}
            ($ :div {:class "flex items-center gap-2"}
              ($ :input {:id (str input-id "-guess")
                         :class (str "ds-input ds-input-bordered ds-input-sm w-32 text-right text-lg font-semibold "
                                  (when totals-match? "text-success"))
                         :type "number"
                         :step "0.01"
                         :value (or local-guess "")
                         :on-change (fn [e]
                                      (set-local-guess! (.. e -target -value)))})
              (when (and (pos? computed-total) (not totals-match?))
                ($ :button {:id (str "btn-use-line-total-" input-id)
                            :class "ds-btn ds-btn-ghost ds-btn-xs"
                            :type "button"
                            :on-click (fn []
                                        (set-local-guess! computed-total))}
                  (t :common/use-line-total))))))

        (when error
          ($ :div {:id (str input-id "-error")
                   :class "text-error text-sm mt-1"}
            error))))))
