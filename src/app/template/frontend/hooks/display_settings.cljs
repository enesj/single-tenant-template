(ns app.template.frontend.hooks.display-settings
  "Unified hook for display settings that provides a single source of truth.
   
   This hook encapsulates ALL display settings merging logic:
   - Entity-specific configs from app-db
   - Default settings
   - Hardcoded view-options from admin config
   
   Components use this hook directly instead of receiving settings via props,
   ensuring reactive updates when settings change."
  (:require
    [app.template.frontend.subs.ui :as ui-subs]
    [uix.re-frame :refer [use-subscribe]]))

(defn use-display-settings
  "Returns all merged display settings for an entity.
   
   This is the single authoritative hook for display settings.
   Components should call this directly instead of receiving settings as props.
   
   Returns a map with:
   - :show-select?      - Show row selection checkboxes
   - :show-edit?        - Show edit buttons on rows
   - :show-delete?      - Show delete buttons on rows
   - :show-timestamps?  - Show created_at/updated_at columns
   - :show-highlights?  - Show row highlighting for recent changes
   - :show-filtering?   - Show filter controls
   - :show-pagination?  - Show pagination controls
   - :show-batch-edit?  - Show batch edit functionality
   - :show-add-button?  - Show add new item button
   - :controls          - Map of control visibility settings
   
   Usage:
     (let [{:keys [show-select? show-edit?]} (use-display-settings :users)]
       ...)"
  [entity-name]
  (let [entity-key (if (keyword? entity-name) entity-name (keyword entity-name))
        ;; Subscribe to entity display settings (already handles all merging logic)
        settings (use-subscribe [::ui-subs/entity-display-settings entity-key])]
    settings))

(defn use-show-select?
  "Returns just the show-select? setting for an entity.
   Convenience hook for components that only need selection visibility."
  [entity-name]
  (:show-select? (use-display-settings entity-name)))

(defn use-show-edit?
  "Returns just the show-edit? setting for an entity.
   Convenience hook for components that only need edit button visibility."
  [entity-name]
  (:show-edit? (use-display-settings entity-name)))

(defn use-show-delete?
  "Returns just the show-delete? setting for an entity.
   Convenience hook for components that only need delete button visibility."
  [entity-name]
  (:show-delete? (use-display-settings entity-name)))

(defn use-show-timestamps?
  "Returns just the show-timestamps? setting for an entity.
   Convenience hook for components that only need timestamp column visibility."
  [entity-name]
  (:show-timestamps? (use-display-settings entity-name)))

(defn use-show-highlights?
  "Returns just the show-highlights? setting for an entity.
   Convenience hook for components that only need highlight visibility."
  [entity-name]
  (:show-highlights? (use-display-settings entity-name)))

(defn use-action-visibility
  "Returns edit and delete visibility together.
   Convenience hook for action button components.
   
   Returns: {:show-edit? bool, :show-delete? bool}"
  [entity-name]
  (let [settings (use-display-settings entity-name)]
    {:show-edit? (:show-edit? settings)
     :show-delete? (:show-delete? settings)}))
