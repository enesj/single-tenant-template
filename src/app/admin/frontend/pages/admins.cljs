(ns app.admin.frontend.pages.admins
  (:require
    [app.admin.frontend.components.generic-admin-entity-page :refer [generic-admin-entity-page]]
    [app.admin.frontend.events.admin-invitations]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defui invite-admin-form []
  (let [[email set-email!] (use-state "")
        [role set-role!] (use-state "admin")
        [show-form? set-show-form!] (use-state false)
        loading? (use-subscribe [:admin/invite-loading?])
        error (use-subscribe [:admin/invite-error])
        handle-submit
        (fn [e]
          (.preventDefault e)
          (when (seq email)
            (rf/dispatch [:admin/invite-admin {:email email :role role}])))]
    ($ :div {:class "space-y-3"}
      (if-not show-form?
        ($ :button
          {:id "btn-invite-admin-toggle"
           :class "ds-btn ds-btn-primary ds-btn-sm"
           :type "button"
           :on-click #(set-show-form! true)}
          ($ :svg {:class "w-4 h-4 mr-1" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
            ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                      :d "M12 6v6m0 0v6m0-6h6m-6 0H6"}))
          "Invite Admin")
        ($ :form {:class "flex flex-wrap items-end gap-3"
                  :id "admin-invitation-form"
                  :on-submit handle-submit}
          ($ :div {:class "flex-1 min-w-[16rem]"}
            ($ :label {:class "ds-label" :for "admin-invitation-email"}
              ($ :span {:class "ds-label-text text-xs"} "Email"))
            ($ :input {:id "admin-invitation-email"
                       :class "ds-input ds-input-bordered ds-input-sm w-full"
                       :type "email"
                       :placeholder "admin@example.com"
                       :value email
                       :required true
                       :on-change #(set-email! (.. % -target -value))}))
          ($ :div {:class "min-w-[10rem]"}
            ($ :label {:class "ds-label" :for "admin-invitation-role"}
              ($ :span {:class "ds-label-text text-xs"} "Role"))
            ($ :select {:id "admin-invitation-role"
                        :class "ds-select ds-select-bordered ds-select-sm w-full"
                        :value role
                        :on-change #(set-role! (.. % -target -value))}
              ($ :option {:value "admin"} "Admin")
              ($ :option {:value "support"} "Support")))
          ($ :button {:id "btn-send-admin-invitation"
                      :class "ds-btn ds-btn-primary ds-btn-sm"
                      :type "submit"
                      :disabled loading?}
            (if loading? "Sending..." "Send Invite"))
          ($ :button {:id "btn-cancel-admin-invitation"
                      :class "ds-btn ds-btn-ghost ds-btn-sm"
                      :type "button"
                      :on-click #(do
                                   (set-show-form! false)
                                   (set-email! "")
                                   (set-role! "admin"))}
            "Cancel")))
      (when error
        ($ :div {:id "admin-invitation-error"
                 :class "ds-alert ds-alert-error ds-alert-sm"}
          ($ :span error))))))

(defui pending-invitations-table []
  (let [invitations (use-subscribe [:admin/invitations])
        loading? (use-subscribe [:admin/invitations-loading?])
        invite-loading? (use-subscribe [:admin/invite-loading?])]
    (use-effect
      (fn []
        (rf/dispatch [:admin/load-invitations])
        js/undefined)
      [])
    ($ :div {:class "mb-6"}
      ($ :h3 {:class "text-lg font-semibold mb-3"} "Pending Invitations")
      (if loading?
        ($ :div {:class "flex justify-center p-4"}
          ($ :div {:class "ds-loading ds-loading-spinner ds-loading-md"}))
        (if (empty? invitations)
          ($ :p {:class "text-sm text-base-content/50 italic"
                 :id "admin-no-pending-invitations"}
            "No pending invitations")
          ($ :div {:class "overflow-x-auto"}
            ($ :table {:class "ds-table ds-table-sm"
                       :id "admin-pending-invitations-table"}
              ($ :thead
                ($ :tr
                  ($ :th "Email")
                  ($ :th "Role")
                  ($ :th "Invited By")
                  ($ :th "Expires")
                  ($ :th "Actions")))
              ($ :tbody
                (for [inv invitations]
                  (let [inv-id (:id inv)
                        expires-at (or (:expires-at inv) (:expires_at inv))]
                    ($ :tr {:key inv-id}
                      ($ :td (:email inv))
                      ($ :td
                        ($ :span {:class "ds-badge ds-badge-sm ds-badge-outline"}
                          (or (:role inv) "")))
                      ($ :td (or (:inviter-name inv) (:inviter_name inv) ""))
                      ($ :td
                        (when expires-at
                          ($ :span {:class "text-xs text-base-content/60"}
                            (.toLocaleDateString (js/Date. expires-at)))))
                      ($ :td {:class "flex gap-1"}
                        ($ :button
                          {:id (str "btn-resend-admin-invitation-" inv-id)
                           :class "ds-btn ds-btn-ghost ds-btn-xs"
                           :disabled invite-loading?
                           :title "Resend invitation"
                           :on-click #(rf/dispatch [:admin/resend-admin-invitation {:id inv-id}])}
                          "Resend")
                        ($ :button
                          {:id (str "btn-revoke-admin-invitation-" inv-id)
                           :class "ds-btn ds-btn-ghost ds-btn-xs text-error"
                           :disabled invite-loading?
                           :title "Revoke invitation"
                           :on-click #(rf/dispatch [:admin/revoke-admin-invitation {:id inv-id}])}
                          "Revoke")))))))))))))

(defui admin-invitations-section []
  ($ :div {:class "mb-6 rounded-lg border border-base-300 bg-base-100 p-4 shadow-sm"}
    ($ :div {:class "mb-4 flex flex-wrap items-start justify-between gap-3"}
      ($ :div
        ($ :h2 {:class "text-xl font-bold"} "Admin Invitations")
        ($ :p {:class "text-sm text-base-content/60"}
          "Invite additional admins and manage pending invitations."))
      ($ invite-admin-form))
    ($ pending-invitations-table)))

(defui admin-admins-page
  "Admin management page with invitation support for owners"
  []
  (let [admin-role (use-subscribe [:admin/current-user-role])
        is-owner? (= :owner admin-role)]
    ($ generic-admin-entity-page
      {:children :admins
       :components (cond-> {}
                     is-owner? (assoc :custom-body-top admin-invitations-section))})))
