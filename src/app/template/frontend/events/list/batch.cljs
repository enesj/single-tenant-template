(ns app.template.frontend.events.list.batch
  "Batch operations for list views - bulk updates and inline editing"
  (:require
   [app.admin.frontend.events.users.utils :as admin-utils]
   [app.frontend.utils.id :as id-utils]
   [app.template.frontend.api :as api]
   [app.template.frontend.api.http :as http]
   [app.template.frontend.db.db :refer [common-interceptors]]
   [app.template.frontend.db.paths :as paths]
   [app.template.frontend.events.form :as form-events]
   [clojure.string]
   [day8.re-frame.http-fx]
   [re-frame.core :as rf]))

;;; -------------------------
;;; Batch Operations
;;; -------------------------

;; Helper function to format dates properly
;; format-date-for-api function moved to utils.date namespace

(rf/reg-event-fx
  ::batch-update
  common-interceptors
  (fn [{:keys [db]} [{:keys [entity-name item-ids values]}]]
    (if-not (and entity-name item-ids (seq item-ids) values)
      ;; Return unchanged DB if we don't have enough info
      {:db db}
      ;; Otherwise process the batch update
      (let [;; Values should already be filtered to only changed fields from the component
            ;; Check for meaningful changes - explicitly fields other than id, created_at, updated_at
            meaningful-keys (disj (set (keys values)) :id :created-at :updated-at)
            has-meaningful-changes? (boolean (seq meaningful-keys))

            ;; Add updatedAt timestamp only if there are actual changes
            base-values (if has-meaningful-changes?
                          (assoc values :updated-at (js/Date.))
                          values)

            ;; Format values based on field types
            formatted-values (let [entity-specs (get-in db [:entities :specs])
                                   entity-spec (get entity-specs entity-name)]
                               (reduce-kv
                                 (fn [acc field-key _value]
                                   ;; Find field spec for this field
                                   (let [field-spec (first (filter #(= (keyword (:id %)) field-key) entity-spec))
                                         input-type (when field-spec (:input-type field-spec))
                                         options (when field-spec (:options field-spec))]

                                     (cond
                                       ;; Format date fields
                                       ;;(= input-type "date")
                                       ;;(update acc field-key format-date-for-api)

                                       ;; Convert number fields (decimal, integer)
                                       (= input-type "number")
                                       (update acc field-key #(if (string? %)
                                                                (js/parseFloat %)
                                                                %))
                                       ;; Convert select fields with keyword options to integers
                                       (and
                                         (= input-type "select")
                                         (vector? options)
                                         (= 2 (count options))
                                         (every? keyword? options))
                                       (update acc field-key #(js/parseInt % 10))

                                       ;; Default case - no formatting needed
                                       :else
                                       acc)))
                                 base-values
                                 base-values))

            ;; Detect admin context and use appropriate endpoint and authentication
            in-admin? (clojure.string/includes? (.-pathname js/window.location) "/admin")
            request-params {:items (mapv (fn [id]
                                           ;; Create a complete item with ID and all values to update
                                           (assoc formatted-values :id id))
                                     item-ids)}]

        (if has-meaningful-changes?
          ;; Use the appropriate batch update endpoint and authentication based on context
          {:db (assoc-in db (paths/entity-loading? entity-name) true)
           :http-xhrio (if in-admin?
                         ;; Use admin HTTP request helper for proper authentication
                         (admin-utils/create-user-http-request
                           :put "/admin/api/users/actions/batch"
                           :params request-params
                           :on-success [::batch-update-success entity-name]
                           :on-failure [::batch-update-failure entity-name])
                         ;; Use regular template API request
                         (http/api-request
                           {:method :put
                            :uri (api/batch-endpoint (name entity-name) "update")
                            :params request-params
                            :timeout 8000
                            :on-success [::batch-update-success entity-name]
                            :on-failure [::batch-update-failure entity-name]}))
           ;; Add a notification to show success at the end
           :dispatch-later [{:ms 100
                             :dispatch [::form-events/set-submitted entity-name true]}]}
          ;; No meaningful changes, just notify the user
          {:dispatch [::form-events/set-field-error entity-name :form
                      {:message "No meaningful changes to update. Please modify at least one field."
                       :color "#FF9800"}]})))))

(rf/reg-event-fx
  ::batch-update-success
  common-interceptors
  (fn [{:keys [db]} [entity-type response]]
    ;; Handle both template and admin API response formats
    (let [updated-records (or (get-in response [:results])     ; Template API format
                            (get-in response [:data :results]) ; Admin API format with results
                            (get-in response [:data])       ; Admin API format without results
                            response)                       ; Fallback
          ;; Extract IDs using the generic utility - but handle nil/empty cases
          updated-ids (when (and updated-records (seq updated-records))
                        (id-utils/extract-ids updated-records))]

      {:db (-> db
             (assoc-in (paths/entity-loading? entity-type) false)
             (assoc-in (paths/entity-error entity-type) nil)
             (update-in [:ui :recently-updated entity-type] #(into (or % #{}) (or updated-ids #{})))
               ;; Close the batch edit form
             (update-in [:ui :batch-edit-inline] dissoc entity-type))
       :dispatch-n [;; Refresh the entities list
                    [:app.template.frontend.events.list.crud/fetch-entities entity-type]
                    ;; Clear any form state
                    [:app.template.frontend.events.form/clear-form entity-type]
                    ;; Show success message
                    [::form-events/set-field-error entity-type :form
                     {:message (str "Successfully updated " (count updated-ids) " " (name entity-type) ".")
                      :color "#4CAF50"}]]})))

(rf/reg-event-fx
  ::batch-update-failure
  common-interceptors
  (fn [{:keys [db]} [entity-type response]]
    {:db (-> db
           (assoc-in (paths/entity-loading? entity-type) false)
           (assoc-in (paths/entity-error entity-type)
             (or (get-in response [:response :error])
               {:message (str "Failed to update " entity-type ". Please try again.")})))
     :dispatch [::form-events/set-field-error entity-type :form
                {:message (str "Failed to update " entity-type ". Please try again.")
                 :color "#F44336"}]}))

;;; -------------------------
;;; Inline Batch Edit
;;; -------------------------

;; Inline Batch Edit Management
(rf/reg-event-fx
  ::show-batch-edit-inline
  common-interceptors
  (fn [{:keys [db]} [entity-type selected-ids]]
    {:db (-> db
           ;; Set inline batch edit state for specific entity
           (assoc-in [:ui :batch-edit-inline entity-type :entity-type] entity-type)
           (assoc-in [:ui :batch-edit-inline entity-type :selected-ids] selected-ids)
           (assoc-in [:ui :batch-edit-inline entity-type :open?] true)
           ;; Ensure add form is not shown
           (assoc-in [:ui :show-add-form] false)
           ;; Close single record editing when batch edit opens
           (assoc-in [:ui :editing] nil))}))

(rf/reg-event-db
  ::hide-batch-edit-inline
  common-interceptors
  (fn [db [entity-type]]
    (update-in db [:ui :batch-edit-inline] dissoc entity-type)))
