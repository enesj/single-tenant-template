(ns app.admin.frontend.components.layout
  (:require
    [app.admin.frontend.subs.auth]
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
                                                    login-events-icon

                                                    settings-icon
                                                    suppliers-icon
                                                    unmapped-items-icon
                                                    user-settings-icon
                                                    users-icon]]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defui admin-sidebar [{:keys [open?]}]
  (let [current-route (use-subscribe [:current-route])
        route-name (or (get-in current-route [:data :name]) (:name current-route))
        current-admin-role (use-subscribe [:admin/current-user-role])
        is-owner? (= current-admin-role :owner)

        system-admin-items (cond-> [{:id "admin-sidebar-dashboard"
                                     :label "Dashboard"
                                     :href "/admin/dashboard"
                                     :icon ($ dashboard-icon {:class "w-6 h-6"})
                                     :active? (contains? #{:admin-dashboard :admin-dashboard-alt} route-name)}
                                    {:id "admin-sidebar-backlog"
                                     :label "Backlog"
                                     :href "/admin/backlog"
                                     :icon ($ suppliers-icon {:class "w-6 h-6"})
                                     :active? (= route-name :admin-backlog)}
                                    {:id "admin-sidebar-tenants"
                                     :label "Tenants"
                                     :href "/admin/tenants"
                                     :icon ($ admins-icon {:class "w-6 h-6"})
                                     :active? (= route-name :admin-tenants)}
                                    {:id "admin-sidebar-users"
                                     :label "Users"
                                     :href "/admin/users"
                                     :icon ($ users-icon {:class "w-6 h-6"})
                                     :active? (= route-name :admin-users)}]
                             is-owner?
                             (conj {:label "Admins"
                                    :href "/admin/admins"
                                    :icon ($ admins-icon {:class "w-6 h-6"})
                                    :active? (= route-name :admin-admins)})

                             true
                             (into [{:label "Audit Logs"
                                     :href "/admin/audit"
                                     :icon ($ audit-icon {:class "w-6 h-6"})
                                     :active? (= route-name :admin-audit)}
                                    {:label "Login Events"
                                     :href "/admin/login-events"
                                     :icon ($ login-events-icon {:class "w-6 h-6"})
                                     :active? (= route-name :admin-login-events)}]))

        expenses-items [{:id "admin-sidebar-expenses-reports"
                         :label "Reports"
                         :href "/admin/reports"
                         :icon ($ chart-bar {:class "w-6 h-6"})
                         :active? (= route-name :admin-reports)}
                        {:id "admin-sidebar-expenses-articles"
                         :label "Articles"
                         :href "/admin/articles"
                         :icon ($ articles-icon {:class "w-6 h-6"})
                         :active? (= route-name :admin-articles)}
                        {:id "admin-sidebar-expenses-manufacturers"
                         :label "Manufacturers"
                         :href "/admin/manufacturers"
                         :icon ($ suppliers-icon {:class "w-6 h-6"})
                         :active? (= route-name :admin-manufacturers)}
                        {:id "admin-sidebar-expenses-categories"
                         :label "Categories"
                         :href "/admin/categories"
                         :icon ($ suppliers-icon {:class "w-6 h-6"})
                         :active? (= route-name :admin-categories)}
                        {:id "admin-sidebar-expenses-subcategories"
                         :label "Subcategories"
                         :href "/admin/subcategories"
                         :icon ($ suppliers-icon {:class "w-6 h-6"})
                         :active? (= route-name :admin-subcategories)}
                        {:id "admin-sidebar-expenses-suppliers"
                         :label "Suppliers"
                         :href "/admin/suppliers"
                         :icon ($ suppliers-icon {:class "w-6 h-6"})
                         :active? (= route-name :admin-suppliers)}
                        {:id "admin-sidebar-expenses-stores"
                         :label "Stores"
                         :href "/admin/stores"
                         :icon ($ suppliers-icon {:class "w-6 h-6"})
                         :active? (= route-name :admin-stores)}
                        {:id "admin-sidebar-expenses-countries"
                         :label "Countries"
                         :href "/admin/countries"
                         :icon ($ suppliers-icon {:class "w-6 h-6"})
                         :active? (= route-name :admin-countries)}
                        {:id "admin-sidebar-expenses-cities"
                         :label "Cities"
                         :href "/admin/cities"
                         :icon ($ suppliers-icon {:class "w-6 h-6"})
                         :active? (= route-name :admin-cities)}

                        {:id "admin-sidebar-expenses-unmapped-aliases"
                         :label "Unmapped Aliases"
                         :href "/admin/unmapped-aliases"
                         :icon ($ unmapped-items-icon {:class "w-6 h-6"})
                         :active? (= route-name :admin-unmapped-aliases)}
                        {:id "admin-sidebar-expenses-article-aliases"
                         :label "Article Aliases"
                         :href "/admin/article-aliases"
                         :icon ($ article-aliases-icon {:class "w-6 h-6"})
                         :active? (= route-name :admin-article-aliases)}
                        {:id "admin-sidebar-expenses-supplier-aliases"
                         :label "Supplier Aliases"
                         :href "/admin/supplier-aliases"
                         :icon ($ article-aliases-icon {:class "w-6 h-6"})
                         :active? (= route-name :admin-supplier-aliases)}
                        {:id "admin-sidebar-expenses-store-aliases"
                         :label "Store Aliases"
                         :href "/admin/store-aliases"
                         :icon ($ article-aliases-icon {:class "w-6 h-6"})
                         :active? (= route-name :admin-store-aliases)}
                        {:id "admin-sidebar-expenses-duplicates"
                         :label "Dedup & Merge"
                         :href "/admin/duplicates"
                         :icon ($ articles-icon {:class "w-6 h-6"})
                         :active? (= route-name :admin-duplicates)}]

        sections [{:title "System Administration" :items system-admin-items}
                  {:title "Domain"
                   :subsections [{:id "admin-sidebar-domain-expenses"
                                  :title "Expenses"
                                  :items expenses-items}]}]]
    ($ sidebar {:title "Admin Panel"
                :open? open?
                :sections sections
                :footer ($ :ul {:class "ds-menu w-full p-0"}
                          ($ :li
                            ($ :a {:href "/admin/admin-settings"
                                   :class (if (= route-name :admin-admin-settings) "ds-active" "")}
                              ($ settings-icon {:class "w-5 h-5"})
                              "Admin Settings"))

                          ($ :li
                            ($ :a {:href "/admin/user-settings"
                                   :class (if (= route-name :admin-user-settings) "ds-active" "")}
                              ($ user-settings-icon {:class "w-5 h-5"})
                              "User Settings")))})))

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
                 :class "ds-btn-circle border border-base-200 bg-base-100/70 text-base-content/90 shadow-sm hover:bg-base-100"
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
        current-user (use-subscribe [:admin/current-user])
        current-role (use-subscribe [:admin/current-user-role])
        admin-name (:full-name current-user)
        admin-email (:email current-user)
        role-str (when current-role (name current-role))
        display-name (if admin-name
                       (let [parts (str/split admin-name #"\s+")
                             first-init (first (first parts))
                             last-init (when (> (count parts) 1) (first (last parts)))]
                         (str first-init (or last-init "")))
                       (when admin-email
                         (first (str/split (str admin-email) #"@"))))]
    ($ :div {:class "flex-shrink-0 flex h-16 bg-base-300 shadow"}
      ($ :div {:class "flex-1 px-4 flex justify-between items-center"}
        ($ :div {:class "flex items-center"}
          ($ :button {:class "p-2 rounded-lg hover:bg-base-200 transition-colors"
                      :id "admin-sidebar-toggle"
                      :on-click on-toggle-sidebar
                      :aria-label "Toggle sidebar"}
            ($ :svg {:class "w-6 h-6" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
              ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                        :d "M4 6h16M4 12h16M4 18h16"}))))
        ($ :div {:class "flex items-center space-x-2"}
          ;; Admin info - only show when actually authenticated (not just cached from localStorage)
          (when (and authenticated? current-user)
            ($ :div {:class "flex items-center space-x-2"}
              ;; Person icon
              ($ :svg {:class "w-5 h-5 text-base-content" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
                ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                          :d "M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"}))
              ;; Name/initials
              ($ :span {:class "font-medium text-sm"}
                (or display-name "Admin"))
              ;; Role badge
              (when role-str
                ($ :span {:class (str "ds-badge ds-badge-sm "
                                   (case role-str
                                     "owner" "ds-badge-primary"
                                     "super_admin" "ds-badge-primary"
                                     "admin" "ds-badge-secondary"
                                     "ds-badge-secondary"))}
                  role-str))))

          ;; Sign Out button - only show when authenticated
          (when authenticated?
            ($ button {:btn-type :error
                       :class "ds-btn-sm"
                       :id "admin-sign-out-btn"
                       :on-click #(rf/dispatch [:admin/logout])}
              "Sign Out"))

          ;; Settings gear (on the right, opens popover with theme)
          ($ admin-settings-panel))))))

(defui admin-layout [{:keys [children]}]
  (let [authenticated? (use-subscribe [:admin/authenticated?])
        loading? (use-subscribe [:admin/loading?])
        [sidebar-open? set-sidebar-open!] (use-state true)]
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
