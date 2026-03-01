(ns app.domain.backend.expenses.handlers.user-manufacturers
  "User-facing manufacturers endpoints.

  These endpoints are mounted under /api/v1/expenses/manufacturers and are intended
  for power users inside the main app.

  IMPORTANT:
  - These routes are role-gated to admin/owner.
  - Responses are normalized to {:data ...} to keep frontend handlers consistent."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [app.domain.backend.expenses.services.manufacturers :as manufacturers]
    [app.shared.adapters.database :as shared-db]
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

(defn- extract-display-name
  [body]
  (or (:display_name body)
    (:display-name body)
    (:displayName body)))

;; -----------------------------------------------------------------------------
;; Handlers
;; -----------------------------------------------------------------------------

(defn list-manufacturers-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (h/ensure-role request h/reference-data-read-roles
                           "Role assignment required")]
        forbidden
        (try
          (let [qp (:query-params request)
                limit (parse-page-limit qp 200)
                offset (parse-page-offset qp)
                search (h/get-param qp :search)
                order-by (h/parse-order-by qp)
                order-dir (h/parse-order-dir qp)
                opts (cond-> {:limit limit
                              :offset offset
                              :search search}
                       order-by (assoc :order-by order-by)
                       order-dir (assoc :order-dir order-dir))
                rows (to-app ((:list manufacturers/service) db opts))
                rows (cond-> rows (sequential? rows) vec)
                total (long (or ((:count manufacturers/service) db {:search search}) 0))]
            (h/json-response {:data rows
                              :total total
                              :limit limit
                              :offset offset}))
          (catch Exception e
            (log/error e "Failed to list manufacturers" {:message (.getMessage e)})
            (h/json-response {:error "Failed to list manufacturers"} 500)))))))

(defn create-manufacturer-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (try
          (let [body (h/read-body-params request)
                display-name (extract-display-name body)
                manufacturer (to-app ((:create! manufacturers/service) db {:display_name display-name}))]
            (h/json-response {:data manufacturer} 201))
          (catch clojure.lang.ExceptionInfo e
            (log/warn "Validation error creating manufacturer" {:error (ex-message e) :data (ex-data e)})
            (h/json-response {:error (ex-message e)} 400))
          (catch Exception e
            (log/error e "Failed to create manufacturer" {:message (.getMessage e)})
            (h/json-response {:error "Failed to create manufacturer"} 500)))))))

(defn update-manufacturer-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (let [manufacturer-id (h/try-parse-uuid (or (get-in request [:path-params :id])
                                                  (get-in request [:parameters :path :id])))]
          (if-not manufacturer-id
            (h/json-response {:error "Invalid manufacturer id"} 400)
            (try
              (let [body (h/read-body-params request)
                    display-name-provided? (or (contains? body :display_name)
                                             (contains? body :display-name)
                                             (contains? body :displayName))
                    display-name (extract-display-name body)
                    updates (cond-> {}
                              display-name-provided? (assoc :display_name display-name))
                    updated (some-> ((:update! manufacturers/service) db manufacturer-id updates)
                              to-app)]
                (if updated
                  (h/json-response {:data updated})
                  (h/not-found-response "Manufacturer not found")))
              (catch clojure.lang.ExceptionInfo e
                (log/warn "Validation error updating manufacturer" {:error (ex-message e)
                                                                    :data (ex-data e)
                                                                    :manufacturer-id (str manufacturer-id)})
                (h/json-response {:error (ex-message e)} 400))
              (catch Exception e
                (log/error e "Failed to update manufacturer" {:message (.getMessage e)
                                                              :manufacturer-id (str manufacturer-id)})
                (h/json-response {:error "Failed to update manufacturer"} 500)))))))))

(defn delete-manufacturer-handler
  [db]
  (fn [request]
    (if-not (h/get-user request)
      (h/unauthorized-response)
      (if-let [forbidden (ensure-admin-or-owner request)]
        forbidden
        (let [manufacturer-id (h/try-parse-uuid (or (get-in request [:path-params :id])
                                                  (get-in request [:parameters :path :id])))]
          (if-not manufacturer-id
            (h/json-response {:error "Invalid manufacturer id"} 400)
            (try
              (let [deleted? (boolean ((:delete! manufacturers/service) db manufacturer-id))]
                (if deleted?
                  (h/json-response {:data {:deleted true}})
                  (h/not-found-response "Manufacturer not found")))
              (catch Exception e
                (log/error e "Failed to delete manufacturer" {:message (.getMessage e)
                                                              :manufacturer-id (str manufacturer-id)})
                (h/json-response {:error "Failed to delete manufacturer"} 500)))))))))

(defn batch-delete-manufacturers-handler
  "Batch delete manufacturers (admin/owner only).

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
                          (:manufacturer_ids body)
                          (:manufacturer-ids body)
                          (:manufacturerIds body)
                          [])
                ids (->> raw-ids (map h/try-parse-uuid) (filter some?) vec)]
            (cond
              (empty? raw-ids)
              (h/json-response {:error "No manufacturer ids provided"} 400)

              (empty? ids)
              (h/json-response {:error "One or more manufacturer ids are invalid"} 400)

              :else
              (let [delete! (:delete! manufacturers/service)
                    deleted-ids (atom [])
                    errors (atom [])]
                (doseq [manufacturer-id ids]
                  (try
                    (if (boolean (delete! db manufacturer-id))
                      (swap! deleted-ids conj (str manufacturer-id))
                      (swap! errors conj {:id (str manufacturer-id)
                                          :error "not found"}))
                    (catch org.postgresql.util.PSQLException e
                      (let [sql-state (.getSQLState e)]
                        (swap! errors conj {:id (str manufacturer-id)
                                            :error (if (= "23503" sql-state)
                                                     "foreign key constraint"
                                                     "database error")
                                            :sql-state sql-state})))
                    (catch Exception e
                      (swap! errors conj {:id (str manufacturer-id)
                                          :error (.getMessage e)}))))
                (h/json-response {:data {:deleted-count (count @deleted-ids)
                                         :deleted-ids (vec @deleted-ids)
                                         :errors (vec @errors)}}))))
          (catch Exception e
            (log/error e "Failed to batch delete manufacturers" {:message (.getMessage e)})
            (h/json-response {:error "Failed to delete manufacturers"} 500)))))))
