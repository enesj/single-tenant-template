(ns app.template.frontend.utils.formatting
  "Reusable formatting utilities for multi-tenant applications")

;; ============================================================================
;; Currency Formatting
;; ============================================================================

(defn format-currency
  "Format a number as currency with dollar sign."
  [value]
  (str "$" (or value 0)))

;; ============================================================================
;; Percentage Formatting
;; ============================================================================

(defn format-percentage
  "Format a number as percentage with specified precision."
  [value & {:keys [precision] :or {precision 1}}]
  (str (.toFixed (js/Number value) precision) "%"))

;; ============================================================================
;; Date Formatting
;; ============================================================================

(defn format-date-month-year
  "Format a date string as abbreviated month and year (e.g., 'Jan 2024')."
  [date-str]
  (when date-str
    (.toLocaleDateString (js/Date. date-str) "en-US" #js {:month "short" :year "numeric"})))

;; ============================================================================
;; Status Color Utilities
;; ============================================================================

(defn get-status-color
  "Get appropriate color class based on value thresholds.

   Parameters:
   - value - The numeric value to evaluate
   - thresholds - Map with :warning and :error thresholds
   - colors - Map with color classes for different states"
  [value thresholds colors]
  (cond
    (and (:error thresholds) (>= value (:error thresholds)))
    (:error colors "ds-bg-error")

    (and (:warning thresholds) (>= value (:warning thresholds)))
    (:warning colors "ds-bg-warning")

    :else
    (:success colors "ds-bg-success")))
