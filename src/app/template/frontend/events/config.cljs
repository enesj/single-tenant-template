(ns app.template.frontend.events.config
  "Configuration and UI state management events for the template frontend.

   Handles application configuration fetching, UI state management, and entity controls."
  (:require
    [app.template.frontend.api :as api]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :as db :refer [common-interceptors]]
    [app.template.frontend.db.entity-specs :as entity-specs]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ========================================================================
;; Configuration Events
;; ========================================================================

(rf/reg-event-fx
  ::fetch-config
  common-interceptors
  (fn [_ _]
    {:http-xhrio (http/api-request
                   {:method :get
                    :uri (:config api/endpoints)
                    :on-success [::fetch-config-success]
                    :on-failure [::fetch-config-failure]})}))

(rf/reg-event-fx
  ::fetch-config-success
  common-interceptors
  (fn [{:keys [db]} [response]]
    (let [models-data (:models-data response)
          validation-specs (:validation-specs response)
          ;; Use models-data to populate entities and lists if available.
          ;; Only run the full `make-db-with-models-data` initialization when
          ;; the entities map is still empty; if entities already contain data
          ;; (e.g., from admin/tenant flows), avoid re-initializing to prevent
          ;; wiping live entity state.
          db-with-models (let [entities (:entities db)]
                           (cond
                             (and models-data (not (seq entities)))
                             (db/make-db-with-models-data db models-data)

                             models-data
                             (assoc db :models-data models-data)

                             :else
                             db))
          ;; Set up default UI state with hardcoded defaults (no longer from config)
          db-with-defaults
          (-> db-with-models
            ;; Initialize UI defaults with hardcoded values
            (assoc-in [:ui :defaults]
              {:show-timestamps? true
               :show-edit? true
               :show-delete? true
               :show-highlights? true
               :show-select? false  ; Default to false for select
               :show-filtering? true
               :show-pagination? true
               :controls {:show-timestamps-control? true
                          :show-edit-control? true
                          :show-delete-control? true
                          :show-highlights-control? true
                          :show-select-control? true
                          :show-filtering-control? true
                          :show-invert-selection? true
                          :show-delete-selected? true}}))

          final-db (cond-> db-with-defaults
                     models-data (assoc :models-data models-data)
                     validation-specs (assoc :validation-specs validation-specs))]

      {:db final-db
       :dispatch [::entity-specs/initialize-entity-specs]})))

(rf/reg-event-db
  ::fetch-config-failure
  common-interceptors
  (fn [db [_]]
    (log/error "Failed to fetch UI configuration")
    db))

;; ========================================================================
;; UI State Management Events
;; ========================================================================

(rf/reg-event-db
  ::set-show-add-form
  (fn [db [_ value]]
    (assoc-in db [:ui :show-add-form] value)))

(rf/reg-event-fx
  ::set-editing
  (fn [{:keys [db]} [_ value]]
    {:db (assoc-in db [:ui :editing] value)
     ;; When single edit starts, close any open batch edit forms
     :fx (if value
           ;; Clear all batch edit forms for all entity types
           (for [entity-type (keys (get-in db [:ui :batch-edit-inline]))]
             [:dispatch [:app.template.frontend.events.list.batch/hide-batch-edit-inline entity-type]])
           ;; Return empty sequence when value is nil/falsy
           [])}))
