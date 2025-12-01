(ns app.template.frontend.events.list.settings
  "Shared list view settings events for template tables.

   Provides reusable event handlers for toggling column visibility,
   filterable fields, and table width across template-driven UIs."
  (:require
    [re-frame.core :as rf]))

(rf/reg-sub
  ::filterable-fields
  (fn [db [_ entity-key]]
    (get-in db [:ui :entity-configs entity-key :filterable-fields] {})))

(rf/reg-sub
  ::table-width
  (fn [db [_ entity-key]]
    (get-in db [:ui :entity-configs entity-key :table-width] 2800)))

(rf/reg-event-db
  ::toggle-field-filtering
  (fn [db [_ entity-name field-name]]
    (if (and entity-name field-name)
      (let [entity-key (if (keyword? entity-name) entity-name (keyword entity-name))
            field-key (if (keyword? field-name) field-name (keyword field-name))
            current-map (get-in db [:ui :entity-configs entity-key :filterable-fields] {})
            current-setting (get current-map field-key true)
            updated-map (assoc current-map field-key (not current-setting))]
        (assoc-in db [:ui :entity-configs entity-key :filterable-fields] updated-map))
      db)))

(rf/reg-event-db
  ::toggle-column-visibility
  (fn [db [_ entity-name field-name]]
    (if (and entity-name field-name)
      (let [entity-key (if (keyword? entity-name) entity-name (keyword entity-name))
            field-key (if (keyword? field-name) field-name (keyword field-name))
            current-map (or (get-in db [:ui :entity-configs entity-key :visible-columns]) {})
            current-setting (get current-map field-key true)
            updated-map (assoc current-map field-key (not current-setting))]
        (assoc-in db [:ui :entity-configs entity-key :visible-columns] updated-map))
      db)))

(rf/reg-event-db
  ::update-table-width
  (fn [db [_ entity-name width]]
    (if (and entity-name width)
      (let [entity-key (if (keyword? entity-name) entity-name (keyword entity-name))
            width-num (if (string? width) (js/parseInt width) width)]
        (assoc-in db [:ui :entity-configs entity-key :table-width] width-num))
      db)))
