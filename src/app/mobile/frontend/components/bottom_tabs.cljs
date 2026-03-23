(ns app.mobile.frontend.components.bottom-tabs
  "Bottom tab bar navigation for the mobile app."
  (:require
    [app.template.frontend.i18n :refer [use-t]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui] :as uix]
    [uix.re-frame :refer [use-subscribe]]))

;; SVG icon paths (Heroicons outline)
(def ^:private icons
  {:dashboard "M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"
   :expenses  "M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01"
   :upload    "M3 9a2 2 0 012-2h.93a2 2 0 001.664-.89l.812-1.22A2 2 0 0110.07 4h3.86a2 2 0 011.664.89l.812 1.22A2 2 0 0018.07 7H19a2 2 0 012 2v9a2 2 0 01-2 2H5a2 2 0 01-2-2V9z M15 13a3 3 0 11-6 0 3 3 0 016 0z"
   :reports   "M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
   :more      "M4 6h16M4 12h16M4 18h16"})

(defui tab-icon [{:keys [icon-key active?]}]
  ($ :svg {:class (str "w-6 h-6 " (if active? "text-primary" "text-base-content/60"))
           :fill "none"
           :viewBox "0 0 24 24"
           :stroke-width "1.5"
           :stroke "currentColor"}
    ($ :path {:stroke-linecap "round"
              :stroke-linejoin "round"
              :d (get icons icon-key)})))

(defui upload-button [{:keys [active? badge-count on-click]}]
  ($ :button
    {:class (str "flex flex-col items-center justify-center relative -mt-5 "
              "w-14 h-14 rounded-full shadow-lg "
              (if active? "bg-primary text-primary-content" "bg-primary text-primary-content"))
     :on-click on-click}
    ($ :svg {:class "w-7 h-7" :fill "none" :viewBox "0 0 24 24" :stroke-width "1.5" :stroke "currentColor"}
      ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :d (:upload icons)}))
    (when (and badge-count (pos? badge-count))
      ($ :span {:class "absolute -top-1 -right-1 bg-error text-error-content text-xs rounded-full w-5 h-5 flex items-center justify-center font-bold"}
        badge-count))))

(defui tab-button [{:keys [icon-key label active? on-click]}]
  ($ :button
    {:class (str "flex flex-col items-center justify-center flex-1 py-2 "
              (when active? "text-primary"))
     :on-click on-click}
    ($ tab-icon {:icon-key icon-key :active? active?})
    ($ :span {:class (str "text-xs mt-0.5 " (if active? "text-primary font-medium" "text-base-content/60"))}
      label)))

(defui bottom-tabs []
  (let [t (use-t)
        active-tab (use-subscribe [:mobile/active-tab])
        pending-count (use-subscribe [:mobile/pending-review-count])
        navigate! (fn [path] (rf/dispatch [:mobile/navigate path]))]
    ($ :nav {:class "fixed bottom-0 left-0 right-0 bg-base-100 border-t border-base-300 z-50 safe-area-inset-bottom"}
      ($ :div {:class "flex items-end justify-around px-2"}
        ($ tab-button {:icon-key :dashboard
                       :label (t :mobile/tab-dashboard)
                       :active? (= active-tab :dashboard)
                       :on-click #(navigate! "/m/dashboard")})
        ($ tab-button {:icon-key :expenses
                       :label (t :mobile/tab-expenses)
                       :active? (= active-tab :expenses)
                       :on-click #(navigate! "/m/expenses")})
        ($ :div {:class "flex flex-col items-center justify-center flex-1"}
          ($ upload-button {:active? (= active-tab :upload)
                            :badge-count pending-count
                            :on-click #(navigate! "/m/upload")}))
        ($ tab-button {:icon-key :reports
                       :label (t :mobile/tab-reports)
                       :active? (= active-tab :reports)
                       :on-click #(navigate! "/m/reports")})
        ($ tab-button {:icon-key :more
                       :label (t :mobile/tab-more)
                       :active? (= active-tab :more)
                       :on-click #(navigate! "/m/more")})))))
