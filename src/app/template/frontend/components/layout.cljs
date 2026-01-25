(ns app.template.frontend.components.layout
  (:require
    [app.template.frontend.components.auth :refer [auth-component]]
    [app.template.frontend.components.icons :refer [arrow-up
                                                    article-aliases-icon
                                                    articles-icon
                                                    chart-bar
                                                    dashboard-icon
                                                    expense-items-icon
                                                    expenses-icon
                                                    price-observations-icon
                                                    payers-icon
                                                    receipts-icon
                                                    suppliers-icon
                                                    unmapped-items-icon]]
    [app.template.frontend.components.settings.global-settings :refer [settings-panel]]
    [app.template.frontend.components.sidebar :refer [sidebar]]
    [reitit.frontend.easy :as rtfe]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defn- stop-and-push! [e route href]
  (.preventDefault e)
  (try
    (rtfe/push-state route)
    (catch :default _e
      ;; If the route name isn't registered (or push-state fails), fall back to
      ;; a plain navigation so sidebar links still work.
      (when href
        (set! (.-href js/window.location) href)))))

(defn- nav-item
  [{:keys [id label href route icon active?]}]
  {:id id
   :label label
   :href href
   :icon icon
   :active? active?
   :on-click (fn [e] (stop-and-push! e route href))})

(defn- normalize-role
  [role]
  (cond
    (keyword? role) (name role)
    (string? role) role
    :else nil))

(defui user-sidebar []
  (let [current-route (use-subscribe [:current-route])
        role (normalize-role (use-subscribe [:user-role]))
        power-user? (contains? #{"admin" "owner"} role)
        can-upload? (contains? #{"member" "admin" "owner"} role)
        route-name (or (get-in current-route [:data :name]) (:name current-route))
        active? (fn [names] (contains? names route-name))

        expense-items (vec
                        (concat
                          [(nav-item {:id "user-sidebar-dashboard"
                                      :label "Dashboard"
                                      :href "/dashboard"
                                      :route :user-dashboard
                                      :icon ($ dashboard-icon {:class "w-6 h-6"})
                                      :active? (active? #{:user-dashboard
                                                          :expenses-dashboard
                                                          :expenses-dashboard-alias})})
                           (nav-item {:id "user-sidebar-receipts"
                                      :label "Receipts"
                                      :href "/receipts"
                                      :route :receipts
                                      :icon ($ receipts-icon {:class "w-6 h-6"})
                                      :active? (active? #{:receipts :receipt-detail})})
                           (nav-item {:id "user-sidebar-expenses-list"
                                      :label "Expenses"
                                      :href "/expenses/list"
                                      :route :expenses-list
                                      :icon ($ expenses-icon {:class "w-6 h-6"})
                                      :active? (active? #{:expenses-list
                                                          :expense-detail
                                                          :expense-new})})]
                          (when power-user?
                            [(nav-item {:id "user-sidebar-expense-items"
                                        :label "Expense Items"
                                        :href "/expense-items"
                                        :route :expense-items
                                        :icon ($ expense-items-icon {:class "w-6 h-6"})
                                        :active? (active? #{:expense-items})})])))

        operations-items (vec
                           (concat
                             (when can-upload?
                               [(nav-item {:id "user-sidebar-expenses-upload"
                                           :label "Upload"
                                           :href "/expenses/upload"
                                           :route :expense-upload
                                           :icon ($ arrow-up {:class "w-6 h-6"})
                                           :active? (active? #{:expense-upload})})])
                             [(nav-item {:id "user-sidebar-expenses-reports"
                                         :label "Reports"
                                         :href "/expenses/reports"
                                         :route :expense-reports
                                         :icon ($ chart-bar {:class "w-6 h-6"})
                                         :active? (active? #{:expense-reports})})]
                             (when power-user?
                               [(nav-item {:id "user-sidebar-unmapped-items"
                                           :label "Unmapped Aliases"
                                           :href "/unmapped-items"
                                           :route :unmapped-items
                                           :icon ($ unmapped-items-icon {:class "w-6 h-6"})
                                           :active? (active? #{:unmapped-items})})])))

        reference-items (vec
                          (concat
                            [(nav-item {:id "user-sidebar-suppliers"
                                        :label "Suppliers"
                                        :href "/suppliers"
                                        :route :expense-suppliers
                                        :icon ($ suppliers-icon {:class "w-6 h-6"})
                                        :active? (active? #{:expense-suppliers})})
                             (nav-item {:id "user-sidebar-payers"
                                        :label "Payers"
                                        :href "/payers"
                                        :route :expense-payers
                                        :icon ($ payers-icon {:class "w-6 h-6"})
                                        :active? (active? #{:expense-payers})})]
                            (when power-user?
                              [(nav-item {:id "user-sidebar-payer-types"
                                          :label "Payer Types"
                                          :href "/payer-types"
                                          :route :expense-payer-types
                                          :icon ($ payers-icon {:class "w-6 h-6"})
                                          :active? (active? #{:expense-payer-types})})
                               (nav-item {:id "user-sidebar-articles"
                                          :label "Articles"
                                          :href "/articles"
                                          :route :expense-articles
                                          :icon ($ articles-icon {:class "w-6 h-6"})
                                          :active? (active? #{:expense-articles})})
                               (nav-item {:id "user-sidebar-manufacturers"
                                          :label "Manufacturers"
                                          :href "/manufacturers"
                                          :route :expense-manufacturers
                                          :icon ($ suppliers-icon {:class "w-6 h-6"})
                                          :active? (active? #{:expense-manufacturers})})
                               (nav-item {:id "user-sidebar-article-aliases"
                                          :label "Aliases"
                                          :href "/article-aliases"
                                          :route :expense-article-aliases
                                          :icon ($ article-aliases-icon {:class "w-6 h-6"})
                                          :active? (active? #{:expense-article-aliases})})
                               (nav-item {:id "user-sidebar-manufacturer-aliases"
                                          :label "Manufacturer Aliases"
                                          :href "/manufacturer-aliases"
                                          :route :expense-manufacturer-aliases
                                          :icon ($ article-aliases-icon {:class "w-6 h-6"})
                                          :active? (active? #{:expense-manufacturer-aliases})})
                               (nav-item {:id "user-sidebar-supplier-aliases"
                                          :label "Supplier Aliases"
                                          :href "/supplier-aliases"
                                          :route :expense-supplier-aliases
                                          :icon ($ article-aliases-icon {:class "w-6 h-6"})
                                          :active? (active? #{:expense-supplier-aliases})})
                               (nav-item {:id "user-sidebar-price-observations"
                                          :label "Price Observations"
                                          :href "/price-observations"
                                          :route :expense-price-observations
                                          :icon ($ price-observations-icon {:class "w-6 h-6"})
                                          :active? (active? #{:expense-price-observations})})])))

        sections [{:title "Expenses" :items expense-items}
                  {:title "Operations" :items operations-items}
                  {:title "Reference" :items reference-items}]]
    ($ sidebar
      {:title "Expenses"
       :sections sections
       :footer ($ :ul {:class "ds-menu w-full p-0"}
                 ($ :li
                   ($ :a {:id "user-sidebar-logout"
                          :href "/logout"
                          :on-click (fn [e] (stop-and-push! e :logout "/logout"))
                          :class (if (= route-name :logout) "ds-active" "")}
                     "Log Out")))})))

(defui user-header []
  ($ :div {:class "flex-shrink-0 flex h-16 bg-base-300 shadow"}
    ($ :div {:class "flex-1 px-4 flex justify-between items-center"}
      ($ :div {:class "flex-1 flex"})
      ($ :div {:class "flex items-center space-x-2"}
        ($ auth-component)
        ($ settings-panel {:global-settings? true})))))

(defui user-layout
  [{:keys [children]}]
  ($ :div {:class "h-screen flex overflow-hidden bg-base-100"}
    ($ user-sidebar)
    ($ :div {:class "flex flex-col w-0 flex-1 overflow-hidden"}
      ($ user-header)
      ($ :main {:class "flex-1 relative overflow-y-auto focus:outline-none bg-base-100"}
        children))))
