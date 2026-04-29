(ns app.mobile.frontend.pages.manual-entry
  "Mobile quick-add expense page orchestration."
  (:require
    [app.domain.frontend.expenses.components.form-fields.helpers
     :refer [current-datetime-local]]
    [app.domain.frontend.expenses.shared.manual-entry.core :as manual-entry]
    [app.mobile.frontend.components.header :refer [mobile-header]]
    [app.mobile.frontend.pages.manual-entry.components :as components]
    [app.mobile.frontend.pages.manual-entry.events]
    [app.mobile.frontend.pages.manual-entry.helpers :as manual-entry-helpers]
    [app.template.frontend.i18n :refer [use-t]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui] :as uix]
    [uix.re-frame :refer [use-subscribe]]))

;; Compatibility for existing callers that import this component from the page ns.
(def autocomplete-field components/autocomplete-field)

(defui manual-entry-page []
  (let [t (use-t)
        payers (use-subscribe [:mobile/payers])
        expense-categories (use-subscribe [:mobile/expense-categories])
        submitting? (use-subscribe [:mobile/manual-entry-submitting?])
        remote-error (use-subscribe [:mobile/manual-entry-error])
        [phase set-phase!] (uix/use-state :phase-1)
        [default-category-preselect-enabled? set-default-category-preselect-enabled!] (uix/use-state true)
        [local-error set-local-error!] (uix/use-state nil)
        [form set-form!] (uix/use-state {:items []
                                         :context {}
                                         :purchased-at (current-datetime-local)
                                         :currency "BAM"
                                         :payer-id nil
                                         :notes ""})
        item-article-ids (manual-entry/article-ids-from-items (:items form))
        selected-supplier-id (get-in form [:context :supplier :id])
        selected-category (get-in form [:context :category])
        effective-payer-id (or (:payer-id form)
                             (manual-entry/payer-default-id payers))
        submit-disabled? (boolean (manual-entry-helpers/submit-error-key (:items form) effective-payer-id))
        visible-error (or local-error remote-error)
        save-expense! (fn [context]
                        (if-let [error-key (manual-entry-helpers/submit-error-key (:items form) effective-payer-id)]
                          (set-local-error! error-key)
                          (do
                            (set-local-error! nil)
                            (rf/dispatch [:mobile/create-expense
                                          (assoc form
                                            :payer-id effective-payer-id
                                            :context context)]))))
        cancel-entry! (fn []
                        (.back js/window.history))]
    (uix/use-effect
      (fn []
        (rf/dispatch [:mobile/fetch-payers])
        (rf/dispatch [:mobile/fetch-expense-categories])
        js/undefined)
      [])

    (uix/use-effect
      (fn []
        (when (and local-error
                (or (seq (:items form))
                  effective-payer-id
                  (seq (:context form))))
          (set-local-error! nil))
        js/undefined)
      [form (:items form) (:context form) effective-payer-id local-error])

    (uix/use-effect
      (fn []
        (when (and effective-payer-id (not (:payer-id form)))
          (set-form! (fn [current-form]
                       (if (:payer-id current-form)
                         current-form
                         (assoc current-form :payer-id effective-payer-id)))))
        js/undefined)
      [form effective-payer-id (:payer-id form)])

    (uix/use-effect
      (fn []
        (rf/dispatch [:mobile/fetch-quick-add-history selected-supplier-id])
        js/undefined)
      [selected-supplier-id])

    (uix/use-effect
      (fn []
        (rf/dispatch [:mobile/fetch-cooccurring-articles item-article-ids selected-supplier-id])
        js/undefined)
      [item-article-ids selected-supplier-id])

    (uix/use-effect
      (fn []
        (when (and (= phase :phase-2) (seq item-article-ids))
          (rf/dispatch [:mobile/fetch-context-suggestions item-article-ids]))
        js/undefined)
      [phase item-article-ids])

    (uix/use-effect
      (fn []
        (when-let [default-category-chip (manual-entry/default-category-chip-to-preselect
                                           expense-categories
                                           nil
                                           default-category-preselect-enabled?
                                           selected-category)]
          (set-form! (fn [current-form]
                       (if (get-in current-form [:context :category])
                         current-form
                         (assoc-in current-form [:context :category] default-category-chip))))
          (set-default-category-preselect-enabled! false))
        js/undefined)
      [expense-categories default-category-preselect-enabled? selected-category])

    ($ :<>
      ($ mobile-header {:title (if (= phase :phase-2)
                                 (t :mobile/back-to-items)
                                 (t :mobile/quick-add-title))
                        :show-back? (= phase :phase-2)
                        :on-back (when (= phase :phase-2)
                                   #(set-phase! :phase-1))})
      (case phase
        :phase-1 ($ components/phase-one {:form form
                                          :set-form! set-form!
                                          :submitting? submitting?
                                          :submit-disabled? submit-disabled?
                                          :error visible-error
                                          :on-disable-default-category-preselect #(set-default-category-preselect-enabled! false)
                                          :on-add-store #(set-phase! :phase-2)
                                          :on-save #(save-expense! (:context form))
                                          :on-cancel cancel-entry!})
        :phase-2 ($ components/phase-two {:form form
                                          :set-form! set-form!
                                          :submitting? submitting?
                                          :submit-disabled? submit-disabled?
                                          :error visible-error
                                          :on-disable-default-category-preselect #(set-default-category-preselect-enabled! false)
                                          :on-save save-expense!
                                          :on-cancel cancel-entry!})))))
