(ns app.template.frontend.pages.management-guide
  "Role-specific management guide page — linked from the management_intro
   onboarding step. Shows different content for owner vs admin."
  (:require
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.i18n :refer [use-t]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defui tool-card [{:keys [title description link link-label]}]
  ($ :div {:class "bg-base-100 rounded-lg border border-base-300 p-4 flex flex-col gap-2"}
    ($ :h3 {:class "font-semibold text-sm"} title)
    ($ :p {:class "text-sm text-base-content/60"} description)
    (when link
      ($ :div {:class "mt-auto pt-2"}
        ($ button {:btn-type :ghost
                   :class "ds-btn-sm"
                   :on-click #(rf/dispatch [:navigate-to link])}
          link-label)))))

(defui management-guide-page []
  (let [t (use-t)
        role (use-subscribe [:user-role])]
    ($ :div {:class "max-w-3xl mx-auto p-6"}
      ;; Header
      ($ :div {:class "mb-8"}
        ($ :div {:class "text-sm ds-breadcrumbs mb-2"}
          ($ :ul
            ($ :li ($ :a {:href "/onboarding"
                          :on-click (fn [e]
                                      (.preventDefault e)
                                      (rf/dispatch [:navigate-to "/onboarding"]))}
                     (t :onboarding/sidebar-label)))
            ($ :li (t :guide/breadcrumb))))
        ($ :h1 {:class "text-2xl font-bold mb-2"}
          (t :guide/title))
        ($ :p {:class "text-base-content/60"}
          (t :guide/subtitle)))

      ;; Role badge
      ($ :div {:class "ds-alert bg-primary/5 border-primary/20 mb-6"}
        ($ :span {:class "text-sm"}
          (t :guide/your-role)
          ($ :span {:class "ds-badge ds-badge-primary ds-badge-sm ml-2"} role)))

      ;; Tools available to all power users (owner + admin)
      ($ :div {:class "mb-6"}
        ($ :h2 {:class "text-lg font-semibold mb-3"} (t :guide/section-data))
        ($ :div {:class "grid grid-cols-1 sm:grid-cols-2 gap-3"}
          ($ tool-card {:title (t :guide/tool-receipts)
                        :description (t :guide/tool-receipts-desc)
                        :link "/receipts"
                        :link-label (t :guide/open)})
          ($ tool-card {:title (t :guide/tool-expense-items)
                        :description (t :guide/tool-expense-items-desc)
                        :link "/expense-items"
                        :link-label (t :guide/open)})
          ($ tool-card {:title (t :guide/tool-unmapped)
                        :description (t :guide/tool-unmapped-desc)
                        :link "/unmapped-items"
                        :link-label (t :guide/open)})
          ($ tool-card {:title (t :guide/tool-reports)
                        :description (t :guide/tool-reports-desc)
                        :link "/expenses/reports"
                        :link-label (t :guide/open)})))

      ;; Reference data
      ($ :div {:class "mb-6"}
        ($ :h2 {:class "text-lg font-semibold mb-3"} (t :guide/section-reference))
        ($ :div {:class "grid grid-cols-1 sm:grid-cols-2 gap-3"}
          ($ tool-card {:title (t :guide/tool-categories)
                        :description (t :guide/tool-categories-desc)
                        :link "/expense-categories"
                        :link-label (t :guide/open)})
          ($ tool-card {:title (t :guide/tool-payers)
                        :description (t :guide/tool-payers-desc)
                        :link "/payers"
                        :link-label (t :guide/open)})
          ($ tool-card {:title (t :guide/tool-suppliers)
                        :description (t :guide/tool-suppliers-desc)
                        :link "/suppliers"
                        :link-label (t :guide/open)})
          ($ tool-card {:title (t :guide/tool-stores)
                        :description (t :guide/tool-stores-desc)
                        :link "/stores"
                        :link-label (t :guide/open)})))

      ;; Owner-only section
      (when (= role "owner")
        ($ :div {:class "mb-6"}
          ($ :h2 {:class "text-lg font-semibold mb-3"} (t :guide/section-workspace))
          ($ :div {:class "grid grid-cols-1 sm:grid-cols-2 gap-3"}
            ($ tool-card {:title (t :guide/tool-members)
                          :description (t :guide/tool-members-desc)
                          :link "/tenant/members"
                          :link-label (t :guide/open)})
            ($ tool-card {:title (t :guide/tool-impersonation)
                          :description (t :guide/tool-impersonation-desc)
                          :link "/tenant/impersonation"
                          :link-label (t :guide/open)})
            ($ tool-card {:title (t :guide/tool-profile)
                          :description (t :guide/tool-profile-desc)
                          :link "/profile"
                          :link-label (t :guide/open)}))))

      ;; Admin section (members but not impersonation)
      (when (= role "admin")
        ($ :div {:class "mb-6"}
          ($ :h2 {:class "text-lg font-semibold mb-3"} (t :guide/section-workspace))
          ($ :div {:class "grid grid-cols-1 sm:grid-cols-2 gap-3"}
            ($ tool-card {:title (t :guide/tool-members)
                          :description (t :guide/tool-members-admin-desc)
                          :link "/tenant/members"
                          :link-label (t :guide/open)}))))

      ;; Back to onboarding
      ($ :div {:class "flex justify-end pt-4"}
        ($ button {:btn-type :primary
                   :on-click #(rf/dispatch [:navigate-to "/onboarding"])}
          (t :guide/back-to-onboarding))))))

(comment
  ;; (require 'app.template.frontend.pages.management-guide :reload)
  :rcf)
