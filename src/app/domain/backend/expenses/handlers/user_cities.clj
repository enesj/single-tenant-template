(ns app.domain.backend.expenses.handlers.user-cities
  "User-facing cities endpoints.

  These endpoints are mounted under /api/v1/expenses/cities and are intended
  for power users inside the main app.

  IMPORTANT:
  - These routes are role-gated to admin/owner.
  - Responses are normalized to {:data ...} to keep frontend handlers consistent."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [app.domain.backend.expenses.services.cities :as cities]
    [app.shared.adapters.database :as shared-db]
    [clojure.string :as str]
    [taoensso.timbre :as log]))

;; -----------------------------------------------------------------------------
;; Helpers
;; -----------------------------------------------------------------------------

(def ^:private to-app shared-db/to-app)

(def ^:private allowed-roles
  #{"admin" "owner"})

(defn- ensure-admin-or-owner
  [request]
  (h/ensure-role request allowed-roles "Only admins and owners can access this page."))

(def ^:private max-page-limit
  500)

(defn- parse-page-limit
  [params default-limit]
  (-> (or (some-> (h/get-param params :limit) parse-long)
        default-limit)
    long
    (max 1)
    (min max-page-limit)))

(defn- parse-page-offset
  [params]
  (max 0 (long (or (some-> (h/get-param params :offset) parse-long) 0))))

(defn- body-has-any-key?
  [body ks]
  (boolean (some #(contains? body %) ks)))

(defn- body-get-first
  [body ks]
  (some #(get body %) ks))

(defn- extract-name
  [body]
  (or (:name body)
    (:display_name body)
    (:display-name body)
    (:displayName body)))

;; -----------------------------------------------------------------------------
;; Handlers
;; -----------------------------------------------------------------------------

(defn list-cities-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (try
          (let [qp (:query-params request)
                limit (parse-page-limit qp 200)
                offset (parse-page-offset qp)
                search (h/get-param qp :search)
                opts {:limit limit
                      :offset offset
                      :search search}
                rows (to-app ((:list cities/service) db opts))
                rows (cond-> rows (sequential? rows) vec)
                total (long (or ((:count cities/service) db {:search search}) 0))]
            (h/json-response {:data rows
                              :total total
                              :limit limit
                              :offset offset}))
          (catch Exception e
            (log/error e "Failed to list cities" {:message (.getMessage e)})
            (h/json-response {:error "Failed to list cities"} 500)))))))

(defn create-city-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (try
          (let [body (h/read-body-params request)
                name (some-> (extract-name body) str str/trim)]
            (if (str/blank? name)
              (h/json-response {:error "name is required"} 400)
              (let [city (to-app ((:create! cities/service) db {:name name}))]
                (h/json-response {:data city} 201))))
          (catch clojure.lang.ExceptionInfo e
            (log/warn "Validation error creating city" {:error (ex-message e) :data (ex-data e)})
            (h/json-response {:error (ex-message e)} 400))
          (catch Exception e
            (log/error e "Failed to create city" {:message (.getMessage e)})
            (h/json-response {:error "Failed to create city"} 500)))))))

(defn update-city-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (let [city-id (h/try-parse-uuid (or (get-in request [:path-params :id])
                                          (get-in request [:parameters :path :id])))]
          (if-not city-id
            (h/json-response {:error "Invalid city id"} 400)
            (try
              (let [body (h/read-body-params request)
                    name-keys [:name :display_name :display-name :displayName]
                    name-provided? (body-has-any-key? body name-keys)
                    name (when name-provided?
                           (some-> (body-get-first body name-keys) str str/trim))
                    updates (cond-> {}
                              name-provided? (assoc :name name))]
                (cond
                  (not name-provided?)
                  (h/json-response {:error "No city fields provided"} 400)

                  (str/blank? name)
                  (h/json-response {:error "name is required"} 400)

                  :else
                  (if-let [updated (some-> ((:update! cities/service) db city-id updates)
                                     to-app)]
                    (h/json-response {:data updated})
                    (h/not-found-response "City not found"))))
              (catch clojure.lang.ExceptionInfo e
                (log/warn "Validation error updating city" {:error (ex-message e)
                                                            :data (ex-data e)
                                                            :city-id (str city-id)})
                (h/json-response {:error (ex-message e)} 400))
              (catch Exception e
                (log/error e "Failed to update city" {:message (.getMessage e)
                                                      :city-id (str city-id)})
                (h/json-response {:error "Failed to update city"} 500)))))))))

(defn batch-delete-cities-handler
  "Batch delete cities (admin/owner only).

  Expects JSON body like:
  {:ids [<uuid> ...]}

  Returns:
  {:data {:deleted-count n :deleted-ids [...] :errors [...]}}"
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (try
          (let [body (h/read-body-params request)
                raw-ids (or (:ids body)
                          (:city_ids body)
                          (:city-ids body)
                          (:cityIds body)
                          [])
                ids (->> raw-ids (map h/try-parse-uuid) (filter some?) vec)]
            (cond
              (empty? raw-ids)
              (h/json-response {:error "No city ids provided"} 400)

              (empty? ids)
              (h/json-response {:error "One or more city ids are invalid"} 400)

              :else
              (let [delete! (:delete! cities/service)
                    deleted-ids (atom [])
                    errors (atom [])]
                (doseq [city-id ids]
                  (try
                    (if (boolean (delete! db city-id))
                      (swap! deleted-ids conj (str city-id))
                      (swap! errors conj {:id (str city-id)
                                          :error "not found"}))
                    (catch org.postgresql.util.PSQLException e
                      (let [sql-state (.getSQLState e)]
                        (swap! errors conj {:id (str city-id)
                                            :error (if (= "23503" sql-state)
                                                     "foreign key constraint"
                                                     "database error")
                                            :sql-state sql-state})))
                    (catch Exception e
                      (swap! errors conj {:id (str city-id)
                                          :error (.getMessage e)}))))
                (h/json-response {:data {:deleted-count (count @deleted-ids)
                                         :deleted-ids (vec @deleted-ids)
                                         :errors (vec @errors)}}))))
          (catch Exception e
            (log/error e "Failed to batch delete cities" {:message (.getMessage e)})
            (h/json-response {:error "Failed to delete cities"} 500)))))))
