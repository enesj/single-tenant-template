(ns app.admin.frontend.components.shared-utils
  "Shared utility functions for admin components to eliminate duplication
   and provide consistent formatting patterns across the admin interface."
  (:require
    [app.shared.date :as date]
    [clojure.string :as str]
    [goog.object :as gobj]
    [uix.core :refer [$ defui]]))

;; ============================================================================
;; Value Formatting Utilities
;; ============================================================================

(defn react-element?
  "Check if a value is a React element"
  [value]
  (and (some? value) (object? value) (gobj/get value "$$typeof")))

(defn format-value
  "Format a value for display in admin components.
   Handles nil, booleans, keywords, strings, numbers, React elements, and vectors.

   Props:
   - value: The value to format
   - nil-text: Text to display for nil values (default: \"—\")
   - capitalize?: Whether to capitalize string values (default: true)"
  ([value]
   (format-value value "—" true))
  ([value nil-text]
   (format-value value nil-text true))
  ([value nil-text capitalize?]
   (cond
     (nil? value) nil-text
     (react-element? value) value
     (vector? value) value
     (boolean? value) (if value "Yes" "No")
     (keyword? value) (-> value
                        name
                        (str/replace "-" " ")
                        ((if capitalize? str/capitalize identity)))
     (string? value) ((if capitalize? identity str/trim) value)
     (number? value) value
     :else (str value))))

;; ============================================================================
;; Date Formatting Utilities
;; ============================================================================

(defn format-date
  "Format ISO date string to readable format (YYYY-MM-DD HH:mm:ss)
   Handles parsing errors gracefully by returning the original string."
  [date-str]
  (when (and date-str (not= date-str "N/A") (not= date-str "—"))
    (try
      ;; Use shared date formatting if available, fall back to manual parsing
      (if-let [parsed-date (date/parse-date-string date-str)]
        (date/format-display-date parsed-date)
        ;; Manual parsing fallback
        (let [date-parts (.split date-str "T")
              date-part (first date-parts)
              time-parts (when (> (count date-parts) 1) (.split (second date-parts) "."))
              time-part (first time-parts)
              year (.substring date-part 0 4)
              month (.substring date-part 5 7)
              day (.substring date-part 8 10)
              hours (when time-part (.substring time-part 0 2))
              minutes (when time-part (.substring time-part 3 5))
              seconds (when time-part (.substring time-part 6 8))]
          (str year "-" month "-" day " " (or hours "00") ":" (or minutes "00") ":" (or seconds "00"))))
      (catch js/Error e
        (js/console.log "Date parsing error for:" date-str e)
        date-str))))

(defn format-relative-time
  "Format date as relative time (e.g., \"2 hours ago\", \"3 days ago\")
   Falls back to absolute date formatting for very old dates."
  [date-str]
  (when date-str
    (try
      (let [date (if (string? date-str)
                   (or (date/parse-date-string date-str)
                     (js/Date. date-str))
                   date-str)
            now (js/Date.)
            diff-ms (- (.getTime now) (.getTime date))
            diff-minutes (/ diff-ms (* 60 1000))
            diff-hours (/ diff-minutes 60)
            diff-days (/ diff-hours 24)]
        (cond
          (< diff-minutes 1) "Just now"
          (< diff-minutes 60) (str (Math/round diff-minutes) " minutes ago")
          (< diff-hours 24) (str (Math/round diff-hours) " hours ago")
          (< diff-days 7) (str (Math/round diff-days) " days ago")
          :else (format-date date-str)))
      (catch js/Error _
        (format-date date-str)))))

;; ============================================================================
;; Badge Components
;; ============================================================================

(defn status-badge
  "Create a status badge with appropriate styling based on status value.

   Props:
   - status: Status value (string, keyword, or nil)
   - options: Map of options
     - :default-class: Default CSS classes when status doesn't match known patterns
     - :capitalize?: Whether to capitalize the display text (default: true)
     - :show-nil?: Whether to show badge for nil values (default: false)
     - :nil-text: Text to show for nil values (default: \"Unknown\")"
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
  "Create a role badge with consistent styling for user/tenant roles.

   Props:
   - role: Role value (string, keyword, or nil)
   - options: Map of options
     - :default-class: CSS classes for the badge (default: admin role styling)
     - :capitalize?: Whether to capitalize the display text (default: true)"
  [role & [options]]
  (let [{:keys [default-class capitalize?]
         :or {default-class "ds-badge capitalize tracking-wide text-base-content/80 bg-primary/5 border-primary/20 font-medium text-xs px-3 py-1 rounded-full shadow-sm"
              capitalize? true}} options
        role-str (when role (str role))]
    ($ :span {:class default-class}
      (or (some-> role-str ((if capitalize? str/capitalize identity))) "Unknown"))))

(defn verification-badge
  "Create a verification badge with appropriate styling.

   Props:
   - verified?: Boolean indicating verification status
   - status: Optional detailed status string
   - options: Map of options
     - :verified-text: Text to show when verified (default: \"Verified\")
     - :unverified-text: Text to show when not verified (default: \"Unverified\")"
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
  "Create a badge for displaying metrics with optional indicators.

   Props:
   - value: The metric value
   - label: Optional label for the metric
   - options: Map of options
     - :variant: Color variant (:primary, :success, :warning, :error, :info)
     - :size: Size variant (:xs, :sm, :md, :lg)
     - :show-trend?: Whether to show trend indicator
     - :trend-value: Trend value (positive/negative number)"
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

;; ============================================================================
;; Modal Header Components
;; ============================================================================

(defui detail-modal-header
  "Rich header layout for admin detail modals.

   Props:
   - title: Primary header title (required)
   - subtitle: Supporting detail text or element
   - eyebrow: Optional small label above the title
   - icon: Optional icon/element displayed on the left
   - icon-bg: Additional classes applied to the icon container
   - meta: Vector of {:label string :value any :value-class string}
   - right: Single element or vector of elements aligned right (badges/actions)
   - class: Extra classes for the header shell"
  [props]
  (let [{:keys [title subtitle eyebrow icon icon-bg meta right class]} props
        present? (fn [v]
                   (cond
                     (nil? v) false
                     (and (string? v) (str/blank? v)) false
                     :else true))
        icon-node (cond
                    (nil? icon) nil
                    (react-element? icon) icon
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
                  (react-element? subtitle) subtitle
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
                        (react-element? item) item
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
                    (react-element? value) value
                    (vector? value) value
                    :else ($ :span {:class (str "text-base-content/70 " (or value-class ""))}
                            value))))
              meta-items)))))))

