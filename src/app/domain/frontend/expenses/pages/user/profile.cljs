(ns app.domain.frontend.expenses.pages.user.profile
  "User profile page for the settings hierarchy rollout."
  (:require
    [clojure.string :as str]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.i18n :refer [use-t]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defui section-card [{:keys [title description children]}]
  ($ :section {:class "bg-white rounded-xl shadow-sm border border-base-200 p-6"}
    ($ :div {:class "mb-4"}
      ($ :h2 {:class "text-lg font-semibold"} title)
      (when description
        ($ :p {:class "text-sm text-base-content/60 mt-1"} description)))
    children))

(defui info-row [{:keys [id label value hint]}]
  ($ :div {:class "flex flex-col gap-1 py-3 border-b border-base-200 last:border-0"}
    ($ :span {:class "text-xs uppercase tracking-wide text-base-content/50"} label)
    ($ :span {:id id :class "font-medium"} (or value "--"))
    (when hint
      ($ :span {:class "text-xs text-base-content/60"} hint))))

(defui profile-page []
  (let [t (use-t)
        user (use-subscribe [:current-user])
        tenant (use-subscribe [:current-tenant])
        role (use-subscribe [:user-role])
        owner? (boolean (use-subscribe [:is-tenant-owner?]))
        can-write? (boolean (use-subscribe [:expenses/can-write?]))
        power-user? (boolean (use-subscribe [:expenses/power-user?]))
        profile (or (use-subscribe [:profile/data]) {})
        loading? (boolean (use-subscribe [:profile/loading?]))
        saving? (boolean (use-subscribe [:profile/saving?]))
        export-loading? (boolean (use-subscribe [:profile/export-loading?]))
        delete-loading? (boolean (use-subscribe [:profile/delete-loading?]))
        error (use-subscribe [:profile/error])
        payers (or (use-subscribe [:user-expenses/payers]) [])
        profile-user (merge user (:user profile))
        settings (or (:settings profile) {})
        tenant-settings (:tenant-settings profile)
        default-payer-id (some-> (:default-payer-id settings) str)
        current-workspace-name (or (:name tenant) (:tenants/name tenant) "")
        current-email-notifications (if (contains? (or tenant-settings {}) :email-notifications)
                                      (boolean (:email-notifications tenant-settings))
                                      true)
        [selected-payer-id set-selected-payer-id!] (use-state default-payer-id)
        [workspace-name set-workspace-name!] (use-state current-workspace-name)
        [email-notifications set-email-notifications!] (use-state current-email-notifications)
        [delete-confirmation set-delete-confirmation!] (use-state "")
        defaults-dirty? (not= (or selected-payer-id "") (or default-payer-id ""))
        workspace-dirty? (not= (str/trim (or workspace-name ""))
                           (str/trim (or current-workspace-name "")))
        notifications-dirty? (not= email-notifications current-email-notifications)]
    (use-effect
      (fn []
        (set-selected-payer-id! default-payer-id)
        (set-workspace-name! current-workspace-name)
        (set-email-notifications! current-email-notifications)
        js/undefined)
      [default-payer-id current-workspace-name current-email-notifications])

    ($ :div {:class "min-h-screen bg-base-100"}
      ($ :header {:class "bg-white border-b border-base-200"}
        ($ :div {:class "max-w-5xl mx-auto px-4 py-4 sm:py-6"}
          ($ :div {:class "text-sm ds-breadcrumbs mb-2"}
            ($ :ul
              ($ :li ($ :a {:href "/expenses"} (t :profile/breadcrumb-expenses)))
              ($ :li (t :profile/breadcrumb-profile))))
          ($ :h1 {:class "text-2xl font-bold"} (t :profile/title))
          ($ :p {:class "text-sm text-base-content/60 mt-1"}
            (t :profile/subtitle))))

      ($ :main {:class "max-w-5xl mx-auto px-4 py-6 space-y-6"}
        (when error
          ($ :div {:id "profile-error-banner" :class "ds-alert ds-alert-error"}
            ($ :span (or error (t :profile/load-error)))))

        (if loading?
          ($ :div {:class "space-y-6"}
            (for [idx (range 3)]
              ($ :div {:key idx :class "h-40 rounded-xl bg-base-200 animate-pulse"})))

          ($ :<>
            ($ section-card {:title (t :profile/account-title)
                             :description (t :profile/account-desc)}
              ($ info-row {:id "profile-user-name"
                           :label (t :profile/name-label)
                           :value (or (:full-name profile-user)
                                    (:full_name profile-user)
                                    (t :profile/value-missing))})
              ($ info-row {:id "profile-user-email"
                           :label (t :profile/email-label)
                           :value (or (:email profile-user)
                                    (t :profile/value-missing))})
              ($ info-row {:id "profile-user-role"
                           :label (t :profile/role-label)
                           :value (or role (t :profile/value-missing))}))

            ($ section-card {:title (t :profile/defaults-title)
                             :description (t :profile/defaults-desc)}
              ($ :div {:class "py-3"}
                ($ :label {:class "block text-xs uppercase tracking-wide text-base-content/50 mb-2"
                           :for "profile-default-payer-select"}
                  (t :profile/default-payer-label))
                ($ :select {:id "profile-default-payer-select"
                            :class "ds-select ds-select-bordered w-full max-w-xl"
                            :value (or selected-payer-id "")
                            :disabled (or loading? (not can-write?))
                            :on-change #(let [value (.. % -target -value)]
                                          (set-selected-payer-id! (when (seq value) value)))}
                  ($ :option {:value ""} (t :profile/payer-none))
                  (for [payer payers]
                    ($ :option {:key (:id payer) :value (:id payer)}
                      (or (:label payer) (:name payer)))))
                ($ :p {:class "text-xs text-base-content/60 mt-2"}
                  (t :profile/default-payer-desc)))
              ($ :div {:class "flex justify-end pt-2"}
                ($ button {:id "btn-profile-save-defaults"
                           :btn-type :primary
                           :loading saving?
                           :disabled (or (not can-write?) (not defaults-dirty?))
                           :on-click #(rf/dispatch [:profile/update-defaults
                                                    {:default-payer-id selected-payer-id}])}
                  (t :profile/save-defaults))))

            (when power-user?
              ($ section-card {:title (t :profile/workspace-title)
                               :description (t :profile/workspace-desc)}
                ($ :div {:class "space-y-4"}
                  ($ :div
                    ($ :label {:class "block text-xs uppercase tracking-wide text-base-content/50 mb-2"
                               :for "profile-workspace-name-input"}
                      (t :profile/workspace-name-label))
                    ($ :input {:id "profile-workspace-name-input"
                               :class "ds-input ds-input-bordered w-full max-w-xl"
                               :type "text"
                               :value workspace-name
                               :disabled (or (not owner?) saving?)
                               :on-change #(set-workspace-name! (.. % -target -value))})
                    ($ :p {:class "text-xs text-base-content/60 mt-2"}
                      (if owner?
                        (t :profile/workspace-name-desc)
                        (t :profile/workspace-name-owner-only))))
                  ($ :div {:class "flex items-center justify-between py-2 border-y border-base-200"}
                    ($ :div {:class "pr-4"}
                      ($ :p {:class "font-medium text-sm"} (t :profile/email-notifications-label))
                      ($ :p {:class "text-xs text-base-content/60"}
                        (t :profile/email-notifications-desc)))
                    ($ :input {:id "profile-email-notifications-toggle"
                               :type "checkbox"
                               :class "ds-toggle ds-toggle-primary"
                               :checked email-notifications
                               :disabled saving?
                               :on-change #(set-email-notifications! (.. % -target -checked))}))
                  ($ :div {:class "flex flex-wrap justify-end gap-2 pt-2"}
                    (when owner?
                      ($ button {:id "btn-profile-save-workspace-name"
                                 :btn-type :primary
                                 :disabled (or saving? (not workspace-dirty?) (str/blank? workspace-name))
                                 :on-click #(rf/dispatch [:profile/update-tenant-name
                                                          {:name (str/trim workspace-name)}])}
                        (t :profile/save-workspace-name)))
                    ($ button {:id "btn-profile-save-workspace-settings"
                               :btn-type :primary
                               :disabled (or saving? (not notifications-dirty?))
                               :on-click #(rf/dispatch [:profile/update-tenant-settings
                                                        {:email-notifications email-notifications}])}
                      (t :profile/save-workspace-settings))))))

            (when power-user?
              ($ section-card {:title (t :profile/data-title)
                               :description (t :profile/data-desc)}
                ($ :div {:class "flex flex-col gap-5"}
                  ($ :div {:class "flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3"}
                    ($ :div
                      ($ :p {:class "font-medium text-sm"} (t :profile/export-label))
                      ($ :p {:class "text-xs text-base-content/60"}
                        (t :profile/export-desc)))
                    ($ button {:id "btn-profile-export-expenses"
                               :btn-type :outline
                               :loading export-loading?
                               :on-click #(rf/dispatch [:user-expenses/export {:format :csv :all true}])}
                      (t :profile/export-btn)))

                  ($ :div {:class "border-t border-base-200 pt-5"}
                    ($ :div {:class "mb-3"}
                      ($ :p {:class "font-medium text-sm text-error"} (t :profile/delete-label))
                      ($ :p {:class "text-xs text-base-content/60"}
                        (t :profile/delete-desc)))
                    ($ :label {:class "block text-xs uppercase tracking-wide text-base-content/50 mb-2"
                               :for "profile-delete-confirmation-input"}
                      (t :profile/delete-confirm-label))
                    ($ :input {:id "profile-delete-confirmation-input"
                               :class "ds-input ds-input-bordered w-full max-w-xs"
                               :type "text"
                               :value delete-confirmation
                               :placeholder (t :profile/delete-confirm-placeholder)
                               :on-change #(set-delete-confirmation! (.. % -target -value))})
                    ($ :div {:class "flex justify-end pt-3"}
                      ($ button {:id "btn-profile-delete-all-expenses"
                                 :btn-type :error
                                 :loading delete-loading?
                                 :disabled (not= "DELETE" delete-confirmation)
                                 :on-click #(do
                                              (rf/dispatch [:user-expenses/delete-all "DELETE_ALL_EXPENSES"])
                                              (set-delete-confirmation! ""))}
                        (t :profile/delete-btn)))))))))))))
