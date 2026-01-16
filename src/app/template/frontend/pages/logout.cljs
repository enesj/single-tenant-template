(ns app.template.frontend.pages.logout
  (:require
    [app.template.frontend.events.auth :as auth-events]
    [app.template.frontend.components.button :refer [button]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))


(defui logout-page
  []
  (let [auth-status (use-subscribe [:auth-status])
        user (use-subscribe [:current-user])]

    ($ :div {:class "min-h-screen flex items-center justify-center bg-base-200"}
      ($ :div {:class "max-w-md w-full bg-base-100 shadow-xl rounded-lg p-8"}
        ($ :div {:class "text-center"}
          ;; Header with logout icon
          ($ :div {:class "mb-6"}
            ($ :div {:class "w-16 h-16 bg-warning/20 rounded-full flex items-center justify-center mx-auto mb-4"}
              ($ :svg {:class "w-8 h-8 text-warning" :fill "none" :stroke "currentColor" :view-box "0 0 24 24"}
                ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                          :d "M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"})))
            ($ :h2 {:class "text-2xl font-bold text-base-content mb-2"} "Sign Out")
            ($ :p {:class "text-base-content/70"} "Are you sure you want to sign out?"))

          ;; Current session info
          (when (:authenticated auth-status)
            ($ :div {:class "mb-6 bg-base-200 rounded-lg p-4"}
              ($ :p {:class "text-sm text-base-content/70 mb-2"} "You are currently signed in as:")
              (when user
                ($ :div
                  ($ :p {:class "font-medium"} (str (or (:full-name user) (:name user) (:email user))))
                  (when-let [email (:email user)]
                    ($ :p {:class "text-sm text-base-content/70"} (str email)))
                  (when-let [role (:role user)]
                    ($ :p {:class "text-xs text-base-content/50"}
                      (str "Role: " (if (keyword? role) (name role) (str role)))))))))


          ;; Action buttons
          ($ :div {:class "space-y-3"}
            ;; Confirm logout button
            ($ button {:btn-type :warning
                       :class "w-full"
                       :id "logout-confirm-btn"
                       :on-click #(rf/dispatch [::auth-events/logout])}
              "Yes, Sign Out")

            ;; Cancel button
            ($ button {:btn-type :outline
                       :class "w-full"
                       :id "logout-cancel-btn"
                       :on-click #(set! (.-href js/window.location) "/")}
              "Cancel")

            ;; Divider and additional info
            ($ :div {:class "ds-divider text-base-content/50"})

            ($ :div {:class "text-xs text-base-content/50 space-y-1"}
              ($ :p "After signing out:")
              ($ :ul {:class "list-disc list-inside text-left space-y-1"}
                ($ :li "Your session will be cleared")
                ($ :li "You'll be redirected to the about page")
                ($ :li "You can sign back in anytime")))))))))
