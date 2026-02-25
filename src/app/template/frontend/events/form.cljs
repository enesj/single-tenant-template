(ns app.template.frontend.events.form
  (:require
    [app.template.frontend.shared.crud.success :as crud-success]
    [app.shared.model-naming :as model-naming]
    [app.template.frontend.api :as api]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :as db :refer [common-interceptors]]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.events.list.crud :as crud-events]
    [clojure.string :as str]
    [day8.re-frame.http-fx]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(defn- convert-keys-to-db
  "Convert all keys in a map from kebab-case to snake_case for database/API compatibility"
  [m]
  (model-naming/app-map-keys->db m))

(def ^:private backlog-type-aliases
  {"issue" "Issue"
   "feature" "Feature"
   "refactoring" "Refactoring"
   "review" "Review"
   "improvment" "Improvment"
   "improvement" "Improvment"})

(def ^:private backlog-status-aliases
  {"waiting" "Waiting"
   "in progres" "In progres"
   "in progress" "In progres"
   "completed" "Completed"
   "need improvments" "Need improvments"
   "need improvements" "Need improvments"})

(defn- backlog-option-value
  [value]
  (if (map? value)
    (or (:value value)
      (get value "value")
      value)
    value))

(defn- canonical-backlog-value
  [aliases value]
  (let [normalized (some-> value backlog-option-value str str/trim)
        lowered (some-> normalized str/lower-case)]
    (or (get aliases lowered) normalized)))

(defn- normalize-backlog-field
  [request-params field aliases]
  (let [field-name (name field)]
    (cond
      (contains? request-params field)
      (if-let [canonical (canonical-backlog-value aliases (get request-params field))]
        (assoc request-params field canonical)
        (dissoc request-params field))

      (contains? request-params field-name)
      (if-let [canonical (canonical-backlog-value aliases (get request-params field-name))]
        (assoc request-params field-name canonical)
        (dissoc request-params field-name))

      :else request-params)))

(defn- normalize-backlog-request-params
  [entity-name request-params]
  (if (= :backlog (model-naming/ensure-app-keyword entity-name))
    (-> request-params
      (normalize-backlog-field :type backlog-type-aliases)
      (normalize-backlog-field :status backlog-status-aliases))
    request-params))

;;; -------------------------
;;; Form Submission
;;; -------------------------

(defn- build-submit-fx
  "Shared implementation for submit-form and process-default-submission.
  Converts values, normalises backlog fields, and returns an fx map."
  [db {:keys [values entity-name editing]}]
  (let [db-values (convert-keys-to-db values)
        request-params (if editing
                         (dissoc db-values :id)
                         db-values)
        request-params (normalize-backlog-request-params entity-name request-params)]
    {:db (assoc-in db (paths/form-submitting? entity-name) true)
     :http-xhrio (if editing
                   (http/update-entity
                     {:entity-name (name entity-name)
                      :id (:id values)
                      :data request-params
                      :on-success [::update-success entity-name]
                      :on-failure [::update-failure entity-name]})
                   (http/create-entity
                     {:entity-name (name entity-name)
                      :data request-params
                      :on-success [::create-success entity-name]
                      :on-failure [::create-failure entity-name]}))}))

(rf/reg-event-fx
  ::submit-form
  common-interceptors
  (fn [{:keys [db]} [opts]]
    (build-submit-fx db opts)))

;; This event is dispatched by the admin form interceptors when no admin-specific
;; overrides apply. It mirrors the default template submission behavior to avoid
;; re-dispatching to the intercepted ::submit-form and causing recursion.
(rf/reg-event-fx
  ::process-default-submission
  common-interceptors
  (fn [{:keys [db]} [opts]]
    (build-submit-fx db opts)))

(rf/reg-event-fx
  ::create-success
  common-interceptors
  (fn [{:keys [db]} [entity-type response]]
    (let [entity-id (crud-success/extract-entity-id response)
          new-db (crud-success/handle-create-success db entity-type response)]
      (log/debug "📤 FORM CREATE-SUCCESS - entity-type:" entity-type
        "extracted entity-id:" entity-id
        "response keys:" (keys response))
      {:db new-db
       :fx (when (and entity-type (keyword? entity-type))
             [[:dispatch [::crud-events/fetch-entities entity-type]]])})))

(rf/reg-event-fx
  ::create-failure
  common-interceptors
  (fn [{:keys [db]} [entity-type response]]
    (let [new-db (-> db
                   (update-in [:forms entity-type] merge
                     {:submitting? false
                      :success false
                      :errors {:form {:message (or (:error response)
                                                 (get-in response [:response :error]
                                                   "Failed to create item"))
                                      :color "#FF9800"}}})
                   (assoc-in (paths/entity-loading? entity-type) false))]
      {:db new-db
       :fx (when (and entity-type (keyword? entity-type))
             (println "📤 FORM CREATE-FAILURE: Dispatching fetch-entities for:" entity-type "type:" (type entity-type) "stack:" (.-stack (js/Error.)))
             [[:dispatch [::crud-events/fetch-entities entity-type]]])})))

