(ns app.admin.frontend.components.layout
  (:require
    [app.admin.frontend.subs.auth]
    [app.admin.frontend.subs.dashboard]
    [app.template.frontend.components.button :refer [button change-theme]]
    [app.template.frontend.components.sidebar :refer [sidebar]]
    [app.template.frontend.components.icons :refer [dashboard-icon users-icon admins-icon user-settings-icon audit-icon login-events-icon expenses-icon receipts-icon suppliers-icon payers-icon articles-icon article-aliases-icon price-observations-icon settings-icon]]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defui admin-sidebar []
  (let [current-route (use-subscribe [:current-route])
        route-name (when current-route (:name current-route))
        current-admin-role (use-subscribe [:admin/current-user-role])
        is-owner? (= current-admin-role :owner)

        system-admin-items (cond-> [{:label "Dashboard" :href "/admin/dashboard" :icon ($ dashboard-icon {:class "w-6 h-6"}) :active? (= route-name :admin-dashboard)}
                                    {:label "Users" :href "/admin/users" :icon ($ users-icon {:class "w-6 h-6"}) :active? (= route-name :admin-users)}]
                             is-owner? (conj {:label "Admins" :href "/admin/admins" :icon ($ admins-icon {:class "w-6 h-6"}) :active? (= route-name :admin-admins)})
                             true (into [{:label "Audit Logs" :href "/admin/audit" :icon ($ audit-icon {:class "w-6 h-6"}) :active? (= route-name :admin-audit)}
                                         {:label "Login Events" :href "/admin/login-events" :icon ($ login-events-icon {:class "w-6 h-6"}) :active? (= route-name :admin-login-events)}]))

        domain-items [{:label "Expenses" :href "/admin/expenses" :icon ($ expenses-icon {:class "w-6 h-6"}) :active? (= route-name :admin-expenses)}
                      {:label "Receipts" :href "/admin/receipts" :icon ($ receipts-icon {:class "w-6 h-6"}) :active? (= route-name :admin-receipts)}
                      {:label "Suppliers" :href "/admin/suppliers" :icon ($ suppliers-icon {:class "w-6 h-6"}) :active? (= route-name :admin-suppliers)}
                      {:label "Payers" :href "/admin/payers" :icon ($ payers-icon {:class "w-6 h-6"}) :active? (= route-name :admin-payers)}
                      {:label "Articles" :href "/admin/articles" :icon ($ articles-icon {:class "w-6 h-6"}) :active? (= route-name :admin-articles)}
                      {:label "Article Aliases" :href "/admin/article-aliases" :icon ($ article-aliases-icon {:class "w-6 h-6"}) :active? (= route-name :admin-article-aliases)}
                      {:label "Price Observations" :href "/admin/price-observations" :icon ($ price-observations-icon {:class "w-6 h-6"}) :active? (= route-name :admin-price-observations)}]

        sections [{:title "System Administration" :items system-admin-items}
                  {:title "Expenses Domain" :items domain-items}]]
    ($ sidebar {:title "Admin Panel"
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
  (let [[expanded? set-expanded!] (use-state false)]
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
            ($ change-theme)))))))

(defui admin-header []
  (let [current-user (use-subscribe [:admin/current-user])
        current-role (use-subscribe [:admin/current-user-role])
        admin-name (or (:full_name current-user) (:full-name current-user))
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
        ($ :div {:class "flex-1 flex"})
        ($ :div {:class "flex items-center space-x-2"}
          ;; Admin info (matches user auth-component exactly)
          (when current-user
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

          ;; Sign Out button
          ($ button {:btn-type :error
                     :class "ds-btn-sm"
                     :id "admin-sign-out-btn"
                     :on-click #(rf/dispatch [:admin/logout])}
            "Sign Out")

          ;; Settings gear (on the right, opens popover with theme)
          ($ admin-settings-panel))))))

(defui admin-layout [{:keys [children]}]
  (let [authenticated? (use-subscribe [:admin/authenticated?])
        loading? (use-subscribe [:admin/loading?])]
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
        ($ admin-sidebar)
        ($ :div {:class "flex flex-col w-0 flex-1 overflow-hidden"}
          ($ admin-header)
          ($ :main {:class "flex-1 relative overflow-y-auto focus:outline-none bg-base-100"}
            ;; In UIX, children are in the :children key of props
            children))))))
