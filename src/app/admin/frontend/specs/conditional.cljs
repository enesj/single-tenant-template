(ns app.admin.frontend.specs.conditional
  "Advanced field customizations with conditional visibility and dynamic behavior"
  (:require
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ========================================================================
;; Conditional Visibility Engine
;; ========================================================================

(defn evaluate-condition
  "Evaluate if a condition matches the record data"
  [condition record]
  (cond
    ;; Simple equality: {:status "active"}
    (and (map? condition) (every? #(not (coll? (val %))) condition))
    (every? (fn [[field expected-value]]
              (= (get record field) expected-value))
      condition)

    ;; Set membership: {:subscription-tier #{"professional" "enterprise"}}
    (and (map? condition) (some #(set? (val %)) condition))
    (every? (fn [[field expected-values]]
              (if (set? expected-values)
                (contains? expected-values (get record field))
                (= (get record field) expected-values)))
      condition)

    ;; Function-based: (fn [record] (> (:user-count record) 10))
    (fn? condition)
    (condition record)

    ;; Default: false
    :else false))

(defn should-show-field?
  "Determine if a field should be visible based on conditional visibility rules"
  [field-spec record]
  (let [conditional-visibility (:conditional-visibility field-spec)]
    (cond
      ;; No conditional rules - always show
      (nil? conditional-visibility) true

      ;; Has show-when rules
      (:show-when conditional-visibility)
      (evaluate-condition (:show-when conditional-visibility) record)

      ;; Has hide-when rules
      (:hide-when conditional-visibility)
      (not (evaluate-condition (:hide-when conditional-visibility) record))

      ;; Default: show
      :else true)))

(defn filter-visible-fields
  "Filter entity spec fields based on conditional visibility for a specific record"
  [entity-spec record]
  (into {}
    (filter (fn [[_field-id field-spec]]
              (should-show-field? field-spec record))
      entity-spec)))

;; ========================================================================
;; Role-Based Field Permissions
;; ========================================================================

;; Define role hierarchy and utility functions first
(def role-hierarchy
  "Admin role hierarchy from lowest to highest"
  [:support :admin :super-admin :platform-admin])

(defn role-level-sufficient?
  "Check if user's role meets minimum required level"
  [user-role min-role]
  (let [user-level (.indexOf role-hierarchy user-role)
        min-level (.indexOf role-hierarchy min-role)]
    (and (>= user-level 0) (>= min-level 0) (>= user-level min-level))))

(defn get-role-permissions
  "Get permissions for a specific role"
  [role]
  (case role
    :support #{:view-basic :view-user-activity}
    :admin #{:view-basic :view-user-activity :view-billing :modify-users}
    :super-admin #{:view-basic :view-user-activity :view-billing :modify-users
                   :view-sensitive :modify-billing :system-admin}
    :platform-admin #{:view-basic :view-user-activity :view-billing :modify-users
                      :view-sensitive :modify-billing :system-admin :platform-config}
    #{}))

(defn get-admin-role
  "Get current admin user's role"
  []
  @(rf/subscribe [:admin/current-user-role]))

(defn has-field-permission?
  "Check if admin user has permission to see this field"
  [field-spec admin-role]
  (let [required-permissions (:required-permissions field-spec)
        min-role (:min-role field-spec)]
    (cond
      ;; No restrictions - always allow
      (and (nil? required-permissions) (nil? min-role)) true

      ;; Check specific permissions
      required-permissions
      (some #(contains? (set required-permissions) %)
        (get-role-permissions admin-role))

      ;; Check minimum role level
      min-role
      (role-level-sufficient? admin-role min-role)

      ;; Default: allow
      :else true)))

;; ========================================================================
;; Computed Fields Engine
;; ========================================================================

;; Helper function defined before use
(defn get-previous-month-revenue
  "Mock function - would query historical data in real implementation"
  [_tenant-id _all-records]
  ;; This would fetch from time-series data or analytics
  (+ 1000 (rand-int 5000)))

(defmulti compute-field-value
  "Compute dynamic field values based on field type"
  (fn [field-type _record _all-records] field-type))

(defmethod compute-field-value :tenant-health-score
  [_ record _all-records]
  (let [user-count (:user-count record 0)
        subscription-status (:subscription-status record)
        onboarding-completed (:onboarding-completed record false)
        last-activity-days (:days-since-last-activity record 0)]
    ;; Simple health scoring algorithm
    (+ (if (> user-count 0) 25 0)
      (case subscription-status
        "active" 25
        "trialing" 15
        "past_due" 10
        0)
      (if onboarding-completed 25 0)
      (cond
        (<= last-activity-days 7) 25
        (<= last-activity-days 30) 15
        (<= last-activity-days 90) 5
        :else 0))))

(defmethod compute-field-value :user-risk-score
  [_ record _all-records]
  (let [failed-logins (:failed-login-attempts record 0)
        account-age-days (:account-age-days record 0)
        suspicious-activity (:suspicious-activity-count record 0)]
    ;; Risk scoring: lower is better
    (+ (min failed-logins 50)  ; Cap at 50 points
      (if (< account-age-days 30) 30 0)  ; New accounts are riskier
      (* suspicious-activity 10))))

(defmethod compute-field-value :revenue-trend
  [_ record all-records]
  (let [current-revenue (:monthly-revenue record 0)
        tenant-id (:id record)
        ;; This would ideally come from a time-series analysis
        previous-revenue (get-previous-month-revenue tenant-id all-records)]
    (if (and current-revenue previous-revenue (> previous-revenue 0))
      (let [change-pct (* 100 (/ (- current-revenue previous-revenue) previous-revenue))]
        {:value change-pct
         :trend (cond
                  (> change-pct 10) :up-strong
                  (> change-pct 0) :up
                  (< change-pct -10) :down-strong
                  (< change-pct 0) :down
                  :else :stable)})
      {:value 0 :trend :unknown})))

;; ========================================================================
;; Dynamic Formatting Engine
;; ========================================================================

(defmulti format-field-display
  "Apply dynamic formatting based on field value and context"
  (fn [field-spec _value _record] (:display-format field-spec)))

(defmethod format-field-display :status-badge
  [_field-spec value _record]
  {:component :badge
   :text value
   :class (case value
            "active" "ds-badge-success"
            "inactive" "ds-badge-warning"
            "suspended" "ds-badge-error"
            "trialing" "ds-badge-info"
            "ds-badge-neutral")})

(defmethod format-field-display :health-score
  [_field-spec value _record]
  {:component :progress-bar
   :value value
   :max 100
   :class (cond
            (>= value 80) "progress-success"
            (>= value 60) "progress-warning"
            :else "progress-error")
   :label (str value "%")})

(defmethod format-field-display :risk-indicator
  [_field-spec value _record]
  {:component :risk-badge
   :value value
   :level (cond
            (<= value 20) :low
            (<= value 50) :medium
            (<= value 80) :high
            :else :critical)
   :class (cond
            (<= value 20) "ds-badge-success"
            (<= value 50) "ds-badge-warning"
            (<= value 80) "ds-badge-error"
            :else "ds-badge-error animate-pulse")})

(defmethod format-field-display :trend-arrow
  [_field-spec value _record]
  (let [trend-data (if (map? value) value {:value value :trend :stable})]
    {:component :trend-display
     :value (:value trend-data)
     :trend (:trend trend-data)
     :icon (case (:trend trend-data)
             :up-strong "📈"
             :up "📊"
             :down "📉"
             :down-strong "📉"
             :stable "➡️"
             :unknown "❓")
     :class (case (:trend trend-data)
              :up-strong "text-green-600 font-bold"
              :up "text-green-500"
              :down "text-red-500"
              :down-strong "text-red-600 font-bold"
              :stable "text-gray-500"
              :unknown "text-gray-400")}))

;; Default formatting
(defmethod format-field-display :default
  [_field-spec value _record]
  {:component :text
   :value value})

;; ========================================================================
;; Integration Functions
;; ========================================================================

(defn enhance-entity-spec-with-advanced-features
  "Add advanced customizations to base entity spec"
  [base-entity-spec advanced-config]
  (reduce-kv
    (fn [acc field-id field-config]
      (if-let [base-field (get base-entity-spec field-id)]
        (assoc acc field-id (merge base-field field-config))
        acc))
    base-entity-spec
    advanced-config))

(defn filter-and-enhance-fields-for-record
  "Apply all advanced field customizations for a specific record"
  [entity-spec record admin-role]
  (->> entity-spec
       ;; Filter by conditional visibility
    (filter (fn [[_field-id field-spec]]
              (should-show-field? field-spec record)))
       ;; Filter by role permissions
    (filter (fn [[_field-id field-spec]]
              (has-field-permission? field-spec admin-role)))
       ;; Convert back to map
    (into {})
       ;; Compute dynamic field values
    (reduce-kv
      (fn [acc field-id field-spec]
        (if-let [compute-type (:compute-type field-spec)]
          (let [computed-value (compute-field-value compute-type record [])]
            (assoc-in acc [field-id :computed-value] computed-value))
          acc))
      entity-spec)))

;; ========================================================================
;; Re-frame Integration
;; ========================================================================

(rf/reg-sub
  :admin/enhanced-entity-spec
  (fn [[_ entity-name record-id]]
    [(rf/subscribe [:admin/entity-spec entity-name])
     (rf/subscribe [:admin/record-by-id entity-name record-id])
     (rf/subscribe [:admin/current-user-role])])
  (fn [[entity-spec record admin-role] [_ _entity-name _record-id]]
    (when (and entity-spec record)
      (filter-and-enhance-fields-for-record entity-spec record admin-role))))

(rf/reg-sub
  :admin/visible-fields-for-record
  (fn [[_ entity-name record-id]]
    (rf/subscribe [:admin/enhanced-entity-spec entity-name record-id]))
  (fn [enhanced-spec [_ entity-name _]]
    ;; Use vector-based configuration for field visibility and ordering
    (let [config @(rf/subscribe [:admin/visible-columns entity-name])
          field-map enhanced-spec]
      (if config
        ;; Use the vector order from config
        (keep #(get field-map %) config)
        ;; Fallback to all fields if no config
        (vals enhanced-spec)))))
