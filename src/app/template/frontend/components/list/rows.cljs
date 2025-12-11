(ns app.template.frontend.components.list.rows
  "Row rendering components for list views.
   
   This module uses reactive cell components from the cells module for selection
   and actions. Components are re-exported for backward compatibility."
  (:require
   [app.frontend.utils.id :as id-utils]
   [app.shared.keywords :as kw]
   [app.template.frontend.components.form :refer [form]]
   [app.template.frontend.components.list.cells :as cells]
   [app.template.frontend.components.list.fields :refer [get-field-display-value]] ;; Import reactive cell components from the cells module
   [app.template.frontend.events.form :as form-events]
   [app.template.frontend.events.list.crud :as crud-events]
   [re-frame.core :as rf]
   [uix.core :as uix :refer [$]]))

;; =============================================================================
;; Re-exports from cells module for backward compatibility
;; =============================================================================

;; Re-export select-checkbox for existing code that imports from rows
(def select-checkbox cells/select-checkbox)

;; Re-export select-all-checkbox for existing code that imports from rows
(def select-all-checkbox cells/select-all-checkbox)

;; Re-export reactive-selection-cell for existing code that imports from rows
(def reactive-selection-cell cells/reactive-selection-cell)

;; Re-export action-buttons for existing code that imports from rows
(def action-buttons cells/action-buttons)

;; =============================================================================
;; Row Content Generation
;; =============================================================================

