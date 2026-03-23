(ns app.mobile.frontend.pages.forgot-password
  "Mobile forgot-password page — email input, send reset link."
  (:require
    [app.template.frontend.i18n :refer [use-t]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui] :as uix]))

(defui forgot-password-page []
  (let [t (use-t)
        [email set-email!] (uix/use-state "")
        [submitted? set-submitted!] (uix/use-state false)
        [loading? set-loading!] (uix/use-state false)]
    ($ :div {:class "min-h-screen flex flex-col justify-center px-6 bg-base-100"}
      ($ :div {:class "mb-8"}
        ($ :a {:class "ds-link text-sm mb-4 inline-block"
               :href "/m/login"}
          "<- Back to login")
        ($ :h1 {:class "text-2xl font-bold"} (t :forgot-password/title))
        ($ :p {:class "text-base-content/60 mt-2 text-sm"}
          (t :forgot-password/subtitle)))

      (if submitted?
        ($ :div {:class "ds-alert ds-alert-success"}
          ($ :span (t :forgot-password/check-email-message)))
        ($ :form {:class "space-y-4"
                  :on-submit (fn [e]
                               (.preventDefault e)
                               (set-loading! true)
                               (rf/dispatch [:mobile/forgot-password
                                             {:email email}
                                             {:on-success #(do (set-loading! false)
                                                             (set-submitted! true))
                                              :on-error #(set-loading! false)}]))}
          ($ :div
            ($ :label {:class "ds-label" :for "email"} (t :common/email))
            ($ :input {:id "email"
                       :type "email"
                       :class "ds-input ds-input-bordered w-full"
                       :placeholder (t :common/email-placeholder)
                       :auto-complete "email"
                       :value email
                       :on-change #(set-email! (.. % -target -value))}))

          ($ :button {:type "submit"
                      :class (str "ds-btn ds-btn-primary w-full h-12 text-base "
                               (when loading? "ds-loading"))
                      :disabled loading?}
            (when-not loading? (t :forgot-password/submit))))))))
