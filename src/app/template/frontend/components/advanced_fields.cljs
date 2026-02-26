(ns app.template.frontend.components.advanced-fields
  "Shared advanced field components with template integration and DaisyUI styling"
  (:require
    [clojure.string :as str]
    [uix.core :refer [$ defui]]))

;; ========================================================================
;; Advanced Field Display Components
;; ========================================================================

(defn status->badge-class
  "Map a status value to a DaisyUI badge variant class.

  Returns a *variant* class (e.g. `ds-badge-success`) or `default-class`.
  Note: this intentionally does not include the base `ds-badge` class.

  Supported inputs: string/keyword/symbol/number/boolean (anything string-coercible)."
  ([status]
   (status->badge-class status {}))
  ([status {:keys [default-class]
            :or {default-class "ds-badge-neutral"}}]
   (let [status-str (cond
                      (nil? status) nil
                      (string? status) status
                      (keyword? status) (name status)
                      (symbol? status) (name status)
                      :else (str status))
         status-lower (some-> status-str str/lower-case)]
     (cond
       (contains? #{"active" "verified" "success" "complete"} status-lower)
       "ds-badge-success"

       (contains? #{"inactive" "trialing" "unverified"} status-lower)
       "ds-badge-warning"

       (contains? #{"suspended" "cancelled" "canceled" "failed" "error"} status-lower)
       "ds-badge-error"

       (contains? #{"invited" "pending" "in-progress"} status-lower)
       "ds-badge-info"

       (contains? #{"archived"} status-lower)
       "ds-badge-ghost"

       :else
       default-class))))

(defui status-badge
  "Renders a status as a colored badge using DaisyUI"
  [{:keys [text class]}]
  ($ :span {:class (str "ds-badge " class)} text))
