(ns app.template.frontend.pages.tenant-members
  "Tenant member management page for admin/owner roles.
   Shows current members with role management and pending invitations."
  (:require
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.confirm-dialog :as confirm-dialog]
    [app.template.frontend.components.icons :refer [delete-icon edit-icon]]
    [app.template.frontend.components.list :refer [list-view]]
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

(defn- normalize-role
  [role]
  (cond
    (keyword? role) (name role)
    (string? role) role
    (some? role) (str role)
    :else nil))

(def tenant-members-entity-spec
  {:fields [{:id "member_name" :label "Name" :type :text}
            {:id "member_email" :label "Email" :type :text :input-type "email"}
            {:id "member_role" :label "Role" :type :text}
            {:id "joined_on" :label "Joined" :type :text}]})

(defn member-management-capabilities
  [current-role is-owner? member]
  (let [current-role* (normalize-role current-role)
        role (normalize-role (member-role member))
        is-target-owner? (= role "owner")]
    {:role role
     :can-change-role? (and (contains? #{"owner" "admin"} current-role*)
                         (not is-target-owner?))
     :can-remove? (and (contains? #{"owner" "admin"} current-role*)
                    (not is-target-owner?))
     :can-transfer? (and is-owner? (= role "admin"))}))

(defn tenant-member-row
  [member]
  (assoc member
    :member_name (member-name member)
    :member_email (member-email member)
    :member_role (member-role member)
    :joined_on (member-joined member)))

;; ============================================================================
;; Member Row Component
;; ============================================================================

(defn member-row-action-state
  [current-role is-owner? member]
  (let [{:keys [can-change-role? can-remove? can-transfer?] :as capabilities}
        (member-management-capabilities current-role is-owner? member)]
    (assoc capabilities
      :show-edit? (and can-change-role? (not (false? (:show-edit? member))))
      :show-delete? (and can-remove? (not (false? (:show-delete? member))))
      :edit-disabled? (true? (:edit-disabled? member))
      :delete-disabled? (true? (:delete-disabled? member))
      :can-transfer? can-transfer?)))

(defui member-transfer-action
  [{:keys [member can-transfer?]}]
  (let [mid (member-id member)
        [confirming? set-confirming!] (use-state false)]
    (when can-transfer?
      (if confirming?
        ($ :div {:class "flex gap-1"}
          ($ button {:btn-type :warning
                     :class "ds-btn-xs"
                     :on-click (fn []
                                 (rf/dispatch [::tenant/transfer-ownership
                                               {:user-id (member-user-id member)}])
                                 (set-confirming! false))}
            "Confirm")
          ($ button {:btn-type :ghost
                     :class "ds-btn-xs"
                     :on-click #(set-confirming! false)}
            "Cancel"))
        ($ button {:btn-type :outline
                   :class "ds-btn-xs"
                   :id (str "transfer-btn-" mid)
                   :on-click #(set-confirming! true)}
          "Transfer")))))

(defui member-edit-form
  [{:keys [member on-success on-cancel]}]
  (let [mid (member-id member)
        current-role (normalize-role (member-role member))
        [selected-role set-selected-role!] (use-state current-role)]
    ($ :form {:class "space-y-4"
              :id (str "tenant-member-edit-form-" mid)
              :on-submit (fn [e]
                           (.preventDefault e)
                           (when (not= selected-role current-role)
                             (rf/dispatch [::tenant/change-member-role
                                           {:member-id mid :role selected-role}]))
                           (when on-success
                             (on-success)))}
      ($ :div {:class "space-y-1"}
        ($ :h3 {:class "text-lg font-semibold"}
          (member-name member))
        ($ :p {:class "text-sm text-base-content/60"}
          (member-email member)))
      ($ :div {:class "space-y-2"}
        ($ :label {:class "ds-label"
                   :for (str "tenant-member-role-select-" mid)}
          ($ :span {:class "ds-label-text"} "Role"))
        ($ :select {:class "ds-select ds-select-bordered w-full"
                    :id (str "tenant-member-role-select-" mid)
                    :value selected-role
                    :on-change #(set-selected-role! (.. % -target -value))}
          ($ :option {:value "viewer"} "Viewer")
          ($ :option {:value "member"} "Member")
          ($ :option {:value "admin"} "Admin")))
      ($ :div {:class "flex justify-end gap-2 pt-2"}
        ($ button {:btn-type :ghost
                   :type "button"
                   :id (str "btn-cancel-tenant-member-edit-" mid)
                   :on-click on-cancel}
          "Cancel")
        ($ button {:btn-type :primary
                   :type "submit"
                   :id (str "btn-save-tenant-member-edit-" mid)
                   :disabled (= selected-role current-role)}
          "Save changes")))))

(defn- render-member-edit-form
  [member {:keys [on-success on-cancel]}]
  ($ member-edit-form
    {:member member
     :on-success on-success
     :on-cancel on-cancel}))

(defn- render-member-row-actions
  [current-role is-owner? member]
  (let [mid (member-id member)
        mid-str (some-> mid str)
        {:keys [show-edit? show-delete? edit-disabled? delete-disabled? can-transfer?]}
        (member-row-action-state current-role is-owner? member)
        on-edit-click (:on-edit-click member)
        item-data (dissoc member
                    :show-edit?
                    :show-delete?
                    :edit-disabled?
                    :delete-disabled?
                    :on-edit-click)]
    ($ :div {:class "flex items-center justify-end gap-2 flex-wrap"}
      (when show-edit?
        ($ button
          {:id (str "btn-edit-tenant-members-" mid-str)
           :btn-type :primary
           :shape "circle"
           :disabled edit-disabled?
           :on-click (fn [e]
                       (.stopPropagation e)
                       (when (and (not edit-disabled?) on-edit-click)
                         (on-edit-click item-data)))}
          ($ edit-icon)))
      (when show-delete?
        ($ button
          {:id (str "btn-delete-tenant-members-" mid-str)
           :btn-type :danger
           :shape "circle"
           :disabled delete-disabled?
           :on-click (fn [e]
                       (.stopPropagation e)
                       (when-not delete-disabled?
                         (confirm-dialog/show-confirm
                           {:title "Remove member"
                            :message (str "Remove " (member-name member) " from this tenant?")
                            :confirm-text "Remove"
                            :on-confirm #(rf/dispatch [::tenant/remove-member {:member-id mid}])})))}
          ($ delete-icon)))
      ($ member-transfer-action
        {:member member
         :can-transfer? can-transfer?}))))

(defn tenant-member-list-props
  [members current-role is-owner?]
  {:entity-name :tenant-members
   :entity-spec tenant-members-entity-spec
   :title "Current Members"
   :rows-override (mapv tenant-member-row (or members []))
   :per-page 25
   :form-display :modal
   :allow-add? false
   :allow-edit? true
   :allow-delete? true
   :display-settings {:show-add-button? false
                      :show-filtering? false
                      :show-select? false
                      :show-batch-edit? false
                      :show-batch-delete? false}
   :render-edit-form render-member-edit-form
   :render-actions #(render-member-row-actions current-role is-owner? %)})

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
        members-loading? (use-subscribe [:tenant/members-loading?])
        invitations (use-subscribe [:tenant/invitations])
        error (use-subscribe [:tenant/error])
        success (use-subscribe [:tenant/success-message])
        current-role (use-subscribe [:user-role])
        tenant (use-subscribe [:current-tenant])
        is-owner? (= (normalize-role current-role) "owner")]

    ($ :div {:class "p-6 max-w-4xl mx-auto"}
      ($ :div {:class "mb-6"}
        ($ :h1 {:class "text-2xl font-bold"} "Members")
        (when tenant
          ($ :p {:class "text-base-content/60 mt-1"}
            (str "Manage members of " (or (:name tenant) (:tenants/name tenant) "your workspace")))))

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

      ($ :div {:class "mb-6"}
        (cond
          members-loading?
          ($ :div {:class "ds-card bg-base-100 shadow-sm border border-base-200"}
            ($ :div {:class "ds-card-body py-12 flex items-center justify-center"}
              ($ :div {:class "ds-loading ds-loading-spinner ds-loading-lg"})))

          (seq members)
          ($ list-view (tenant-member-list-props members current-role is-owner?))

          :else
          ($ :div {:class "ds-card bg-base-100 shadow-sm border border-base-200"}
            ($ :div {:class "ds-card-body"}
              ($ :h2 {:class "ds-card-title text-lg mb-4"} "Current Members")
              ($ :p {:class "text-base-content/60"} "No members yet.")))))

      ($ :div {:class "ds-card bg-base-100 shadow-sm border border-base-200"}
        ($ :div {:class "ds-card-body"}
          ($ :h2 {:class "ds-card-title text-lg mb-4"} "Invitations")

          ($ invite-form)

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
