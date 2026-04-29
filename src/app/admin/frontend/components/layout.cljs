(ns app.admin.frontend.components.layout
  (:require
    [app.admin.frontend.subs.auth]
    [app.admin.frontend.subs.audit]
    [app.admin.frontend.subs.dashboard]
    [app.template.frontend.components.button :refer [button change-theme]]
    [app.template.frontend.components.sidebar :refer [sidebar]]
    [app.template.frontend.components.icons :refer [admins-icon
                                                    article-aliases-icon
                                                    articles-icon
                                                    arrow-path
                                                    audit-icon
                                                    chart-bar
                                                    dashboard-icon
                                                    expenses-icon
                                                    login-events-icon
                                                    logout-icon
                                                    receipts-icon
                                                    search-icon
                                                    settings-icon
                                                    suppliers-icon
                                                    unmapped-items-icon
                                                    user-settings-icon
                                                    users-icon]]
                            [app.template.frontend.utils.navigation-config :as nav-config]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defui admin-sidebar [{:keys [open?]}]
  (let [current-route (use-subscribe [:current-route])
        route-name (or (get-in current-route [:data :name]) (:name current-route))
        current-admin-role (use-subscribe [:admin/current-user-role])
        is-owner? (= current-admin-role :owner)
        unread-api-failures (use-subscribe [:admin/unread-api-failure-count])
        navigation (use-subscribe [:admin/navigation])

        system-admin-items (cond-> [{:id "admin-sidebar-dashboard"
                   :nav-id :dashboard
                                     :label "Dashboard"
                                     :href "/admin/dashboard"
                                     :icon ($ dashboard-icon {:class "w-6 h-6"})
                                     :active? (contains? #{:admin-dashboard :admin-dashboard-alt} route-name)}
                                    {:id "admin-sidebar-backlog"
                   :nav-id :backlog
                                     :label "Backlog"
                                     :href "/admin/backlog"
                                     :icon ($ suppliers-icon {:class "w-6 h-6"})
                                     :active? (= route-name :admin-backlog)}
                                    {:id "admin-sidebar-tenants"
                   :nav-id :tenants
                                     :label "Tenants & Memberships"
                                     :href "/admin/tenants"
                                     :icon ($ admins-icon {:class "w-6 h-6"})
                                     :active? (= route-name :admin-tenants)}
                                    {:id "admin-sidebar-users"
                   :nav-id :users
                                     :label "User Accounts"
                                     :href "/admin/users"
                                     :icon ($ users-icon {:class "w-6 h-6"})
                                     :active? (= route-name :admin-users)}]
                             is-owner?
                             (conj {:id "admin-sidebar-admins"
                               :nav-id :admins
                               :label "Admins"
                                    :href "/admin/admins"
                                    :icon ($ admins-icon {:class "w-6 h-6"})
                                    :active? (= route-name :admin-admins)})

                             true
                             (into [{:id "admin-sidebar-audit-logs"
                                :nav-id :audit-logs
                                :label "Audit Logs"
                                     :href "/admin/audit"
                                     :icon ($ audit-icon {:class "w-6 h-6"})
                                     :active? (= route-name :admin-audit)
                                     :badge unread-api-failures}
                               {:id "admin-sidebar-login-events"
                                :nav-id :login-events
                                :label "Login Events"
                                     :href "/admin/login-events"
                                     :icon ($ login-events-icon {:class "w-6 h-6"})
                                     :active? (= route-name :admin-login-events)}]))

        expenses-items [{:id "admin-sidebar-expenses-reports"
                              :nav-id :reports
                         :label "Reports"
                         :href "/admin/reports"
                         :icon ($ chart-bar {:class "w-6 h-6"})
                         :active? (= route-name :admin-reports)}
                        {:id "admin-sidebar-expenses-settings"
                              :nav-id :expenses-settings
                         :label "Settings"
                         :href "/admin/expenses-settings"
                         :icon ($ settings-icon {:class "w-6 h-6"})
                         :active? (= route-name :admin-expenses-settings)}
                        {:id "admin-sidebar-expenses-search"
                              :nav-id :search
                         :label "Search"
                         :href "/admin/search"
                         :icon ($ search-icon {:class "w-6 h-6"})
                         :active? (= route-name :admin-search)}
                        {:id "admin-sidebar-expenses-expenses"
                              :nav-id :expenses
                         :label "Expenses"
                         :href "/admin/expenses"
                         :icon ($ expenses-icon {:class "w-6 h-6"})
                         :active? (= route-name :admin-expenses)}
                        {:id "admin-sidebar-expenses-receipts"
                              :nav-id :receipts
                         :label "Receipts"
                         :href "/admin/receipts"
                         :icon ($ receipts-icon {:class "w-6 h-6"})
                         :active? (= route-name :admin-receipts)}
                        {:id "admin-sidebar-expenses-articles"
                              :nav-id :articles
                         :label "Articles"
                         :href "/admin/articles"
                         :icon ($ articles-icon {:class "w-6 h-6"})
                         :active? (= route-name :admin-articles)}
                        {:id "admin-sidebar-expenses-manufacturers"
                              :nav-id :manufacturers
                         :label "Manufacturers"
                         :href "/admin/manufacturers"
                         :icon ($ suppliers-icon {:class "w-6 h-6"})
                         :active? (= route-name :admin-manufacturers)}
                        {:id "admin-sidebar-expenses-categories"
                              :nav-id :categories
                         :label "Categories"
                         :href "/admin/categories"
                         :icon ($ suppliers-icon {:class "w-6 h-6"})
                         :active? (= route-name :admin-categories)}
                        {:id "admin-sidebar-expenses-subcategories"
                              :nav-id :subcategories
                         :label "Subcategories"
                         :href "/admin/subcategories"
                         :icon ($ suppliers-icon {:class "w-6 h-6"})
                         :active? (= route-name :admin-subcategories)}
                        {:id "admin-sidebar-expenses-suppliers"
                              :nav-id :suppliers
                         :label "Suppliers"
                         :href "/admin/suppliers"
                         :icon ($ suppliers-icon {:class "w-6 h-6"})
                         :active? (= route-name :admin-suppliers)}
                        {:id "admin-sidebar-expenses-stores"
                              :nav-id :stores
                         :label "Stores"
                         :href "/admin/stores"
                         :icon ($ suppliers-icon {:class "w-6 h-6"})
                         :active? (= route-name :admin-stores)}
                        {:id "admin-sidebar-expenses-countries"
                              :nav-id :countries
                         :label "Countries"
                         :href "/admin/countries"
                         :icon ($ suppliers-icon {:class "w-6 h-6"})
                         :active? (= route-name :admin-countries)}
                        {:id "admin-sidebar-expenses-cities"
                              :nav-id :cities
                         :label "Cities"
                         :href "/admin/cities"
                         :icon ($ suppliers-icon {:class "w-6 h-6"})
                         :active? (= route-name :admin-cities)}
                        {:id "admin-sidebar-expenses-unmapped-aliases"
                              :nav-id :unmapped-aliases
                         :label "Unmapped Aliases"
                         :href "/admin/unmapped-aliases"
                         :icon ($ unmapped-items-icon {:class "w-6 h-6"})
                         :active? (= route-name :admin-unmapped-aliases)}
                        {:id "admin-sidebar-expenses-article-aliases"
                              :nav-id :article-aliases
                         :label "Article Aliases"
                         :href "/admin/article-aliases"
                         :icon ($ article-aliases-icon {:class "w-6 h-6"})
                         :active? (= route-name :admin-article-aliases)}
                        {:id "admin-sidebar-expenses-supplier-aliases"
                              :nav-id :supplier-aliases
                         :label "Supplier Aliases"
                         :href "/admin/supplier-aliases"
                         :icon ($ article-aliases-icon {:class "w-6 h-6"})
                         :active? (= route-name :admin-supplier-aliases)}
                        {:id "admin-sidebar-expenses-store-aliases"
                              :nav-id :store-aliases
                         :label "Store Aliases"
                         :href "/admin/store-aliases"
                         :icon ($ article-aliases-icon {:class "w-6 h-6"})
                         :active? (= route-name :admin-store-aliases)}
                        {:id "admin-sidebar-expenses-duplicates"
                              :nav-id :duplicates
                         :label "Dedup & Merge"
                         :href "/admin/duplicates"
                         :icon ($ articles-icon {:class "w-6 h-6"})
                         :active? (= route-name :admin-duplicates)}]

                       fallback-sections [{:nav-id :system-administration
                            :title "System Administration"
                            :items system-admin-items}
                                {:nav-id :expenses
                            :title "Expenses"
                            :items expenses-items}]
                       sections (nav-config/apply-navigation navigation fallback-sections)
                       sidebar-title (or (:title navigation) "Admin Panel")]
                        ($ sidebar {:title sidebar-title
                :open? open?
                :sections sections
                :footer ($ :div {:class "p-3 border-t border-base-300"}
                          ($ :ul {:class "ds-menu w-full p-0"}
                            ($ :li
                              ($ :a {:href "/admin/admin-settings"
                                     :class (if (= route-name :admin-admin-settings) "ds-active" "")}
                                ($ settings-icon {:class "w-5 h-5"})
                                "Admin Settings"))
                            ($ :li
                              ($ :a {:href "/admin/user-settings"
                                     :class (if (= route-name :admin-user-settings) "ds-active" "")}
                                ($ user-settings-icon {:class "w-5 h-5"})
                                "User Settings")))
                          ($ :button {:id "admin-sidebar-logout"
                                      :class "flex items-center gap-2 text-sm font-medium py-2 px-2 rounded-lg w-full text-error hover:bg-error/10 transition-colors"
                                      :on-click #(rf/dispatch [:admin/logout])}
                            ($ logout-icon {:class "w-4 h-4"})
                            "Sign Out"))})))

(defui admin-settings-panel
  "Simple settings dropdown with theme selector"
  []
  (let [[expanded? set-expanded!] (use-state false)
        reload-everything! (fn []
                             (rf/dispatch [:app.template.frontend.events.config/fetch-config {:force? true}])
                             (rf/dispatch [:admin/load-ui-configs {:force? true}])
                             (set-expanded! false))]
    ($ :div {:class "relative"}
      ;; Gear icon button
      ($ button {:btn-type :ghost
                 :class "ds-btn-circle"
                 :id "admin-settings-gear"
                 :title "Settings"
                 :on-click #(set-expanded! (not expanded?))}
        ($ settings-icon {:class "w-6 h-6"}))

      ;; Simple dropdown - just theme selector
      (when expanded?
        ($ :div {:class "absolute right-0 mt-2 w-48 z-50 bg-base-100 border border-base-300 rounded-lg shadow-lg p-3"}
          ($ :div {:class "flex items-center justify-between gap-3"}
            ($ :span {:class "text-sm font-medium text-base-content"} "Theme")
            ($ change-theme))

          ($ :div {:class "flex items-center justify-between gap-3 mt-3 pt-3 border-t border-base-200"}
            ($ :span {:class "text-sm font-medium text-base-content"} "Reload")
            ($ button {:btn-type :ghost
                       :class "ds-btn-circle ds-btn-xs"
                       :id "btn-admin-reload-everything"
                       :title "Reload everything"
                       :on-click reload-everything!}
              ($ arrow-path {:class "w-4 h-4"}))))))))

(defui admin-header [{:keys [on-toggle-sidebar]}]
  (let [authenticated? (use-subscribe [:admin/authenticated?])
        current-user   (use-subscribe [:admin/current-user])
        current-role   (use-subscribe [:admin/current-user-role])
        admin-name     (:full-name current-user)
        admin-email    (:email current-user)
        role-str       (when current-role (name current-role))]
    ($ :div {:class "flex-shrink-0 flex h-16 bg-base-300 shadow"}
      ($ :div {:class "flex-1 px-4 flex items-center"}
        ;; Left: hamburger
        ($ :div {:class "flex-none flex items-center"}
          ($ :button {:class     "p-2 rounded-lg hover:bg-base-200 transition-colors"
                      :id        "admin-sidebar-toggle"
                      :on-click  on-toggle-sidebar
                      :aria-label "Toggle sidebar"}
            ($ :svg {:class "w-6 h-6" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
              ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                        :d "M4 6h16M4 12h16M4 18h16"}))))

        ;; Center: admin user info
        ($ :div {:class "flex-1 flex justify-center items-center"}
          (when (and authenticated? current-user)
            ($ :div {:class "flex items-end space-x-2"}
              ;; Person icon (aligned to bottom like provider icon in user header)
              ($ :svg {:class "w-6 h-6 text-base-content/60 mb-0.5" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
                ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                          :d "M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"}))
              ;; Name + email stacked
              ($ :div {:class "flex flex-col items-center"}
                ($ :span {:class "font-bold text-lg text-base-content leading-tight"}
                  (or admin-name admin-email "Admin"))
                (when (and admin-name admin-email)
                  ($ :span {:class "text-sm text-base-content/60 leading-tight"}
                    admin-email)))
              ;; Role badge
              (when role-str
                ($ :span {:class (str "ds-badge ds-badge-md ml-2 "
                                   (case role-str
                                     "owner"       "ds-badge-primary"
                                     "super_admin" "ds-badge-primary"
                                     "ds-badge-secondary"))}
                  role-str)))))

        ;; Right: settings gear
        ($ :div {:class "flex-none flex items-center"}
          ($ admin-settings-panel))))))

(defui admin-layout [{:keys [children]}]
  (let [authenticated? (use-subscribe [:admin/authenticated?])
        loading? (use-subscribe [:admin/loading?])
        [sidebar-open? set-sidebar-open!] (use-state true)]
    ;; Fetch unread API failure count on mount and when auth state changes
    (use-effect
      (fn []
        (when authenticated?
          (rf/dispatch [:admin/fetch-unread-api-failures]))
        js/undefined)
      [authenticated?])
    (when ^boolean js/goog.DEBUG
      (js/console.log "admin-layout state"
        (clj->js {:authenticated? authenticated?
                  :loading? loading?})))
    (if loading?
      ;; While we're actively checking auth, show a global spinner.
      ($ :div {:class "h-screen flex items-center justify-center bg-base-100"}
        ($ :div {:class "ds-loading ds-loading-spinner ds-loading-lg text-primary"}))
      ;; Once not loading, always render the admin shell; the inner auth-guard
      ;; component handles whether to show protected content or a login prompt.
      ($ :div {:class "h-screen flex overflow-hidden bg-base-100"}
        ($ admin-sidebar {:open? sidebar-open?})
        ($ :div {:class "flex flex-col w-0 flex-1 overflow-hidden"}
          ($ admin-header {:on-toggle-sidebar #(set-sidebar-open! (not sidebar-open?))})
          ($ :main {:class "flex-1 relative overflow-y-auto focus:outline-none bg-base-100"}
            ;; In UIX, children are in the :children key of props
            children))))))
