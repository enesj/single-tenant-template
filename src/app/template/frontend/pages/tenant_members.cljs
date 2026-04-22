(ns app.template.frontend.pages.tenant-members
  "Tenant member management page for admin/owner roles.
   Shows current members with role management and pending invitations."
  (:require
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.confirm-dialog :as confirm-dialog]
    [app.template.frontend.components.icons :refer [delete-icon edit-icon]]
    [app.template.frontend.components.list :refer [list-view]]
    [app.template.frontend.events.tenant :as tenant]
    [app.template.frontend.i18n :refer [use-t]]
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

(defn- tenant-members-entity-spec
  "Entity spec for the list-view component. Accepts t for translated labels."
  [t]
  {:fields [{:id "member_name" :label (t :common/name) :type :text}
            {:id "member_email" :label (t :common/email) :type :text :input-type "email"}
            {:id "member_role"
             :label (t :common/role)
             :type :text
             :formatter "role-badge"}
            {:id "membership_status"
             :label (t :common/membership)
             :type :text
             :formatter "status-badge"}
            {:id "account_status"
             :label (t :common/account)
             :type :text
             :formatter "status-badge"}
            {:id "joined_on" :label (t :common/joined) :type :text}]})

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
    :membership_status (normalize-role
                         (or (:status member)
                           (:tenant_memberships/status member)
                           (:membership_status member)))
    :account_status (normalize-role
                      (or (:user_status member)
                        (:user-status member)
                        (:users/status member)
                        (:account_status member)))
    :joined_on (member-joined member)))

;; ============================================================================
;; Member Row Component
;; ============================================================================

(defn member-row-action-state
  "Computes action availability and translated reason strings for a member row.
   Accepts t (translate fn) for i18n of tooltip / reason strings."
  [current-role is-owner? member t]
  (let [{:keys [role can-change-role? can-remove? can-transfer?] :as capabilities}
        (member-management-capabilities current-role is-owner? member)
        membership-status (normalize-role
                            (or (:status member)
                              (:tenant_memberships/status member)
                              (:membership_status member)))
        account-status (normalize-role
                         (or (:user_status member)
                           (:user-status member)
                           (:users/status member)
                           (:account_status member)))
        membership-active? (or (nil? membership-status)
                             (= membership-status "active"))
        account-active? (or (nil? account-status)
                          (= account-status "active"))
        can-enable? (and can-remove?
                      (= membership-status "suspended"))
        can-disable? (and can-remove? membership-active?)
        can-transfer? (and can-transfer? membership-active? account-active?)
        policy-show-edit? (not (false? (:show-edit? member)))
        policy-show-delete? (not (false? (:show-delete? member)))
        show-edit? (and policy-show-edit? membership-active?)
        show-delete? (and policy-show-delete? membership-active?)
        show-enable? (and policy-show-delete? can-enable?)
        edit-disabled? (or (true? (:edit-disabled? member))
                         (not can-change-role?)
                         (not membership-active?))
        delete-disabled? (or (true? (:delete-disabled? member))
                           (not can-disable?))
        edit-disabled-reason (when (and show-edit? edit-disabled?)
                               (cond
                                 (true? (:edit-disabled? member))
                                 (t :tenant/reason-editing-disabled)

                                 (= role "owner")
                                 (t :tenant/reason-owner-role)

                                 (not membership-active?)
                                 (t :tenant/reason-membership-disabled)

                                 :else
                                 (t :tenant/reason-no-edit-permission)))
        delete-disabled-reason (when (and show-delete? delete-disabled?)
                                 (cond
                                   (true? (:delete-disabled? member))
                                   (t :tenant/reason-deletion-disabled)

                                   (= role "owner")
                                   (t :tenant/reason-owner-remove)

                                   (not membership-active?)
                                   (t :tenant/reason-membership-disabled)

                                   :else
                                   (t :tenant/reason-no-remove-permission)))]
    (assoc capabilities
      :membership-status membership-status
      :account-status account-status
      :membership-active? membership-active?
      :account-active? account-active?
      :can-enable? can-enable?
      :can-disable? can-disable?
      :show-enable? show-enable?
      :show-edit? show-edit?
      :show-delete? show-delete?
      :edit-disabled? edit-disabled?
      :delete-disabled? delete-disabled?
      :edit-disabled-reason edit-disabled-reason
      :delete-disabled-reason delete-disabled-reason
      :can-transfer? can-transfer?)))

