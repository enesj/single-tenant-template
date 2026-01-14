(ns app.admin.frontend.components.ui
  "UI components and layouts for admin interface."
  (:require
    [app.template.frontend.components.advanced-fields :as advanced-fields]
    [app.template.frontend.components.detail :as template-detail]
    [clojure.string :as str]
    [uix.core :refer [$]]))

(defn status-badge
  "Create a status badge with appropriate styling based on status value."
  [status & [options]]
  (let [{:keys [default-class capitalize? show-nil? nil-text]
         :or {default-class "ds-badge-outline ds-badge-neutral"
              capitalize? true
              show-nil? false
              nil-text "Unknown"}} options
        status-str (when status (str status))
        badge-variant (advanced-fields/status->badge-class status {:default-class default-class})]
    (when (or status show-nil?)
      ($ advanced-fields/status-badge
        {:text (or (some-> status-str ((if capitalize? str/capitalize identity))) nil-text)
         :class (str "uppercase tracking-wide text-xs px-3 py-1 rounded-full border shadow-sm " badge-variant)}))))

(defn role-badge
  "Create a role badge with consistent styling for user/tenant roles."
  [role & [options]]
  (let [{:keys [default-class capitalize?]
         :or {default-class "ds-badge capitalize tracking-wide text-base-content/80 bg-primary/5 border-primary/20 font-medium text-xs px-3 py-1 rounded-full shadow-sm"
              capitalize? true}} options
        role-str (when role (str role))]
    ($ :span {:class default-class}
      (or (some-> role-str ((if capitalize? str/capitalize identity))) "Unknown"))))

(defn verification-badge
  "Create a verification badge with appropriate styling."
  [verified? status & [options]]
  (let [{:keys [verified-text unverified-text]
         :or {verified-text "Verified"
              unverified-text "Unverified"}} options
        status-str (when status (str status))
        label (cond
                verified? verified-text
                (some? status-str) (str/capitalize status-str)
                :else unverified-text)
        variant (if verified?
                  "bg-success/10 text-success/80 border-success/30"
                  "bg-warning/10 text-warning/80 border-warning/30")]
    ($ :span {:class (str "ds-badge uppercase tracking-wide text-xs px-3 py-1 rounded-full border shadow-sm font-medium " variant)}
      label)))

(defn metric-badge
  "Create a badge for displaying metrics with optional indicators."
  [value label & [options]]
  (let [{:keys [variant size show-trend? trend-value]
         :or {variant :primary
              size :sm
              show-trend? false}} options
        variant-class (case variant
                        :primary "ds-badge-primary"
                        :success "ds-badge-success"
                        :warning "ds-badge-warning"
                        :error "ds-badge-error"
                        :info "ds-badge-info"
                        "ds-badge-primary")
        size-class (case size
                     :xs "ds-badge-xs"
                     :sm "ds-badge-sm"
                     :md "ds-badge-md"
                     :lg "ds-badge-lg"
                     "ds-badge-sm")
        trend-indicator (when (and show-trend? (some? trend-value))
                          ($ :span {:class (str "ml-1 text-xs "
                                             (if (pos? trend-value) "text-success" "text-error"))}
                            (if (pos? trend-value) "↑" "↓")))]
    ($ :span {:class (str "ds-badge " variant-class " " size-class)}
      (str value (when label (str " " label)) trend-indicator))))

(def detail-modal-header template-detail/detail-modal-header)
(def detail-field template-detail/detail-field)
(def detail-card template-detail/detail-card)
(def ip-address-badge template-detail/ip-address-badge)
