(ns app.template.frontend.utils.column-config
  "Simplified helpers for vector-based column configuration."
  (:require
    [app.admin.frontend.config.loader :as admin-config]
    [app.admin.frontend.subs.config :as admin-subs]
    [app.template.frontend.events.list.settings :as settings-events]))

(defn vector-config?
  "Return true when vector-based admin configuration is available for the entity."
  [entity-kw]
  (try
    (boolean (and entity-kw (admin-config/has-vector-config? entity-kw)))
    (catch :default _ false)))

(defn visible-columns-source
  "Return the appropriate subscription vector for visible columns."
  [vector-mode? entity-kw]
  (if vector-mode?
    [::admin-subs/visible-columns entity-kw]
    [:app.template.frontend.subs.ui/visible-columns entity-kw]))

(defn get-visible-columns
  "Get visible columns directly as vector (no conversion needed!)"
  [vector-mode? entity-kw raw-value]
  (if-not vector-mode?
    ;; Legacy/template mode keeps boolean-map as-is
    (or raw-value {})
    ;; Vector-config mode: convert vector of keys -> boolean map for all available columns
    (let [normalize (fn [k]
                      (cond
                        (nil? k) nil
                        (keyword? k) k
                        (string? k) (keyword k)
                        :else (keyword (str k))))
          visible-set (into #{} (keep normalize) (or raw-value []))
          available (or (admin-config/get-available-columns entity-kw) [])
          ;; Build a complete boolean map so table/rows can resolve definitively
          base-map (into {}
                     (map (fn [k]
                            (let [kk (normalize k)]
                              [kk (contains? visible-set kk)]))
                       available))
          ;; Ensure always-visible columns cannot be false
          adjusted (reduce (fn [m k]
                             (if (admin-config/is-always-visible? entity-kw k)
                               (assoc m k true)
                               m))
                     base-map
                     (keep normalize available))]
      adjusted)))

(defn toggle-column-event
  "Dispatch vector for toggling a column's visibility."
  [entity-kw field-id]
  [:admin/toggle-column-visibility entity-kw field-id])

(defn toggle-filter-event
  "Dispatch vector for toggling a column's filterable state."
  [entity-kw field-id]
  [::settings-events/toggle-field-filtering entity-kw field-id])

(defn update-table-width-event
  "Dispatch vector for updating table width."
  [entity-kw width]
  [::settings-events/update-table-width entity-kw width])
