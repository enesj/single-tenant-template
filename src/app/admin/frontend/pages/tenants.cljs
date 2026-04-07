(ns app.admin.frontend.pages.tenants
  "Platform admin tenant management page.
   Displays tenant list with search/filter, and tenant detail with members."
  (:require
    [app.admin.frontend.components.layout :refer [admin-layout]]
    [app.admin.frontend.events.tenants :as tenants]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.dropdown.action :as dropdown]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.components.list.cells :as list-cells]
    [app.template.frontend.components.modal-wrapper :refer [modal-wrapper]]
    [app.template.frontend.events.list.ui-state :as list-ui-state]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-callback use-effect use-state]]
    [uix.re-frame :refer [use-subscribe]]

    app.admin.frontend.adapters.tenants))

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

(def ^:private tenants-entity-spec
  {:fields [{:id "name" :label "Name" :type :text}
            {:id "slug" :label "Slug" :type :text}
            {:id "status" :label "Status" :type :text}
            {:id "member_count" :label "Members" :type :number}
            {:id "owner_name"
             :label "Owner"
             :type :text}
            {:id "created_at"
             :label "Created"
             :type :datetime
             :input-type "datetime-local"}]})

(defn tenant-detail-action-groups
  [on-select tenant-id]
  [{:group-title "View"
    :items [{:id "see-details"
             :icon "👁️"
             :label "See details"
             :on-click (fn [_event]
                         (on-select tenant-id))}]}])

(defn- render-tenant-row-actions
  [on-select tenant]
  (let [tenant-id (or (:id tenant) (:tenants/id tenant))]
    ($ list-cells/action-buttons
      {:item tenant
       :entity-name :tenants
       :show-edit? (:show-edit? tenant)
       :show-delete? (:show-delete? tenant)
       :edit-disabled? (:edit-disabled? tenant)
       :delete-disabled? (:delete-disabled? tenant)
       :on-edit-click (:on-edit-click tenant)
       :custom-actions (fn [_]
                         ($ :div {:class "flex items-center"}
                           ($ dropdown/action-dropdown
                             {:entity-id tenant-id
                              :actions (tenant-detail-action-groups on-select tenant-id)
                              :position :portal})))})))

