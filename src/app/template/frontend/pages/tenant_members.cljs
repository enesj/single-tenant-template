(ns app.template.frontend.pages.tenant-members
  "Tenant member management page for admin/owner roles.
   Shows current members with role management and pending invitations."
  (:require
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.events.tenant :as tenant]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-state]]
    [uix.re-frame :refer [use-subscribe]]))

;; ============================================================================
;; Role Helpers
;; ============================================================================

(defn- role-badge-class [role]
  (case role
    "owner" "ds-badge-primary"
    "admin" "ds-badge-secondary"
    "member" "ds-badge-accent"
    "viewer" "ds-badge-ghost"
    "ds-badge-ghost"))

(defn- member-id [m]
  (or (:id m) (:tenant_memberships/id m)))

(defn- member-user-id [m]
  (or (:user-id m) (:user_id m) (:tenant_memberships/user_id m)))

(defn- member-role [m]
  (or (:role m) (:tenant_memberships/role m)))

(defn- member-name [m]
  (or (:user_full_name m) (:user-full-name m)
    (:full-name m) (:full_name m) (:users/full_name m)
    (:user_email m) (:email m) (:users/email m) "Unknown"))

(defn- member-email [m]
  (or (:user_email m) (:user-email m) (:email m) (:users/email m) ""))

(defn- member-joined [m]
  (let [d (or (:created_at m) (:created-at m)
            (:joined-at m) (:joined_at m) (:tenant_memberships/joined_at m))]
    (when d
      (subs (str d) 0 (min 10 (count (str d)))))))

;; ============================================================================
;; Member Row Component
;; ============================================================================

