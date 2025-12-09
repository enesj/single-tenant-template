(ns app.template.frontend.events.list.settings
  "Shared list view settings events for template tables.

   USER PREFERENCES PATH STRUCTURE:
   ================================
   All user preferences are stored under [:ui :entity-prefs <entity>]:
   
   - [:ui :entity-prefs <entity> :display :show-*]     Display toggles
   - [:ui :entity-prefs <entity> :columns :visible]    Visible columns map
   - [:ui :entity-prefs <entity> :columns :width]      Table width
   - [:ui :entity-prefs <entity> :filters :fields]     Filterable fields map
   
   LEGACY PATHS (deprecated, will be removed):
   - [:ui :entity-configs <entity> :filterable-fields]
   - [:ui :entity-configs <entity> :visible-columns]
   - [:ui :entity-configs <entity> :table-width]
   
   Events read from both paths during migration, write to new path only."
  (:require
    [app.template.frontend.interceptors.persistence :as persistence]
    [re-frame.core :as rf]))

(rf/reg-sub
  ::filterable-fields
  (fn [db [_ entity-key]]
    ;; Read from new path first, fall back to legacy path
    (or (get-in db [:ui :entity-prefs entity-key :filters :fields])
      (get-in db [:ui :entity-configs entity-key :filterable-fields])
      {})))

(rf/reg-sub
  ::table-width
  (fn [db [_ entity-key]]
    ;; Read from new path first, fall back to legacy path
    (or (get-in db [:ui :entity-prefs entity-key :columns :width])
      (get-in db [:ui :entity-configs entity-key :table-width])
      2800)))

(rf/reg-event-db
  ::toggle-field-filtering
  [persistence/persist-entity-prefs]
  (fn [db [_ entity-name field-name]]
    (if (and entity-name field-name)
      (let [entity-key (if (keyword? entity-name) entity-name (keyword entity-name))
            field-key (if (keyword? field-name) field-name (keyword field-name))
            ;; Read from new path first, fall back to legacy
            current-map (or (get-in db [:ui :entity-prefs entity-key :filters :fields])
                          (get-in db [:ui :entity-configs entity-key :filterable-fields])
                          {})
            current-setting (get current-map field-key true)
            updated-map (assoc current-map field-key (not current-setting))]
        ;; Write to new path only
        (assoc-in db [:ui :entity-prefs entity-key :filters :fields] updated-map))
      db)))

(rf/reg-event-db
  ::toggle-column-visibility
  [persistence/persist-entity-prefs]
  (fn [db [_ entity-name field-name]]
    (if (and entity-name field-name)
      (let [entity-key (if (keyword? entity-name) entity-name (keyword entity-name))
            field-key (if (keyword? field-name) field-name (keyword field-name))
            ;; Read from new path first, fall back to legacy
            current-map (or (get-in db [:ui :entity-prefs entity-key :columns :visible])
                          (get-in db [:ui :entity-configs entity-key :visible-columns])
                          {})
            current-setting (get current-map field-key true)
            updated-map (assoc current-map field-key (not current-setting))]
        ;; Write to new path only
        (assoc-in db [:ui :entity-prefs entity-key :columns :visible] updated-map))
      db)))

(rf/reg-event-db
  ::update-table-width
  [persistence/persist-entity-prefs]
  (fn [db [_ entity-name width]]
    (if (and entity-name width)
      (let [entity-key (if (keyword? entity-name) entity-name (keyword entity-name))
            width-num (if (string? width) (js/parseInt width) width)]
        ;; Write to new path only
        (assoc-in db [:ui :entity-prefs entity-key :columns :width] width-num))
      db)))
