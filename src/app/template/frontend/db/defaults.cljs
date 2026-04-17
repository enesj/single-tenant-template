(ns app.template.frontend.db.defaults
  (:require
    [app.template.frontend.db.schemas :as schemas]
    [app.template.frontend.db.validation :as validation]))

(def default-session-state
  {:loading? false
   :authenticated? false
   :session-valid? true
   :permissions #{}})

(defn make-default-list-state
  []
  {:current-page 1
   ;; Start per-page unset so pages can seed from config defaults
   :per-page nil
   :total-items 0
   :pagination {:current-page 1
                ;; Unset here as well; components fall back to pagination/default-page-size
                :per-page nil
                :total-items 0}
   :sorts []
   :selected-ids #{}
   :filters {}
   :active-filters {}
   :search {:term nil
            :columns []
            :pending? false}
   :column-visibility {}
   :filter-modal {:open? false}
   :batch-edit {:popup {:open? false}
                :inline {:open? false}}
   :loading? false
   :error nil
   :last-refreshed-at nil})

(defn make-default-entity-configs
  "Construct per-entity UI configuration placeholders keyed by entity keyword.
  Keeps toggles nil so defaults fall back to global values while ensuring maps exist."
  [md]
  (let [md-map (schemas/models-data->map md)]
    (into {}
      (for [[entity-key _] md-map
            :let [ek (schemas/normalize-entity-key entity-key)]]
        [ek {:show-timestamps? nil
             :show-edit? nil
             :show-delete? nil
             :show-highlights? nil
             :show-select? nil
             :show-filtering? nil
             :show-pagination? nil
             :controls {}
             :filterable-fields {}
             :visible-columns {}
             :column-order []
             :fields []
             :effective-spec nil
             :search {:term nil :columns []}
             :overrides {}
             :defaults {}}]))))

(defn make-default-entities
  [md]
  (let [md-map (schemas/models-data->map md)
        entities (into {}
                   (for [[k _] md-map
                         :let [ek (schemas/normalize-entity-key k)]]
                     [ek {:data {}
                          :ids []
                          :metadata {:loading? false
                                     :error nil
                                     :last-updated nil}}]))
        first-entity (some-> md-map keys first schemas/normalize-entity-key)]
    (assoc entities
      :ui {:entity-name first-entity
           :current-page nil
           :theme "light"}
      :specs {})))

(def default-db
  {:current-route nil
   :locale :bs
   :controllers []
   :entities {}
   :models-data nil
   :specs {}
   :validation-specs {}
   :session default-session-state
   :forms {}
   :ui {:current-page nil
        :current-entity-type nil
        :theme "light"
        :show-add-form false
        :editing nil
        :editing-id nil
        :filter-modal {:open? false}
        :batch-edit {:active? false}
        :batch-edit-popup {:open? false}
        :batch-edit-inline {}
        :recently-updated {}
        :recently-created {}
        :show-timestamps? false
        :show-edit? true
        :show-delete? true
        :show-highlights? true
        :show-select? false
        :show-filtering? true
        :show-pagination? true
        :defaults {}
        :controls {}
        :entity-configs {}
        :entity-prefs {}
        :lists {}
        :notifications []
        :sidebar {:collapsed? false}
        :modals {}
        :toasts []}
   :entity-fetches {}
   :csrf-token nil})

(defn make-db-with-models-data
  "Creates a complete database with models-data populated"
  [base-db models-data]
  (let [models-map (schemas/models-data->map models-data)
        entity-keys (->> models-map keys (map schemas/normalize-entity-key))
        default-configs (make-default-entity-configs models-map)
        existing-configs (get-in base-db [:ui :entity-configs] {})
        merged-configs (merge-with
                         (fn [defaults overrides]
                           (if (and (map? defaults) (map? overrides))
                             (merge defaults overrides)
                             (or overrides defaults)))
                         default-configs
                         existing-configs)
        lists (into {}
                (for [entity-key entity-keys]
                  [entity-key (make-default-list-state)]))
        first-entity (first entity-keys)]
    (-> base-db
      (assoc :models-data models-data)
      (assoc :entities (make-default-entities models-map))
      (assoc-in [:ui :lists] lists)
      (assoc-in [:ui :entity-configs] merged-configs)
      (update-in [:ui :current-entity-type] #(or % first-entity))
      (validation/debug-validate-critical-state))))
