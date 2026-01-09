(ns app.domain.frontend.expenses.components.form-fields.selects
  "Select input components for expense form (supplier, article, expense)"
  (:require
    [app.domain.frontend.expenses.components.form-fields.helpers :as h]
    [app.domain.frontend.expenses.events.articles :as articles-events]
    [app.domain.frontend.expenses.events.suppliers :as suppliers-events]
    [app.domain.frontend.expenses.ui.select-options :as select-options]
    [app.template.frontend.components.common :as common]
    [app.template.frontend.components.modal :refer [modal]]
    [app.template.frontend.components.form.fields.select :refer [select-input]]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defui supplier-select-with-inline-create
  [{:keys [id label error required inline class on-change value form-id formId field-spec]}]
  (let [suppliers (use-subscribe [:expenses/suppliers])
        creating? (use-subscribe [:expenses/supplier-inline-create-loading?])
        create-error (use-subscribe [:expenses/supplier-inline-create-error])
        [open? set-open!] (use-state false)
        [display-name set-display-name!] (use-state "")
        [local-error set-local-error!] (use-state nil)

        form-id* (or form-id formId)
        field-key (or (some-> (:id field-spec) name)
                    (some-> id h/dom-id)
                    "supplier_id")
        base-id (or form-id* "expense-form")

        default-display-name (some-> (or (:create-default-display-name field-spec)
                                       (:new-supplier-default-display-name field-spec)
                                       (:default-display-name field-spec))
                               str
                               str/trim
                               not-empty)

        select-id (or (when (string? id) id)
                    (str base-id "-" field-key "-select"))
        error-id (str select-id "-error")
        add-btn-id (str "btn-add-supplier-" base-id)

        modal-id (str base-id "-" field-key "-create-supplier-modal")
        name-input-id (str base-id "-" field-key "-create-supplier-display-name-input")
        cancel-btn-id (str "btn-cancel-supplier-create-" base-id)
        save-btn-id (str "btn-save-supplier-create-" base-id)

        label-class (str "ds-label" (when inline " mb-0 min-w-[150px] text-left"))
        wrapper-class (str "mb-4" (if inline " flex flex-row items-start gap-4"
                                    " flex flex-col items-start gap-4"))

        receipt-id-str (some-> (or (:receipt-id field-spec) (:receipt_id field-spec)) str)
        supplier-guess (some-> (or (:receipt-supplier-guess field-spec)
                                 (:supplier-guess field-spec)
                                 (:supplier_guess field-spec))
                         str
                         str/trim
                         not-empty)
        supplier-guess-id (when receipt-id-str (str "receipt-supplier-guess-" receipt-id-str))

        field-error-msg (cond
                          (nil? error) nil
                          (string? error) error
                          (map? error) (or (:message error) (str error))
                          :else (str error))
        opts (vec (or (seq (:options field-spec))
                    (h/options-from-items suppliers select-options/supplier-label)))

        open-modal (fn []
                     (set-local-error! nil)
                     (set-display-name! (or default-display-name ""))
                     (rf/dispatch [::suppliers-events/clear-inline-create])
                     (set-open! true))
        close-modal (fn []
                      (set-open! false)
                      (set-display-name! "")
                      (set-local-error! nil)
                      (rf/dispatch [::suppliers-events/clear-inline-create]))
        submit (fn []
                 (let [name* (str/trim (str display-name))]
                   (cond
                     (str/blank? name*)
                     (set-local-error! "Display name is required.")

                     creating?
                     nil

                     :else
                     (do
                       (set-local-error! nil)
                       (rf/dispatch [::suppliers-events/create-inline
                                     {:display_name name*}
                                     (fn [supplier]
                                       (when (and supplier (fn? on-change))
                                         (when-let [new-id (:id supplier)]
                                           (on-change (str new-id))))
                                       (close-modal))])))))]

    (use-effect
      (fn []
        (rf/dispatch [::suppliers-events/load-list {:limit 200 :offset 0}])
        js/undefined)
      [])

    (use-effect
      (fn []
        (when (and open?
                (str/blank? (str display-name))
                (not (str/blank? (str default-display-name))))
          (set-display-name! default-display-name))
        js/undefined)
      [display-name open? default-display-name])

    ($ :<>
      ($ :div {:class wrapper-class}
        ($ common/label {:text (or label "Supplier")
                         :class label-class
                         :for select-id
                         :required required})
        ($ :div {:class (when inline "flex-1 w-full text-left")}
          ($ :div {:class "flex items-start gap-2"}
            ($ :div {:class (str "flex-1 " (or class ""))}
              ($ common/select {:id select-id
                                :value value
                                :options opts
                                :on-change on-change}))
            ($ :button {:id add-btn-id
                        :type "button"
                        :class "ds-btn ds-btn-ghost ds-btn-sm"
                        :on-click open-modal}
              "New"))
          (when supplier-guess
            ($ :div {:id supplier-guess-id
                     :class "text-xs text-base-content/60 self-center max-w-[18rem] truncate"}
              (str "Guess: " supplier-guess))))
        (when field-error-msg
          ($ :div {:id error-id
                   :class "text-error text-sm mt-1"}
            field-error-msg)))

      (when open?
        ($ modal {:id modal-id
                  :on-close close-modal
                  :draggable? false
                  :width "520px"
                  :header "New supplier"}
          ($ :div {:class "p-6 space-y-4"}
            (when (or local-error create-error)
              ($ :div {:class "ds-alert ds-alert-error"}
                ($ :span (or local-error create-error))))

            ($ :div {:class "space-y-2"}
              ($ :label {:class "ds-label" :for name-input-id}
                "Display name")
              ($ :input {:id name-input-id
                         :class "ds-input ds-input-bordered w-full"
                         :type "text"
                         :value display-name
                         :auto-focus true
                         :on-key-down (fn [e]
                                        (when (= "Enter" (.-key e))
                                          (.preventDefault e)
                                          (.stopPropagation e)
                                          (submit)))
                         :on-change (fn [e]
                                      (set-display-name! (.. e -target -value)))})

              ($ :div {:class "flex justify-end gap-2 pt-4"}
                ($ :button {:id cancel-btn-id
                            :type "button"
                            :class "ds-btn"
                            :on-click close-modal}
                  "Cancel")
                ($ :button {:id save-btn-id
                            :type "button"
                            :class "ds-btn ds-btn-primary"
                            :disabled (or creating?
                                        (str/blank? (str/trim (str display-name))))
                            :on-click (fn [e]
                                        (.preventDefault e)
                                        (.stopPropagation e)
                                        (submit))}
                  (if creating? "Saving..." "Create"))))))))))

(defui supplier-select-input
  [{:keys [id label error required inline class on-change value form-id formId]}]
  (let [suppliers (use-subscribe [:expenses/suppliers])
        form-id* (or form-id formId)
        field-id (or id (when form-id* (str form-id* "-select")))
        options (h/options-from-items suppliers select-options/supplier-label)]
    (use-effect
      (fn []
        (rf/dispatch [::suppliers-events/load-list {:limit 200 :offset 0}])
        js/undefined)
      [])
    ($ select-input
      {:id field-id
       :formId form-id*
       :label label
       :error error
       :required required
       :inline inline
       :class class
       :value value
       :options options
       :on-change on-change})))

(defui article-select-input
  [{:keys [id label error required inline class on-change value form-id formId]}]
  (let [articles (use-subscribe [:expenses/articles])
        form-id* (or form-id formId)
        field-id (or id (when form-id* (str form-id* "-select")))
        options (h/options-from-items articles select-options/article-label)]
    (use-effect
      (fn []
        (rf/dispatch [::articles-events/load-list {:limit 200 :offset 0}])
        js/undefined)
      [])
    ($ select-input
      {:id field-id
       :formId form-id*
       :label label
       :error error
       :required required
       :inline inline
       :class class
       :value value
       :options options
       :on-change on-change})))

(defui expense-select-input
  [{:keys [id label error required inline class on-change value form-id formId]}]
  (let [expenses (use-subscribe [:expenses/expenses])
        form-id* (or form-id formId)
        field-id (or id (when form-id* (str form-id* "-select")))
        options (h/options-from-items expenses select-options/expense-label)]
    (use-effect
      (fn []
        (rf/dispatch [:app.domain.frontend.expenses.events.expenses/load-list {:limit 200 :offset 0}])
        js/undefined)
      [])
    ($ select-input
      {:id field-id
       :formId form-id*
       :label label
       :error error
       :required required
       :inline inline
       :class class
       :value value
       :options options
       :on-change on-change})))
