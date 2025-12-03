(ns app.admin.frontend.components.layout
  (:require
    [app.admin.frontend.subs.auth]
    [app.admin.frontend.subs.dashboard]
    [app.template.frontend.components.button :refer [button change-theme]]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defui admin-sidebar []
  (let [current-route (use-subscribe [:current-route])
        route-name (when current-route (:name current-route))
        current-admin-role (use-subscribe [:admin/current-user-role])
        is-owner? (= current-admin-role :owner)]
    ($ :div {:class "hidden md:flex md:flex-shrink-0"}
      ($ :div {:class "flex flex-col w-64"}
        ($ :div {:class "flex flex-col h-0 flex-1 bg-base-200"}
          ($ :div {:class "flex-1 flex flex-col pt-5 pb-4 overflow-y-auto"}
            ($ :div {:class "flex items-center flex-shrink-0 px-4"}
              ($ :h1 {:class "text-xl font-bold text-base-content"} "Admin Panel"))
            ($ :nav {:class "mt-5 flex-1 px-2 space-y-1"}
              ;; Dashboard
              ($ :a {:href "/admin/dashboard"
                     :class (str "group flex items-center px-2 py-2 text-sm font-medium rounded-md "
                              (if (= route-name :admin-dashboard)
                                "bg-primary text-primary-content"
                                "text-base-content hover:bg-base-300"))}
                ($ :svg {:class "mr-3 h-6 w-6" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
                  ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                            :d "M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"}))
                "Dashboard")

              ;; Users
              ($ :a {:href "/admin/users"
                     :class (str "group flex items-center px-2 py-2 text-sm font-medium rounded-md "
                              (if (= route-name :admin-users)
                                "bg-primary text-primary-content"
                                "text-base-content hover:bg-base-300"))}
                ($ :svg {:class "mr-3 h-6 w-6" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
                  ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                            :d "M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z"}))
                "Users")

              ;; Admin Management (owner only)
              (when is-owner?
                ($ :a {:href "/admin/admins"
                       :class (str "group flex items-center px-2 py-2 text-sm font-medium rounded-md "
                                (if (= route-name :admin-admins)
                                  "bg-primary text-primary-content"
                                  "text-base-content hover:bg-base-300"))}
                  ($ :svg {:class "mr-3 h-6 w-6" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
                    ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                              :d "M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"}))
                  "Admins"))

              ;; Audit Logs
              ($ :a {:href "/admin/audit"
                     :class (str "group flex items-center px-2 py-2 text-sm font-medium rounded-md "
                              (if (= route-name :admin-audit)
                                "bg-primary text-primary-content"
                                "text-base-content hover:bg-base-300"))}
                ($ :svg {:class "mr-3 h-6 w-6" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
                  ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                            :d "M9 17v-6a2 2 0 012-2h8M9 17h10M9 17H5a2 2 0 01-2-2V7a2 2 0 012-2h8m6 0v10a2 2 0 01-2 2h-2m-4-12h6"}))
                "Audit Logs")

              ;; Login Events
              ($ :a {:href "/admin/login-events"
                     :class (str "group flex items-center px-2 py-2 text-sm font-medium rounded-md "
                              (if (= route-name :admin-login-events)
                                "bg-primary text-primary-content"
                                "text-base-content hover:bg-base-300"))}
                ($ :svg {:class "mr-3 h-6 w-6" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
                  ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                            :d "M3 17a4 4 0 014-4h10a4 4 0 010 8H7a4 4 0 01-4-4zm7-9a3 3 0 116 0 3 3 0 01-6 0z"}))
                "Login Events")

              ;; Divider
              ($ :div {:class "border-t border-base-300 my-2"})

              ;; Settings Overview
              ($ :a {:href "/admin/settings"
                     :class (str "group flex items-center px-2 py-2 text-sm font-medium rounded-md "
                              (if (= route-name :admin-settings)
                                "bg-primary text-primary-content"
                                "text-base-content hover:bg-base-300"))}
                ($ :svg {:class "mr-3 h-6 w-6" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
                  ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                            :d "M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"})
                  ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                            :d "M15 12a3 3 0 11-6 0 3 3 0 016 0z"}))
                "Settings Overview"))))))))

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
        ($ :svg {:class "w-6 h-6" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
          ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                    :d "M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"})
          ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                    :d "M15 12a3 3 0 11-6 0 3 3 0 016 0z"})))

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
