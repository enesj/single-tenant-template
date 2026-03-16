(ns app.template.frontend.pages.invitation-accept
  "Invitation acceptance page. Users land here from invitation email links.
   Extracts token from URL query params and provides an accept button."
  (:require
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.events.tenant :as tenant]
    [app.template.frontend.i18n :refer [use-t]]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect]]
    [uix.re-frame :refer [use-subscribe]]))

(defui invitation-accept-page []
  (let [t (use-t)
        auth-status (use-subscribe [:auth-status])
        authenticated? (:authenticated auth-status)
        current-user-email (:email (:user auth-status))
        error (use-subscribe [:tenant/error])
        token (use-subscribe [:tenant/accept-token])
        tenant-loading? (use-subscribe [:tenant/loading?])
        ;; Extract token from URL if not yet stored
        url-token (some-> (re-find #"[?&]token=([^&]*)" js/window.location.search)
                    second
                    js/decodeURIComponent)]

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
            (t :invitation/title))

          (cond
            ;; No token in URL
            (and (not token) (not url-token))
            ($ :div
              ($ :p {:class "text-slate-500 mb-4"}
                (t :invitation/no-token))
              ($ button
                {:on-click #(rf/dispatch [:navigate-to "/dashboard"])
                 :variant :outline
                 :class "w-full"}
                (t :invitation/go-to-dashboard)))

            ;; Not authenticated — redirect to login
            (not authenticated?)
            ($ :div
              ($ :p {:class "text-slate-500 mb-6"}
                (t :invitation/sign-in-required))
              ($ button
                {:on-click #(set! (.-href js/window.location)
                              (str "/login?return=" (js/encodeURIComponent (.-href js/window.location))))
                 :btn-type :primary
                 :class "w-full"}
                (t :common/sign-in)))

            ;; Error state
            error
            (let [email-mismatch? (str/includes? error "logged in as")
                  translated-error
                  (condp #(str/includes? %2 %1) error
                    "logged in as"   error ;; already descriptive with both emails
                    "expired"        (t :invitation/error-expired)
                    "no longer valid" (t :invitation/error-not-valid)
                    "already a member" (t :invitation/error-already-member)
                    (t :invitation/error-generic))]
              ($ :div
                ($ :div {:class "ds-alert ds-alert-error mb-4"}
                  ($ :span translated-error))
                (when email-mismatch?
                  ($ button
                    {:on-click #(do (rf/dispatch [::tenant/clear-messages])
                                  (rf/dispatch [:app.template.frontend.events.auth.ids/logout])
                                  ;; After logout clears session, redirect to login with return URL
                                  (js/setTimeout
                                    (fn [] (set! (.-href js/window.location)
                                             (str "/login?return=" (js/encodeURIComponent (.-href js/window.location)))))
                                    500))
                     :btn-type :primary
                     :class "w-full mb-2"}
                    (t :invitation/sign-in-different)))
                ($ button
                  {:on-click #(rf/dispatch [:navigate-to "/dashboard"])
                   :variant :outline
                   :class "w-full"}
                  (t :invitation/go-to-dashboard))))

            ;; Ready to accept
            :else
            ($ :div
              ($ :p {:class "text-slate-500 mb-2"}
                (t :invitation/ready-text))
              (when current-user-email
                ($ :p {:class "text-sm text-slate-400 mb-6"}
                  (t :invitation/logged-in-as) " " ($ :strong current-user-email)))
              ($ button
                {:on-click #(rf/dispatch [::tenant/accept-invitation
                                          {:token (or token url-token)}])
                 :btn-type :primary
                 :class "w-full"
                 :loading tenant-loading?
                 :id "accept-invitation-btn"}
                (t :invitation/accept)))))))))

(comment
  ;; (require 'app.template.frontend.pages.invitation-accept :reload)
  :rcf)
