(ns app.domain.frontend.expenses.pages.user.expense-settings
  "User-facing expense settings page."
  (:require
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.i18n :refer [use-t]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-state]]
    [uix.re-frame :refer [use-subscribe]]))

;; ========================================================================
;; Setting Section Component
;; ========================================================================

(defui setting-section [{:keys [title description children]}]
  ($ :div {:class "bg-white rounded-xl shadow-sm border border-base-200 p-6"}
    ($ :div {:class "mb-4"}
      ($ :h3 {:class "font-semibold"} title)
      (when description
        ($ :p {:class "text-sm text-base-content/60 mt-1"} description)))
    children))

(defui setting-row [{:keys [label description children]}]
  ($ :div {:class "flex items-center justify-between py-3 border-b border-base-200 last:border-0"}
    ($ :div {:class "flex-1"}
      ($ :p {:class "font-medium text-sm"} label)
      (when description
        ($ :p {:class "text-xs text-base-content/60"} description)))
    ($ :div {:class "ml-4"}
      children)))

;; ========================================================================
;; Main Page
;; ========================================================================

(defui expense-settings-page []
  (let [t (use-t)
        user (use-subscribe [:current-user])
        settings (or (use-subscribe [:user-expenses/settings]) {})
        loading? (boolean (use-subscribe [:user-expenses/settings-loading?]))
        saving? (boolean (use-subscribe [:user-expenses/settings-saving?]))
        power-user? (use-subscribe [:expenses/power-user?])
        can-write? (use-subscribe [:expenses/can-write?])
        [default-currency set-default-currency!] (use-state (or (:default-currency settings) "BAM"))
        ;; Keep payer-id normalized as (string-or-nil). Avoid "" so we don't mark
        ;; the form dirty when the user re-selects "None".
        [default-payer set-default-payer!] (use-state (some-> (:default-payer-id settings) str))
        [default-category set-default-category!] (use-state (some-> (:default-expense-category-id settings) str))
        [default-note set-default-note!] (use-state (or (:default-note settings) ""))
        expense-categories (or (use-subscribe [:user-expenses/expense-categories]) [])
        [notifications set-notifications!] (use-state (if (contains? settings :notifications-enabled)
                                                        (:notifications-enabled settings)
                                                        true))
        [auto-post-after-upload-enabled set-auto-post-after-upload-enabled!] (use-state (boolean (:auto-post-after-upload-enabled settings)))
        [receipt-refine-enabled set-receipt-refine-enabled!] (use-state (boolean (:receipt-refine-enabled settings)))
        payers (or (use-subscribe [:user-expenses/payers]) [])

        ;; Compute "dirty" state to enable Save only when values differ.
        current-currency (or (:default-currency settings) "BAM")
        current-payer (some-> (:default-payer-id settings) str)
        current-category (some-> (:default-expense-category-id settings) str)
        current-note (or (:default-note settings) "")
        current-notifications (if (contains? settings :notifications-enabled)
                                (boolean (:notifications-enabled settings))
                                true)
        current-auto-post-after-upload-enabled (boolean (:auto-post-after-upload-enabled settings))
        current-receipt-refine-enabled (boolean (:receipt-refine-enabled settings))
        dirty? (or (not= default-currency current-currency)
                 (not= (some-> default-payer str) current-payer)
                 (not= (some-> default-category str) current-category)
                 (not= default-note current-note)
                 (not= (boolean notifications) current-notifications)
                 (not= (boolean auto-post-after-upload-enabled) current-auto-post-after-upload-enabled)
                 (not= (boolean receipt-refine-enabled) current-receipt-refine-enabled))

        handle-save (fn []
                      (let [settings {:default-currency default-currency
                                      :default-payer-id default-payer
                                      :default-expense-category-id default-category
                                      :default-note default-note
                                      :notifications-enabled notifications
                                      :auto-post-after-upload-enabled auto-post-after-upload-enabled
                                      :receipt-refine-enabled receipt-refine-enabled}]
                        (rf/dispatch [:user-expenses/save-settings settings])))]

    ;; Fetch settings and payers on mount
    (use-effect
      (fn []
        (rf/dispatch [:user-expenses/fetch-settings])
        (rf/dispatch [:user-expenses/fetch-payers {:limit 100}])
        (rf/dispatch [:user-expenses/fetch-expense-categories {:limit 500 :offset 0}])
        js/undefined)
      [])

    ;; Update local state when settings load
    (use-effect
      (fn []
        (when (seq settings)
          ;; Always set from settings (including nil payer-id / false notifications).
          (set-default-currency! (or (:default-currency settings) "BAM"))
          (set-default-payer! (some-> (:default-payer-id settings) str))
          (set-default-category! (some-> (:default-expense-category-id settings) str))
          (set-default-note! (or (:default-note settings) ""))
          (set-notifications!
            (if (contains? settings :notifications-enabled)
              (boolean (:notifications-enabled settings))
              true))
          (set-auto-post-after-upload-enabled! (boolean (:auto-post-after-upload-enabled settings)))
          (set-receipt-refine-enabled!
            (boolean (:receipt-refine-enabled settings)))))
      [settings])

    ($ :div {:class "min-h-screen bg-base-100"}
      ;; Header
      ($ :header {:class "bg-white border-b border-base-200"}
        ($ :div {:class "max-w-4xl mx-auto px-4 py-4 sm:py-6"}
          ($ :div {:class "flex items-center justify-between"}
            ($ :div
              ($ :div {:class "text-sm ds-breadcrumbs"}
                ($ :ul
                  ($ :li ($ :a {:href "/expenses"} (t :expense-settings/breadcrumb-expenses)))
                  ($ :li (t :expense-settings/breadcrumb-settings))))
              ($ :h1 {:class "text-xl sm:text-2xl font-bold"} (t :expense-settings/title)))
            ($ :div {:class "flex gap-2"}
              ($ button {:btn-type :ghost
                         :id "btn-cancel-settings"
                         :on-click #(rf/dispatch [:navigate-to "/expenses"])}
                (t :expense-settings/cancel))
              ($ button {:btn-type :primary
                         :id "btn-save-settings"
                         :loading saving?
                         :disabled (or (not can-write?) (not dirty?) loading?)
                         :on-click handle-save}
                (t :expense-settings/save))))))

      ;; Read-only banner for viewers
      (when-not can-write?
        ($ :div {:class "max-w-4xl mx-auto px-4 pt-4"
                 :id "settings-read-only-banner"}
          ($ :div {:class "ds-alert ds-alert-info"}
            ($ :span (t :expense-settings/read-only-notice)))))

      ;; Content
      ($ :main {:class "max-w-4xl mx-auto px-4 py-6 space-y-6"}
        (if loading?
          ($ :div {:class "space-y-6"}
            (for [i (range 3)]
              ($ :div {:key i :class "bg-base-200 rounded-xl h-48 animate-pulse"})))

          ($ :<>
            ;; Default preferences
            ($ setting-section {:title (t :expense-settings/section-preferences)
                                :description (t :expense-settings/section-pref-desc)}
              ($ :div {:class "space-y-1"}
                ($ setting-row {:label (t :expense-settings/currency-label)
                                :description (t :expense-settings/currency-desc)}
                  ($ :select {:id "settings-currency-select"
                              :class "ds-select ds-select-sm ds-select-bordered"
                              :value default-currency
                              :disabled (not can-write?)
                              :on-change #(set-default-currency! (.. % -target -value))}
                    ($ :option {:value "BAM"} "BAM - Convertible Mark")
                    ($ :option {:value "EUR"} "EUR - Euro")
                    ($ :option {:value "USD"} "USD - US Dollar")))

                ($ setting-row {:label (t :expense-settings/payer-label)
                                :description (t :expense-settings/payer-desc)}
                  ($ :select {:id "settings-payer-select"
                              :class "ds-select ds-select-sm ds-select-bordered"
                              :value (or default-payer "")
                              :disabled (not can-write?)
                              :on-change #(let [v (.. % -target -value)]
                                            (set-default-payer! (when (seq v) v)))}
                    ($ :option {:value ""} (t :expense-settings/payer-none))
                    (for [p payers]
                      ($ :option {:key (:id p) :value (:id p)}
                        (:label p)))))

                ($ setting-row {:label (t :expense-settings/category-label)
                                :description (t :expense-settings/category-desc)}
                  ($ :select {:id "settings-category-select"
                              :class "ds-select ds-select-sm ds-select-bordered"
                              :value (or default-category "")
                              :disabled (not can-write?)
                              :on-change #(let [v (.. % -target -value)]
                                            (set-default-category! (when (seq v) v)))}
                    ($ :option {:value ""} (t :expense-settings/category-none))
                    (for [c expense-categories]
                      ($ :option {:key (:id c) :value (:id c)}
                        (:name c)))))

                ($ setting-row {:label (t :expense-settings/note-label)
                                :description (t :expense-settings/note-desc)}
                  ($ :textarea {:id "settings-default-note"
                                :class "ds-textarea ds-textarea-bordered ds-textarea-sm w-full max-w-xs"
                                :rows 2
                                :value default-note
                                :disabled (not can-write?)
                                :placeholder (t :expense-settings/note-placeholder)
                                :on-change #(set-default-note! (.. % -target -value))}))))

            ;; Notifications
            ($ setting-section {:title (t :expense-settings/section-notif)
                                :description (t :expense-settings/section-notif-desc)}
              ($ setting-row {:label (t :expense-settings/email-notif)
                              :description (t :expense-settings/email-notif-desc)}
                ($ :input {:id "settings-notifications-toggle"
                           :type "checkbox"
                           :class "ds-toggle ds-toggle-primary"
                           :checked notifications
                           :disabled (not can-write?)
                           :on-change #(set-notifications! (.. % -target -checked))})))

             ;; Receipts
            ($ setting-section {:title (t :expense-settings/section-receipts)
                                :description (t :expense-settings/section-receipts-desc)}
              ($ setting-row {:label (t :expense-settings/auto-post-label)
                              :description (t :expense-settings/auto-post-desc)}
                ($ :input {:id "settings-auto-post-after-upload-toggle"
                           :type "checkbox"
                           :class "ds-toggle ds-toggle-primary"
                           :checked auto-post-after-upload-enabled
                           :disabled (not can-write?)
                           :on-change #(set-auto-post-after-upload-enabled! (.. % -target -checked))}))
              ($ setting-row {:label (t :expense-settings/refine-label)
                              :description (t :expense-settings/refine-desc)}
                ($ :input {:id "settings-receipt-refine-toggle"
                           :type "checkbox"
                           :class "ds-toggle ds-toggle-primary"
                           :checked receipt-refine-enabled
                           :disabled (not can-write?)
                           :on-change #(set-receipt-refine-enabled! (.. % -target -checked))})))

            ;; Account Info
            ($ setting-section {:title (t :expense-settings/section-account)
                                :description (t :expense-settings/section-account-desc)}
              ($ :div {:class "space-y-3"}
                ($ :div {:class "flex items-center gap-4 p-3 bg-base-200 rounded-lg"}
                  ($ :div {:class "w-12 h-12 bg-primary/10 rounded-full flex items-center justify-center"}
                    ($ :span {:class "text-xl"} "👤"))
                  ($ :div
                    ($ :p {:class "font-medium"}
                      (or (:full-name user) (:email user) "User"))
                    ($ :p {:class "text-sm text-base-content/60"}
                      (:email user))))
                ($ :p {:class "text-sm text-base-content/60"}
                  (t :expense-settings/account-update-note))))

            ;; Data Management (export + danger zone)
            ($ setting-section {:title (t :expense-settings/section-data)
                                :description (t :expense-settings/section-data-desc)}
              ($ :div {:class "space-y-3"}
                ($ :div {:class "flex items-center justify-between"}
                  ($ :div
                    ($ :p {:class "font-medium text-sm"} (t :expense-settings/export-label))
                    ($ :p {:class "text-xs text-base-content/60"}
                      (t :expense-settings/export-desc)))
                  ($ button {:btn-type :outline
                             :id "btn-export-data"
                             :size :sm
                             :on-click #(rf/dispatch [:user-expenses/export {:format :csv :all true}])}
                    (t :expense-settings/export-btn)))

                ;; Danger Zone - only for admin/owner
                (when power-user?
                  ($ :div {:class "border-t pt-3 mt-3"}
                    ($ :div {:class "flex items-center justify-between"}
                      ($ :div
                        ($ :p {:class "font-medium text-sm text-error"} (t :expense-settings/delete-all-label))
                        ($ :p {:class "text-xs text-base-content/60"}
                          (t :expense-settings/delete-all-desc)))
                      ($ button {:btn-type :error
                                 :id "btn-delete-all-expenses"
                                 :size :sm
                                 :on-click (fn []
                                             (rf/dispatch [:confirm-dialog/show
                                                           {:title (t :expense-settings/delete-all-confirm-title)
                                                            :message (t :expense-settings/delete-all-confirm-msg)
                                                            :confirm-text (t :expense-settings/delete-all-confirm-btn)
                                                            :on-confirm (fn []
                                                                          (rf/dispatch [:user-expenses/delete-all "DELETE_ALL_EXPENSES"]))}]))}
                        (t :expense-settings/delete-all-btn)))))))))))))
