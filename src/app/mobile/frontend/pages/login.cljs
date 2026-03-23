(ns app.mobile.frontend.pages.login
  "Mobile login page — simplified full-screen login form."
  (:require
    [app.template.frontend.components.icons :refer [google-icon]]
    [app.template.frontend.i18n :refer [use-t]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui] :as uix]
    [uix.re-frame :refer [use-subscribe]]))

(defui login-page []
  (let [t (use-t)
        [email set-email!] (uix/use-state "")
        [password set-password!] (uix/use-state "")
        [loading? set-loading!] (uix/use-state false)
        [error set-error!] (uix/use-state nil)]
    ($ :div {:class "min-h-screen flex flex-col justify-center px-6 bg-base-100"}
      ($ :div {:class "mb-8 text-center"}
        ($ :h1 {:class "text-2xl font-bold"} (t :common/sign-in))
        ($ :p {:class "text-base-content/60 mt-2 text-sm"}
          "Bookkeeping Mobile"))

      (when error
        ($ :div {:class "ds-alert ds-alert-error mb-4 text-sm"}
          ($ :span error)))

      ($ :form {:class "space-y-4"
                :on-submit (fn [e]
                             (.preventDefault e)
                             (set-loading! true)
                             (set-error! nil)
                             (rf/dispatch [:mobile/login
                                           {:email email :password password}
                                           {:on-error (fn [msg]
                                                        (set-loading! false)
                                                        (set-error! msg))}]))}
        ($ :div
          ($ :label {:class "ds-label" :for "email"} (t :common/email))
          ($ :input {:id "email"
                     :type "email"
                     :class "ds-input ds-input-bordered w-full"
                     :placeholder (t :common/email-placeholder)
                     :auto-complete "email"
                     :value email
                     :on-change #(set-email! (.. % -target -value))}))

        ($ :div
          ($ :label {:class "ds-label" :for "password"} (t :common/password))
          ($ :input {:id "password"
                     :type "password"
                     :class "ds-input ds-input-bordered w-full"
                     :placeholder (t :common/password-placeholder)
                     :auto-complete "current-password"
                     :value password
                     :on-change #(set-password! (.. % -target -value))}))

        ($ :button {:type "submit"
                    :class (str "ds-btn ds-btn-primary w-full h-12 text-base "
                             (when loading? "ds-loading"))
                    :disabled loading?}
          (when-not loading? (t :common/sign-in))))

      ;; Divider
      ($ :div {:class "flex items-center my-6"}
        ($ :div {:class "flex-1 border-t border-base-300"})
        ($ :span {:class "px-3 text-sm text-base-content/50"} (t :common/or))
        ($ :div {:class "flex-1 border-t border-base-300"}))

      ;; Google OAuth button
      ($ :button {:type "button"
                  :class "ds-btn ds-btn-outline w-full h-12 text-base gap-2"
                  :on-click #(set! (.-href js/window.location) "/login/google")}
        ($ google-icon)
        (t :login/continue-google))

      ($ :div {:class "mt-6 text-center"}
        ($ :a {:class "ds-link ds-link-primary text-sm"
               :href "/m/forgot-password"}
          (t :login/forgot-password)))

      ($ :div {:class "mt-8 text-center text-sm text-base-content/50"}
        ($ :p "Don't have an account?")
        ($ :p "Sign up on desktop.")))))
