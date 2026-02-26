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

(def detail-modal-header template-detail/detail-modal-header)

(def detail-card template-detail/detail-card)
(def ip-address-badge template-detail/ip-address-badge)
