(ns app.template.backend.routes.utils
  "Utility functions for reducing duplication in route handlers.
   Provides centralized error handling, request data extraction, and response patterns."
  (:require
    [app.shared.adapters.database :as db-adapter]
    [clojure.stacktrace]
    [ring.util.response :as response]
    [taoensso.timbre :as log]))

;; ================================================================================
;; Error Handling
;; ================================================================================

(defn error-response
  "Generate appropriate error response based on error type.

   Args:
   - error-type: Keyword identifying the error type
   - error-data: Map of error data (from ex-data)
   - handler-name: String name of the handler (for logging)
   - e: The exception (for logging)

   Returns appropriate Ring response."
  [error-type error-data handler-name e]
  (case error-type
    :entity-not-found
    (response/not-found {:error "Entity not found"})

    :item-not-found
    (response/not-found {:error "Item not found"})

    :validation-error
    (response/bad-request {:error (or (:message error-data) "Validation failed")
                           :details (:errors error-data)})

    :foreign-key-constraint
    (response/status
      (response/response {:error (:message error-data)})
      409)

    ;; Default error response
    (do
      (log/error e (str "Error in " handler-name " handler"))
      (log/error "Exception type:" (type e))
      (log/error "Exception message:" (.getMessage e))
      (log/error "Exception data:" (ex-data e))
      (log/error "Stack trace:" (with-out-str (clojure.stacktrace/print-stack-trace e)))
      (response/status (response/response {:error "Internal server error"}) 500))))

(defmacro with-error-handling
  "Wrap handler body with standardized error handling.

   Usage:
   (with-error-handling \"get-items\"
     ...handler body...)"
  [handler-name & body]
  `(try
     ~@body
     (catch Exception e#
       (let [error-data# (ex-data e#)
             error-type# (:type error-data#)]
         (error-response error-type# error-data# ~handler-name e#)))))

;; ================================================================================
;; Request Data Extraction
;; ================================================================================

(defn extract-common-data
  "Extract commonly used data from request.

   Returns a map with:
   - :tenant-id
   - :user-id
   - :entity-key (if entity param exists)
   - :item-id (if id param exists)
   - :admin (if admin request)
   - :is-admin? (boolean indicating admin context)"
  [request]
  (let [admin (:admin request)
        is-admin? (some? admin)
        base-data (if is-admin?
                    {:tenant-id nil  ; Admins bypass tenant isolation
                     :user-id (:id admin)
                     :admin admin
                     :is-admin? true}
                    {:tenant-id (get-in request [:session :tenant-id])
                     :user-id (get-in request [:session :user :id])
                     :is-admin? false})]
    (cond-> base-data
      (get-in request [:path-params :entity])
      (assoc :entity-key (keyword (get-in request [:path-params :entity])))

      (get-in request [:path-params :id])
      (assoc :item-id (get-in request [:path-params :id])))))

(defn extract-query-params
  "Extract and parse common query parameters.

   Returns a map with parsed query parameters:
   - :filters (when present and map)
   - :order-by (as keyword when present)
   - :limit (as integer when present)
   - :offset (as integer when present)
   - :include-joins? (boolean)
   - :return-joins? (boolean)
   - :continue-on-error? (boolean)"
  [request]
  (let [query-params (:query-params request)
        parse-int (fn [s]
                    (when (string? s)
                      (try (Integer/parseInt s)
                        (catch NumberFormatException _ nil))))]
    (cond-> {}
      ;; Filters
      (contains? query-params :filters)
      (assoc :filters (when-let [f (:filters query-params)]
                        (when (map? f) f)))

      ;; Order by
      (contains? query-params :order-by)
      (assoc :order-by (when-let [o (:order-by query-params)]
                         (keyword o)))

      ;; Pagination
      (contains? query-params :limit)
      (assoc :limit (parse-int (:limit query-params)))

      (contains? query-params :offset)
      (assoc :offset (parse-int (:offset query-params)))

      ;; Boolean flags
      (= "true" (:include-joins query-params))
      (assoc :include-joins? true)

      (= "true" (:return-joins query-params))
      (assoc :return-joins? true)

      (= "true" (:continue-on-error query-params))
      (assoc :continue-on-error? true))))

(defn build-options
  "Build options map from query parameters and additional data.

   Args:
   - query-params: Map of parsed query parameters
   - extra-opts: Additional options to merge

   Returns consolidated options map."
  [query-params extra-opts]
  (merge
    ;; Query param options
    (select-keys query-params [:filters :order-by :limit :offset
                               :include-joins? :return-joins? :continue-on-error?])
    ;; Extra options
    extra-opts))

;; ================================================================================
;; Response Helpers
;; ================================================================================

(defn success-response
  "Create a success response with optional status code.
   Automatically converts snake_case database keys to kebab-case for frontend."
  ([data]
   (-> data
     db-adapter/convert-db-keys->app-keys
     db-adapter/convert-app-keys->camel-keys
     response/response))
  ([data status]
   (-> data
     db-adapter/convert-db-keys->app-keys
     db-adapter/convert-app-keys->camel-keys
     response/response
     (response/status status))))

(defn not-found-response
  "Create a not found response with custom message."
  [message]
  (response/not-found {:error message}))

(defn bad-request-response
  "Create a bad request response with error details."
  [message & [details]]
  (cond-> {:error message}
    details (assoc :details details)
    :always response/bad-request))

(defn conflict-response
  "Create a conflict response (409) with error message."
  [message]
  (response/status
    (response/response {:error message})
    409))

;; ================================================================================
;; Handler Creation Helpers
;; ================================================================================

(defn create-crud-handler
  "Create a CRUD handler with standard error handling and data extraction.

   Args:
   - handler-name: String name for error logging
   - handler-fn: Function that takes extracted data and returns response

   The handler-fn will receive a map with:
   - All data from extract-common-data
   - :query-params from extract-query-params
   - :body-params from request
   - :request (the original request)

   Returns Ring handler function."
  [handler-name handler-fn]
  (fn [request]
    (with-error-handling handler-name
      (let [common-data (extract-common-data request)
            query-params (extract-query-params request)
            body-params (get-in request [:body-params])
            handler-data (merge common-data
                           {:query-params query-params
                            :body-params body-params
                            :request request})]
        (handler-fn handler-data)))))
