(ns app.admin.frontend.components.ui
  "UI components and layouts for admin interface."
  (:require
    [app.admin.frontend.components.format :as fmt]
    [clojure.string :as str]
    [uix.core :refer [$ defui]]))

(defn status-badge
  "Create a status badge with appropriate styling based on status value."
  [status & [options]]
  (let [{:keys [default-class capitalize? show-nil? nil-text]
         :or {default-class "ds-badge ds-badge-outline"
              capitalize? true
              show-nil? false
              nil-text "Unknown"}} options
        status-str (when status (str status))
        status-lower (when status-str (str/lower-case status-str))
        badge-variant (case status-lower
                        "active" "ds-badge-success"
                        "suspended" "ds-badge-error"
                        "inactive" "ds-badge-warning"
                        "invited" "ds-badge-info"
                        "pending" "ds-badge-info"
                        "archived" "ds-badge-ghost"
                        "cancelled" "ds-badge-error"
                        "trialing" "ds-badge-warning"
                        "verified" "ds-badge-success"
                        "unverified" "ds-badge-warning"
                        "complete" "ds-badge-success"
                        "in-progress" "ds-badge-info"
                        "failed" "ds-badge-error"
                        "error" "ds-badge-error"
                        "success" "ds-badge-success"
                        default-class)]
    (when (or status show-nil?)
      ($ :span {:class (str "ds-badge uppercase tracking-wide text-xs px-3 py-1 rounded-full border shadow-sm " badge-variant)}
        (or (some-> status-str ((if capitalize? str/capitalize identity))) nil-text)))))

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

(defui detail-modal-header
  "Rich header layout for admin detail modals."
  [props]
  (let [{:keys [title subtitle eyebrow icon icon-bg meta right class]} props
        present? (fn [v]
                   (cond
                     (nil? v) false
                     (and (string? v) (str/blank? v)) false
                     :else true))
        icon-node (cond
                    (nil? icon) nil
                    (fmt/react-element? icon) icon
                    (vector? icon) icon
                    :else ($ :span {:class "text-xl font-semibold text-primary"} icon))
        normalized-right (when right
                           (if (sequential? right)
                             (into [] (remove nil?) right)
                             [right]))
        meta-items (->> meta
                     (keep (fn [{:keys [label value value-class] :as item}]
                             (when (and item (present? value))
                               {:label label
                                :value value
                                :value-class value-class}))))]
    ($ :div {:class (str "relative w-full overflow-hidden rounded-xl border border-base-200 bg-base-100 shadow-sm "
                      (or class ""))}
      ($ :div {:class "absolute inset-0 bg-gradient-to-r from-primary/10 via-accent/5 to-transparent opacity-90"})
      ($ :div {:class "relative flex flex-col gap-4 px-6 py-5 select-none"}
        ($ :div {:class "flex items-start justify-between gap-4"}
          ($ :div {:class "flex items-start gap-4"}
            (when icon-node
              ($ :div {:class (str "flex h-12 w-12 items-center justify-center rounded-2xl bg-white shadow-md ring-1 ring-primary/20 "
                                (or icon-bg ""))}
                icon-node))
            ($ :div {:class "space-y-1"}
              (when eyebrow
                ($ :span {:class "text-xs font-semibold uppercase tracking-wide text-primary/80"}
                  eyebrow))
              ($ :h2 {:class "text-xl font-semibold text-base-content"}
                title)
              (when subtitle
                (cond
                  (fmt/react-element? subtitle) subtitle
                  (vector? subtitle) subtitle
                  :else ($ :p {:class "text-sm text-base-content/70"}
                          subtitle)))))
          (when (seq normalized-right)
            ($ :div {:class "flex flex-wrap items-center justify-end gap-2"}
              (map-indexed
                (fn [idx item]
                  (when item
                    ($ :div {:key idx :class "flex items-center"}
                      (cond
                        (fmt/react-element? item) item
                        (vector? item) item
                        :else ($ :span {:class "text-sm text-base-content/80"} item)))))
                normalized-right))))
        (when (seq meta-items)
          ($ :div {:class "flex flex-wrap gap-3"}
            (map-indexed
              (fn [idx {:keys [label value value-class]}]
                ($ :div {:key idx
                         :class "flex items-center gap-2 rounded-full border border-base-200 bg-base-100/80 px-3 py-1 text-xs text-base-content/70 shadow-sm"}
                  (when label
                    ($ :span {:class "font-semibold text-base-content/80"}
                      label))
                  (cond
                    (fmt/react-element? value) value
                    (vector? value) value
                    :else ($ :span {:class (str "text-base-content/70 " (or value-class ""))}
                            value))))
              meta-items)))))))

(defui detail-field
  "Display a detail field with label and formatted value."
  [props]
  (let [{:keys [label value hint]} props
        formatted (fmt/format-value value "—" true)]
    ($ :div {:class "flex justify-between items-center py-3 border-b border-base-200 last:border-b-0"}
      ($ :span {:class "text-sm font-medium text-base-content/70 min-w-[120px]"}
        label)
      (cond
        (fmt/react-element? formatted)
        ($ :div {:class "text-base ml-4 flex-1"} formatted)

        (vector? formatted)
        ($ :div {:class "text-base ml-4 flex-1"} formatted)

        :else
        ($ :span {:class "text-base font-medium text-base-content ml-4 flex-1 text-right"}
          formatted))
      (when hint
        ($ :div {:class "col-span-2 text-xs text-base-content/50 italic mt-1"} hint)))))

(defui detail-card
  "Create a card containing multiple detail fields."
  [props]
  (let [{:keys [title fields options]} props
        {:keys [class title-class]} (or options {})]
    ($ :div {:class (str "ds-card ds-card-bordered bg-base-100 shadow-lg p-4 " (or class ""))}
      ($ :div {:class "flex items-center gap-2 mb-3"}
        ($ :div {:class "w-1 h-4 rounded-full bg-primary"})
        ($ :h3 {:class (str "text-base font-semibold text-base-content " (or title-class ""))}
          title))
      ($ :div {:class "space-y-3"}
        (map-indexed
          (fn [idx field]
            ($ detail-field (assoc field :key idx)))
          fields)))))

(defn ip-address-badge
  "Create a formatted IP address badge."
  [ip]
  (if (and ip (not= ip "Unknown") (not= ip "N/A"))
    ($ :span {:class "ds-badge ds-badge-outline ds-badge-sm font-mono"} ip)
    ($ :span {:class "text-base-content/50 italic"} "Unknown")))
