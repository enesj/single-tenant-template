(ns app.template.frontend.pages.impersonation-grants
  "Impersonation grant management page for tenant owners.
   Allows listing, creating, and revoking impersonation grants
   that let platform admins access the tenant."
  (:require
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.events.impersonation :as impersonation]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-state]]
    [uix.re-frame :refer [use-subscribe]]))

;; ============================================================================
;; Helpers
;; ============================================================================

(defn- grant-id [g]
  (or (:id g) (:impersonation_grants/id g)))

(defn- grant-admin-email [g]
  (or (:admin-email g) (:admin_email g) (:impersonation_grants/admin_email g) ""))

(defn- grant-role [g]
  (or (:role g) (:impersonation_grants/role g) "viewer"))

(defn- grant-status [g]
  (or (:status g) (:impersonation_grants/status g) "active"))

(defn- grant-created [g]
  (let [d (or (:created-at g) (:created_at g) (:impersonation_grants/created_at g))]
    (when d
      (subs (str d) 0 (min 10 (count (str d)))))))

(defn- role-badge-class [role]
  (case role
    "owner" "ds-badge-primary"
    "admin" "ds-badge-secondary"
    "member" "ds-badge-accent"
    "viewer" "ds-badge-ghost"
    "ds-badge-ghost"))

(defn- status-badge-class [status]
  (case status
    "active" "ds-badge-success"
    "revoked" "ds-badge-error"
    "expired" "ds-badge-warning"
    "ds-badge-ghost"))

;; ============================================================================
;; Grant Row Component
;; ============================================================================