(defn- row-content
  "Generates the content for a table row."
  [{:keys [entity-spec item show-timestamps? actions visible-columns entity-name selected-ids on-select-change]}]

  (let [;; Safely get field values from entity-spec - FIXED: Check :fields key first
        entity-fields (cond
                        (and (map? entity-spec) (:fields entity-spec))
                        (let [fields (:fields entity-spec)]
                          (if (sequential? fields)
                            fields
                            []))

                        (map? entity-spec) (->> (vals entity-spec)
                                             (filter map?)  ; Only keep maps
                                             (filter #(contains? % :id)) ; Only keep field definitions with :id
                                             ;; CRITICAL FIX: Sort by display-order to match header ordering
                                             (sort-by #(or (get-in % [:admin :display-order]) 999)))
                        (sequential? entity-spec) entity-spec
                        :else [])

        ;; Filter out the raw timestamp fields to avoid duplication
        filtered-entity-fields (remove (fn [field]
                                         (let [field-id (keyword (:id field))]
                                           (#{:created-at :updated-at} field-id)))
                                 entity-fields)

        ;; Process field values, but only for visible columns
        field-values (mapv (fn [field]
                             (let [field-id (keyword (:id field))
                                   ;; Use vector-config driven visibility map directly
                                   is-column-visible? (let [user-setting (get visible-columns field-id ::not-found)]
                                                        (if (not= user-setting ::not-found) user-setting true))]
                               ;; Only include this field if the column is visible
                               (when is-column-visible?
                                 (let [;; Try both namespaced and unnamespaced field lookup
                                       value (or (get item field-id)
                                               (get item (keyword (str (kw/ensure-name entity-name)
                                                                    "/"
                                                                    (kw/ensure-name field-id)))))]
                                   (get-field-display-value field value)))))
                       filtered-entity-fields)

        ;; Filter out nil values (from columns that should be hidden)
        filtered-field-values (filterv some? field-values)

        ;; Add timestamp values if requested, but only if they're visible
        timestamps (when show-timestamps?
                     (let [created-key :created-at
                           updated-key :updated-at
                           legacy-map {created-key :created-at
                                       updated-key :updated-at}
                           sentinel ::not-found
                           resolve-setting (fn [settings key]
                                             (let [legacy (get legacy-map key)
                                                   current (get settings key sentinel)
                                                   legacy-val (if legacy (get settings legacy sentinel) sentinel)]
                                               (cond
                                                 (not= current sentinel) current
                                                 (not= legacy-val sentinel) legacy-val
                                                 :else nil)))
                           column-visible? (fn [key]
                                             (let [value (resolve-setting visible-columns key)]
                                               (if (nil? value) true value)))
                           fetch-value (fn [entity key]
                                         (let [legacy (get legacy-map key)
                                               direct (or (get entity key)
                                                        (get entity (keyword (str (kw/ensure-name entity-name)
                                                                               "/"
                                                                               (kw/ensure-name key)))))
                                               legacy-val (when legacy
                                                            (or (get entity legacy)
                                                              (get entity (keyword (str (kw/ensure-name entity-name)
                                                                                     "/"
                                                                                     (kw/ensure-name legacy))))))]
                                           (or direct legacy-val)))
                           render-timestamp (fn [value]
                                              ($ :span {:class "whitespace-nowrap"}
                                                (when value
                                                  (let [date (js/Date. value)
                                                        month (.toLocaleString date "en-US" #js {:month "short"})
                                                        day (.getDate date)
                                                        hours (.getHours date)
                                                        minutes (.getMinutes date)
                                                        formatted-time (str (when (< hours 10) "0") hours ":" (when (< minutes 10) "0") minutes)]
                                                    ($ :div
                                                      ($ :span {:class "text-primary"} (str month " " day))
                                                      ($ :span {:class "ml-1"} formatted-time))))))]
                       [(when (column-visible? created-key)
                          (render-timestamp (fetch-value item created-key)))
                        (when (column-visible? updated-key)
                          (render-timestamp (fetch-value item updated-key)))]))

        ;; Filter out nil values from timestamps (from timestamps that should be hidden)
        filtered-timestamps (filterv some? timestamps)]

    ;; Return all cell values in the same order as headers: select checkbox (if enabled), base fields, timestamps, actions
    ;; Use the reactive-selection-cell component which subscribes to show-select? directly
    ;; This ensures the checkbox visibility updates immediately when the user toggles the setting
    (vec (concat [(fn [] ($ cells/reactive-selection-cell
                           {:entity-name entity-name
                            :item item
                            :selected-ids selected-ids
                            :on-select-change on-select-change}))]
           filtered-field-values
           filtered-timestamps
           [(fn [] actions)]))))

;; =============================================================================
;; Row Rendering
;; =============================================================================

(defn render-row
  "Renders a single row in the list, either as a form or as a table row."
  [{:keys [entity-spec editing set-editing! entity-name recently-updated-ids recently-created-ids selected-ids on-select-change visible-columns] :as props} {:keys [item]}]
  (let [props-map (js->clj props :keywordize-keys true)
        ;; Use the explicit entity-spec (vector-config) for table rows.
        ;; Do not fall back to form-entity-spec to avoid label drift.
        effective-entity-spec entity-spec

        ;; FIX: Keep item as ClojureScript data structure instead of converting to JS
        item-clj (if (map? item) item (js->clj item :keywordize-keys true))

        row-props (merge props-map
                    {:initial-values (into {}
                                       (map (fn [[k v]]
                                              ;; Convert namespaced keys to simple keys for form fields
                                              (let [simple-key (if (and (keyword? k) (namespace k))
                                                                 (keyword (name k))
                                                                 k)]
                                                [simple-key v]))
                                         item-clj))
                     :entity-spec effective-entity-spec
                     :on-cancel #(do
                                   (rf/dispatch [::crud-events/clear-error (keyword (:entity-name props))])
                                   (rf/dispatch [::form-events/clear-form-errors (keyword (:entity-name props))])
                                   ;; Clear the editing state to hide the inline edit form
                                   (set-editing! nil))
                     :item item-clj})
        ;; Use the generic ID extraction utility
        item-id (id-utils/extract-entity-id item-clj)
        item-id-int (if (string? item-id) (js/parseInt item-id) item-id)
        item-id-str (str item-id)

        ;; Helper function to check if a string looks like a UUID
        is-uuid? (fn [s] (and (string? s) (re-matches #"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$" s)))

        ;; Smart comparison that handles both UUIDs and numeric IDs
        ;; IMPORTANT: Only show edit form if both item-id and editing are non-null and equal
        is-editing (and (some? item-id)
                     (some? editing)
                     (cond
                       ;; If both are UUIDs, compare as strings
                       (and (is-uuid? item-id) (is-uuid? editing))
                       (= item-id editing)

                       ;; If both are numbers, compare as numbers
                       (and (number? item-id) (number? editing))
                       (= item-id editing)

                       ;; If one is string and one is number, try to convert and compare
                       (and (string? item-id) (number? editing))
                       (= (js/parseInt item-id) editing)

                       (and (number? item-id) (string? editing))
                       (= item-id (js/parseInt editing))

                       ;; If both are strings but not UUIDs, compare as strings
                       (and (string? item-id) (string? editing))
                       (= item-id editing)

                       ;; Default case: direct comparison
                       :else
                       (= item-id editing)))

        ;; Ensure recently-updated-ids and recently-created-ids are sets, not nil
        updated-ids (or recently-updated-ids #{})
        created-ids (or recently-created-ids #{})

        ;; Check if this item ID is in the set of recently updated or created IDs
        ;; Try both string and integer versions for comparison
        recently-updated? (and (set? updated-ids)
                            (or (contains? updated-ids item-id)
                              (contains? updated-ids item-id-int)
                              (contains? updated-ids item-id-str)))
        recently-created? (and (set? created-ids)
                            (or (contains? created-ids item-id)
                              (contains? created-ids item-id-int)
                              (contains? created-ids item-id-str)))]

    (if is-editing
      ;; If the item is being edited, render the edit form
      ($ form (assoc row-props :editing editing))
      ;; Otherwise, render the table row with action buttons and select checkbox
      {:cells (row-content {:entity-spec effective-entity-spec
                            :item item                      ; Keep using original item for display
                            ;; If a full actions override is provided (e.g., on admin pages),
                            ;; use it exclusively. Otherwise, fall back to the template defaults
                            ;; which can optionally include custom actions.
                            :actions (if-let [override (:actions-override props)]
                                       (override (assoc item-clj
                                                   :show-edit? (:show-edit? props)
                                                   :show-delete? (:show-delete? props)))
                                       ($ cells/action-buttons
                                         {:item item-clj
                                          :entity-name (:entity-name props)
                                          :show-edit? (:show-edit? props)
                                          :show-delete? (:show-delete? props)
                                          :custom-actions (:custom-actions props)}))
                            :show-timestamps? (:show-timestamps? props)
                            :visible-columns visible-columns
                            :entity-name entity-name
                            ;; Pass selected-ids and on-select-change for reactive-selection-cell
                            :selected-ids (or selected-ids #{})
                            :on-select-change on-select-change})
       :recently-updated? recently-updated?
       :recently-created? recently-created?})))
