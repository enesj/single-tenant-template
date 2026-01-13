(ns app.domain.frontend.expenses.pages.user.expense-settings
  "User-facing expense settings page."
  (:require
    [app.template.frontend.components.button :refer [button]]
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
  (let [user (use-subscribe [:current-user])
        settings (or (use-subscribe [:user-expenses/settings]) {})
        loading? (boolean (use-subscribe [:user-expenses/settings-loading?]))
        saving? (boolean (use-subscribe [:user-expenses/settings-saving?]))
        power-user? (use-subscribe [:expenses/power-user?])
        [default-currency set-default-currency!] (use-state (or (:default_currency settings) "BAM"))
        ;; Keep payer-id normalized as (string-or-nil). Avoid "" so we don't mark
        ;; the form dirty when the user re-selects "None".
        [default-payer set-default-payer!] (use-state (some-> (:default_payer_id settings) str))
        [notifications set-notifications!] (use-state (if (contains? settings :notifications_enabled)
                                                        (:notifications_enabled settings)
                                                        true))
        payers (or (use-subscribe [:user-expenses/payers]) [])

        ;; Compute "dirty" state to enable Save only when values differ.
        current-currency (or (:default_currency settings) "BAM")
        current-payer (some-> (:default_payer_id settings) str)
        current-notifications (if (contains? settings :notifications_enabled)
                                (boolean (:notifications_enabled settings))
                                true)
        dirty? (or (not= default-currency current-currency)
                 (not= (some-> default-payer str) current-payer)
                 (not= (boolean notifications) current-notifications))

        handle-save (fn []
                      (let [settings {:default_currency default-currency
                                      :default_payer_id default-payer
                                      :notifications_enabled notifications}]
                        (rf/dispatch [:user-expenses/save-settings settings])))]

    ;; Fetch settings and payers on mount
    (use-effect
      (fn []
        (rf/dispatch [:user-expenses/fetch-settings])
        (rf/dispatch [:user-expenses/fetch-payers {:limit 100}])
        js/undefined)
      [])

    ;; Update local state when settings load
    (use-effect
      (fn []
        (when (seq settings)
          ;; Always set from settings (including nil payer-id / false notifications).
          (set-default-currency! (or (:default_currency settings) "BAM"))
          (set-default-payer! (some-> (:default_payer_id settings) str))
          (set-notifications!
            (if (contains? settings :notifications_enabled)
              (boolean (:notifications_enabled settings))
              true))))
      [settings])

    ($ :div {:class "min-h-screen bg-base-100"}
      ;; Header
      ($ :header {:class "bg-white border-b border-base-200"}
        ($ :div {:class "max-w-4xl mx-auto px-4 py-4 sm:py-6"}
          ($ :div {:class "flex items-center justify-between"}
            ($ :div
              ($ :div {:class "text-sm ds-breadcrumbs"}
                ($ :ul
                  ($ :li ($ :a {:href "/expenses"} "Expenses"))
                  ($ :li "Settings")))
              ($ :h1 {:class "text-xl sm:text-2xl font-bold"} "Expense Settings"))
            ($ :div {:class "flex gap-2"}
              ($ button {:btn-type :ghost
                         :id "btn-cancel-settings"
                         :on-click #(rf/dispatch [:navigate-to "/expenses"])}
                "Cancel")
              ($ button {:btn-type :primary
                         :id "btn-save-settings"
                         :loading saving?
                         :disabled (or (not dirty?) loading?)
                         :on-click handle-save}
                "Save Changes")))))

      ;; Content
      ($ :main {:class "max-w-4xl mx-auto px-4 py-6 space-y-6"}
        (if loading?
          ($ :div {:class "space-y-6"}
            (for [i (range 3)]
              ($ :div {:key i :class "bg-base-200 rounded-xl h-48 animate-pulse"})))

          ($ :<>
            ;; Default preferences
            ($ setting-section {:title "Default Preferences"
                                :description "Set your default values for new expenses."}
              ($ :div {:class "space-y-1"}
                ($ setting-row {:label "Default Currency"
                                :description "Currency used for new expenses"}
                  ($ :select {:id "settings-currency-select"
                              :class "ds-select ds-select-sm ds-select-bordered"
                              :value default-currency
                              :on-change #(set-default-currency! (.. % -target -value))}
                    ($ :option {:value "BAM"} "BAM - Convertible Mark")
                    ($ :option {:value "EUR"} "EUR - Euro")
                    ($ :option {:value "USD"} "USD - US Dollar")))

                ($ setting-row {:label "Default Payer"
                                :description "Payer automatically selected for new expenses"}
                  ($ :select {:id "settings-payer-select"
                              :class "ds-select ds-select-sm ds-select-bordered"
                              :value (or default-payer "")
                              :on-change #(let [v (.. % -target -value)]
                                            (set-default-payer! (when (seq v) v)))}
                    ($ :option {:value ""} "None")
                    (for [p payers]
                      ($ :option {:key (:id p) :value (:id p)}
                        (:label p)))))))

            ;; Notifications
            ($ setting-section {:title "Notifications"
                                :description "Manage how you receive updates about your expenses."}
              ($ setting-row {:label "Email Notifications"
                              :description "Receive weekly expense summaries"}
                ($ :input {:id "settings-notifications-toggle"
                           :type "checkbox"
                           :class "ds-toggle ds-toggle-primary"
                           :checked notifications
                           :on-change #(set-notifications! (.. % -target -checked))})))

            ;; Account Info
            ($ setting-section {:title "Account Information"
                                :description "Your account details for expense tracking."}
              ($ :div {:class "space-y-3"}
                ($ :div {:class "flex items-center gap-4 p-3 bg-base-200 rounded-lg"}
                  ($ :div {:class "w-12 h-12 bg-primary/10 rounded-full flex items-center justify-center"}
                    ($ :span {:class "text-xl"} "👤"))
                  ($ :div
                    ($ :p {:class "font-medium"}
                      (or (:full_name user) (:email user) "User"))
                    ($ :p {:class "text-sm text-base-content/60"}
                      (:email user))))
                ($ :p {:class "text-sm text-base-content/60"}
                  "To update your account information, please visit your profile settings.")))

            ;; Data Management (export + danger zone)
            ($ setting-section {:title "Data Management"
                                :description "Export or manage your expense data."}
              ($ :div {:class "space-y-3"}
                ($ :div {:class "flex items-center justify-between"}
                  ($ :div
                    ($ :p {:class "font-medium text-sm"} "Export All Data")
                    ($ :p {:class "text-xs text-base-content/60"}
                      "Download all your expense records as CSV"))
                  ($ button {:btn-type :outline
                             :id "btn-export-data"
                             :size :sm
                             :on-click #(rf/dispatch [:user-expenses/export {:format :csv :all true}])}
                    "Export"))

                ;; Danger Zone - only for admin/owner
                (when power-user?
                  ($ :div {:class "border-t pt-3 mt-3"}
                    ($ :div {:class "flex items-center justify-between"}
                      ($ :div
                        ($ :p {:class "font-medium text-sm text-error"} "Delete All Expenses")
                        ($ :p {:class "text-xs text-base-content/60"}
                          "Permanently remove all your expense records"))
                      ($ button {:btn-type :error
                                 :id "btn-delete-all-expenses"
                                 :size :sm
                                 :on-click (fn []
                                             (rf/dispatch [:confirm-dialog/show
                                                           {:title "Delete All Expenses?"
                                                            :message "This action cannot be undone. All your expense records will be permanently deleted."
                                                            :confirm-text "Delete All"
                                                            :on-confirm (fn []
                                                                          (rf/dispatch [:user-expenses/delete-all "DELETE_ALL_EXPENSES"]))}]))}
                        "Delete All"))))))))))))
