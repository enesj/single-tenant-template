(ns app.template.frontend.utils.shared
  "Generic utilities for shared component functionality across all application modules.

   This namespace provides utilities that work with the shared components:
   - Entity management helpers
   - State management patterns
   - Event dispatch utilities
   - Common configuration helpers"

  (:require
    [app.template.frontend.subs.entity :as entity-subs]
    [app.template.frontend.subs.list :as list-subs]
    [app.template.frontend.subs.ui :as ui-subs]
    [app.shared.keywords :as kw]
    [taoensso.timbre :as log]
    [re-frame.core :refer [dispatch]]
    [uix.core :refer [use-effect]]
    [uix.re-frame :refer [use-subscribe]]))

;; Entity Management Utilities

(defn- module-key
  "Create a keyword namespaced with the given module prefix."
  [module-prefix suffix]
  (keyword (kw/ensure-name module-prefix) suffix))

(defn- admin-entity-spec-sub
  [entity-name]
  [:admin/entity-specs-by-name entity-name])

(defn use-entity-state
  "Generic entity state management hook.

   Returns a map with:
   - :loading? - boolean indicating if entity data is loading
   - :error - error message if any

   - :success-message - success message if any
   - :selected-ids - collection of selected entity IDs
   - :entity-spec - entity specification from configs
   - :display-settings - display settings from UI state

   Parameters:
   - entity-name - keyword for entity type
   - module-prefix - module prefix for subscriptions (e.g., :admin, :tenant)"
  [entity-name module-prefix]

  (let [entity-name-str (kw/ensure-name entity-name)
        loading? (use-subscribe [(module-key module-prefix (str entity-name-str "-loading?"))])
        error (use-subscribe [(module-key module-prefix (str entity-name-str "-error"))])
        success-message (use-subscribe [(module-key module-prefix "success-message")])
        selected-ids (use-subscribe [::list-subs/selected-ids entity-name])
        entity-spec (if (= module-prefix :admin)
                      (use-subscribe (admin-entity-spec-sub entity-name))
                      (use-subscribe [(module-key module-prefix "entity-specs-by-name") entity-name]))
        display-settings (use-subscribe [::ui-subs/entity-display-settings entity-name])]
    {:loading? loading?
     :error error
     :success-message success-message
     :selected-ids selected-ids
     :entity-spec entity-spec
     :display-settings display-settings}))

(defn use-entity-spec
  "Gets entity specification and display settings for an entity type.

   Parameters:
   - entity-name - keyword for entity type
   - module-prefix - module prefix for subscriptions (e.g., :admin, :tenant)

   Returns a map with :entity-spec and :display-settings."
  [entity-name module-prefix]
  (let [entity-spec (if (= module-prefix :admin)
                      (use-subscribe (admin-entity-spec-sub entity-name))
                      (use-subscribe [(module-key module-prefix "entity-specs-by-name") entity-name]))
        display-settings (use-subscribe [::ui-subs/entity-display-settings entity-name])]
    {:entity-spec entity-spec
     :display-settings display-settings}))

(defn use-paginated-entities
  "Get paginated entities for an entity type.

   Parameters:
   - entity-name - keyword for entity type
   - module-prefix - module prefix for subscriptions"
  [entity-name _module-prefix]

  (use-subscribe [::entity-subs/paginated-entities entity-name]))

;; Event Dispatch Utilities

(defn dispatch-entity-operation
  "Standardized entity operation dispatch.

   Parameters:
   - module-prefix - module prefix (e.g., :admin, :tenant)
   - entity-type - keyword for entity type
   - operation-type - keyword for operation type (e.g., :fetch, :create, :update, :delete)
   - operation-data - data for the operation
   - operation-id - optional ID for specific entity operations"
  ([module-prefix entity-type operation-type operation-data]
   (let [event-name (keyword (kw/ensure-name module-prefix)
                      (str (kw/ensure-name entity-type)
                        "-" (kw/ensure-name operation-type)))]
     (dispatch [event-name operation-data])))

  ([module-prefix entity-type operation-type operation-data operation-id]
   (let [event-name (keyword (kw/ensure-name module-prefix)
                      (str (kw/ensure-name entity-type)
                        "-" (kw/ensure-name operation-type)))]
     (dispatch [event-name operation-id operation-data]))))

