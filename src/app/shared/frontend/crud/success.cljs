(ns app.shared.frontend.crud.success
  "Shared success handling for all CRUD operations.
   Ensures consistent behavior for:
   - Tracking recently-updated/recently-created IDs for highlighting
   - Clearing loading states
   - Extracting entity IDs from responses
   
   This module provides a single source of truth for CRUD success handling,
   eliminating duplication across:
   - app.template.frontend.events.form
   - app.shared.frontend.bridges.crud
   - app.admin.frontend.events.users.template.success-handlers
   - app.admin.frontend.events.users.bulk-operations")

;; =============================================================================
;; ID Extraction
;; =============================================================================

(defn extract-entity-id
  "Extract entity ID from response, handling both simple :id and namespaced keys.
   
   Examples:
   - {:id 123} -> 123
   - {:users/id 456} -> 456
   - {:transaction-types/id 789} -> 789"
  [response]
  (or (:id response)
      ;; Find any keyword with local name \"id\" (e.g., :transaction-types/id, :users/id)
    (->> response
      (filter (fn [[k _]] (and (keyword? k) (= (name k) "id"))))
      first
      second)))

;; =============================================================================
;; Recently Created/Updated Tracking
;; =============================================================================

(defn track-recently-created
  "Add entity ID to recently-created set for highlighting.
   Returns updated db."
  [db entity-type entity-id]
  (update-in db [:ui :recently-created entity-type]
    (fn [ids] (conj (or ids #{}) entity-id))))

(defn track-recently-updated
  "Add entity ID to recently-updated set for highlighting.
   Returns updated db."
  [db entity-type entity-id]
  (update-in db [:ui :recently-updated entity-type]
    (fn [ids] (conj (or ids #{}) entity-id))))

(defn track-recently-updated-bulk
  "Add multiple entity IDs to recently-updated set for highlighting.
   Returns updated db."
  [db entity-type entity-ids]
  (update-in db [:ui :recently-updated entity-type]
    (fn [ids] (reduce conj (or ids #{}) entity-ids))))

;; =============================================================================
;; Form State Management
;; =============================================================================

(defn clear-form-success-state
  "Set form state to indicate successful submission.
   Returns map to merge into form state."
  []
  {:submitting? false
   :success true
   :submitted? true
   :errors nil
   :server-errors nil})

;; =============================================================================
;; High-Level Success Handlers
;; =============================================================================

(defn handle-create-success
  "Standard create success handling.
   Extracts entity ID from response and tracks it as recently created.
   Returns updated db with form state cleared and ID tracked."
  [db entity-type response]
  (let [entity-id (extract-entity-id response)]
    (-> db
      (update-in [:forms entity-type] merge (clear-form-success-state))
      (track-recently-created entity-type entity-id))))

(defn handle-update-success
  "Standard update success handling.
   Extracts entity ID from response or uses provided-id and tracks it as recently updated.
   Returns updated db with form state cleared and ID tracked."
  ([db entity-type response]
   (handle-update-success db entity-type nil response))
  ([db entity-type provided-id response]
   (let [entity-id (or (extract-entity-id response) provided-id)]
     (-> db
       (update-in [:forms entity-type] merge (clear-form-success-state))
       (track-recently-updated entity-type entity-id)))))

(defn handle-bulk-update-success
  "Bulk update success handling.
   Tracks multiple entity IDs as recently updated.
   Returns updated db with IDs tracked."
  [db entity-type entity-ids]
  (track-recently-updated-bulk db entity-type entity-ids))
