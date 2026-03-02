(ns app.admin.frontend.pages.tenants
  "Platform admin tenant management page.
   Displays tenant list with search/filter, and tenant detail with members."
  (:require
    [app.admin.frontend.components.layout :refer [admin-layout]]
    [app.admin.frontend.events.tenants :as tenants]
    [app.template.frontend.components.button :refer [button]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-state]]
    [uix.re-frame :refer [use-subscribe]]))

;; ============================================================================
;; Helpers
;; ============================================================================

(defn- role-badge-class [role]
  (case (str role)
    "owner" "ds-badge-primary"
    "admin" "ds-badge-secondary"
    "member" "ds-badge-accent"
    "viewer" "ds-badge-ghost"
    "ds-badge-ghost"))

(defn- status-badge-class [status]
  (case (str status)
    "active" "ds-badge-success"
    "suspended" "ds-badge-error"
    "provisioning" "ds-badge-warning"
    "archived" "ds-badge-ghost"
    "ds-badge-ghost"))

(defn- format-date [d]
  (when d
    (subs (str d) 0 (min 10 (count (str d))))))

;; ============================================================================
;; Member Row (in tenant detail)
;; ============================================================================

(defui admin-member-row [{:keys [member tenant-id is-admin-owner?]}]
  (let [mid (or (:id member) (:tenant_memberships/id member))
        role (str (or (:role member) (:tenant_memberships/role member)))
        email (or (:user_email member) (:email member) "")
        name (or (:user_full_name member) (:full_name member) email)
        created (format-date (or (:created_at member) (:joined_at member)))
        is-target-owner? (= role "owner")
        can-modify? (and is-admin-owner? (not is-target-owner?))
        [confirming? set-confirming!] (use-state nil)]
    ($ :tr {:id (str "admin-member-row-" mid)}
      ($ :td {:class "font-medium"} name)
      ($ :td email)
      ($ :td
        ($ :span {:class (str "ds-badge ds-badge-sm " (role-badge-class role))}
          role))
      ($ :td {:class "text-sm text-base-content/60"} created)
      ($ :td {:class "flex gap-2 items-center"}
        (when can-modify?
          ($ :select {:class "ds-select ds-select-xs ds-select-bordered w-24"
                      :id (str "admin-role-select-" mid)
                      :value role
                      :on-change (fn [e]
                                   (let [new-role (.. e -target -value)]
                                     (when (not= new-role role)
                                       (rf/dispatch [::tenants/change-member-role
                                                     {:tenant-id tenant-id
                                                      :member-id mid
                                                      :role new-role}]))))}
            ($ :option {:value "viewer"} "viewer")
            ($ :option {:value "member"} "member")
            ($ :option {:value "admin"} "admin")))
        (when can-modify?
          (if (= confirming? :remove)
            ($ :div {:class "flex gap-1"}
              ($ button {:btn-type :error :class "ds-btn-xs"
                         :on-click (fn []
                                     (rf/dispatch [::tenants/remove-member
                                                   {:tenant-id tenant-id :member-id mid}])
                                     (set-confirming! nil))}
                "Confirm")
              ($ button {:btn-type :ghost :class "ds-btn-xs"
                         :on-click #(set-confirming! nil)}
                "Cancel"))
            ($ button {:btn-type :ghost :class "ds-btn-xs text-error"
                       :id (str "admin-remove-btn-" mid)
                       :on-click #(set-confirming! :remove)}
              "Remove")))))))

;; ============================================================================
;; Tenant Detail View
;; ============================================================================

(defui tenant-detail-view [{:keys [on-back]}]
  (let [tenant (use-subscribe [:admin/tenant-detail])
        members (use-subscribe [:admin/tenant-members])
        members-loading? (use-subscribe [:admin/tenant-members-loading?])
        error (use-subscribe [:admin/tenants-error])
        admin-role (use-subscribe [:admin/current-user-role])
        is-admin-owner? (= admin-role :owner)
        tenant-id (or (:id tenant) (:tenants/id tenant))]
    ($ :div {:class "p-6 max-w-5xl mx-auto"}
      ;; Back button
      ($ :button {:class "ds-btn ds-btn-ghost ds-btn-sm mb-4"
                  :id "admin-tenants-back-btn"
                  :on-click on-back}
        "← Back to Tenants")

      (when error
        ($ :div {:class "ds-alert ds-alert-error mb-4"}
          ($ :span error)))

      (when tenant
        ($ :<>
          ;; Tenant info header
          ($ :div {:class "ds-card bg-base-100 shadow-sm border border-base-200 mb-6"}
            ($ :div {:class "ds-card-body"}
              ($ :div {:class "flex items-center justify-between"}
                ($ :div
                  ($ :h1 {:class "text-2xl font-bold"} (:name tenant))
                  ($ :p {:class "text-base-content/60 mt-1"}
                    (str "Slug: " (:slug tenant))))
                ($ :div {:class "flex gap-2 items-center"}
                  ($ :span {:class (str "ds-badge " (status-badge-class (:status tenant)))}
                    (str (:status tenant)))
                  (when (:member_count tenant)
                    ($ :span {:class "text-sm text-base-content/60"}
                      (str (:member_count tenant) " members")))))
              ($ :div {:class "grid grid-cols-2 md:grid-cols-4 gap-4 mt-4 text-sm"}
                ($ :div
                  ($ :span {:class "text-base-content/60"} "Owner: ")
                  ($ :span {:class "font-medium"} (or (:owner_name tenant) (:owner_email tenant) "—")))
                ($ :div
                  ($ :span {:class "text-base-content/60"} "Created: ")
                  ($ :span {:class "font-medium"} (format-date (:created_at tenant)))))))

          ;; Members table
          ($ :div {:class "ds-card bg-base-100 shadow-sm border border-base-200"}
            ($ :div {:class "ds-card-body"}
              ($ :h2 {:class "ds-card-title text-lg mb-4"} "Members")
              (if members-loading?
                ($ :div {:class "flex justify-center py-4"}
                  ($ :div {:class "ds-loading ds-loading-spinner ds-loading-md"}))
                (if (seq members)
                  ($ :div {:class "overflow-x-auto"}
                    ($ :table {:class "ds-table ds-table-sm"
                               :id "admin-tenant-members-table"}
                      ($ :thead
                        ($ :tr
                          ($ :th "Name")
                          ($ :th "Email")
                          ($ :th "Role")
                          ($ :th "Joined")
                          ($ :th "Actions")))
                      ($ :tbody
                        (for [m members]
                          ($ admin-member-row
                            {:key (or (:id m) (:tenant_memberships/id m))
                             :member m
                             :tenant-id tenant-id
                             :is-admin-owner? is-admin-owner?})))))
                  ($ :p {:class "text-base-content/60"} "No members found."))))))))))

;; ============================================================================
;; Tenant List View
;; ============================================================================

(defui tenant-list-view [{:keys [on-select]}]
  (let [tenants (use-subscribe [:admin/tenants])
        total (use-subscribe [:admin/tenants-total])
        loading? (use-subscribe [:admin/tenants-loading?])
        error (use-subscribe [:admin/tenants-error])
        [search set-search!] (use-state "")
        [status set-status!] (use-state nil)]
    ($ :div {:class "p-6 max-w-5xl mx-auto"}
      ;; Header
      ($ :div {:class "mb-6"}
        ($ :h1 {:class "text-2xl font-bold"} "Tenants")
        ($ :p {:class "text-base-content/60 mt-1"}
          (str "Platform tenant management — " total " total")))

      (when error
        ($ :div {:class "ds-alert ds-alert-error mb-4"}
          ($ :span error)))

      ;; Search and filter bar
      ($ :div {:class "flex gap-3 items-end flex-wrap mb-4"}
        ($ :div {:class "flex-1 min-w-[200px]"}
          ($ :input {:class "ds-input ds-input-bordered w-full"
                     :id "admin-tenants-search"
                     :type "text"
                     :placeholder "Search by name or slug..."
                     :value search
                     :on-change (fn [e]
                                  (let [v (.. e -target -value)]
                                    (set-search! v)
                                    (rf/dispatch [::tenants/set-search v])))}))
        ($ :select {:class "ds-select ds-select-bordered"
                    :id "admin-tenants-status-filter"
                    :value (or status "")
                    :on-change (fn [e]
                                 (let [v (.. e -target -value)
                                       v (when (seq v) v)]
                                   (set-status! v)
                                   (rf/dispatch [::tenants/set-status-filter v])))}
          ($ :option {:value ""} "All Statuses")
          ($ :option {:value "active"} "Active")
          ($ :option {:value "provisioning"} "Provisioning")
          ($ :option {:value "suspended"} "Suspended")
          ($ :option {:value "archived"} "Archived")))

      ;; Loading
      (when loading?
        ($ :div {:class "flex justify-center py-4"}
          ($ :div {:class "ds-loading ds-loading-spinner ds-loading-md"})))

      ;; Tenant table
      ($ :div {:class "ds-card bg-base-100 shadow-sm border border-base-200"}
        ($ :div {:class "ds-card-body"}
          (if (seq tenants)
            ($ :div {:class "overflow-x-auto"}
              ($ :table {:class "ds-table ds-table-sm"
                         :id "admin-tenants-table"}
                ($ :thead
                  ($ :tr
                    ($ :th "Name")
                    ($ :th "Slug")
                    ($ :th "Status")
                    ($ :th "Members")
                    ($ :th "Owner")
                    ($ :th "Created")))
                ($ :tbody
                  (for [t tenants]
                    (let [tid (or (:id t) (:tenants/id t))]
                      ($ :tr {:key tid
                              :class "cursor-pointer hover:bg-base-200"
                              :id (str "tenant-row-" tid)
                              :on-click #(on-select tid)}
                        ($ :td {:class "font-medium"} (:name t))
                        ($ :td {:class "text-base-content/60"} (:slug t))
                        ($ :td
                          ($ :span {:class (str "ds-badge ds-badge-sm " (status-badge-class (:status t)))}
                            (str (:status t))))
                        ($ :td (str (or (:member_count t) 0)))
                        ($ :td (or (:owner_name t) (:owner_email t) "—"))
                        ($ :td {:class "text-sm text-base-content/60"} (format-date (:created_at t)))))))))
            (when-not loading?
              ($ :p {:class "text-base-content/60"} "No tenants found."))))))))

;; ============================================================================
;; Main Page Component
;; ============================================================================

(defui admin-tenants-page []
  (let [[selected-tenant-id set-selected-tenant-id!] (use-state nil)]
    ($ admin-layout
      (if selected-tenant-id
        ($ tenant-detail-view
          {:on-back (fn []
                      (set-selected-tenant-id! nil)
                      (rf/dispatch [::tenants/clear-detail]))})
        ($ tenant-list-view
          {:on-select (fn [tid]
                        (set-selected-tenant-id! tid)
                        (rf/dispatch [::tenants/fetch-tenant-detail tid])
                        (rf/dispatch [::tenants/fetch-tenant-members tid]))})))))

(comment
  ;; (require 'app.admin.frontend.pages.tenants :reload)
  :rcf)
