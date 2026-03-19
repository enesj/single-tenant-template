(ns app.admin.frontend.pages.domain.expenses.global-settings
  "Admin page for global expenses settings."
  (:require
    [clojure.string :as str]
    [app.admin.frontend.components.layout :as layout]
    [app.domain.frontend.expenses.events.admin-global-settings]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.stats :refer [page-header]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defn- fmt-rate [rate]
  (if (nil? rate)
    "—"
    (.toLocaleString (js/Number rate) "en-US"
      #js {:minimumFractionDigits 4
           :maximumFractionDigits 6})))

(defn- fmt-timestamp [value]
  (if (seq (str value))
    (str value)
    "—"))

(defui section-card [{:keys [title subtitle actions children]}]
  ($ :section {:class "bg-base-100 rounded-2xl border border-base-300 shadow-sm overflow-hidden"}
    ($ :div {:class "px-6 py-4 border-b border-base-200 bg-base-200/30 flex flex-col gap-3 md:flex-row md:items-center md:justify-between"}
      ($ :div
        ($ :h2 {:class "text-lg font-bold text-base-content"} title)
        (when subtitle
          ($ :p {:class "text-sm text-base-content/60 mt-1"} subtitle)))
      (when actions
        ($ :div {:class "flex items-center gap-2"} actions)))
    ($ :div {:class "p-6"} children)))