(defui grant-row [{:keys [grant]}]
  (let [gid (grant-id grant)
        status (grant-status grant)
        is-active? (= status "active")
        [confirming? set-confirming!] (use-state false)]
    ($ :tr {:id (str "grant-row-" gid)}
      ($ :td {:class "font-medium"} (grant-admin-email grant))
      ($ :td
        ($ :span {:class (str "ds-badge ds-badge-sm " (role-badge-class (grant-role grant)))}
          (grant-role grant)))
      ($ :td
        ($ :span {:class (str "ds-badge ds-badge-sm " (status-badge-class status))}
          status))
      ($ :td {:class "text-sm text-base-content/60"} (grant-created grant))
      ($ :td
        (when is-active?
          (if confirming?
            ($ :div {:class "flex gap-1"}
              ($ button {:btn-type :error :class "ds-btn-xs"
                         :on-click (fn []
                                     (rf/dispatch [::impersonation/revoke-grant {:id gid}])
                                     (set-confirming! false))}
                "Confirm")
              ($ button {:btn-type :ghost :class "ds-btn-xs"
                         :on-click #(set-confirming! false)}
                "Cancel"))
            ($ button {:btn-type :ghost :class "ds-btn-xs text-error"
                       :id (str "revoke-grant-btn-" gid)
                       :on-click #(set-confirming! true)}
              "Revoke")))))))

;; ============================================================================
;; Create Grant Form
;; ============================================================================

(defui create-grant-form []
  (let [[email set-email!] (use-state "")
        [role set-role!] (use-state "viewer")
        loading? (use-subscribe [:impersonation/loading?])]
    ($ :form {:class "flex gap-3 items-end flex-wrap"
              :id "create-grant-form"
              :on-submit (fn [e]
                           (.preventDefault e)
                           (when (seq email)
                             (rf/dispatch [::impersonation/create-grant
                                           {:admin-email email :role role}])
                             (set-email! "")))}
      ($ :div {:class "flex-1 min-w-[200px]"}
        ($ :label {:class "ds-label"}
          ($ :span {:class "ds-label-text"} "Admin Email"))
        ($ :input {:class "ds-input ds-input-bordered w-full"
                   :id "grant-admin-email-input"
                   :type "email"
                   :placeholder "admin@example.com"
                   :value email
                   :required true
                   :on-change #(set-email! (.. % -target -value))}))
      ($ :div
        ($ :label {:class "ds-label"}
          ($ :span {:class "ds-label-text"} "Role"))
        ($ :select {:class "ds-select ds-select-bordered"
                    :id "grant-role-select"
                    :value role
                    :on-change #(set-role! (.. % -target -value))}
          ($ :option {:value "viewer"} "Viewer")
          ($ :option {:value "member"} "Member")
          ($ :option {:value "admin"} "Admin")))
      ($ button {:btn-type :primary
                 :type "submit"
                 :class "ds-btn-sm"
                 :id "grant-submit-btn"
                 :loading loading?}
        "Grant Access"))))

;; ============================================================================
;; Main Page Component
;; ============================================================================

(defui impersonation-grants-page []
  (let [grants (use-subscribe [:impersonation/grants])
        loading? (use-subscribe [:impersonation/loading?])
        error (use-subscribe [:impersonation/error])
        success (use-subscribe [:impersonation/success-message])
        role (use-subscribe [:user-role])
        tenant (use-subscribe [:current-tenant])
        is-owner? (= role "owner")]

    ($ :div {:class "p-6 max-w-4xl mx-auto"}
      ;; Page header
      ($ :div {:class "mb-6"}
        ($ :h1 {:class "text-2xl font-bold"} "Impersonation Grants")
        (when tenant
          ($ :p {:class "text-base-content/60 mt-1"}
            (str "Manage admin access to "
              (or (:name tenant) (:tenants/name tenant) "your workspace")))))

      ;; Access denied for non-owners
      (if-not is-owner?
        ($ :div {:class "ds-alert ds-alert-warning"
                 :id "impersonation-access-denied"}
          ($ :span "Only tenant owners can manage impersonation grants."))

        ($ :<>
          ;; Alerts
          (when error
            ($ :div {:class "ds-alert ds-alert-error mb-4"
                     :id "impersonation-error-alert"}
              ($ :span error)
              ($ :button {:class "ds-btn ds-btn-ghost ds-btn-xs"
                          :on-click #(rf/dispatch [::impersonation/clear-messages])}
                "Dismiss")))

          (when success
            ($ :div {:class "ds-alert ds-alert-success mb-4"
                     :id "impersonation-success-alert"}
              ($ :span success)
              ($ :button {:class "ds-btn ds-btn-ghost ds-btn-xs"
                          :on-click #(rf/dispatch [::impersonation/clear-messages])}
                "Dismiss")))

          ;; Explanation card
          ($ :div {:class "ds-card bg-base-200/50 border border-base-200 mb-6"}
            ($ :div {:class "ds-card-body py-3 px-4"}
              ($ :p {:class "text-sm text-base-content/70"}
                "Impersonation grants allow platform administrators to access your workspace "
                "with a specific role for support and troubleshooting purposes. "
                "You can revoke access at any time.")))

          ;; Create grant form
          ($ :div {:class "ds-card bg-base-100 shadow-sm border border-base-200 mb-6"}
            ($ :div {:class "ds-card-body"}
              ($ :h2 {:class "ds-card-title text-lg mb-4"} "Grant Admin Access")
              ($ create-grant-form)))

          ;; Loading state
          (when loading?
            ($ :div {:class "flex justify-center py-4"}
              ($ :div {:class "ds-loading ds-loading-spinner ds-loading-md"})))

          ;; Grants table
          ($ :div {:class "ds-card bg-base-100 shadow-sm border border-base-200"}
            ($ :div {:class "ds-card-body"}
              ($ :h2 {:class "ds-card-title text-lg mb-4"} "Active Grants")
              (if (seq grants)
                ($ :div {:class "overflow-x-auto"}
                  ($ :table {:class "ds-table ds-table-sm"
                             :id "impersonation-grants-table"}
                    ($ :thead
                      ($ :tr
                        ($ :th "Admin Email")
                        ($ :th "Role")
                        ($ :th "Status")
                        ($ :th "Created")
                        ($ :th "Actions")))
                    ($ :tbody
                      (for [g grants]
                        ($ grant-row {:key (grant-id g) :grant g})))))
                ($ :p {:class "text-base-content/60"}
                  "No impersonation grants yet.")))))))))

(comment
  ;; (require 'app.template.frontend.pages.impersonation-grants :reload)
  :rcf)
