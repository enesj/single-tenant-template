(ns app.template.frontend.utils.column-config
  "Simplified helpers for vector-based column configuration."
  (:require
    [app.admin.frontend.config.loader :as admin-config]
    [app.shared.keywords :as kw]
    [app.shared.model-naming :as model-naming]
    [app.template.frontend.events.list.settings :as settings-events]))

(defn vector-config?
  "Return true when vector-based admin configuration is available for the entity."
  [entity-kw]
  (try
    (boolean (and entity-kw (admin-config/has-vector-config? entity-kw)))
    (catch :default _ false)))


(defn visible-columns-source
  "Return the subscription vector for visible columns.

  NOTE: We intentionally use the unified `:app.template.frontend.subs.ui/visible-columns`
  subscription in both modes so policy defaults/locks apply consistently.
  `vector-mode?` is still accepted so callers/tests can gate other vector-config behavior."
  [_vector-mode? entity-kw]
  [:app.template.frontend.subs.ui/visible-columns entity-kw])

(defn get-visible-columns
  "Get visible columns as a boolean map.

  In legacy/template mode, the source is already a boolean map.

  In vector-config mode, the source may be either:
  - a boolean map (preferred; unified subs already resolved), or
  - a vector of column keys (older admin vector-config shape)."
  [vector-mode? entity-kw raw-value]
  (if-not vector-mode?
    ;; Legacy/template mode keeps boolean-map as-is
    (or raw-value {})
    (cond
      (map? raw-value)
      raw-value

      ;; Vector-config mode: convert vector of keys -> boolean map for all available columns
      :else
      (let [normalize (fn [k]
                        (model-naming/ensure-app-keyword (kw/ensure-name k)))
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
        adjusted))))

(defn toggle-column-event
  "Dispatch vector for toggling a column's visibility."
  [entity-kw field-id]
  [:app.admin.frontend.events.config/toggle-column-visibility entity-kw field-id])

(defn toggle-filter-event
  "Dispatch vector for toggling a column's filterable state."
  [entity-kw field-id]
  [::settings-events/toggle-field-filtering entity-kw field-id])

(defn update-table-width-event
  "Dispatch vector for updating table width."
  [entity-kw width]
  [::settings-events/update-table-width entity-kw width])

(defn normalize-column-key
  "Normalize a column key to a simple app keyword (no namespace)."
  [k]
  (when k
    (model-naming/ensure-app-keyword (kw/ensure-name k))))

(defn field->id
  "Extract a field id from a field spec, keyword, or string." 
  [field]
  (cond
    (map? field) (:id field)
    (keyword? field) field
    (string? field) (keyword field)
    :else field))

(defn order-fields
  "Order entity fields based on a user-defined column order.

  Unknown fields keep their original relative order." 
  [fields column-order]
  (let [normalized-order (->> (or column-order []) (keep normalize-column-key) vec)
        order-index (zipmap normalized-order (range))]
    (if (seq normalized-order)
      (->> fields
        (map-indexed (fn [idx field]
                       {:field field
                        :index idx
                        :order (get order-index (normalize-column-key (field->id field)) 999999)}))
        (sort-by (fn [{:keys [order index]}] [order index]))
        (mapv :field))
      (vec fields))))

(defn reorder-vector
  "Move an item inside a vector from one index to another." 
  [items from-index to-index]
  (let [items (vec items)
        item (nth items from-index)
        without (vec (concat (subvec items 0 from-index)
                       (subvec items (inc from-index))))
  insert-index (-> to-index (max 0) (min (count without)))]
    (vec (concat (subvec without 0 insert-index)
           [item]
           (subvec without insert-index)))))
