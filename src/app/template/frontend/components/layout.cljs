(ns app.template.frontend.components.layout
  (:require
    [app.template.frontend.components.auth :refer [auth-component]]
    [app.template.frontend.components.icons :refer [arrow-up
                                                    chart-bar
                                                    dashboard-icon
                                                    expenses-icon
                                                    payers-icon
                                                    receipts-icon
                                                    settings-icon
                                                    suppliers-icon]]
    [app.template.frontend.components.settings.global-settings :refer [settings-panel]]
    [app.template.frontend.components.sidebar :refer [sidebar]]
    [reitit.frontend.easy :as rtfe]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defn- stop-and-push! [e route]
  (.preventDefault e)
  (rtfe/push-state route))

(defn- nav-item
  [{:keys [id label href route icon active?]}]
  {:id id
   :label label
   :href href
   :icon icon
   :active? active?
   :on-click (fn [e] (stop-and-push! e route))})

(defui user-sidebar []
  (let [current-route (use-subscribe [:current-route])
        route-name (or (get-in current-route [:data :name]) (:name current-route))
        active? (fn [names] (contains? names route-name))

        expense-items [(nav-item {:id "user-sidebar-dashboard"
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
                                                      :expense-new})})
                       (nav-item {:id "user-sidebar-expenses-upload"
                                  :label "Upload"
                                  :href "/expenses/upload"
                                  :route :expense-upload
                                  :icon ($ arrow-up {:class "w-6 h-6"})
                                  :active? (active? #{:expense-upload})})
                       (nav-item {:id "user-sidebar-expenses-reports"
                                  :label "Reports"
                                  :href "/expenses/reports"
                                  :route :expense-reports
                                  :icon ($ chart-bar {:class "w-6 h-6"})
                                  :active? (active? #{:expense-reports})})
                       (nav-item {:id "user-sidebar-expenses-settings"
                                  :label "Settings"
                                  :href "/expenses/settings"
                                  :route :expense-settings
                                  :icon ($ settings-icon {:class "w-6 h-6"})
                                  :active? (active? #{:expense-settings})})]

        reference-items [(nav-item {:id "user-sidebar-suppliers"
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

        app-items [(nav-item {:id "user-sidebar-home"
                              :label "Home"
                              :href "/"
                              :route :home
                              :active? (active? #{:home :home-explicit})})
                   (nav-item {:id "user-sidebar-entities"
                              :label "Entities"
                              :href "/entities"
                              :route :entities
                              :active? (active? #{:entities :entities-slash :entity-add :entity-detail :entity-update})})

                   (nav-item {:id "user-sidebar-about"
                              :label "About"
                              :href "/about"
                              :route :about
                              :active? (active? #{:about :about-slash})})]

        sections [{:title "Expenses" :items expense-items}
                  {:title "Reference" :items reference-items}
                  {:title "App" :items app-items}]]
    ($ sidebar
      {:title "App"
       :sections sections
       :footer ($ :ul {:class "ds-menu w-full p-0"}
                 ($ :li
                   ($ :a {:id "user-sidebar-change-password"
                          :href "/change-password"
                          :on-click (fn [e] (stop-and-push! e :change-password))
                          :class (if (= route-name :change-password) "ds-active" "")}
                     "Change Password"))

                 ($ :li
                   ($ :a {:id "user-sidebar-logout"
                          :href "/logout"
                          :on-click (fn [e] (stop-and-push! e :logout))
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
