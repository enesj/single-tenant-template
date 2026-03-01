(ns app.template.frontend.pages.invitation-accept
  "Invitation acceptance page. Users land here from invitation email links.
   Extracts token from URL query params and provides an accept button."
  (:require
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.events.tenant :as tenant]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect]]
    [uix.re-frame :refer [use-subscribe]]))

(defui invitation-accept-page []
  (let [authenticated? (:authenticated (use-subscribe [:auth-status]))
        accept-loading? (get (use-subscribe [:auth-status]) :loading?)
        error (use-subscribe [:tenant/error])
        token (use-subscribe [:tenant/accept-token])
        tenant-loading? (use-subscribe [:tenant/loading?])
        ;; Extract token from URL if not yet stored
        url-token (some-> js/window.location.search
                    (js/URLSearchParams.)
                    (.get "token"))]

    ;; Store token from URL on mount
    (use-effect
      (fn []
        (when (and url-token (not token))
          (rf/dispatch [::tenant/accept-invitation-init url-token]))
        js/undefined)
      [url-token token])

    ($ :div {:class "min-h-screen bg-gradient-to-b from-slate-50 to-white flex items-center justify-center px-4"}
      ($ :div {:class "max-w-md w-full"}
        ($ :div {:class "bg-white rounded-2xl shadow-xl border border-slate-100 p-8 text-center"}
          ;; Icon
          ($ :div {:class "mx-auto w-20 h-20 bg-green-100 rounded-full flex items-center justify-center mb-6"}
            ($ :svg {:class "w-10 h-10 text-green-600"
                     :fill "none"
                     :stroke "currentColor"
                     :viewBox "0 0 24 24"}
              ($ :path {:stroke-linecap "round"
                        :stroke-linejoin "round"
                        :stroke-width "2"
                        :d "M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"})))

          ($ :h1 {:class "text-2xl font-bold text-slate-800 mb-2"}
            "Workspace Invitation")

          (cond
            ;; No token in URL
            (and (not token) (not url-token))
            ($ :div
              ($ :p {:class "text-slate-500 mb-4"}
                "No invitation token found. Please check your invitation link.")
              ($ button
                {:on-click #(rf/dispatch [:navigate-to "/dashboard"])
                 :variant :outline
                 :class "w-full"}
                "Go to Dashboard"))

            ;; Not authenticated — redirect to login
            (not authenticated?)
            ($ :div
              ($ :p {:class "text-slate-500 mb-6"}
                "Please sign in to accept this invitation.")
              ($ button
                {:on-click #(set! (.-href js/window.location)
                              (str "/login?return=" (js/encodeURIComponent (.-href js/window.location))))
                 :btn-type :primary
                 :class "w-full"}
                "Sign In"))

            ;; Error state
            error
            ($ :div
              ($ :div {:class "ds-alert ds-alert-error mb-4"}
                ($ :span error))
              ($ :p {:class "text-slate-500 mb-4"}
                "The invitation may have expired or already been used.")
              ($ button
                {:on-click #(rf/dispatch [:navigate-to "/dashboard"])
                 :variant :outline
                 :class "w-full"}
                "Go to Dashboard"))

            ;; Ready to accept
            :else
            ($ :div
              ($ :p {:class "text-slate-500 mb-6"}
                "You've been invited to join a workspace. Click below to accept.")
              ($ button
                {:on-click #(rf/dispatch [::tenant/accept-invitation
                                          {:token (or token url-token)}])
                 :btn-type :primary
                 :class "w-full"
                 :loading tenant-loading?
                 :id "accept-invitation-btn"}
                "Accept Invitation"))))))))

(comment
  ;; (require 'app.template.frontend.pages.invitation-accept :reload)
  :rcf)
