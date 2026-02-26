(ns app.template.frontend.components.batch-edit.utils
  "Utility functions for batch editing operations"
  (:require
    [app.shared.keywords :as kw]))

(defn find-identical-values
  "Find fields that have identical values across all selected items.
   Returns a map of field-key -> common-value for fields where all items have the same value.
   For fields with different values, returns nil to indicate mixed values."
  [items entity-spec & [entity-name]]
  (when (and (seq items) (seq entity-spec))
    (let [field-keys (map #(keyword (:id %)) entity-spec)
          ;; Determine the namespace prefix based on entity-name or infer from data
          namespace-prefix (or (when entity-name (kw/ensure-name entity-name))
                              ;; Fallback: try to infer from the first item's keys
                             (when-let [first-item (first items)]
                               (some #(when (and (keyword? %) (namespace %))
                                        (namespace %))
                                 (keys first-item)))
                              ;; Default fallback
                             "users")]

      ;; Debug raw data structure to understand key formats

      (->> field-keys
        (reduce (fn [acc field-key]
                  (let [field-values (map #(get % field-key) items)
                           ;; Use dynamic namespace prefix
                        namespaced-key (keyword (str namespace-prefix "/" (kw/ensure-name field-key)))
                        namespaced-values (map #(get % namespaced-key) items)
                        unique-values (set field-values)
                        unique-namespaced (set namespaced-values)
                        final-values (if (and (= 1 (count unique-values))
                                           (nil? (first unique-values))
                                           (= 1 (count unique-namespaced))
                                           (not (nil? (first unique-namespaced))))
                                            ;; Use namespaced value if regular is null
                                       namespaced-values
                                            ;; Use regular values
                                       field-values)
                        final-unique (set final-values)]

                    (cond
                         ;; If all values are identical, use the common value
                      (= 1 (count final-unique))
                      (assoc acc field-key (first final-unique))

                         ;; If there are multiple different values, return nil for mixed
                      :else
                      (assoc acc field-key nil))))
          {})))))




