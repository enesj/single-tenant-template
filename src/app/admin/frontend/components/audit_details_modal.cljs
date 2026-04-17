(ns app.admin.frontend.components.audit-details-modal
  "Audit log details modal component"
  (:require
    [app.admin.frontend.components.format :as fmt]
    [app.admin.frontend.components.ui :as ui]
    [app.shared.date :as date]
    [app.template.frontend.components.modal-wrapper :refer [modal-wrapper]]
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.components.copy-button :refer [copy-to-clipboard-button]]
    [app.template.frontend.components.json-viewer :refer [json-viewer json-copy-text]]
    [clojure.string :as str]
    [re-frame.core :refer [dispatch]]
    [taoensso.timbre :as log]
    [uix.core :refer [$ defui]]
    [uix.re-frame :refer [use-subscribe]]))

(defn- present?
  [value]
  (cond
    (nil? value) false
    (and (string? value) (str/blank? value)) false
    :else true))

(defn- present-string
  [value]
  (when (present? value)
    (some-> value str str/trim not-empty)))

(defn- format-timestamp-display
  [timestamp]
  (when (present? timestamp)
    (if (string? timestamp)
      (if-let [parsed-date (date/parse-date-string timestamp)]
        (date/format-display-date parsed-date)
        timestamp)
      (date/format-display-date timestamp))))

(defn- action-badge-class
  [action]
  (case (keyword (str action))
    :create "ds-badge-success"
    :update "ds-badge-warning"
    :delete "ds-badge-error"
    :login "ds-badge-info"
    :logout "ds-badge-ghost"
    "ds-badge-neutral"))

(defn- render-action-badge
  ([action]
   (render-action-badge action "ds-badge"))
  ([action base-class]
   (when (present? action)
     ($ :span {:class (str base-class " ds-badge-outline " (action-badge-class action))}
       (fmt/format-value action)))))

(defn- audit-change-map
  [{:keys [changes]}]
  (if (map? changes)
    changes
    {}))

(defn- audit-actor-summary
  [{:keys [actor-display-name admin-name admin-ref]}]
  (cond
    (present? actor-display-name) actor-display-name
    (and (present? admin-name) (present? admin-ref) (not= admin-name admin-ref))
    (str admin-name " (" admin-ref ")")
    (present? admin-name) admin-name
    (present? admin-ref) admin-ref
    :else "System"))

(defn- audit-target-type
  [{:keys [entity-type target-type]}]
  (or (present-string entity-type)
    (present-string target-type)))

(defn- audit-target-id
  [{:keys [entity-id target-id]}]
  (or (present-string entity-id)
    (present-string target-id)))

(defn- audit-target-summary
  [{:keys [entity-name] :as audit-log}]
  (let [entity-name* (present-string entity-name)
        entity-type* (some-> (audit-target-type audit-log)
                       fmt/format-value
                       present-string)
        entity-id* (audit-target-id audit-log)]
    (->> [(or entity-name* entity-type*)
          (when (and entity-name* entity-type*
                  (not= (str/lower-case entity-name*)
                    (str/lower-case entity-type*)))
            entity-type*)
          entity-id*]
      (remove nil?)
      (str/join " • "))))

