(ns app.template.frontend.pages.onboarding
  "Workspace onboarding page — guided checklist for new users."
  (:require
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.events.onboarding :as onboarding]
    [app.template.frontend.i18n :refer [use-t]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

;; ============================================================================
;; Step Config (display metadata)
;; ============================================================================

(def step-config
  {"name_workspace"       {:icon "building"    :color "primary"   :link "/tenant/members"}
   "setup_profile"        {:icon "user"        :color "secondary" :link "/profile"}
  "rename_default_category" {:icon "tag"      :color "accent"    :link "/expense-categories"}
   "configure_categories" {:icon "tag"         :color "accent"    :link "/expense-categories"}
   "review_payer_types"   {:icon "credit-card" :color "accent"    :link "/payer-types"}
   "invite_members"       {:icon "user-plus"   :color "primary"   :link "/tenant/members"}
   "management_intro"     {:icon "shield"      :color "secondary" :link "/management-guide"}
   "upload_first_receipt" {:icon "upload"      :color "primary"   :link "/expenses/upload"}
   "browse_intro"         {:icon "eye"         :color "accent"    :link "/expenses"}})

;; ============================================================================
;; Step Icons (inline SVG)
;; ============================================================================

(defui step-icon [{:keys [kind class]}]
  (let [{:keys [icon]} (get step-config kind)]
    (case icon
      "building"
      ($ :svg {:class class :xmlns "http://www.w3.org/2000/svg" :viewBox "0 0 24 24"
               :fill "none" :stroke "currentColor" :stroke-width "2"}
        ($ :path {:d "M3 21h18M3 7v14m6-14v14m6-14v14m6-14v14M3 7l9-4 9 4M6 11h.01M6 15h.01M12 11h.01M12 15h.01M18 11h.01M18 15h.01"}))
      "user"
      ($ :svg {:class class :xmlns "http://www.w3.org/2000/svg" :viewBox "0 0 24 24"
               :fill "none" :stroke "currentColor" :stroke-width "2"}
        ($ :path {:d "M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"})
        ($ :circle {:cx "12" :cy "7" :r "4"}))
      "tag"
      ($ :svg {:class class :xmlns "http://www.w3.org/2000/svg" :viewBox "0 0 24 24"
               :fill "none" :stroke "currentColor" :stroke-width "2"}
        ($ :path {:d "M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"})
        ($ :line {:x1 "7" :y1 "7" :x2 "7.01" :y2 "7"}))
      "credit-card"
      ($ :svg {:class class :xmlns "http://www.w3.org/2000/svg" :viewBox "0 0 24 24"
               :fill "none" :stroke "currentColor" :stroke-width "2"}
        ($ :rect {:x "1" :y "4" :width "22" :height "16" :rx "2" :ry "2"})
        ($ :line {:x1 "1" :y1 "10" :x2 "23" :y2 "10"}))
      "user-plus"
      ($ :svg {:class class :xmlns "http://www.w3.org/2000/svg" :viewBox "0 0 24 24"
               :fill "none" :stroke "currentColor" :stroke-width "2"}
        ($ :path {:d "M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"})
        ($ :circle {:cx "8.5" :cy "7" :r "4"})
        ($ :line {:x1 "20" :y1 "8" :x2 "20" :y2 "14"})
        ($ :line {:x1 "23" :y1 "11" :x2 "17" :y2 "11"}))
      "shield"
      ($ :svg {:class class :xmlns "http://www.w3.org/2000/svg" :viewBox "0 0 24 24"
               :fill "none" :stroke "currentColor" :stroke-width "2"}
        ($ :path {:d "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"}))
      "upload"
      ($ :svg {:class class :xmlns "http://www.w3.org/2000/svg" :viewBox "0 0 24 24"
               :fill "none" :stroke "currentColor" :stroke-width "2"}
        ($ :path {:d "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"})
        ($ :polyline {:points "17 8 12 3 7 8"})
        ($ :line {:x1 "12" :y1 "3" :x2 "12" :y2 "15"}))
      "eye"
      ($ :svg {:class class :xmlns "http://www.w3.org/2000/svg" :viewBox "0 0 24 24"
               :fill "none" :stroke "currentColor" :stroke-width "2"}
        ($ :path {:d "M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"})
        ($ :circle {:cx "12" :cy "12" :r "3"}))
      ;; Default
      ($ :svg {:class class :xmlns "http://www.w3.org/2000/svg" :viewBox "0 0 24 24"
               :fill "none" :stroke "currentColor" :stroke-width "2"}
        ($ :circle {:cx "12" :cy "12" :r "10"})))))

;; ============================================================================
;; Status Icons
;; ============================================================================

(defui check-icon [{:keys [class]}]
  ($ :svg {:class class :xmlns "http://www.w3.org/2000/svg" :viewBox "0 0 24 24"
           :fill "none" :stroke "currentColor" :stroke-width "3"}
    ($ :polyline {:points "20 6 9 17 4 12"})))

(defui skip-icon [{:keys [class]}]
  ($ :svg {:class class :xmlns "http://www.w3.org/2000/svg" :viewBox "0 0 24 24"
           :fill "none" :stroke "currentColor" :stroke-width "2"}
    ($ :path {:d "M5 12h14M12 5l7 7-7 7"})))

;; ============================================================================
;; Step Row Component
;; ============================================================================

(defui step-row [{:keys [step step-loading? on-complete on-skip t]}]
  (let [kind (:kind step)
        status (:status step)
        completed? (= status "completed")
        skipped? (= status "skipped")
        pending? (= status "pending")
        loading? (= step-loading? kind)
        {:keys [link]} (get step-config kind)
        step-title-key (keyword "onboarding" (str "step-" kind))
        step-desc-key (keyword "onboarding" (str "step-" kind "-desc"))]
    ($ :div {:class (str "ds-card ds-card-compact bg-base-100 shadow-sm border "
                      (cond completed? "border-success/30 bg-success/5"
                        skipped? "border-base-300/50 opacity-60"
                        :else "border-base-300"))}
      ($ :div {:class "ds-card-body flex-row items-center gap-4 p-4"}
        ;; Status indicator
        ($ :div {:class (str "flex-shrink-0 w-10 h-10 rounded-full flex items-center justify-center "
                          (cond completed? "bg-success text-success-content"
                            skipped? "bg-base-300 text-base-content/50"
                            :else "bg-primary/10 text-primary"))}
          (cond
            completed? ($ check-icon {:class "w-5 h-5"})
            skipped? ($ skip-icon {:class "w-5 h-5"})
            :else ($ step-icon {:kind kind :class "w-5 h-5"})))

        ;; Step info
        ($ :div {:class "flex-1 min-w-0"}
          ($ :div {:class (str "font-medium "
                            (when (or completed? skipped?) "line-through opacity-70"))}
            (t step-title-key))
          ($ :div {:class "text-sm text-base-content/60 mt-0.5"}
            (t step-desc-key)))

        ;; Actions
        (when pending?
          ($ :div {:class "flex items-center gap-2 flex-shrink-0"}
            (when link
              ($ button {:class "ds-btn ds-btn-outline ds-btn-primary ds-btn-sm"
                         :on-click #(rf/dispatch [:navigate-to link])}
                (t :onboarding/go-to-step)))
            ($ button {:class "ds-btn ds-btn-primary ds-btn-sm"
                       :disabled loading?
                       :on-click #(on-complete kind)}
              (if loading?
                ($ :span {:class "ds-loading ds-loading-spinner ds-loading-xs"})
                (t :onboarding/mark-done)))
            ($ button {:class "ds-btn ds-btn-ghost ds-btn-sm"
                       :disabled loading?
                       :on-click #(on-skip kind)}
              (t :onboarding/skip))))))))

;; ============================================================================
;; Progress Bar
;; ============================================================================

(defui progress-bar [{:keys [completed total]}]
  (let [pct (if (pos? total) (Math/round (* 100 (/ completed total))) 0)]
    ($ :div {:class "mb-6"}
      ($ :div {:class "flex justify-between items-center mb-2"}
        ($ :span {:class "text-sm font-medium text-base-content/70"}
          (str completed "/" total))
        ($ :span {:class "text-sm font-medium text-primary"}
          (str pct "%")))
      ($ :progress {:class "ds-progress ds-progress-primary w-full"
                    :value pct :max 100}))))

;; ============================================================================
;; Role Difference Notice
;; ============================================================================

(defui role-difference-notice [{:keys [differences t]}]
  (when (seq differences)
    ($ :div {:class "ds-alert ds-alert-info mb-6 shadow-sm"}
      ($ :div
        ($ :h3 {:class "font-bold text-sm mb-2"}
          (t :onboarding/workspace-differences))
        ($ :ul {:class "text-sm space-y-1"}
          (for [diff differences]
            ($ :li {:key (:capability diff)}
              ($ :span {:class "font-medium"}
                (t (keyword "onboarding" (str "diff-" (:capability diff)))))
              ": "
              ($ :span (:detail diff)))))))))

;; ============================================================================
;; Main Page
;; ============================================================================

(defui onboarding-page []
  (let [t (use-t)
        progress (use-subscribe [:onboarding/progress])
        steps (use-subscribe [:onboarding/steps])
        differences (use-subscribe [:onboarding/differences])
        loading? (use-subscribe [:onboarding/loading?])
        error (use-subscribe [:onboarding/error])
        step-loading? (use-subscribe [:onboarding/data])
        completed (use-subscribe [:onboarding/completed-count])
        total (use-subscribe [:onboarding/total-count])
        all-done? (use-subscribe [:onboarding/all-done?])
        tenant-name (use-subscribe [:tenant-name])
        step-loading-kind (:step-loading? step-loading?)]

    ($ :div {:class "max-w-2xl mx-auto p-6"}
      ;; Header
      ($ :div {:class "text-center mb-8"}
        ($ :h1 {:class "text-2xl font-bold mb-2"}
          (t :onboarding/welcome))
        (when tenant-name
          ($ :p {:class "text-base-content/60"}
            (t :onboarding/workspace-intro {:name tenant-name}))))

      ;; Loading
      (when loading?
        ($ :div {:class "flex justify-center py-12"}
          ($ :span {:class "ds-loading ds-loading-spinner ds-loading-lg text-primary"})))

      ;; Error
      (when error
        ($ :div {:class "ds-alert ds-alert-error mb-6"}
          ($ :span error)))

      ;; Content
      (when (and (not loading?) progress)
        ($ :<>
          ;; Differences notice (for users entering a new workspace)
          ($ role-difference-notice {:differences differences :t t})

          ;; Progress bar
          ($ progress-bar {:completed completed :total total})

          ;; Steps list
          ($ :div {:class "space-y-3"}
            (for [step steps]
              ($ step-row {:key (:kind step)
                           :step step
                           :step-loading? step-loading-kind
                           :on-complete #(rf/dispatch [::onboarding/complete-step {:step-kind %}])
                           :on-skip #(rf/dispatch [::onboarding/skip-step {:step-kind %}])
                           :t t})))

          ;; Actions footer
          ($ :div {:class "mt-8 flex justify-between items-center"}
            ($ button {:class "ds-btn ds-btn-ghost ds-btn-sm"
                       :on-click #(rf/dispatch [::onboarding/skip-all])}
              (t :onboarding/skip-all))
            (if all-done?
              ($ button {:class "ds-btn ds-btn-primary"
                         :on-click #(rf/dispatch [::onboarding/dismiss])}
                (t :onboarding/finish))
              ($ button {:class "ds-btn ds-btn-outline ds-btn-sm"
                         :on-click #(rf/dispatch [:navigate-to "/dashboard"])}
                (t :onboarding/continue-later)))))))))

(comment
  ;; (require 'app.template.frontend.pages.onboarding :reload)
  :rcf)