(rf/reg-event-fx
  ::update-success
  common-interceptors
  (fn [{:keys [db]} [entity-type response]]
    (let [entity-id (crud-success/extract-entity-id response)
          new-db (crud-success/handle-update-success db entity-type response)]
      (log/debug "📤 FORM UPDATE-SUCCESS - entity-type:" entity-type
        "extracted entity-id:" entity-id
        "response keys:" (keys response))
      {:db new-db
       :fx (when (and entity-type (keyword? entity-type))
             [[:dispatch [::crud-events/fetch-entities entity-type]]])})))

(rf/reg-event-fx
  ::update-failure
  common-interceptors
  (fn [{:keys [db]} [entity-type response]]
    (let [new-db (-> db
                   (update-in [:forms entity-type] merge
                     {:submitting? false
                      :success false
                      :errors {:form {:message (or (:error response)
                                                 (get-in response [:response :error]
                                                   "Failed to update item"))
                                      :color "#FF9800"}}})
                   (assoc-in (paths/entity-loading? entity-type) false))]
      {:db new-db
       :fx (when (and entity-type (keyword? entity-type))
             (println "📤 FORM UPDATE-FAILURE: Dispatching fetch-entities for:" entity-type "type:" (type entity-type) "stack:" (.-stack (js/Error.)))
             [[:dispatch [::crud-events/fetch-entities entity-type]]])})))

(rf/reg-event-db
  ::set-submitted
  common-interceptors
  (fn [db [entity-type submitted?]]
    (assoc-in db (paths/form-submitted? entity-type) submitted?)))

;;; -------------------------
;;; Field Validation
;;; -------------------------

(rf/reg-event-db
  ::set-field-error
  common-interceptors
  (fn [db [entity-type field-id error]]
    (assoc-in db (paths/form-field-error entity-type field-id) error)))

(rf/reg-event-db
  ::clear-field-error
  common-interceptors
  (fn [db [entity-type field-id]]
    (assoc-in db (paths/form-field-error entity-type field-id) nil)))

(rf/reg-event-db
  ::clear-form-errors
  common-interceptors
  (fn [db [entity-type]]
    (assoc-in db (paths/form-errors entity-type) nil)))

;;; -------------------------
;;; Server Validation
;;; -------------------------

(rf/reg-event-fx
  ::set-server-field-error
  common-interceptors
  (fn [{:keys [db]} [{:keys [dirty entity-name]}]]
    {:db (update-in db (paths/form-waiting entity-name) (fnil into #{}) (keys dirty))
     :http-xhrio (http/api-request
                   {:method :post
                    :uri (api/validate-endpoint (name entity-name))
                    :params dirty
                    :on-success [::set-server-field-error-success entity-name (-> dirty first key keyword)]
                    :on-failure [::set-server-field-error-failure entity-name]})}))

(rf/reg-event-db
  ::set-server-field-error-success
  common-interceptors
  (fn [db [entity-type field-id response]]
    (if (:valid? response)
      (-> db
        (assoc-in (paths/form-server-errors entity-type field-id) nil)
        (assoc-in (paths/form-waiting entity-type) nil)
        (assoc-in (paths/form-success entity-type field-id) true))
      (-> db
        (update-in (paths/form-waiting entity-type) #(disj (or % #{}) field-id))
        (assoc-in (paths/form-server-errors entity-type field-id) {:message (:error response) :color (:color response)})
        (assoc-in (paths/form-success entity-type field-id) false)))))

(rf/reg-event-db
  ::set-server-field-error-failure
  common-interceptors
  (fn [db [entity-type _]]
    (update-in db (paths/form-waiting entity-type) empty)))

;;; -------------------------
;;; Dirty Field Tracking
;;; -------------------------

(rf/reg-event-db
  ::set-dirty-fields
  common-interceptors
  (fn [db [entity-type dirty-fields]]
    (let [existing-dirty-fields (get-in db (paths/form-dirty-fields entity-type) #{})
          updated-dirty-fields (if (set? dirty-fields)
                                 (into existing-dirty-fields dirty-fields)
                                 (set dirty-fields))]
      (assoc-in db (paths/form-dirty-fields entity-type) updated-dirty-fields))))

(rf/reg-event-fx
  ::cancel-form
  common-interceptors
  (fn [{:keys [db]} [entity-type]]
    (let [batch-edit? (get-in db [:ui :batch-edit])]
      {:db (-> db
             (assoc-in [:ui :show-add-form] false)
             ;; Only clear editing state if not in batch edit mode
             (cond-> (not batch-edit?) (assoc-in [:ui :editing] nil))
             (update-in [:forms entity-type] merge
               {:values {}
                :errors nil
                :server-errors nil
                :submitted? false
                :submitting? false
                :success false}))})))

(rf/reg-event-db
  ::clear-form
  common-interceptors
  (fn [db [entity-type]]
    (update-in db [:forms entity-type] merge
      {:values {}
       :errors nil
       :server-errors nil
       :submitted? false
       :submitting? false
       :success false})))