(defn tenant-list-props
  [on-select]
  {:entity-name :tenants
   :entity-spec tenants-entity-spec
   :title "Tenants"
   :allow-add? false
   :allow-edit? true
   :allow-delete? true
   :form-display :modal
   :display-settings {:show-add-button? false
                      :show-filtering? false}
   :render-actions #(render-tenant-row-actions on-select %)})

;; ============================================================================
;; Member Row (in tenant detail)
;; ============================================================================

(defui admin-member-row [{:keys [member tenant-id is-admin-owner?]}]
  (let [mid (or (:id member) (:tenant_memberships/id member))
        role (str (or (:role member) (:tenant_memberships/role member)))
        email-label (or (:email-masked member) (:email_masked member) "Hidden")
        name (or (:user-display-name member)
               (:user_display_name member)
               (:user_full_name member)
               (:full_name member)
               (:user-ref member)
               (:user_ref member)
               email-label)
        created (format-date (or (:created_at member) (:joined_at member)))
        is-target-owner? (= role "owner")
        can-modify? (and is-admin-owner? (not is-target-owner?))
        [confirming? set-confirming!] (use-state nil)]
    ($ :tr {:id (str "admin-member-row-" mid)}
      ($ :td {:class "font-medium"} name)
      ($ :td email-label)
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

(defn tenant-detail-modal-props
  [visible? on-close]
  {:id "admin-tenant-details-modal"
   :visible? visible?
   :title "Tenant details"
   :size :extra-large
   :draggable? true
   :on-close on-close
   :close-button-id "btn-close-admin-tenant-details-modal"
   :content-class "p-0"})

(defui tenant-detail-view
  [{:keys [on-close show-back-button?]
    :or {show-back-button? true}}]
  (let [tenant (use-subscribe [:admin/tenant-detail])
        members (use-subscribe [:admin/tenant-members])
        members-loading? (use-subscribe [:admin/tenant-members-loading?])
        error (use-subscribe [:admin/tenants-error])
        admin-role (use-subscribe [:admin/current-user-role])
        is-admin-owner? (= admin-role :owner)
        tenant-id (or (:id tenant) (:tenants/id tenant))
        owner-label (or (:owner_name tenant)
                      (:owner-name tenant)
                      (:owner_ref tenant)
                      (:owner-ref tenant)
                      "—")]
    ($ :div {:class "p-6 max-w-5xl mx-auto"}
      (when show-back-button?
        ($ :button {:class "ds-btn ds-btn-ghost ds-btn-sm mb-4"
                    :id "admin-tenants-back-btn"
                    :on-click on-close}
          "← Back to Tenants"))

      (when error
        ($ :div {:class "ds-alert ds-alert-error mb-4"}
          ($ :span error)))

      (when tenant
        ($ :<>
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
                  ($ :span {:class "font-medium"} owner-label))
                ($ :div
                  ($ :span {:class "text-base-content/60"} "Created: ")
                  ($ :span {:class "font-medium"} (format-date (:created_at tenant)))))))

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
                          ($ :th "Email hint")
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

(defui tenant-detail-modal
  [{:keys [visible? on-close]}]
  (let [tenant-loading? (use-subscribe [:admin/tenants-loading?])
        tenant (use-subscribe [:admin/tenant-detail])
        error (use-subscribe [:admin/tenants-error])]
    ($ modal-wrapper
      (tenant-detail-modal-props visible? on-close)
      (cond
        tenant-loading?
        ($ :div {:class "flex items-center justify-center py-16"}
          ($ :span {:class "ds-loading ds-loading-spinner ds-loading-lg text-primary"}))

        (or tenant error)
        ($ tenant-detail-view {:on-close on-close
                               :show-back-button? false})

        :else
        ($ :div {:class "flex items-center justify-center py-16 text-base-content/60"}
          "Loading tenant details…")))))

(defui tenant-list-view [{:keys [on-select]}]
  (let [total (or (use-subscribe [:admin/tenants-total]) 0)
        search (or (use-subscribe [:admin/tenants-search]) "")
        status (use-subscribe [:admin/tenants-status-filter])]
    ($ :div {:class "p-6"}
      ($ :div {:class "mb-6"}
        ($ :h1 {:class "text-2xl font-bold"} "Tenants")
        ($ :p {:class "text-base-content/60 mt-1"}
          (str "Platform tenant management — " total " total")))

      ($ :div {:class "flex gap-3 items-end flex-wrap mb-4"}
        ($ :div {:class "flex-1 min-w-[200px]"}
          ($ :input {:class "ds-input ds-input-bordered w-full"
                     :id "admin-tenants-search"
                     :type "text"
                     :placeholder "Search by name or slug..."
                     :value search
                     :on-change (fn [e]
                                  (rf/dispatch [::tenants/set-search (.. e -target -value)]))}))
        ($ :select {:class "ds-select ds-select-bordered"
                    :id "admin-tenants-status-filter"
                    :value (or status "")
                    :on-change (fn [e]
                                 (let [value (.. e -target -value)]
                                   (rf/dispatch [::tenants/set-status-filter
                                                 (when (seq value) value)])))}
          ($ :option {:value ""} "All Statuses")
          ($ :option {:value "active"} "Active")
          ($ :option {:value "provisioning"} "Provisioning")
          ($ :option {:value "suspended"} "Suspended")
          ($ :option {:value "archived"} "Archived")))

      ($ :div {:id "admin-tenants-table"}
        ($ list-view (tenant-list-props on-select))))))

;; ============================================================================
;; Main Page Component
;; ============================================================================

(defui admin-tenants-page []
  (let [[selected-tenant-id set-selected-tenant-id!] (use-state nil)
        refresh-list (use-callback
                       (fn []
                         (rf/dispatch-sync [::list-ui-state/set-pagination-mode :tenants :server])
                         (rf/dispatch-sync [::list-ui-state/set-refresh-event :tenants [::tenants/load-list]])
                         (rf/dispatch [::tenants/load-list]))
                       [])
        open-tenant-detail (use-callback
                             (fn [tenant-id]
                               (set-selected-tenant-id! tenant-id)
                               (rf/dispatch [::tenants/fetch-tenant-detail tenant-id])
                               (rf/dispatch [::tenants/fetch-tenant-members tenant-id]))
                             [])
        close-tenant-detail (use-callback
                              (fn []
                                (set-selected-tenant-id! nil)
                                (rf/dispatch [::tenants/clear-detail]))
                              [])]
    (use-effect
      (fn []
        (refresh-list)
        js/undefined)
      [refresh-list])
    ($ admin-layout
      ($ tenant-list-view {:on-select open-tenant-detail})
      ($ tenant-detail-modal {:visible? (some? selected-tenant-id)
                              :on-close close-tenant-detail}))))

(comment
  ;; (require 'app.admin.frontend.pages.tenants :reload)
  :rcf)