(defui member-row [{:keys [member current-role is-owner?]}]
  (let [mid (member-id member)
        role (member-role member)
        is-target-owner? (= role "owner")
        can-change-role? (and (contains? #{"owner" "admin"} current-role)
                           (not is-target-owner?))
        can-remove? (and (contains? #{"owner" "admin"} current-role)
                      (not is-target-owner?))
        can-transfer? (and is-owner? (= role "admin"))
        [confirming? set-confirming!] (use-state nil)]
    ($ :tr {:id (str "member-row-" mid)}
      ($ :td {:class "font-medium"} (member-name member))
      ($ :td (member-email member))
      ($ :td
        ($ :span {:class (str "ds-badge ds-badge-sm " (role-badge-class role))}
          role))
      ($ :td {:class "text-sm text-base-content/60"} (member-joined member))
      ($ :td {:class "flex gap-2 items-center"}
        ;; Role change dropdown
        (when can-change-role?
          ($ :select {:class "ds-select ds-select-xs ds-select-bordered w-24"
                      :id (str "role-select-" mid)
                      :value role
                      :on-change (fn [e]
                                   (let [new-role (.. e -target -value)]
                                     (when (not= new-role role)
                                       (rf/dispatch [::tenant/change-member-role
                                                     {:member-id mid :role new-role}]))))}
            ($ :option {:value "viewer"} "viewer")
            ($ :option {:value "member"} "member")
            ($ :option {:value "admin"} "admin")))

        ;; Transfer ownership
        (when can-transfer?
          (if (= confirming? :transfer)
            ($ :div {:class "flex gap-1"}
              ($ button {:btn-type :warning :class "ds-btn-xs"
                         :on-click (fn []
                                     (rf/dispatch [::tenant/transfer-ownership
                                                   {:user-id (member-user-id member)}])
                                     (set-confirming! nil))}
                "Confirm")
              ($ button {:btn-type :ghost :class "ds-btn-xs"
                         :on-click #(set-confirming! nil)}
                "Cancel"))
            ($ button {:btn-type :outline :class "ds-btn-xs"
                       :id (str "transfer-btn-" mid)
                       :on-click #(set-confirming! :transfer)}
              "Transfer")))

        ;; Remove member
        (when can-remove?
          (if (= confirming? :remove)
            ($ :div {:class "flex gap-1"}
              ($ button {:btn-type :error :class "ds-btn-xs"
                         :on-click (fn []
                                     (rf/dispatch [::tenant/remove-member {:member-id mid}])
                                     (set-confirming! nil))}
                "Confirm")
              ($ button {:btn-type :ghost :class "ds-btn-xs"
                         :on-click #(set-confirming! nil)}
                "Cancel"))
            ($ button {:btn-type :ghost :class "ds-btn-xs text-error"
                       :id (str "remove-btn-" mid)
                       :on-click #(set-confirming! :remove)}
              "Remove")))))))

;; ============================================================================
;; Invitation Row Component
;; ============================================================================

(defui invitation-row [{:keys [invitation]}]
  (let [inv-id (or (:id invitation) (:tenant_invitations/id invitation))
        email (or (:email invitation) (:tenant_invitations/email invitation))
        role (or (:role invitation) (:tenant_invitations/role invitation))
        status (or (:status invitation) (:tenant_invitations/status invitation))
        created (let [d (or (:created-at invitation) (:created_at invitation)
                          (:tenant_invitations/created_at invitation))]
                  (when d (subs (str d) 0 (min 10 (count (str d))))))
        [confirming? set-confirming!] (use-state false)]
    ($ :tr {:id (str "invitation-row-" inv-id)}
      ($ :td email)
      ($ :td
        ($ :span {:class (str "ds-badge ds-badge-sm " (role-badge-class role))}
          role))
      ($ :td
        ($ :span {:class "ds-badge ds-badge-sm ds-badge-warning"} (or status "pending")))
      ($ :td {:class "text-sm text-base-content/60"} created)
      ($ :td
        (if confirming?
          ($ :div {:class "flex gap-1"}
            ($ button {:btn-type :error :class "ds-btn-xs"
                       :on-click (fn []
                                   (rf/dispatch [::tenant/revoke-invitation {:id inv-id}])
                                   (set-confirming! false))}
              "Confirm")
            ($ button {:btn-type :ghost :class "ds-btn-xs"
                       :on-click #(set-confirming! false)}
              "Cancel"))
          ($ button {:btn-type :ghost :class "ds-btn-xs text-error"
                     :id (str "revoke-btn-" inv-id)
                     :on-click #(set-confirming! true)}
            "Revoke"))))))

;; ============================================================================
;; Invite Form Component
;; ============================================================================

(defui invite-form []
  (let [[email set-email!] (use-state "")
        [role set-role!] (use-state "member")
        loading? (use-subscribe [:tenant/loading?])]
    ($ :form {:class "flex gap-3 items-end flex-wrap"
              :id "invite-form"
              :on-submit (fn [e]
                           (.preventDefault e)
                           (when (seq email)
                             (rf/dispatch [::tenant/invite-member {:email email :role role}])
                             (set-email! "")))}
      ($ :div {:class "flex-1 min-w-[200px]"}
        ($ :label {:class "ds-label"}
          ($ :span {:class "ds-label-text"} "Email"))
        ($ :input {:class "ds-input ds-input-bordered w-full"
                   :id "invite-email-input"
                   :type "email"
                   :placeholder "user@example.com"
                   :value email
                   :required true
                   :on-change #(set-email! (.. % -target -value))}))
      ($ :div
        ($ :label {:class "ds-label"}
          ($ :span {:class "ds-label-text"} "Role"))
        ($ :select {:class "ds-select ds-select-bordered"
                    :id "invite-role-select"
                    :value role
                    :on-change #(set-role! (.. % -target -value))}
          ($ :option {:value "viewer"} "Viewer")
          ($ :option {:value "member"} "Member")
          ($ :option {:value "admin"} "Admin")))
      ($ button {:btn-type :primary
                 :type "submit"
                 :class "ds-btn-sm"
                 :id "invite-submit-btn"
                 :loading loading?}
        "Send Invite"))))

;; ============================================================================
;; Main Page Component
;; ============================================================================

(defui tenant-members-page []
  (let [members (use-subscribe [:tenant/members])
        invitations (use-subscribe [:tenant/invitations])
        error (use-subscribe [:tenant/error])
        success (use-subscribe [:tenant/success-message])
        role (use-subscribe [:user-role])
        tenant (use-subscribe [:current-tenant])
        is-owner? (= role "owner")]

    ($ :div {:class "p-6 max-w-4xl mx-auto"}
      ;; Page header
      ($ :div {:class "mb-6"}
        ($ :h1 {:class "text-2xl font-bold"} "Members")
        (when tenant
          ($ :p {:class "text-base-content/60 mt-1"}
            (str "Manage members of " (or (:name tenant) (:tenants/name tenant) "your workspace")))))

      ;; Alerts
      (when error
        ($ :div {:class "ds-alert ds-alert-error mb-4"
                 :id "tenant-error-alert"}
          ($ :span error)
          ($ :button {:class "ds-btn ds-btn-ghost ds-btn-xs"
                      :on-click #(rf/dispatch [::tenant/clear-messages])}
            "Dismiss")))

      (when success
        ($ :div {:class "ds-alert ds-alert-success mb-4"
                 :id "tenant-success-alert"}
          ($ :span success)
          ($ :button {:class "ds-btn ds-btn-ghost ds-btn-xs"
                      :on-click #(rf/dispatch [::tenant/clear-messages])}
            "Dismiss")))

      ;; Section 1: Current Members
      ($ :div {:class "ds-card bg-base-100 shadow-sm border border-base-200 mb-6"}
        ($ :div {:class "ds-card-body"}
          ($ :h2 {:class "ds-card-title text-lg mb-4"} "Current Members")
          (if (seq members)
            ($ :div {:class "overflow-x-auto"}
              ($ :table {:class "ds-table ds-table-sm"
                         :id "members-table"}
                ($ :thead
                  ($ :tr
                    ($ :th "Name")
                    ($ :th "Email")
                    ($ :th "Role")
                    ($ :th "Joined")
                    ($ :th "Actions")))
                ($ :tbody
                  (for [m members]
                    ($ member-row {:key (member-id m)
                                   :member m
                                   :current-role role
                                   :is-owner? is-owner?})))))
            ($ :p {:class "text-base-content/60"} "No members yet."))))

      ;; Section 2: Invitations
      ($ :div {:class "ds-card bg-base-100 shadow-sm border border-base-200"}
        ($ :div {:class "ds-card-body"}
          ($ :h2 {:class "ds-card-title text-lg mb-4"} "Invitations")

          ;; Invite form
          ($ invite-form)

          ;; Pending invitations table
          (when (seq invitations)
            ($ :div {:class "overflow-x-auto mt-4"}
              ($ :table {:class "ds-table ds-table-sm"
                         :id "invitations-table"}
                ($ :thead
                  ($ :tr
                    ($ :th "Email")
                    ($ :th "Role")
                    ($ :th "Status")
                    ($ :th "Sent")
                    ($ :th "Actions")))
                ($ :tbody
                  (for [inv invitations]
                    ($ invitation-row {:key (or (:id inv) (:tenant_invitations/id inv))
                                       :invitation inv})))))))))))

(comment
  ;; (require 'app.template.frontend.pages.tenant-members :reload)
  :rcf)
