(ns app.template.frontend.pages.expenses-list
  "User-facing expense list page with filtering and pagination."
  (:require
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.components.confirm-dialog :as confirm-dialog]
    [app.template.frontend.components.icons :refer [edit-icon delete-icon]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-state use-effect]]
    [uix.re-frame :refer [use-subscribe]]
    [app.template.frontend.subs.user-expenses]))

;; ========================================================================
;; Formatting helpers
;; ========================================================================

(defn format-money
  "Format a number with currency."
  [amount currency]
  (cond
    (nil? amount) "--"
    :else (try
            (.toLocaleString (js/Number amount) "en-US"
              #js {:style "currency"
                   :currency (or currency "USD")
                   :minimumFractionDigits 2
                   :maximumFractionDigits 2})
            (catch :default _
              (str (or currency "$") " " (.toFixed (js/Number amount) 2))))))

(defn format-date
  "Full date format for list view."
  [date-str]
  (when date-str
    (.toLocaleDateString (js/Date. date-str) "en-US"
      #js {:year "numeric" :month "short" :day "numeric"})))

(defn status-badge [expense]
  (let [posted? (:is_posted expense)]
    ($ :span {:class (str "ds-badge ds-badge-sm "
                       (if posted? "ds-badge-success" "ds-badge-warning"))}
      (if posted? "Posted" "Pending"))))

;; ========================================================================
;; Table Components
;; ========================================================================

(defui expense-table-row [{:keys [expense on-view on-edit]}]
  (let [{:keys [id supplier_display_name payer_label total_amount currency purchased_at]} expense]
    ($ :tr {:class "hover:bg-base-200 cursor-pointer"
            :on-click #(when on-view (on-view id))}
      ($ :td {:class "font-medium"} (or supplier_display_name "—"))
      ($ :td (or payer_label "—"))
      ($ :td {:class "text-right font-mono"} (format-money total_amount currency))
      ($ :td (format-date purchased_at))
      ($ :td (status-badge expense))
      ($ :td {:class "text-right"}
        ($ :div {:class "flex gap-1 justify-end"}
          ($ :button {:class "ds-btn ds-btn-ghost ds-btn-xs"
                      :on-click (fn [e]
                                  (.stopPropagation e)
                                  (when on-view (on-view id)))}
            "View")
          ($ :button {:class "ds-btn ds-btn-ghost ds-btn-xs"
                      :on-click (fn [e]
                                  (.stopPropagation e)
                                  (when on-edit (on-edit id)))}
            "Edit"))))))

(defui expense-table-skeleton []
  ($ :tr
    (for [i (range 6)]
      ($ :td {:key i}
        ($ :div {:class "h-4 bg-base-300 rounded animate-pulse"})))))

;; ========================================================================
;; Pagination Component
;; ========================================================================

(defui pagination [{:keys [total limit offset on-page-change]}]
  (let [current-page (inc (quot offset limit))
        total-pages (max 1 (js/Math.ceil (/ total limit)))
        can-prev? (> current-page 1)
        can-next? (< current-page total-pages)]
    ($ :div {:class "flex items-center justify-between mt-4"}
      ($ :span {:class "text-sm text-base-content/70"}
        (str "Showing " (inc offset) "-" (min (+ offset limit) total) " of " total))
      ($ :div {:class "ds-join"}
        ($ :button {:class (str "ds-join-item ds-btn ds-btn-sm" (when-not can-prev? " ds-btn-disabled"))
                    :disabled (not can-prev?)
                    :on-click #(on-page-change (- offset limit))}
          "«")
        ($ :span {:class "ds-join-item ds-btn ds-btn-sm ds-btn-active"}
          (str "Page " current-page " of " total-pages))
        ($ :button {:class (str "ds-join-item ds-btn ds-btn-sm" (when-not can-next? " ds-btn-disabled"))
                    :disabled (not can-next?)
                    :on-click #(on-page-change (+ offset limit))}
          "»")))))

;; ========================================================================
;; Filter Component
;; ========================================================================

(defui filters [{:keys [on-filter-change]}]
  (let [[show-filters? set-show-filters!] (use-state false)]
    ($ :div {:class "mb-4"}
      ($ :div {:class "flex items-center gap-2 mb-2"}
        ($ :button {:class "ds-btn ds-btn-ghost ds-btn-sm"
                    :on-click #(set-show-filters! (not show-filters?))}
          (if show-filters? "Hide Filters" "Show Filters")
          ($ :span {:class "ml-1"} (if show-filters? "▲" "▼"))))
      (when show-filters?
        ($ :div {:class "bg-base-200 p-4 rounded-lg grid grid-cols-1 md:grid-cols-3 gap-4"}
          ($ :div
            ($ :label {:class "ds-label"} ($ :span {:class "ds-label-text"} "Status"))
            ($ :select {:class "ds-select ds-select-sm ds-select-bordered w-full"
                        :on-change #(on-filter-change :is_posted (.. % -target -value))}
              ($ :option {:value ""} "All")
              ($ :option {:value "true"} "Posted")
              ($ :option {:value "false"} "Pending")))
          ($ :div
            ($ :label {:class "ds-label"} ($ :span {:class "ds-label-text"} "From Date"))
            ($ :input {:type "date"
                       :class "ds-input ds-input-sm ds-input-bordered w-full"
                       :on-change #(on-filter-change :from (.. % -target -value))}))
          ($ :div
            ($ :label {:class "ds-label"} ($ :span {:class "ds-label-text"} "To Date"))
            ($ :input {:type "date"
                       :class "ds-input ds-input-sm ds-input-bordered w-full"
                       :on-change #(on-filter-change :to (.. % -target -value))})))))))

;; ========================================================================
;; Main Page
;; ========================================================================

(defui my-expense-actions
  [{:keys [id] :as item}]
  (let [expense-id (or id (:id item))]
    (when expense-id
      ($ :div {:class "flex items-center justify-end gap-2"}
        ($ button
          {:btn-type :primary
           :shape "circle"
           :on-click (fn [e]
                       (.stopPropagation e)
                       (rf/dispatch [:navigate-to (str "/expenses/" expense-id)]))}
          ($ edit-icon))
        ($ button
          {:btn-type :danger
           :shape "circle"
           :on-click (fn [e]
                       (.stopPropagation e)
                       (confirm-dialog/show-confirm
                         {:title "Delete expense"
                          :message "Do you want to delete this expense?"
                          :on-confirm #(rf/dispatch [:user-expenses/delete-expense expense-id])
                          :on-cancel nil}))}
          ($ delete-icon))))))

(defui expenses-list-page []
  (let [entity-name :expenses
        ;; Use shared entity specs when available; fall back to nil which
        ;; list-view can still handle for basic rendering.
        entity-spec (use-subscribe [:entity-specs/by-name entity-name])
        error (use-subscribe [:user-expenses/recent-error])]

    ;; Ensure we kick off a user-scoped fetch so that the shared
    ;; template entity store for :expenses and its FK references is
    ;; populated via the user-expenses pipeline and the expenses
    ;; admin adapter sync events.
    (use-effect
      (fn []
        ;; Primary list data – user-scoped expenses
        (rf/dispatch [:user-expenses/fetch-recent {:limit 25 :offset 0}])
        ;; Reference data for FK columns in the list-view (supplier & payer)
        (rf/dispatch [:user-expenses/fetch-suppliers {:limit 100 :offset 0}])
        (rf/dispatch [:user-expenses/fetch-payers {:limit 100 :offset 0}])
        js/undefined)
      [])

    ($ :div {:class "min-h-screen bg-base-100"}
      ;; Header
      ($ :header {:class "bg-white border-b border-base-200"}
        ($ :div {:class "max-w-6xl mx-auto px-4 py-4 sm:py-6"}
          ($ :div {:class "flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4"}
            ($ :div
              ($ :h1 {:class "text-xl sm:text-2xl font-bold text-base-content"} "My Expenses")
              ($ :p {:class "text-sm text-base-content/70"}
                "View and manage your expense history"))
            ($ :div {:class "flex gap-2"}
              ($ button {:btn-type :primary
                         :on-click #(rf/dispatch [:navigate-to "/expenses/new"])}
                "+ New Expense")
              ($ button {:btn-type :ghost
                         :on-click #(rf/dispatch [:navigate-to "/expenses"])}
                "Dashboard")))))

      ;; Error banner (from user-expenses pipeline)
      (when error
        ($ :div {:class "max-w-6xl mx-auto px-4 mt-4"}
          ($ :div {:class "ds-alert ds-alert-error"}
            ($ :span error))))

      ;; Main content: generic list-view backed by shared entity store
      ($ :main {:class "max-w-6xl mx-auto px-4 py-6"}
        ($ list-view
          {:entity-name entity-name
           :entity-spec entity-spec
           :title "My Expenses"
           :render-actions my-expense-actions
           ;; User-facing defaults – no bulk selection/edit/delete, but keep
           ;; filtering + pagination.
           :display-settings {:show-select? false
                              :show-edit? false
                              :show-delete? false
                              :show-filtering? true
                              :show-pagination? true
                              :show-add-button? false
                              :per-page 25}})))))
