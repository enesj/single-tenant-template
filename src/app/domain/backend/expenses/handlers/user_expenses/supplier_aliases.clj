(ns app.domain.backend.expenses.handlers.user-expenses.supplier-aliases
  "User-facing supplier alias handlers (admin/owner only)."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [app.domain.backend.expenses.services.supplier-aliases :as supplier-aliases]
    [taoensso.timbre :as log]))

(def ^:private power-user-roles
  #{"admin" "owner"})

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
                limit (or (some-> (h/get-param params :limit) parse-long) 50)
                offset (or (some-> (h/get-param params :offset) parse-long) 0)
                search (h/get-param params :search)
                supplier-id (h/try-parse-uuid (h/get-param params :supplier_id))
                unmapped-only (h/parse-boolean-param params :unmapped-only)
                opts (cond-> {:limit limit :offset offset}
                       (some? search) (assoc :search search)
                       supplier-id (assoc :supplier_id supplier-id)
                       (some? unmapped-only) (assoc :unmapped-only unmapped-only))
                rows (vec (supplier-aliases/list-supplier-aliases db opts))]
            (h/json-response {:data rows
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