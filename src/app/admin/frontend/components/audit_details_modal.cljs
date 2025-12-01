(ns app.admin.frontend.components.audit-details-modal
  "Audit log details modal component"
  (:require
    [app.admin.frontend.components.shared-utils :as utils]
    [app.shared.date :as date]
    [app.template.frontend.components.modal :refer [modal]]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.copy-button :refer [copy-to-clipboard-button]]
    [app.template.frontend.components.json-viewer :refer [json-viewer json-copy-text]]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch]]
    [taoensso.timbre :as log]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defui audit-identity-block
  [{:keys [audit-log]}]
  (let [{:keys [action entity-type created-at timestamp admin-email admin-name ip-address]} audit-log
        action-label (when action (utils/format-value action))
        entity-label (when entity-type (utils/format-value entity-type))
        timestamp-primary (when (or created-at timestamp)
                            (let [ts (or created-at timestamp)]
                              (if (string? ts)
                                (if-let [parsed-date (date/parse-date-string ts)]
                                  (date/format-display-date parsed-date)
                                  ts)
                                (date/format-display-date ts))))
        admin-summary (cond
                        (and admin-name admin-email) (str admin-name " (" admin-email ")")
                        admin-email admin-email
                        admin-name admin-name
                        :else nil)
        summary-text (->> [(when admin-summary
                             (str admin-summary
                               (cond
                                 (and action-label entity-label) (str " performed " (str/lower-case action-label) " on " (str/lower-case entity-label))
                                 action-label (str " performed " (str/lower-case action-label))
                                 entity-label (str " interacted with " (str/lower-case entity-label))
                                 :else " performed an action")))
                           (when (and (nil? admin-summary) action-label entity-label)
                             (str (str/capitalize (str/lower-case action-label)) " on " (str/lower-case entity-label)))
                           (when (and (nil? admin-summary) action-label (nil? entity-label))
                             (str (str/capitalize (str/lower-case action-label)) " event"))]
                       (remove nil?)
                       (str/join ""))
        entity-detail (when (or entity-type ip-address timestamp-primary)
                        (->> [(when timestamp-primary
                                (str "At " timestamp-primary))
                              (when ip-address
                                (str "from " ip-address))
                              (when entity-type
                                (if entity-label
                                  (str "targeting " (str/lower-case entity-label))
                                  (str "targeting " (str/lower-case (str entity-type)))))]
                          (remove nil?)
                          (str/join " ")))]
    ($ :div {:class "rounded-xl border border-base-200 bg-base-100/80 p-5 mb-6 space-y-3"}
      ($ :div {:class "flex items-start justify-between gap-3"}
        ($ :div {:class "space-y-2"}
          ($ :div {:class "flex items-center gap-2"}
            ($ :div {:class "w-1 h-4 rounded-full bg-primary"})
            ($ :h3 {:class "text-base font-semibold text-base-content"} "Event Summary"))
          ($ :p {:class "text-sm text-base-content/80 leading-relaxed"}
            (if (seq summary-text)
              summary-text
              (or action-label "No recent activity"))))
        (when action-label
          ($ :span {:class "ds-badge ds-badge-outline ds-badge-sm text-xs"}
            action-label)))
      (when (or entity-detail admin-summary)
        ($ :div {:class "flex flex-wrap gap-2 text-xs text-base-content/60"}
          (when admin-summary
            ($ :span {:class "rounded-full bg-base-200/80 px-2 py-1"}
              (str "Admin: " admin-summary)))
          (when entity-detail
            ($ :span {:class "rounded-full bg-base-200/60 px-2 py-1"}
              entity-detail))))
      (when (and (not admin-summary) ip-address)
        ($ :div {:class "text-xs text-base-content/50"}
          (str "Origin IP: " ip-address))))))