(defn- audit-signal-fields
  [audit-log]
  (let [changes (audit-change-map audit-log)
        api-name (or (present-string (:api-name audit-log))
                   (present-string (:api-name changes)))
        operation (or (present-string (:operation audit-log))
                    (present-string (:operation changes)))
        severity (or (present-string (:severity audit-log))
                   (present-string (:severity changes)))
        http-status (if (contains? audit-log :http-status)
                      (:http-status audit-log)
                      (when (contains? changes :http-status)
                        (:http-status changes)))
        error-type (or (present-string (:error-type audit-log))
                     (present-string (:error-type changes)))
        error-message (or (present-string (:error-message audit-log))
                        (present-string (:error-message changes)))
        request-url (or (present-string (:request-url audit-log))
                      (present-string (:request-url changes)))
        retry-attempted? (or (contains? audit-log :retry-attempted)
                           (contains? changes :retry-attempted))
        retry-attempted (if (contains? audit-log :retry-attempted)
                          (:retry-attempted audit-log)
                          (:retry-attempted changes))
        retry-succeeded? (or (contains? audit-log :retry-succeeded)
                           (contains? changes :retry-succeeded))
        retry-succeeded (if (contains? audit-log :retry-succeeded)
                          (:retry-succeeded audit-log)
                          (:retry-succeeded changes))
        triggering-user-name (or (present-string (:triggering-user-name audit-log))
                               (present-string (:triggering-user-name changes)))
        triggering-user-id (or (present-string (:triggering-user-id audit-log))
                             (present-string (:triggering-user-id changes)))
        triggering-user-summary (cond
                                  (and triggering-user-name triggering-user-id)
                                  (str triggering-user-name " (" triggering-user-id ")")

                                  triggering-user-name triggering-user-name
                                  triggering-user-id ($ :span {:key "triggering-user-id"
                                                               :class "font-mono text-sm"}
                                                       triggering-user-id)
                                  :else nil)]
    (vec
      (keep identity
        [{:label "Source"
          :value api-name}
         {:label "Operation"
          :value operation}
         {:label "Severity"
          :value (when severity
                   (ui/status-badge severity {:capitalize? false}))}
         {:label "HTTP Status"
          :value (when (some? http-status)
                   ($ :span {:key "http-status"
                             :class "font-mono text-sm"}
                     (str http-status)))}
         {:label "Error Type"
          :value error-type}
         {:label "Error Message"
          :value (when error-message
                   ($ :div {:key "error-message"
                            :class "text-sm text-base-content/80 break-words max-w-xl"}
                     error-message))}
         {:label "Request URL"
          :value (when request-url
                   ($ :div {:key "request-url"
                            :class "text-sm text-base-content/70 break-all max-w-xl"}
                     request-url))}
         {:label "Retry Attempted"
          :value (when retry-attempted?
                   (if retry-attempted "Yes" "No"))}
         {:label "Retry Succeeded"
          :value (when retry-succeeded?
                   (if retry-succeeded "Yes" "No"))}
         {:label "Triggering User"
          :value triggering-user-summary}]))))

(defui audit-identity-block
  [{:keys [audit-log]}]
  (let [{:keys [action created-at timestamp ip-address context-summary]} audit-log
        action-label (when (present? action)
                       (fmt/format-value action))
        actor-summary (audit-actor-summary audit-log)
        target-summary (audit-target-summary audit-log)
        timestamp-primary (format-timestamp-display (or created-at timestamp))
        summary-text (cond
                       (and (present? actor-summary) (present? context-summary))
                       (str actor-summary " — " context-summary)

                       (and (present? actor-summary) (present? action-label) (present? target-summary))
                       (str actor-summary " — " action-label " • " target-summary)

                       (present? context-summary) context-summary
                       (present? action-label) action-label
                       :else "No recent activity")
        detail-pills (keep identity
                       [(when timestamp-primary
                          (str "At " timestamp-primary))
                        (when (present? target-summary)
                          (str "Target: " target-summary))
                        (when (present? ip-address)
                          (str "Origin IP: " ip-address))])]
    ($ :div {:class "rounded-xl border border-base-200 bg-base-100/80 p-5 mb-6 space-y-3"}
      ($ :div {:class "flex items-start justify-between gap-3"}
        ($ :div {:class "space-y-2"}
          ($ :div {:class "flex items-center gap-2"}
            ($ :div {:class "w-1 h-4 rounded-full bg-primary"})
            ($ :h3 {:class "text-base font-semibold text-base-content"} "Event Summary"))
          ($ :p {:class "text-sm text-base-content/80 leading-relaxed"}
            summary-text))
        (render-action-badge action "ds-badge ds-badge-sm text-xs"))
      (when (seq detail-pills)
        ($ :div {:class "flex flex-wrap gap-2 text-xs text-base-content/60"}
          (for [detail detail-pills]
            ($ :span {:key detail
                      :class "rounded-full bg-base-200/60 px-2 py-1"}
              detail)))))))

