(ns app.admin.frontend.components.layout
  (:require
    [app.template.frontend.components.button :refer [button change-theme]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defui admin-sidebar []
  (let [current-route (use-subscribe [:current-route])
        route-name (when current-route (:name current-route))]
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

(defui admin-header []
  (let [current-user (use-subscribe [:admin/current-user])]
    ($ :div {:class "flex-shrink-0 flex h-16 bg-base-300 shadow"}
      ($ :div {:class "flex-1 px-4 flex justify-between"}
        ($ :div {:class "flex-1 flex"})
        ($ :div {:class "ml-4 flex items-center md:ml-6"}
          ;; Template theme selector
          ($ :div {:class "mr-4"}
            ($ change-theme))

          ;; User menu
          ($ :div {:class "ml-3 relative"}
            ($ :div {:class "flex items-center"}
              ($ :span {:class "text-sm text-base-content mr-4"}
                (or (:full-name current-user) (:email current-user)))
              ($ button {:btn-type :error
                         :class "ds-btn-sm"
                         :on-click #(rf/dispatch [:admin/logout])}
                "Logout"))))))))

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