(defn dispatch-batch-operation
  "Standardized batch operation dispatch.

   Parameters:
   - module-prefix - module prefix (e.g., :admin, :tenant)
   - entity-type - keyword for entity type
   - operation-type - keyword for operation type (e.g., :status, :role, :tier)
   - operation-value - value for the operation
   - ids - collection of entity IDs to operate on"
  [module-prefix entity-type operation-type operation-value ids]

  (let [event-name (keyword (kw/ensure-name module-prefix)
                     (str "bulk-update-"
                       (kw/ensure-name entity-type)
                       "-"
                       (kw/ensure-name operation-type)))]
    (dispatch [event-name ids operation-value])))

;; Key Generation Utilities

(defn get-entity-loading-key
  "Gets the standard loading key for an entity type.

   Parameters:
   - module-prefix - module prefix (e.g., :admin, :tenant)
   - entity-name - entity type keyword"
  [module-prefix entity-name]

  (keyword (kw/ensure-name module-prefix)
    (str (kw/ensure-name entity-name) "-loading?")))

(defn get-entity-error-key
  "Gets the standard error key for an entity type.

   Parameters:
   - module-prefix - module prefix (e.g., :admin, :tenant)
   - entity-name - entity type keyword"
  [module-prefix entity-name]

  (keyword (kw/ensure-name module-prefix)
    (str (kw/ensure-name entity-name) "-error")))

(defn get-batch-actions-key
  "Gets the standard batch actions visibility key for an entity type.

   Parameters:
   - module-prefix - module prefix (e.g., :admin, :tenant)
   - entity-name - entity type keyword"
  [module-prefix entity-name]

  (keyword (kw/ensure-name module-prefix)
    (str "batch-" (kw/ensure-name entity-name) "-actions-visible?")))

(defn get-batch-selected-key
  "Gets the standard batch selected IDs key for an entity type.

   Parameters:
   - module-prefix - module prefix (e.g., :admin, :tenant)
   - entity-name - entity type keyword"
  [module-prefix entity-name]

  (keyword (kw/ensure-name module-prefix)
    (str "batch-selected-" (kw/ensure-name entity-name) "-ids")))

;; Common Configuration Helpers

(defn default-list-display-settings
  "Default display settings for list components."
  []
  {:show-edit? true
   :show-delete? true
   :show-filtering? true
   :show-select? true
   :show-timestamps? true
   :show-highlights? true
   :show-pagination? true
   :show-add-button? true
   :per-page 25
   :page-size 25})

(defn merge-display-settings
  "Merge default display settings with custom overrides.

   Parameters:
   - defaults - default settings map
   - overrides - custom settings to merge"
  [defaults overrides]

  (merge defaults overrides))

;; Entity ID Utilities

(defn extract-entity-id
  "Extract entity ID from entity data.

   Handles both namespaced (:table/id) and non-namespaced (:id) keys."
  [entity]

  (or (:id entity)
    (some (fn [[k v]] (when (and (keyword? k) (= (name k) "id")) v)) entity)))

(defn extract-entity-ids
  "Extract entity IDs from collection of entities.

   Returns only non-nil IDs."
  [entities]

  (->> entities
    (map extract-entity-id)
    (filter some?)
    vec))

;; Selection Utilities

(defn select-all-entities
  "Select all entities in a collection.

   Parameters:
   - entities - collection of entities
   - on-select - function to call with selected IDs"
  [entities on-select]

  (let [ids (extract-entity-ids entities)]
    (when (seq ids)
      (on-select ids))))

(defn clear-selection
  "Clear current selection.

   Parameters:
   - on-clear - function to call to clear selection"
  [on-clear]

  (on-clear []))

;; Validation Utilities

(defn validate-entity-operation
  "Validate that an entity operation can be performed.

   Returns {:valid? boolean :error string}"
  [entity _operation-type]

  (cond
    (nil? entity)
    {:valid? false :error "Entity cannot be nil"}

    (nil? (extract-entity-id entity))
    {:valid? false :error "Entity must have a valid ID"}

    :else
    {:valid? true}))

;; Effect Utilities

(defn use-entity-initialization
  "Hook to run initialization and cleanup logic for entity screens.

   Parameters:
   - entity-name - keyword identifying the entity (used in logs for context)
   - init-fn - function invoked on mount; may optionally return a cleanup fn
   - cleanup-fn - function invoked when component unmounts"
  [_entity-name init-fn cleanup-fn]
  (use-effect
    (fn []
      (let [maybe-dispose (when (fn? init-fn) (init-fn))]
        (fn []
          (when (fn? cleanup-fn)
            (cleanup-fn))
          (when (fn? maybe-dispose)
            (maybe-dispose)))))
    [init-fn cleanup-fn]))