(defui transfer-icon []
  ($ :svg {:xmlns "http://www.w3.org/2000/svg"
           :class "w-4 h-4"
           :fill "none"
           :viewBox "0 0 24 24"
           :stroke "currentColor"
           :stroke-width "2"}
    ($ :path {:stroke-linecap "round"
              :stroke-linejoin "round"
              :d "M8 7h12m0 0l-4-4m4 4l-4 4M16 17H4m0 0l4 4m-4-4l4-4"})))

(defui member-transfer-action
  [{:keys [member can-transfer?]}]
  (let [t (use-t)
        mid (member-id member)
        [confirming? set-confirming!] (use-state false)]
    (when can-transfer?
      (if confirming?
        ($ :div {:class "flex gap-1 whitespace-nowrap"}
          ($ button {:btn-type :warning
                     :class "ds-btn-sm whitespace-nowrap"
                     :title (t :tenant/transfer)
                     :on-click (fn []
                                 (rf/dispatch [::tenant/transfer-ownership
                                               {:user-id (member-user-id member)}])
                                 (set-confirming! false))}
            (t :common/confirm))
          ($ button {:btn-type :ghost
                     :class "ds-btn-sm whitespace-nowrap"
                     :on-click #(set-confirming! false)}
            (t :common/cancel)))
        ($ button {:btn-type :warning
                   :shape "circle"
                   :id (str "transfer-btn-" mid)
                   :title (t :tenant/transfer)
                   :on-click #(set-confirming! true)}
          ($ transfer-icon))))))

(defui member-edit-form
  [{:keys [member on-success on-cancel]}]
  (let [t (use-t)
        mid (member-id member)
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
          ($ :span {:class "ds-label-text"} (t :common/role)))
        ($ :select {:class "ds-select ds-select-bordered w-full"
                    :id (str "tenant-member-role-select-" mid)
                    :value selected-role
                    :on-change #(set-selected-role! (.. % -target -value))}
          ($ :option {:value "viewer"} (t :tenant/role-viewer))
          ($ :option {:value "member"} (t :tenant/role-member))
          ($ :option {:value "admin"} (t :tenant/role-admin))))
      ($ :div {:class "flex justify-end gap-2 pt-2"}
        ($ button {:btn-type :ghost
                   :type "button"
                   :id (str "btn-cancel-tenant-member-edit-" mid)
                   :on-click on-cancel}
          (t :common/cancel))
        ($ button {:btn-type :primary
                   :type "submit"
                   :id (str "btn-save-tenant-member-edit-" mid)
                   :disabled (= selected-role current-role)}
          (t :common/save-changes))))))

(defn- render-member-edit-form
  [member {:keys [on-success on-cancel]}]
  ($ member-edit-form
    {:member member
     :on-success on-success
     :on-cancel on-cancel}))

(defn- render-member-row-actions
  "Plain fn — receives t as param since plain fns cannot call hooks."
  [current-role is-owner? t member]
  (let [mid (member-id member)
        mid-str (some-> mid str)
        {:keys [show-edit?
                show-delete?
                show-enable?
                edit-disabled?
                delete-disabled?
                edit-disabled-reason
                delete-disabled-reason
                can-transfer?]}
        (member-row-action-state current-role is-owner? member t)
        on-edit-click (:on-edit-click member)
        item-data (dissoc member
                    :show-edit?
                    :show-delete?
                    :edit-disabled?
                    :delete-disabled?
                    :on-edit-click)]
    ($ :div {:class "flex items-center justify-center gap-2"}
      (when show-edit?
        ($ button
          {:id (str "btn-edit-tenant-members-" mid-str)
           :btn-type :primary
           :shape "circle"
           :disabled edit-disabled?
           :title (or edit-disabled-reason (t :tenant/disable-member-title))
           :on-click (fn [e]
                       (.stopPropagation e)
                       (when (and (not edit-disabled?) on-edit-click)
                         (on-edit-click item-data)))}
          ($ edit-icon)))

      ;; Disable membership
      (when show-delete?
        ($ button
          {:id (str "btn-disable-tenant-members-" mid-str)
           :btn-type :danger
           :shape "circle"
           :disabled delete-disabled?
           :title (or delete-disabled-reason (t :tenant/disable-member-title))
           :on-click (fn [e]
                       (.stopPropagation e)
                       (when-not delete-disabled?
                         (confirm-dialog/show-confirm
                           {:title (t :tenant/disable-member-title)
                            :message (t :tenant/disable-member-msg (member-name member))
                            :confirm-text (t :tenant/disable)
                            :on-confirm #(rf/dispatch [::tenant/remove-member {:member-id mid}])})))}
          ($ delete-icon)))

      ;; Enable membership
      (when show-enable?
        ($ button
          {:id (str "btn-enable-tenant-members-" mid-str)
           :btn-type :success
           :shape "circle"
           :title (t :tenant/enable-member-title)
           :on-click (fn [e]
                       (.stopPropagation e)
                       (confirm-dialog/show-confirm
                         {:title (t :tenant/enable-member-title)
                          :message (t :tenant/enable-member-msg (member-name member))
                          :confirm-text (t :tenant/enable)
                          :on-confirm #(rf/dispatch [::tenant/set-member-status
                                                     {:member-id mid
                                                      :status "active"}])}))}
          "✓"))

      ($ member-transfer-action
        {:member member
         :can-transfer? can-transfer?}))))

(defn- render-invite-add-form
  [{:keys [on-success on-cancel]}]
  ($ :div {:class "space-y-4"}
    ($ invite-form {:on-success on-success})
    ($ :div {:class "flex justify-end"}
      ($ button {:btn-type :ghost
                 :type "button"
                 :on-click on-cancel}
        "Cancel"))))

(defn tenant-member-list-props
  [members current-role is-owner? t]
  {:entity-name :tenant-members
   :entity-spec (tenant-members-entity-spec t)
   :title (t :tenant/current-members)
   :per-page 25
   :form-display :modal
   :allow-add? true
   :add-button-label (t :tenant/send-invite)
   :allow-edit? true
   :allow-delete? true
   :display-settings {:show-filtering? false
                      :show-select? false
                      :show-batch-edit? false
                      :show-batch-delete? false}
   :rows-override (mapv tenant-member-row (or members []))
   :render-add-form render-invite-add-form
   :render-edit-form render-member-edit-form
   :render-actions #(render-member-row-actions current-role is-owner? t %)})

;; ============================================================================
;; Invitation Row Component
;; ============================================================================

(defui invitation-row [{:keys [invitation]}]
  (let [t (use-t)
        inv-id (or (:id invitation) (:tenant_invitations/id invitation))
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
              (t :common/confirm))
            ($ button {:btn-type :ghost :class "ds-btn-xs"
                       :on-click #(set-confirming! false)}
              (t :common/cancel)))
          ($ :div {:class "flex gap-2"}
            ($ button {:btn-type :ghost :class "ds-btn-xs text-info"
                       :id (str "resend-btn-" inv-id)
                       :on-click #(rf/dispatch [::tenant/resend-invitation {:id inv-id}])}
              (t :tenant/resend))
            ($ button {:btn-type :ghost :class "ds-btn-xs text-error"
                       :id (str "revoke-btn-" inv-id)
                       :on-click #(set-confirming! true)}
              (t :tenant/revoke))))))))

;; ============================================================================
;; Invite Form Component
;; ============================================================================

(defui invite-form
  [{:keys [on-success]}]
  (let [t (use-t)
        [email set-email!] (use-state "")
        [role set-role!] (use-state "member")
        loading? (use-subscribe [:tenant/loading?])]
    ($ :form {:class "flex gap-3 items-end flex-wrap"
              :id "invite-form"
              :on-submit (fn [e]
                           (.preventDefault e)
                           (when (seq email)
                             (rf/dispatch [::tenant/invite-member {:email email :role role}])
                             (set-email! "")
                             (when on-success (on-success))))}
      ($ :div {:class "flex-1 min-w-[200px]"}
        ($ :label {:class "ds-label"}
          ($ :span {:class "ds-label-text"} (t :common/email)))
        ($ :input {:class "ds-input ds-input-bordered w-full"
                   :id "invite-email-input"
                   :type "email"
                   :placeholder "user@example.com"
                   :value email
                   :required true
                   :on-change #(set-email! (.. % -target -value))}))
      ($ :div
        ($ :label {:class "ds-label"}
          ($ :span {:class "ds-label-text"} (t :common/role)))
        ($ :select {:class "ds-select ds-select-bordered"
                    :id "invite-role-select"
                    :value role
                    :on-change #(set-role! (.. % -target -value))}
          ($ :option {:value "viewer"} (t :tenant/role-viewer))
          ($ :option {:value "member"} (t :tenant/role-member))
          ($ :option {:value "admin"} (t :tenant/role-admin))))
      ($ button {:btn-type :primary
                 :type "submit"
                 :class "ds-btn-sm"
                 :id "invite-submit-btn"
                 :loading loading?}
        (t :tenant/send-invite)))))

;; ============================================================================
;; Main Page Component
;; ============================================================================

(defui tenant-members-page []
  (let [t (use-t)
        members (use-subscribe [:tenant/members])
        members-loading? (use-subscribe [:tenant/members-loading?])
        invitations (use-subscribe [:tenant/invitations])
        error (use-subscribe [:tenant/error])
        success (use-subscribe [:tenant/success-message])
        current-role (use-subscribe [:user-role])
        tenant (use-subscribe [:current-tenant])
        is-owner? (= (normalize-role current-role) "owner")]

    ($ :div {:class "w-full px-4 py-6"}
      ($ :div {:class "mb-6"}
        ($ :h1 {:class "text-2xl font-bold"} (t :tenant/members-title))
        (when tenant
          ($ :p {:class "text-base-content/60 mt-1"}
            (t :tenant/members-subtitle
              (or (:name tenant) (:tenants/name tenant) "your workspace")))))

      (when error
        ($ :div {:class "ds-alert ds-alert-error mb-4"
                 :id "tenant-error-alert"}
          ($ :span error)
          ($ :button {:class "ds-btn ds-btn-ghost ds-btn-xs"
                      :on-click #(rf/dispatch [::tenant/clear-messages])}
            (t :common/dismiss))))

      (when success
        ($ :div {:class "ds-alert ds-alert-success mb-4"
                 :id "tenant-success-alert"}
          ($ :span success)
          ($ :button {:class "ds-btn ds-btn-ghost ds-btn-xs"
                      :on-click #(rf/dispatch [::tenant/clear-messages])}
            (t :common/dismiss))))

      ($ :div {:class "mb-6"}
        (cond
          members-loading?
          ($ :div {:class "ds-card bg-base-100 shadow-sm border border-base-200"}
            ($ :div {:class "ds-card-body py-12 flex items-center justify-center"}
              ($ :div {:class "ds-loading ds-loading-spinner ds-loading-lg"})))

          (seq members)
          ($ list-view (tenant-member-list-props members current-role is-owner? t))

          :else
          ($ :div {:class "ds-card bg-base-100 shadow-sm border border-base-200"}
            ($ :div {:class "ds-card-body"}
              ($ :h2 {:class "ds-card-title text-lg mb-4"} (t :tenant/current-members))
              ($ :p {:class "text-base-content/60"} (t :tenant/no-members))))))

      ($ :div {:class "ds-card bg-base-100 shadow-sm border border-base-200"}
        ($ :div {:class "ds-card-body"}
          ($ :h2 {:class "ds-card-title text-lg mb-4"} (t :tenant/invitations))

          ($ invite-form)

          (when (seq invitations)
            ($ :div {:class "overflow-x-auto mt-4"}
              ($ :table {:class "ds-table ds-table-sm"
                         :id "invitations-table"}
                ($ :thead
                  ($ :tr
                    ($ :th (t :common/email))
                    ($ :th (t :common/role))
                    ($ :th (t :common/status))
                    ($ :th (t :tenant/sent))
                    ($ :th (t :common/actions))))
                ($ :tbody
                  (for [inv invitations]
                    ($ invitation-row {:key (or (:id inv) (:tenant_invitations/id inv))
                                       :invitation inv})))))))))))

(comment
  ;; (require 'app.template.frontend.pages.tenant-members :reload)
  :rcf)
