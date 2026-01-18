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
    [clojure.string]
    [app.shared.keywords :as kw]
    [app.shared.model-naming :as model-naming]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.interceptors.persistence :as persistence]
    [re-frame.core :as rf]))

(rf/reg-sub
  ::filterable-fields
  (fn [db [_ entity-key]]
    (let [entity-key (model-naming/ensure-app-keyword entity-key)]
    ;; Read from new path first, fall back to legacy path
    (or (get-in db (paths/entity-prefs-filters-fields entity-key))
      (get-in db (conj (paths/entity-display-settings entity-key) :filterable-fields))
      {}))))

(rf/reg-sub
  ::table-width
  (fn [db [_ entity-key]]
    (let [entity-key (model-naming/ensure-app-keyword entity-key)]
    ;; Read from new path first, fall back to legacy path
    (or (get-in db (paths/entity-prefs-columns-width entity-key))
      (get-in db (conj (paths/entity-display-settings entity-key) :table-width))
      2800))))

(rf/reg-sub
  ::column-order
  (fn [db [_ entity-key]]
    (let [entity-key (model-naming/ensure-app-keyword entity-key)
          normalize (fn [k]
                      (model-naming/ensure-app-keyword (kw/ensure-name k)))
          stored (get-in db (paths/entity-prefs-columns-order entity-key))
          legacy (get-in db (conj (paths/entity-display-settings entity-key) :column-order))
          visible-order (get-in db (paths/entity-prefs-columns-visible-order entity-key))]
      (cond
        (seq stored) (->> stored (keep normalize) vec)
        (seq legacy) (->> legacy (keep normalize) vec)
        (seq visible-order) (->> visible-order (keep normalize) vec)
        :else []))))

(rf/reg-event-db
  ::toggle-field-filtering
  [persistence/persist-entity-prefs]
  (fn [db [_ entity-name field-name]]
    (if (and entity-name field-name)
      (let [entity-key (model-naming/ensure-app-keyword entity-name)
            field-key (model-naming/ensure-app-keyword (kw/ensure-name field-name))
            ;; Read from new path first, fall back to legacy
            current-map (or (get-in db (paths/entity-prefs-filters-fields entity-key))
                          (get-in db (conj (paths/entity-display-settings entity-key) :filterable-fields))
                          {})
            current-setting (get current-map field-key true)
            updated-map (assoc current-map field-key (not current-setting))]
        ;; Write to new path only
        (assoc-in db (paths/entity-prefs-filters-fields entity-key) updated-map))
      db)))

(rf/reg-event-db
  ::toggle-column-visibility
  [persistence/persist-entity-prefs]
  (fn [db [_ entity-name field-name]]
    (if (and entity-name field-name)
      (let [entity-key (model-naming/ensure-app-keyword entity-name)
            field-key (model-naming/ensure-app-keyword (kw/ensure-name field-name))
            route-name (get-in db (paths/current-route-name))
            admin-route? (and route-name
                           (clojure.string/starts-with? (name route-name) "admin"))
            entity-view-options (if admin-route?
                                 (merge
                                   (get-in db [:admin :config :view-options entity-key])
                                   (get-in db [:admin :settings :view-options entity-key]))
                                 (get-in db [:domain :config :view-options entity-key]))
            locked-cols (:column-locks entity-view-options)
              locked? (and (map? locked-cols)
                        (contains? (into #{} (keep model-naming/ensure-app-keyword (keys locked-cols))) field-key))
            table-config (if admin-route?
                           (get-in db [:admin :config :table-columns entity-key])
                           (get-in db [:domain :config :table-columns entity-key]))
              always-visible? (contains? (into #{} (keep model-naming/ensure-app-keyword (or (:always-visible table-config) [])))
                               field-key)
            ;; Read from new path first, fall back to legacy
            current-map (or (get-in db (paths/entity-prefs-columns-visible entity-key))
                          (get-in db (conj (paths/entity-display-settings entity-key) :visible-columns))
                          {})
            current-setting (get current-map field-key true)
            updated-map (assoc current-map field-key (not current-setting))]
        (if (or locked? always-visible?)
          db
          ;; Write to new path only
          (assoc-in db (paths/entity-prefs-columns-visible entity-key) updated-map)))
      db)))

(rf/reg-event-db
  ::update-table-width
  [persistence/persist-entity-prefs]
  (fn [db [_ entity-name width]]
    (if (and entity-name width)
      (let [entity-key (model-naming/ensure-app-keyword entity-name)
            width-num (if (string? width) (js/parseInt width) width)]
        ;; Write to new path only
        (assoc-in db (paths/entity-prefs-columns-width entity-key) width-num))
      db)))

(rf/reg-event-db
  ::set-column-order
  [persistence/persist-entity-prefs]
  (fn [db [_ entity-name column-order]]
    (if entity-name
      (let [entity-key (model-naming/ensure-app-keyword entity-name)
            normalize (fn [k]
                        (model-naming/ensure-app-keyword (kw/ensure-name k)))
            normalized (->> (or column-order []) (keep normalize) vec)]
        (assoc-in db (paths/entity-prefs-columns-order entity-key) normalized))
      db)))