(defui audit-details-body
  [{:keys [audit-log]}]
  (let [{:keys [id created-at timestamp action actor-id ip-address user-agent changes]} audit-log
        actor-summary (audit-actor-summary audit-log)
        entity-type* (audit-target-type audit-log)
        entity-id* (audit-target-id audit-log)
        event-detail-fields (audit-signal-fields audit-log)]
    ($ :div {:class "space-y-6"}
      ($ audit-identity-block {:audit-log audit-log})

      ($ :div {:class "grid lg:grid-cols-2 gap-6"}
        ($ ui/detail-card
          {:title "Basic Information"
           :fields [{:label "Audit ID"
                     :value ($ :span {:class "font-mono text-sm"} id)}
                    {:label "Timestamp"
                     :value (or (format-timestamp-display (or created-at timestamp))
                              "No timestamp")}
                    {:label "Action"
                     :value (render-action-badge action)}
                    {:label "Entity Type"
                     :value (when entity-type*
                              ($ :span {:class "ds-badge ds-badge-outline ds-badge-sm"}
                                (fmt/format-value entity-type*)))}
                    {:label "Entity ID"
                     :value (when entity-id*
                              ($ :span {:class "font-mono text-sm"}
                                entity-id*))}]})
        ($ ui/detail-card
          {:title "Actor Information"
           :fields [{:label "Actor"
                     :value ($ :span {:class "font-medium"}
                              actor-summary)}
                    {:label "Actor ID"
                     :value (when actor-id
                              ($ :span {:class "font-mono text-sm"}
                                actor-id))}
                    {:label "IP Address"
                     :value (ui/ip-address-badge ip-address)}
                    {:label "User Agent"
                     :value ($ :div {:class "text-sm text-base-content/70 max-w-xs break-words"}
                              (or user-agent "Unknown"))}]}))

      (when (seq event-detail-fields)
        ($ ui/detail-card
          {:title "Event Details"
           :fields event-detail-fields}))

      (when changes
        ($ ui/detail-card
          {:title "Data Changes"
           :fields [{:label "Changes"
                     :value ($ :div {:class "relative bg-base-100 border border-base-300 rounded-lg p-4 my-2"}
                              ($ copy-to-clipboard-button
                                {:text (json-copy-text changes)})
                              ($ :div {:class "bg-base-200/50 rounded-md p-4 max-h-80 overflow-y-auto border border-base-200 min-h-[100px]"}
                                (let [raw-content (str changes)
                                      has-content? (and raw-content
                                                     (not (str/blank? raw-content))
                                                     (not= raw-content "{}")
                                                     (not= raw-content "[]")
                                                     (not= raw-content "null"))]
                                  (if has-content?
                                    ($ json-viewer {:data changes})
                                    ($ :div {:class "text-center text-base-content/60 py-8"}
                                      ($ :div {:class "text-lg mb-2"} "📝")
                                      ($ :div "No changes data available")
                                      ($ :div {:class "text-sm mt-1"}
                                        (str "Raw data: '" raw-content "'")))))))}]}))

      ($ :div {:class "ds-card ds-card-bordered bg-gradient-to-br from-warning/10 to-error/10 shadow-lg p-4"}
        ($ :div {:class "flex items-center gap-2 mb-3"}
          ($ :div {:class "w-1 h-4 rounded-full bg-error"})
          ($ :h3 {:class "text-base font-semibold text-base-content"}
            "Actions"))
        ($ :div {:class "flex flex-wrap gap-2"}
          ($ button {:id "btn-export-audit-details"
                     :btn-type :outline
                     :class "ds-btn-sm"
                     :on-click #(do
                                  (log/info "Exporting audit log from details modal:" id)
                                  (dispatch [:admin/export-single-audit-log audit-log]))}
            ($ :svg {:class "w-4 h-4" :fill "none" :stroke "currentColor" :view-box "0 0 24 24"}
              ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M12 10v6m0 0l-3-3m3 3l3-3m2 8H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"}))
            "Export")

          (when actor-id
            ($ button {:id "btn-filter-by-admin-from-details"
                       :btn-type :outline
                       :class "ds-btn-sm"
                       :on-click #(do
                                    (log/info "Filtering by admin from details modal:" actor-id)
                                    (dispatch [:admin/apply-audit-filter :admin-id actor-id])
                                    (dispatch [:admin/hide-audit-details]))}
              ($ :svg {:class "w-4 h-4" :fill "none" :stroke "currentColor" :view-box "0 0 24 24"}
                ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2" :d "M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.207A1 1 0 013 6.5V4z"}))
              "Filter by Admin"))

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
            {:keys [id action]} (or audit-log {})
            audit-id (when (present? id) id)
            action-label (when (present? action) (fmt/format-value action))
            entity-summary (audit-target-summary (or audit-log {}))
            actor-summary (audit-actor-summary (or audit-log {}))
            action-badge (render-action-badge action "ds-badge ds-badge-lg")
            header-meta (cond-> []
                          (present? entity-summary)
                          (conj {:label "Entity"
                                 :value entity-summary}))
            header-subtitle (or (present-string actor-summary)
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
            header ($ ui/detail-modal-header
                     {:title "Audit Log Details"
                      :subtitle header-subtitle
                      :icon ($ :span {:class "text-lg font-semibold text-primary"}
                              icon-letter)
                      :icon-bg "bg-primary/10 text-primary"
                      :right header-right
                      :meta header-meta})]
        ($ modal-wrapper
          {:id "admin-audit-details-modal"
           :visible? true
           :title "Audit Log Details"
           :header header
           :size :large
           :draggable? true
           :resizable? true
           :on-close close!
           :close-button-id "btn-close-admin-audit-details-modal"
           :content-class "p-6"}
          ($ audit-details-body {:audit-log audit-log}))))))