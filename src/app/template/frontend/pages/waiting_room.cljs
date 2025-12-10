(ns app.template.frontend.pages.waiting-room
  "Waiting room page for users with 'unassigned' role.
   Shows a friendly message explaining their account is pending approval."
  (:require
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]
    [app.template.frontend.components.button :refer [button]]))

(defui waiting-room-page []
  (let [user (use-subscribe [:current-user])
        user-name (or (:full_name user) (:full-name user) (:email user) "User")]
    ($ :div {:class "min-h-screen bg-gradient-to-b from-slate-50 to-white flex items-center justify-center px-4"}
      ($ :div {:class "max-w-md w-full"}
        ;; Card container
        ($ :div {:class "bg-white rounded-2xl shadow-xl border border-slate-100 p-8 text-center"}
          ;; Icon
          ($ :div {:class "mx-auto w-20 h-20 bg-amber-100 rounded-full flex items-center justify-center mb-6"}
            ($ :svg {:class "w-10 h-10 text-amber-600"
                     :fill "none"
                     :stroke "currentColor"
                     :viewBox "0 0 24 24"}
              ($ :path {:stroke-linecap "round"
                        :stroke-linejoin "round"
                        :stroke-width "2"
                        :d "M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"})))

          ;; Welcome text
          ($ :h1 {:class "text-2xl font-bold text-slate-800 mb-2"}
            (str "Welcome, " user-name "!"))

          ($ :h2 {:class "text-lg font-medium text-slate-600 mb-4"}
            "Your account is pending approval")

          ;; Description
          ($ :p {:class "text-slate-500 mb-6"}
            "An administrator will review your account shortly. Once approved, you'll be able to access the expense tracking features.")

          ;; Status indicator
          ($ :div {:class "flex items-center justify-center space-x-2 text-amber-600 mb-6"}
            ($ :div {:class "w-2 h-2 bg-amber-500 rounded-full animate-pulse"})
            ($ :span {:class "text-sm font-medium"} "Waiting for approval"))

          ;; Actions
          ($ :div {:class "space-y-3"}
            ($ button
              {:on-click #(rf/dispatch [:app.template.frontend.events.auth/fetch-auth-status])
               :variant :outline
               :class "w-full"}
              "Check Status")

            ($ button
              {:on-click #(rf/dispatch [:app.template.frontend.events.auth/logout])
               :variant :ghost
               :class "w-full text-slate-500"}
              "Sign Out")))

        ;; Footer note
        ($ :p {:class "mt-6 text-center text-sm text-slate-400"}
          "Need help? Contact your administrator.")))))