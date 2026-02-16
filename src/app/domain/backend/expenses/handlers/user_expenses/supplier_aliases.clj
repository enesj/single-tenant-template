(ns app.domain.backend.expenses.handlers.user-expenses.supplier-aliases
  "User-facing supplier alias handlers (admin/owner only)."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [app.domain.backend.expenses.services.supplier-aliases :as supplier-aliases]
    [taoensso.timbre :as log]))

(def ^:private power-user-roles
  #{"admin" "owner"})

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

(defn list-supplier-aliases-handler
  "List supplier aliases for power users.

	Query params:
	- limit (default 50)
	- offset (default 0)
	- search (string, optional)
	- supplier_id (uuid, optional)
	- unmapped-only (boolean, optional)"
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request power-user-roles
                           "Only admins and owners can view supplier aliases")]
        forbidden
        (try
          (let [params (:query-params request)
                limit (parse-page-limit params 50)
                offset (parse-page-offset params)
                search (h/get-param params :search)
                supplier-id (h/try-parse-uuid (h/get-param params :supplier_id))
                unmapped-only (h/parse-boolean-param params :unmapped-only)
                opts (cond-> {:limit limit :offset offset}
                       (some? search) (assoc :search search)
                       supplier-id (assoc :supplier_id supplier-id)
                       (some? unmapped-only) (assoc :unmapped-only unmapped-only))
                rows (vec (supplier-aliases/list-supplier-aliases db opts))
                total (long (or (supplier-aliases/count-supplier-aliases db opts) 0))]
            (h/json-response {:data rows
                              :total total
                              :limit limit
                              :offset offset}))
          (catch Exception e
            (log/error e "Failed to list supplier aliases" {:query-params (:query-params request)})
            (h/json-response {:error "Failed to list supplier aliases"} 500))))
      (h/unauthorized-response))))

(defn update-supplier-alias-handler
  "Update supplier alias fields (admin/owner only)."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request power-user-roles
                           "Only admins and owners can update supplier aliases")]
        forbidden
        (try
          (let [alias-id (h/try-parse-uuid (get-in request [:path-params :id]))
                body (h/read-body-params request)
                payload (select-keys body [:raw_label :raw_label_normalized :supplier_id :confidence])
                update! (:update! supplier-aliases/service)]
            (cond
              (nil? alias-id)
              (h/not-found-response "Supplier alias not found")

              (empty? payload)
              (h/json-response {:error "No supplier alias fields provided"} 400)

              :else
              (if-let [updated (update! db alias-id payload)]
                (h/json-response {:data updated})
                (h/not-found-response "Supplier alias not found"))))
          (catch Exception e
            (log/error e "Failed to update supplier alias" {:alias-id (get-in request [:path-params :id])})
            (h/json-response {:error "Failed to update supplier alias"} 500))))
      (h/unauthorized-response))))

(defn delete-supplier-alias-handler
  "Delete supplier alias (admin/owner only)."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request power-user-roles
                           "Only admins and owners can delete supplier aliases")]
        forbidden
        (try
          (let [alias-id (h/try-parse-uuid (get-in request [:path-params :id]))
                delete! (:delete! supplier-aliases/service)
                deleted? (when alias-id (delete! db alias-id))]
            (if deleted?
              (h/json-response {:data {:deleted true}})
              (h/not-found-response "Supplier alias not found")))
          (catch Exception e
            (log/error e "Failed to delete supplier alias" {:alias-id (get-in request [:path-params :id])})
            (h/json-response {:error "Failed to delete supplier alias"} 500))))
      (h/unauthorized-response))))

(defn batch-delete-supplier-aliases-handler
  "Batch delete supplier aliases (admin/owner only).

  Expects JSON body like:
  {:ids [<uuid> ...]}

  Returns:
  {:data {:deleted-count n :deleted-ids [...] :errors [...]}}"
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request power-user-roles
                           "Only admins and owners can delete supplier aliases")]
        forbidden
        (try
          (let [body (h/read-body-params request)
                raw-ids (or (:ids body)
                          (:supplier_alias_ids body)
                          (:supplier-alias-ids body)
                          (:supplierAliasIds body)
                          [])
                ids (->> raw-ids (map h/try-parse-uuid) (filter some?) vec)]
            (cond
              (empty? raw-ids)
              (h/json-response {:error "No supplier alias ids provided"} 400)

              (empty? ids)
              (h/json-response {:error "One or more supplier alias ids are invalid"} 400)

              :else
              (let [delete! (:delete! supplier-aliases/service)
                    deleted-ids (atom [])
                    errors (atom [])]
                (doseq [alias-id ids]
                  (try
                    (if (boolean (delete! db alias-id))
                      (swap! deleted-ids conj (str alias-id))
                      (swap! errors conj {:id (str alias-id)
                                          :error "not found"}))
                    (catch Exception e
                      (swap! errors conj {:id (str alias-id)
                                          :error (.getMessage e)}))))
                (h/json-response {:data {:deleted-count (count @deleted-ids)
                                         :deleted-ids (vec @deleted-ids)
                                         :errors (vec @errors)}}))))
          (catch Exception e
            (log/error e "Failed to batch delete supplier aliases")
            (h/json-response {:error "Failed to delete supplier aliases"} 500))))
      (h/unauthorized-response))))