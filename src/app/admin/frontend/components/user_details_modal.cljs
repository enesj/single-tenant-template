(ns app.admin.frontend.components.user-details-modal
  "Rich user details modal built on the shared template modal component."
  (:require
    [app.admin.frontend.components.shared-utils :as utils]
    [app.shared.date :as date]
    [app.template.frontend.components.modal :refer [modal]]
    [app.template.frontend.components.cards :refer [quick-actions-card]]
    [re-frame.core :refer [dispatch]]
    [clojure.string :as str]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defui user-identity-block
  [{:keys [user]}]
  (let [{:keys [full-name email role status last-login auth-provider email-verified email-verification-status]} user
        present? (fn [value]
                   (cond
                     (nil? value) false
                     (and (string? value) (str/blank? value)) false
                     :else true))
        display-name (or (when (present? full-name) full-name)
                       (when (present? email) email)
                       "Unknown user")
        initials (utils/user-initials full-name email)
        status-badge (when (present? status)
                       (utils/status-badge status {:capitalize? true}))
        role-badge (when (present? role)
                     (utils/role-badge role))
        verification-badge (utils/verification-badge email-verified email-verification-status)
        summary-text (->> [(when (present? display-name) display-name)
                           (when (present? role) (str " · " (-> role str (str/replace "-" " ") str/capitalize)))
                           (when (present? status) (str " · Status: " (-> status str (str/replace "-" " ") str/capitalize)))]
                       (remove nil?)
                       (str/join ""))
        detail-note (when last-login
                      (str "Last activity " (date/format-display-date last-login)))
        chip-items (->> [(when (present? email)
                           {:label "Email"
                            :value email
                            :class "font-mono"})
                         (when detail-note
                           {:label nil
                            :value detail-note})
                         (when (present? auth-provider)
                           {:label "Auth"
                            :value (some-> auth-provider str str/capitalize)})]
                     (keep identity))]
    ($ :div {:class "rounded-xl border border-base-200 bg-base-100/80 p-5 mb-6 space-y-3"}
      ($ :div {:class "flex items-start justify-between gap-4"}
        ($ :div {:class "flex items-start gap-3"}
          ($ :div {:class "flex h-12 w-12 items-center justify-center rounded-2xl bg-primary/10 text-lg font-semibold text-primary"}
            initials)
          ($ :div {:class "space-y-2"}
            ($ :div {:class "flex items-center gap-2"}
              ($ :div {:class "w-1 h-4 rounded-full bg-primary"})
              ($ :h3 {:class "text-base font-semibold text-base-content"} "User Summary"))
            ($ :p {:class "text-sm text-base-content/80 leading-relaxed"}
              (if (seq summary-text)
                summary-text
                "User profile details will appear here."))))
        ($ :div {:class "flex flex-wrap items-center justify-end gap-2"}
          (keep identity [status-badge role-badge verification-badge])))
      (when (seq chip-items)
        ($ :div {:class "flex flex-wrap gap-2 text-xs text-base-content/70"}
          (map-indexed
            (fn [idx {:keys [label value class]}]
              ($ :span {:key idx
                        :class (str "rounded-full bg-base-200/70 px-3 py-1 " (or class ""))}
                (if label
                  (str label ": " value)
                  value)))
            chip-items))))))

(defui user-actions-card
  [{:keys [user]}]
  (let [{:keys [id _email email-verified]} user]
    ($ quick-actions-card
      {:title "Actions"
       :bg-gradient "from-warning/10 to-error/10"
       :actions [{:label "Impersonate"
                  :icon-path "M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"
                  :button-class "ds-btn-info"
                  :on-click #(dispatch [:admin/impersonate-user id])}
                 {:label "Reset Password"
                  :icon-path "M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z"
                  :button-class "ds-btn-warning"
                  :on-click #(dispatch [:admin/reset-user-password id])}
                 {:label (if email-verified "Verified" "Verify Email")
                  :icon-path "M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z"
                  :button-class "ds-btn-success"
                  :disabled email-verified
                  :on-click #(dispatch [:admin/force-verify-email id])}
                 {:label "View Activity"
                  :icon-path "M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z"
                  :button-class "ds-btn-outline"
                  :on-click #(dispatch [:admin/view-user-activity id])}]})))

(defui user-details-body
  [{:keys [user error]}]
  (let [{:keys [email full-name id role status created-at updated-at
                last-login _auth-provider email-verified email-verification-status
                tenant-name tenant-slug tenant-subscription-tier]} user]
    ($ :div {:class "space-y-4"}
      (when error
        ($ :div {:class "ds-alert ds-alert-error"}
          ($ :span error)))
      ($ user-identity-block {:user user})
      ($ :div {:class "grid lg:grid-cols-2 gap-4"}
        ($ utils/detail-card
          {:title "Account"
           :fields [{:label "Name" :value full-name}
                    {:label "Email" :value ($ :span {:class "ds-badge ds-badge-outline"} (or email "—"))}
                    {:label "User ID" :value id}
                    {:label "Role" :value (utils/role-badge role)}
                    {:label "Status" :value (utils/status-badge status)}
                    {:label "Last login" :value (date/format-display-date last-login)}]})
        ($ utils/detail-card
          {:title "Verification"
           :fields [{:label "Email Status" :value (utils/verification-badge email-verified email-verification-status)}
                    {:label "Account Created" :value (date/format-display-date created-at)}
                    {:label "Last Updated" :value (date/format-display-date updated-at)}]}))
      ($ utils/detail-card
        {:title "Tenant"
         :fields [{:label "Organization" :value (utils/tenant-label tenant-name tenant-slug)}
                  {:label "Subscription"
                   :value (utils/format-value tenant-subscription-tier)}]})
      ($ user-actions-card {:user user}))))

(defui user-details-modal
  "Orchestrates the modal visibility, loading state, and body rendering."
  []
  (let [open? (use-subscribe [:admin/user-details-modal-open?])
        user (use-subscribe [:admin/current-user-details])
        loading? (use-subscribe [:admin/loading-user-details?])
        error (use-subscribe [:admin/user-details-error])]
    (when (or open? loading?)
      (let [close! #(dispatch [:admin/hide-user-details])
            present? (fn [value]
                       (cond
                         (nil? value) false
                         (and (string? value) (str/blank? value)) false
                         :else true))
            {:keys [email full-name]} (or user {})
            initials (utils/user-initials full-name email)
            header-subtitle (cond
                              (present? email) email
                              loading? "Loading user information…"
                              :else "User profile overview")
            header ($ utils/detail-modal-header
                     {:title "User Details"
                      :subtitle header-subtitle
                      :icon ($ :span {:class "text-lg font-semibold text-primary"}
                              initials)
                      :icon-bg "bg-primary/10 text-primary"})]

        ($ modal {:id "admin-user-details-modal"
                  :on-close close!
                  :draggable? true
                  :width "700px"
                  :class "max-w-[90vw] h-[80vh] flex flex-col"
                  :header header
                  :header-class "p-0 border-0 bg-transparent mb-4"}
          ($ :div {:class "flex-1 overflow-y-auto p-4"}
            (cond
              loading?
              ($ :div {:class "flex items-center justify-center py-16"}
                ($ :span {:class "ds-loading ds-loading-spinner ds-loading-lg text-primary"}))

              user
              ($ user-details-body {:user user :error error})

              error
              ($ :div {:class "ds-alert ds-alert-error m-4"}
                ($ :span error))

              :else
              ($ :div {:class "m-8 text-center text-base-content/60"}
                "No user details available."))))))))