(defui admin-expenses-global-settings-page []
  (let [settings (or (use-subscribe [:admin/expenses-settings-settings]) {})
        settings-loading? (boolean (use-subscribe [:admin/expenses-settings-settings-loading?]))
        settings-saving? (boolean (use-subscribe [:admin/expenses-settings-settings-saving?]))
        settings-error (use-subscribe [:admin/expenses-settings-settings-error])
        currencies (or (use-subscribe [:admin/expenses-settings-currencies]) [])
        currencies-loading? (boolean (use-subscribe [:admin/expenses-settings-currencies-loading?]))
        currencies-saving? (boolean (use-subscribe [:admin/expenses-settings-currencies-saving?]))
        currencies-error (use-subscribe [:admin/expenses-settings-currencies-error])
        rates (or (use-subscribe [:admin/expenses-settings-rates]) [])
        rates-date (use-subscribe [:admin/expenses-settings-rates-date])
        rates-loading? (boolean (use-subscribe [:admin/expenses-settings-rates-loading?]))
        rates-fetching? (boolean (use-subscribe [:admin/expenses-settings-rates-fetching?]))
        rates-error (use-subscribe [:admin/expenses-settings-rates-error])
        rates-last-result (use-subscribe [:admin/expenses-settings-rates-last-result])
        alerts (or (use-subscribe [:admin/expenses-settings-alerts]) [])
        alerts-loading? (boolean (use-subscribe [:admin/expenses-settings-alerts-loading?]))
        alerts-saving? (boolean (use-subscribe [:admin/expenses-settings-alerts-saving?]))
        alerts-error (use-subscribe [:admin/expenses-settings-alerts-error])
        current-default-currency (or (:default-currency settings) "BAM")
        current-default-note (or (:default-note settings) "")
        current-auto-publish? (boolean (:auto-publish-after-upload settings))
        current-ai-enhancement? (boolean (:ai-receipt-enhancement settings))
        [default-currency set-default-currency!] (use-state current-default-currency)
        [default-note set-default-note!] (use-state current-default-note)
        [auto-publish? set-auto-publish!] (use-state current-auto-publish?)
        [ai-enhancement? set-ai-enhancement!] (use-state current-ai-enhancement?)
        [new-currency-code set-new-currency-code!] (use-state "")
        [new-currency-name set-new-currency-name!] (use-state "")
        dirty? (or (not= default-currency current-default-currency)
                 (not= default-note current-default-note)
                 (not= auto-publish? current-auto-publish?)
                 (not= ai-enhancement? current-ai-enhancement?))
        can-add-currency? (and (= 3 (count (str/trim new-currency-code)))
                            (seq (str/trim new-currency-name))
                            (not currencies-saving?))]
    (use-effect
      (fn []
        (rf/dispatch [:admin/expenses-settings-load])
        js/undefined)
      [])

    (use-effect
      (fn []
        (set-default-currency! current-default-currency)
        (set-default-note! current-default-note)
        (set-auto-publish! current-auto-publish?)
        (set-ai-enhancement! current-ai-enhancement?)
        js/undefined)
      [current-default-currency current-default-note current-auto-publish? current-ai-enhancement?])

    ($ layout/admin-layout
      ($ :div {:class "p-6 min-h-screen space-y-6"}
        ($ page-header {:title "Expenses Settings"
                        :subtitle "Manage default currency behavior, enabled currencies, exchange rates, and fetch alerts for the expenses domain."
                        :icon "M12 8c-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4-1.79-4-4-4zm8.94 3a7.97 7.97 0 00-.69-1.68l2.03-1.58-2-3.46-2.39.96a8.23 8.23 0 00-2.9-1.68L14.5 1h-4.99l-.51 2.56a8.23 8.23 0 00-2.9 1.68l-2.39-.96-2 3.46 2.03 1.58A7.97 7.97 0 003.06 11l-2.56.5v5l2.56.5c.15.58.38 1.14.69 1.68l-2.03 1.58 2 3.46 2.39-.96c.87.72 1.84 1.29 2.9 1.68L9.5 27h5l.5-2.56a8.23 8.23 0 002.9-1.68l2.39.96 2-3.46-2.03-1.58c.31-.54.54-1.1.69-1.68l2.56-.5v-5l-2.56-.5z"})

        ($ section-card
          {:title "Global defaults"
           :subtitle "These values feed the effective settings users inherit unless they override their own defaults."
           :actions ($ button {:id "btn-admin-expenses-settings-save"
                               :btn-type :primary
                               :loading settings-saving?
                               :disabled (or settings-loading? (not dirty?))
                               :on-click #(rf/dispatch [:admin/expenses-settings-update-settings
                                                        {:default-currency default-currency
                                                         :default-note (when (seq (str/trim default-note)) (str/trim default-note))
                                                         :auto-publish-after-upload auto-publish?
                                                         :ai-receipt-enhancement ai-enhancement?}])}
                      "Save changes")}
          (when settings-error
            ($ :div {:id "admin-expenses-settings-error" :class "ds-alert ds-alert-error mb-4"}
              ($ :span settings-error)))
          (if settings-loading?
            ($ :div {:class "grid grid-cols-1 xl:grid-cols-2 gap-6"}
              (for [idx (range 4)]
                ($ :div {:key idx :class "h-20 bg-base-200 rounded-xl animate-pulse"})))
            ($ :div {:class "grid grid-cols-1 xl:grid-cols-2 gap-6"}
              ($ :div {:class "space-y-2"}
                ($ :label {:class "text-sm font-medium" :for "admin-expenses-default-currency"}
                  "Default currency")
                ($ :select {:id "admin-expenses-default-currency"
                            :class "ds-select ds-select-bordered w-full"
                            :value default-currency
                            :on-change #(set-default-currency! (.. % -target -value))}
                  (for [currency currencies]
                    ($ :option {:key (:code currency) :value (:code currency)}
                      (str (:code currency) " — " (:name currency)))))
                ($ :p {:class "text-xs text-base-content/60"}
                  "Only currencies from the enabled list can be selected as the workspace default."))

              ($ :div {:class "space-y-2 xl:row-span-2"}
                ($ :label {:class "text-sm font-medium" :for "admin-expenses-default-note"}
                  "Default note")
                ($ :textarea {:id "admin-expenses-default-note"
                              :class "ds-textarea ds-textarea-bordered w-full min-h-32"
                              :value default-note
                              :placeholder "Optional note applied to new manual expenses"
                              :on-change #(set-default-note! (.. % -target -value))})
                ($ :p {:class "text-xs text-base-content/60"}
                  "Leave this blank if new expenses should start without a default note."))

              ($ :div {:class "rounded-xl border border-base-300 p-4 flex items-center justify-between gap-4"}
                ($ :div
                  ($ :p {:class "font-medium text-sm"} "Auto-publish after upload")
                  ($ :p {:class "text-xs text-base-content/60"}
                    "Controls the global default for receipt uploads that have complete extraction data."))
                ($ :input {:id "admin-expenses-auto-publish-toggle"
                           :type "checkbox"
                           :class "ds-toggle ds-toggle-primary"
                           :checked auto-publish?
                           :on-change #(set-auto-publish! (.. % -target -checked))}))

              ($ :div {:class "rounded-xl border border-base-300 p-4 flex items-center justify-between gap-4"}
                ($ :div
                  ($ :p {:class "font-medium text-sm"} "AI receipt enhancement")
                  ($ :p {:class "text-xs text-base-content/60"}
                    "Controls the global default for receipt refinement before expenses are created."))
                ($ :input {:id "admin-expenses-ai-enhancement-toggle"
                           :type "checkbox"
                           :class "ds-toggle ds-toggle-primary"
                           :checked ai-enhancement?
                           :on-change #(set-ai-enhancement! (.. % -target -checked))})))))

        ($ section-card
          {:title "Enabled currencies"
           :subtitle "BAM remains locked as the base currency. Newly added currencies become available to users immediately."}
          (when currencies-error
            ($ :div {:id "admin-expenses-currencies-error" :class "ds-alert ds-alert-error mb-4"}
              ($ :span currencies-error)))
          ($ :div {:class "grid grid-cols-1 xl:grid-cols-[2fr,1fr] gap-6"}
            ($ :div {:class "overflow-auto rounded-xl border border-base-300"}
              ($ :table {:class "ds-table w-full"}
                ($ :thead
                  ($ :tr
                    ($ :th "Code")
                    ($ :th "Name")
                    ($ :th "Role")
                    ($ :th {:class "text-right"} "Action")))
                ($ :tbody
                  (if currencies-loading?
                    ($ :tr
                      ($ :td {:col-span 4 :class "text-center py-8 text-base-content/60"}
                        "Loading currencies..."))
                    (for [currency currencies]
                      ($ :tr {:key (:code currency)}
                        ($ :td {:class "font-mono font-semibold"} (:code currency))
                        ($ :td (:name currency))
                        ($ :td
                          ($ :span {:class (str "ds-badge ds-badge-sm "
                                             (if (:is-base currency) "ds-badge-primary" "ds-badge-ghost"))}
                            (if (:is-base currency) "Base" "Enabled")))
                        ($ :td {:class "text-right"}
                          ($ button {:id (str "btn-admin-remove-currency-" (str/lower-case (:code currency)))
                                     :btn-type :outline
                                     :class "ds-btn-sm"
                                     :disabled (or currencies-saving? (:is-base currency))
                                     :on-click #(rf/dispatch [:admin/expenses-settings-remove-currency (:code currency)])}
                            (if (:is-base currency) "Locked" "Remove")))))))))

            ($ :div {:class "rounded-xl border border-base-300 p-4 space-y-3 h-fit"}
              ($ :h3 {:class "font-semibold"} "Add currency")
              ($ :div {:class "space-y-2"}
                ($ :label {:class "text-sm font-medium" :for "admin-expenses-new-currency-code"}
                  "Currency code")
                ($ :input {:id "admin-expenses-new-currency-code"
                           :class "ds-input ds-input-bordered w-full font-mono uppercase"
                           :value new-currency-code
                           :max-length 3
                           :placeholder "EUR"
                           :on-change #(set-new-currency-code! (-> % .-target .-value str/upper-case))}))
              ($ :div {:class "space-y-2"}
                ($ :label {:class "text-sm font-medium" :for "admin-expenses-new-currency-name"}
                  "Currency name")
                ($ :input {:id "admin-expenses-new-currency-name"
                           :class "ds-input ds-input-bordered w-full"
                           :value new-currency-name
                           :placeholder "Euro"
                           :on-change #(set-new-currency-name! (.. % -target -value))}))
              ($ button {:id "btn-admin-add-currency"
                         :btn-type :primary
                         :loading currencies-saving?
                         :disabled (not can-add-currency?)
                         :on-click #(do
                                      (rf/dispatch [:admin/expenses-settings-add-currency
                                                    {:code (str/trim new-currency-code)
                                                     :name (str/trim new-currency-name)}])
                                      (set-new-currency-code! "")
                                      (set-new-currency-name! ""))}
                "Add currency"))))

        ($ section-card
          {:title "Today's exchange rates"
           :subtitle (str "Daily rates cached for " (or rates-date "today") ".")
           :actions ($ button {:id "btn-admin-refresh-exchange-rates"
                               :btn-type :secondary
                               :loading rates-fetching?
                               :disabled rates-loading?
                               :on-click #(rf/dispatch [:admin/expenses-settings-fetch-rates-now])}
                      "Fetch latest rates")}
          (when rates-error
            ($ :div {:id "admin-expenses-rates-error" :class "ds-alert ds-alert-error mb-4"}
              ($ :span rates-error)))
          (when-let [status (:status rates-last-result)]
            ($ :div {:id "admin-expenses-rates-last-status"
                     :class (str "ds-alert mb-4 "
                              (case status
                                :fallback "ds-alert-warning"
                                :error "ds-alert-error"
                                "ds-alert-success"))}
              ($ :span
                (case status
                  :fallback (str "Used fallback rates"
                              (when-let [fallback-date (:fallback-date rates-last-result)]
                                (str " from " fallback-date))
                              ".")
                  :error "No exchange rates were available."
                  "Exchange rates fetched successfully."))))
          ($ :div {:class "overflow-auto rounded-xl border border-base-300"}
            ($ :table {:class "ds-table w-full"}
              ($ :thead
                ($ :tr
                  ($ :th "Currency")
                  ($ :th {:class "text-right"} "Rate")
                  ($ :th "Fetched at")
                  ($ :th "Source")))
              ($ :tbody
                (cond
                  rates-loading?
                  ($ :tr
                    ($ :td {:col-span 4 :class "text-center py-8 text-base-content/60"}
                      "Loading exchange rates..."))

                  (empty? rates)
                  ($ :tr
                    ($ :td {:col-span 4 :class "text-center py-8 text-base-content/60"}
                      "No rates cached for today yet."))

                  :else
                  (for [rate rates]
                    ($ :tr {:key (str (:currency-code rate) "-" (:rate-date rate))}
                      ($ :td {:class "font-mono font-semibold"} (:currency-code rate))
                      ($ :td {:class "text-right tabular-nums"} (fmt-rate (:rate rate)))
                      ($ :td {:class "text-xs text-base-content/60"} (fmt-timestamp (:fetched-at rate)))
                      ($ :td
                        ($ :span {:class (str "ds-badge ds-badge-sm "
                                           (if (:is-fallback rate) "ds-badge-warning" "ds-badge-success"))}
                          (if (:is-fallback rate) "Fallback" "Live"))))))))))

        ($ section-card
          {:title "Fetch alerts"
           :subtitle "Warnings created when the exchange-rate fetcher had to fall back or could not fetch fresh data."}
          (when alerts-error
            ($ :div {:id "admin-expenses-alerts-error" :class "ds-alert ds-alert-error mb-4"}
              ($ :span alerts-error)))
          (cond
            alerts-loading?
            ($ :div {:class "text-base-content/60"} "Loading alerts...")

            (empty? alerts)
            ($ :div {:id "admin-expenses-alerts-empty" :class "ds-alert ds-alert-success"}
              ($ :span "No unacknowledged exchange rate alerts."))

            :else
            ($ :div {:class "space-y-3"}
              (for [alert alerts]
                ($ :div {:key (:id alert)
                         :id (str "admin-expenses-alert-" (:id alert))
                         :class "rounded-xl border border-warning/30 bg-warning/10 p-4 flex flex-col gap-3 lg:flex-row lg:items-start lg:justify-between"}
                  ($ :div {:class "space-y-1"}
                    ($ :p {:class "font-semibold text-sm"}
                      (str "Rate date: " (:rate-date alert)))
                    ($ :p {:class "text-sm text-base-content/80"} (:error-message alert))
                    (when-let [fallback-date (:fallback-source-date alert)]
                      ($ :p {:class "text-xs text-base-content/60"}
                        (str "Fallback source date: " fallback-date)))
                    ($ :p {:class "text-xs text-base-content/50"}
                      (str "Created at: " (fmt-timestamp (:created-at alert)))))
                  ($ button {:id (str "btn-admin-ack-alert-" (:id alert))
                             :btn-type :outline
                             :class "ds-btn-sm"
                             :loading alerts-saving?
                             :on-click #(rf/dispatch [:admin/expenses-settings-ack-alert (:id alert)])}
                    "Acknowledge"))))))))))