;; ============================================================================
;; Display Field Components
;; ============================================================================

(defui detail-field
  "Display a detail field with label and formatted value.

   Props:
   - label: Field label
   - value: Field value (will be auto-formatted)
   - hint: Optional hint text to display below
   - format-options: Map of formatting options passed to format-value"
  [props]
  (let [{:keys [label value hint]} props
        formatted (format-value value "—" true)]
    ($ :div {:class "flex justify-between items-center py-3 border-b border-base-200 last:border-b-0"}
      ($ :span {:class "text-sm font-medium text-base-content/70 min-w-[120px]"}
        label)
      (cond
        (react-element? formatted)
        ($ :div {:class "text-base ml-4 flex-1"} formatted)

        (vector? formatted)
        ($ :div {:class "text-base ml-4 flex-1"} formatted)

        :else
        ($ :span {:class "text-base font-medium text-base-content ml-4 flex-1 text-right"}
          formatted))
      (when hint
        ($ :div {:class "col-span-2 text-xs text-base-content/50 italic mt-1"} hint)))))

(defui detail-card
  "Create a card containing multiple detail fields.

   Props:
   - title: Card title
   - fields: Vector of field maps with :label, :value, and optional :hint
   - format-options: Map of formatting options for field values
   - options: Map of card options
     - :class: Additional CSS classes
     - :title-class: Additional title CSS classes"
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

;; ============================================================================
;; User/Tenant Specific Utilities
;; ============================================================================

(defn user-initials
  "Return graceful initials based on full name or email.

   Props:
   - full-name: User's full name
   - email: User's email address"
  [full-name email]
  (let [source (or full-name email "")
        parts (->> (str/split source #"\s+")
                (remove str/blank?)
                (take 2))
        initials (->> parts
                   (map #(subs % 0 (min 1 (count %))))
                   (str/join "")
                   (str/upper-case))]
    (cond
      (seq initials) initials
      (and email (not (str/blank? email))) (-> email (subs 0 1) str/upper-case)
      :else "U")))

(defn tenant-label
  "Create a formatted tenant label with name and slug.

   Props:
   - tenant-name: Tenant name
   - tenant-slug: Tenant slug"
  [tenant-name tenant-slug]
  (cond
    (and tenant-name tenant-slug) (str tenant-name " (" tenant-slug ")")
    tenant-name tenant-name
    tenant-slug tenant-slug
    :else "—"))

(defn ip-address-badge
  "Create a formatted IP address badge.

   Props:
   - ip: IP address string"
  [ip]
  (if (and ip (not= ip "Unknown") (not= ip "N/A"))
    ($ :span {:class "ds-badge ds-badge-outline ds-badge-sm font-mono"} ip)
    ($ :span {:class "text-base-content/50 italic"} "Unknown")))

;; ============================================================================
;; Export Utilities
;; ============================================================================

(defn create-csv-export-data
  "Prepare data for CSV export with proper formatting.

   Props:
   - data: Vector of maps to export
   - headers: Vector of column headers
   - key-fn: Function to extract values from each map"
  [data headers key-fn]
  (let [header-row (str/join "," headers)
        data-rows (mapv (fn [item]
                          (str/join ","
                            (mapv (fn [header]
                                    (let [value (key-fn item header)]
                                      (cond
                                        (string? value) (str "\"" value "\"")
                                        (nil? value) ""
                                        :else (str value))))
                              headers)))
                    data)]
    (str header-row "\n" (str/join "\n" data-rows))))

(defn download-as-json
  "Download data as JSON file.

   Props:
   - data: Data to download
   - filename: Filename for the download"
  [data filename]
  (let [json-str (js/JSON.stringify (clj->js data) nil 2)
        blob (js/Blob. #js [json-str] #js {:type "application/json"})
        url (.createObjectURL js/URL blob)
        link (js/document.createElement "a")]
    (set! (.-href link) url)
    (set! (.-download link) filename)
    (.click js/document.body link)
    (.revokeObjectURL js/URL url)))

;; ============================================================================
;; Validation Utilities
;; ============================================================================

(defn validate-email-format
  "Basic email format validation."
  [email]
  (and (string? email)
    (not (str/blank? email))
    (re-matches #".+@.+\..+" email)))

(defn validate-required-fields
  "Validate that required fields are present and not blank.

   Props:
   - data: Map of data to validate
   - required-fields: Set of required field keys"
  [data required-fields]
  (reduce-kv
    (fn [errors field value]
      (if (or (nil? value)
            (and (string? value) (str/blank? value)))
        (assoc errors field "This field is required")
        errors))
    {}
    (select-keys data required-fields)))
