(ns app.template.frontend.pages.tenant-select
  "Tenant selection page shown when a user belongs to multiple tenants
   and no tenant is active in the session."
  (:require
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.events.tenant :as tenant]
    [app.template.frontend.i18n :refer [use-t]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defn- role-badge-class [role]
  (case role
    "owner" "ds-badge-primary"
    "admin" "ds-badge-secondary"
    "member" "ds-badge-accent"
    "viewer" "ds-badge-ghost"
    "ds-badge-ghost"))

(defui tenant-card [{:keys [tenant on-select loading?]}]
  (let [tenant-name (or (:tenant-name tenant) (:name tenant) (:tenants/name tenant) "Unnamed")
        role (or (:role tenant) (:tenant_memberships/role tenant) "member")
        tenant-id (or (:tenant-id tenant) (:tenant_id tenant)
                    (:tenants/id tenant) (:id tenant))]
    ($ :div {:class "ds-card bg-base-100 shadow-md hover:shadow-lg transition-shadow cursor-pointer border border-base-200"
             :id (str "tenant-card-" tenant-id)
             :on-click (when-not loading?
                         #(on-select tenant-id))}
      ($ :div {:class "ds-card-body p-5"}
        ($ :div {:class "flex items-center justify-between"}
          ($ :div {:class "flex flex-col"}
            ($ :h3 {:class "font-semibold text-lg"} tenant-name)
            ($ :span {:class (str "ds-badge ds-badge-sm mt-1 " (role-badge-class role))}
              role))
          ($ :svg {:class "w-5 h-5 text-base-content/40"
                   :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
            ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                      :d "M9 5l7 7-7 7"})))))))

(defui tenant-select-page []
  (let [t (use-t)
        available-tenants (use-subscribe [:available-tenants])
        loading? (use-subscribe [:tenant/loading?])
        error (use-subscribe [:tenant/error])
        user (use-subscribe [:current-user])
        user-name (or (:full-name user) (:email user) "User")]
    ($ :div {:class "min-h-screen bg-gradient-to-b from-slate-50 to-white flex items-center justify-center px-4"}
      ($ :div {:class "max-w-md w-full"}
        ;; Card container
        ($ :div {:class "bg-white rounded-2xl shadow-xl border border-slate-100 p-8 text-center"}
          ;; Icon
          ($ :div {:class "mx-auto w-20 h-20 bg-blue-100 rounded-full flex items-center justify-center mb-6"}
            ($ :svg {:class "w-10 h-10 text-blue-600"
                     :fill "none"
                     :stroke "currentColor"
                     :viewBox "0 0 24 24"}
              ($ :path {:stroke-linecap "round"
                        :stroke-linejoin "round"
                        :stroke-width "2"
                        :d "M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"})))

          ;; Welcome text
          ($ :h1 {:class "text-2xl font-bold text-slate-800 mb-2"}
            (t :tenant/welcome user-name))

          ($ :h2 {:class "text-lg font-medium text-slate-600 mb-6"}
            (t :tenant/select-workspace))

          ;; Error
          (when error
            ($ :div {:class "ds-alert ds-alert-error mb-4"}
              ($ :span error)))

          ;; Loading
          (when loading?
            ($ :div {:class "flex items-center justify-center py-4"}
              ($ :span {:class "ds-loading ds-loading-spinner ds-loading-md"})))

          ;; Tenant list
          (when (and (not loading?) (seq available-tenants))
            ($ :div {:class "space-y-3 text-left"}
              (for [t* available-tenants]
                ($ tenant-card
                  {:key (or (:tenant-id t*) (:tenant_id t*) (:id t*))
                   :tenant t*
                   :loading? loading?
                   :on-select (fn [tid]
                                (rf/dispatch [::tenant/switch-tenant {:tenant-id tid}]))}))))

          ;; No tenants fallback
          (when (and (not loading?) (empty? available-tenants))
            ($ :p {:class "text-slate-500 py-4"}
              (t :tenant/no-workspaces))))

        ;; Footer
        ($ :div {:class "mt-6 text-center"}
          ($ button
            {:on-click #(rf/dispatch [:app.template.frontend.events.auth/logout])
             :variant :ghost
             :class "text-sm text-slate-500"}
            (t :common/sign-out)))))))

(comment
  ;; (require 'app.template.frontend.pages.tenant-select :reload)
  :rcf)