(defui audit-details-body
  [{:keys [audit-log]}]
  (let [{:keys [id created-at timestamp action entity-type entity-id admin-email admin-name
                ip-address user-agent changes]} audit-log]
    ;; Capture debug info for display
    ($ :div {:class "space-y-6"}
      ($ audit-identity-block {:audit-log audit-log})

      ;; Main Information Cards
      ($ :div {:class "grid lg:grid-cols-2 gap-6"}
        ($ utils/detail-card
          {:title "Basic Information"
           :fields [{:label "Audit ID" :value ($ :span {:class "font-mono text-sm"} id)}
                    {:label "Timestamp" :value (if (or created-at timestamp)
                                                 (let [ts (or created-at timestamp)]
                                                   (if (string? ts)
                                                     ;; If it's a string, try to parse it first
                                                     (if-let [parsed-date (date/parse-date-string ts)]
                                                       (date/format-display-date parsed-date)
                                                       ;; If parsing fails, show the original string
                                                       ts)
                                                     ;; If it's already a date object, format it
                                                     (date/format-display-date ts)))
                                                 "No timestamp")}
                    {:label "Action" :value ($ :span {:class (str "ds-badge ds-badge-outline "
                                                               (case (keyword action)
                                                                 :create "ds-badge-success"
                                                                 :update "ds-badge-warning"
                                                                 :delete "ds-badge-error"
                                                                 :login "ds-badge-info"
                                                                 :logout "ds-badge-ghost"
                                                                 "ds-badge-neutral"))}
                                              (utils/format-value action))}
                    {:label "Entity Type" :value (when entity-type
                                                   ($ :span {:class "ds-badge ds-badge-outline ds-badge-sm"} entity-type))}
                    {:label "Entity ID" :value (when entity-id
                                                 ($ :span {:class "font-mono text-sm"} entity-id))}]})
        ($ utils/detail-card
          {:title "Administrative Information"
           :fields [{:label "Admin Email" :value ($ :div {:class "flex flex-col"}
                                                   ($ :span {:class "font-medium"} admin-email)
                                                   (when admin-name
                                                     ($ :span {:class "text-sm text-base-content/60"} admin-name)))}
                    {:label "IP Address" :value (utils/ip-address-badge ip-address)}
                    {:label "User Agent" :value ($ :div {:class "text-sm text-base-content/70 max-w-xs break-words"}
                                                  (or user-agent "Unknown"))}]}))

      ;; Enhanced Data Changes Section - full width
      (when changes
        (js/console.log "Rendering changes section with data:" changes)
        ($ utils/detail-card
          {:title "Data Changes"
           :fields [{:label "Changes"
                     :value ($ :div {:class "relative bg-base-100 border border-base-300 rounded-lg p-4 my-2"}
                                ;; Copy to clipboard button (uses shared normalization)
                              ($ copy-to-clipboard-button
                                {:text (json-copy-text changes)})

                              ;; Enhanced code display with more space
                              ($ :div {:class "bg-base-200/50 rounded-md p-4 max-h-80 overflow-y-auto border border-base-200 min-h-[100px]"}
                                (let [raw-content (str changes)
                                      has-content? (and raw-content
                                                     (not (str/blank? raw-content))
                                                     (not= raw-content "{}")
                                                     (not= raw-content "[]")
                                                     (not= raw-content "null"))]
                                  (js/console.log "Raw changes content:" raw-content)
                                  (js/console.log "Has content?" has-content?)

                                  (if has-content?
                                      ;; Use template JSON viewer directly
                                    ($ json-viewer {:data changes})

                                      ;; No meaningful content
                                    ($ :div {:class "text-center text-base-content/60 py-8"}
                                      ($ :div {:class "text-lg mb-2"} "📝")
                                      ($ :div "No changes data available")
                                      ($ :div {:class "text-sm mt-1"}
                                        (str "Raw data: '" raw-content "'")))))))}]}))

      ;; Enhanced Actions Section
      ($ :div {:class "ds-card ds-card-bordered bg-gradient-to-br from-warning/10 to-error/10 shadow-lg p-4"}
        ($ :div {:class "flex items-center gap-2 mb-3"}
          ($ :div {:class "w-1 h-4 rounded-full bg-error"})
          ($ :h3 {:class "text-base font-semibold text-base-content"}
            "Actions"))
        ($ :div {:class "flex flex-wrap gap-2"}
          ;; Export this audit log
          ($ button {:id "btn-export-audit-details"
                     :btn-type :outline
                     :class "ds-btn-sm"
                     :on-click #(do
                                  (log/info "Exporting audit log from details modal:" id)
                                  (dispatch [:admin/export-single-audit-log audit-log]))}
            ($ :svg {:class "w-4 h-4" :fill "none" :stroke "currentColor" :view-box "0 0 24 24"}
              ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"}))
            "Export")

          ;; Filter by this admin
          (when admin-email
            ($ button {:id "btn-filter-by-admin-from-details"
                       :btn-type :outline
                       :class "ds-btn-sm"
                       :on-click #(do
                                    (log/info "Filtering by admin from details modal:" admin-email)
                                    (dispatch [:admin/apply-audit-filter :admin-email admin-email])
                                    (dispatch [:admin/hide-audit-details]))}
              ($ :svg {:class "w-4 h-4" :fill "none" :stroke "currentColor" :view-box "0 0 24 24"}
                ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.207A1 1 0 013 6.5V4z"}))
              "Filter by Admin"))

          ;; Filter by action
          (when action
            ($ button {:id "btn-filter-by-action-from-details"
                       :btn-type :outline
                       :class "ds-btn-sm"
                       :on-click #(do
                                    (log/info "Filtering by action from details modal:" action)
                                    (dispatch [:admin/apply-audit-filter :action action])
                                    (dispatch [:admin/hide-audit-details]))}
              ($ :svg {:class "w-4 h-4" :fill "none" :stroke "currentColor" :view-box "0 0 24 24"}
                ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M7 4V2a1 1 0 011-1h4a1 1 0 011 1v2h4a1 1 0 010 2H3a1 1 0 010-2h4z"})
                ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M4 6h12l-1 10H5L4 6z"}))
              "Filter by Action"))

          ;; Delete button (dangerous action)
          ($ button {:id "btn-delete-audit-from-details"
                     :btn-type :error
                     :class "ds-btn-sm"
                     :on-click #(do
                                  (log/info "Delete audit log from details modal:" id)
                                  (when (js/confirm
                                          (str "Are you sure you want to delete this audit log?\n\n"
                                            "This action cannot be undone and may affect compliance."))
                                    (dispatch [:admin/delete-audit-log id])
                                    (dispatch [:admin/hide-audit-details])))}
            ($ :svg {:class "w-4 h-4" :fill "none" :stroke "currentColor" :view-box "0 0 24 24"}
              ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"}))
            "Delete")

          ;; Close button
          ($ button {:id "btn-close-audit-details"
                     :btn-type :primary
                     :class "ds-btn-sm"
                     :on-click #(dispatch [:admin/hide-audit-details])}
            "Close"))))))

(defui audit-details-modal
  "Modal displaying detailed audit log information"
  []
  (let [visible? (use-subscribe [:admin/audit-details-modal-visible?])
        audit-log (use-subscribe [:admin/audit-details-modal-audit-log])]
    (when (or visible? audit-log)
      (let [close! #(dispatch [:admin/hide-audit-details])
            present? (fn [value]
                       (cond
                         (nil? value) false
                         (and (string? value) (str/blank? value)) false
                         :else true))
            {:keys [id action entity-type entity-id admin-email admin-name]} (or audit-log {})
            audit-id (when (present? id) id)
            action-str (when (present? action) (str action))
            action-key (when action-str (keyword action-str))
            action-label (when (present? action) (utils/format-value action))
            action-badge (when action-label
                           ($ :span {:class (str "ds-badge ds-badge-lg "
                                              (case action-key
                                                :create "ds-badge-success"
                                                :update "ds-badge-warning"
                                                :delete "ds-badge-error"
                                                :login "ds-badge-info"
                                                :logout "ds-badge-ghost"
                                                "ds-badge-neutral"))}
                             action-label))
            entity-label (when (present? entity-type) (str entity-type))
            entity-summary (when (or entity-label (present? entity-id))
                             (str (or entity-label "Entity")
                               (when (present? entity-id)
                                 (str " • " entity-id))))
            admin-summary (cond
                            (and (present? admin-name) (present? admin-email))
                            (str admin-name " (" admin-email ")")
                            (present? admin-email) admin-email
                            (present? admin-name) admin-name
                            :else nil)
            header-meta (let [primary-name (cond
                                             (present? entity-summary)
                                             {:label "Entity"
                                              :value entity-summary}
                                             audit-id
                                             {:label "Audit ID"
                                              :value ($ :span {:class "font-mono"} audit-id)}
                                             :else nil)]
                          (cond-> []
                            primary-name (conj primary-name)))
            header-subtitle (or admin-summary
                              (when audit-id (str "Audit ID: " audit-id))
                              "Audit log overview")
            icon-letter (let [source (or action-label "A")
                              trimmed (str/trim (str source))]
                          (if (pos? (count trimmed))
                            (subs trimmed 0 1)
                            "A"))
            close-button ($ button {:btn-type :ghost
                                    :shape "circle"
                                    :class "ds-btn-sm"
                                    :on-click close!}
                           "✕")
            header-right (->> [action-badge close-button]
                           (keep identity)
                           (into []))
            header ($ utils/detail-modal-header
                     {:title "Audit Log Details"
                      :subtitle header-subtitle
                      :icon ($ :span {:class "text-lg font-semibold text-primary"}
                              icon-letter)
                      :icon-bg "bg-primary/10 text-primary"
                      :right header-right
                      :meta header-meta})]

        ($ modal {:id "admin-audit-details-modal"
                  :on-close close!
                  :draggable? true
                  :width "800px"
                  :class "max-w-[90vw] h-[80vh] flex flex-col"
                  :header header
                  :header-class "p-0 border-0 bg-transparent mb-4"}

          ($ :div {:class "flex-1 overflow-y-auto p-6"}
            ($ audit-details-body {:audit-log audit-log})))))))
