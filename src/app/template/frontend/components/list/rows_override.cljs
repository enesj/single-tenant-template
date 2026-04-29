(ns app.template.frontend.components.list.rows-override
  (:require
    [app.template.frontend.components.list.overrides :as overrides]))

(defn apply-rows-override-transforms
  [{:keys [rows active-filters sorts entity-name entity-spec server-pagination?]}]
  (if server-pagination?
    (vec rows)
    (overrides/apply-rows-override-transforms {:rows rows
                                               :active-filters active-filters
                                               :sorts sorts
                                               :entity-name entity-name
                                               :entity-spec entity-spec})))

(defn selected-item?
  [selected-ids item]
  (overrides/selected-item? selected-ids item))

(defn apply-selection-visibility
  [rows selected-ids {:keys [show-selected-rows? show-unselected-rows?]}]
  (overrides/apply-selection-visibility rows selected-ids {:show-selected-rows? show-selected-rows?
                                                           :show-unselected-rows? show-unselected-rows?}))