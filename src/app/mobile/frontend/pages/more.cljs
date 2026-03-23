(ns app.mobile.frontend.pages.more
  "Mobile more menu — receipts, language indicator, logout."
  (:require
    [app.mobile.frontend.components.header :refer [mobile-header]]
    [app.template.frontend.i18n :refer [use-t]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

;; ========================================================================
;; Events
;; ========================================================================

(rf/reg-event-fx
  :mobile/logout
  (fn [_ _]
    {:http-xhrio {:method :post
                  :uri "/api/v1/auth/logout"
                  :format {:write identity :content-type "application/json"}
                  :response-format {:read identity :description "raw"}
                  :on-success [:mobile/logout-done]
                  :on-failure [:mobile/logout-done]}}))

(rf/reg-event-fx
  :mobile/logout-done
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:session :authenticated?] false)
     :fx [[:dispatch [:mobile/navigate "/m/login"]]]}))

;; ========================================================================
;; Components
;; ========================================================================

(defui menu-item [{:keys [icon-path label sublabel on-click]}]
  ($ :button
    {:class "flex items-center w-full p-4 bg-base-100 rounded-xl shadow-sm active:bg-base-200 transition-colors"
     :on-click on-click}
    ($ :div {:class "flex items-center justify-center w-10 h-10 bg-base-200 rounded-lg mr-3"}
      ($ :svg {:class "w-5 h-5 text-base-content/60" :fill "none" :viewBox "0 0 24 24" :stroke-width "1.5" :stroke "currentColor"}
        ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :d icon-path})))
    ($ :div {:class "flex-1 text-left"}
      ($ :p {:class "text-sm font-medium"} label)
      (when sublabel
        ($ :p {:class "text-xs text-base-content/50"} sublabel)))
    ($ :svg {:class "w-4 h-4 text-base-content/30" :fill "none" :viewBox "0 0 24 24" :stroke-width "2" :stroke "currentColor"}
      ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :d "M8.25 4.5l7.5 7.5-7.5 7.5"}))))

;; ========================================================================
;; Main page
;; ========================================================================

(defui more-page []
  (let [t (use-t)
        locale (use-subscribe [:locale])]
    ($ :<>
      ($ mobile-header {:title (t :mobile/tab-more "More")})

      ($ :div {:class "p-4 space-y-3 pb-24"}
        ;; Receipts
        ($ menu-item
          {:icon-path "M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z"
           :label (t :mobile/receipts-title "Receipts")
           :sublabel (t :mobile/view-receipts "View all uploaded receipts")
           :on-click #(rf/dispatch [:mobile/navigate "/m/receipts"])})

        ;; Search
        ($ menu-item
          {:icon-path "M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z"
           :label (t :mobile/search "Search")
           :sublabel (t :mobile/search-expenses "Search expenses by supplier")
           :on-click #(rf/dispatch [:mobile/navigate "/m/expenses"])})

        ;; Language indicator
        ($ :div {:class "flex items-center p-4 bg-base-100 rounded-xl shadow-sm"}
          ($ :div {:class "flex items-center justify-center w-10 h-10 bg-base-200 rounded-lg mr-3"}
            ($ :svg {:class "w-5 h-5 text-base-content/60" :fill "none" :viewBox "0 0 24 24" :stroke-width "1.5" :stroke "currentColor"}
              ($ :path {:stroke-linecap "round" :stroke-linejoin "round"
                        :d "M10.5 21l5.25-11.25L21 21m-9-3h7.5M3 5.621a48.474 48.474 0 016-.371m0 0c1.12 0 2.233.038 3.334.114M9 5.25V3m3.334 2.364C11.176 10.658 7.69 15.08 3 17.502m9.334-12.138c.896.061 1.785.147 2.666.257m-4.589 8.495a18.023 18.023 0 01-3.827-5.802"})))
          ($ :div {:class "flex-1"}
            ($ :p {:class "text-sm font-medium"}
              (t :mobile/language "Language"))
            ($ :p {:class "text-xs text-base-content/50"}
              (t :mobile/change-on-desktop "Change language on desktop")))
          ($ :span {:class "ds-badge ds-badge-primary ds-badge-sm"}
            (if (= locale :bs) "BS" "EN")))

        ;; Divider
        ($ :div {:class "ds-divider my-2"})

        ;; Logout
        ($ :button
          {:class "flex items-center w-full p-4 bg-base-100 rounded-xl shadow-sm active:bg-error/10 transition-colors"
           :on-click #(rf/dispatch [:mobile/logout])}
          ($ :div {:class "flex items-center justify-center w-10 h-10 bg-error/10 rounded-lg mr-3"}
            ($ :svg {:class "w-5 h-5 text-error" :fill "none" :viewBox "0 0 24 24" :stroke-width "1.5" :stroke "currentColor"}
              ($ :path {:stroke-linecap "round" :stroke-linejoin "round"
                        :d "M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15m3 0l3-3m0 0l-3-3m3 3H9"})))
          ($ :p {:class "text-sm font-medium text-error"}
            (t :common/sign-out "Sign Out")))))))
