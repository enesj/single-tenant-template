(ns app.domain.backend.expenses.handlers.user-expenses.manufacturer-aliases
	"User-facing manufacturer alias handlers (admin/owner only)."
	(:require
		[app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
		[app.domain.backend.expenses.services.manufacturer-aliases :as manufacturer-aliases]
		[clojure.string :as str]
		[taoensso.timbre :as log]))

(def ^:private power-user-roles
	#{"admin" "owner"})

(defn list-manufacturer-aliases-handler
	"List manufacturer aliases for power users.

	Query params:
	- limit (default 50)
	- offset (default 0)
	- search (string, optional)
	- manufacturer_id (uuid, optional)
	- unmapped-only (boolean, optional)"
	[db]
	(fn [request]
		(if-let [_user-id (h/get-user-id request)]
			(if-let [forbidden (h/ensure-role request power-user-roles
													 "Only admins and owners can view manufacturer aliases")]
				forbidden
				(try
					(let [params (:query-params request)
								limit (or (some-> (h/get-param params :limit) parse-long) 50)
								offset (or (some-> (h/get-param params :offset) parse-long) 0)
								search (h/get-param params :search)
								manufacturer-id (h/try-parse-uuid (h/get-param params :manufacturer_id))
								unmapped-only (h/parse-boolean-param params :unmapped-only)
								opts (cond-> {:limit limit :offset offset}
											 (some? search) (assoc :search search)
											 manufacturer-id (assoc :manufacturer_id manufacturer-id)
											 (some? unmapped-only) (assoc :unmapped-only unmapped-only))
								rows (vec (manufacturer-aliases/list-manufacturer-aliases db opts))]
						(h/json-response {:data rows
															:limit limit
															:offset offset}))
					(catch Exception e
						(log/error e "Failed to list manufacturer aliases" {:query-params (:query-params request)})
						(h/json-response {:error "Failed to list manufacturer aliases"} 500))))
			(h/unauthorized-response))))

(defn update-manufacturer-alias-handler
	"Update manufacturer alias fields (admin/owner only)."
	[db]
	(fn [request]
		(if-let [_user-id (h/get-user-id request)]
			(if-let [forbidden (h/ensure-role request power-user-roles
													 "Only admins and owners can update manufacturer aliases")]
				forbidden
				(try
					(let [alias-id (h/try-parse-uuid (get-in request [:path-params :id]))
								body (h/read-body-params request)
								raw-label (some-> (or (:raw_label body) (:raw-label body)) str)
								raw-label-normalized (some-> (or (:raw_label_normalized body) (:raw-label-normalized body)) str)
								manufacturer-id-key-present? (or (contains? body :manufacturer_id)
																								 (contains? body :manufacturer-id))
								manufacturer-id-raw (or (:manufacturer_id body) (:manufacturer-id body))
								manufacturer-id (cond
																	(string? manufacturer-id-raw)
																	(when-not (str/blank? manufacturer-id-raw)
																		(h/try-parse-uuid manufacturer-id-raw))

																	(instance? java.util.UUID manufacturer-id-raw)
																	manufacturer-id-raw

																	:else nil)
								confidence-key-present? (contains? body :confidence)
								confidence (:confidence body)
								payload (cond-> {}
													(some? raw-label) (assoc :raw_label raw-label)
													(some? raw-label-normalized) (assoc :raw_label_normalized raw-label-normalized)
													manufacturer-id-key-present? (assoc :manufacturer_id manufacturer-id)
													confidence-key-present? (assoc :confidence confidence))
								update! (:update! manufacturer-aliases/service)]
						(cond
							(nil? alias-id)
							(h/not-found-response "Manufacturer alias not found")

							(empty? payload)
							(h/json-response {:error "No manufacturer alias fields provided"} 400)

							:else
							(if-let [updated (update! db alias-id payload)]
								(h/json-response {:data updated})
								(h/not-found-response "Manufacturer alias not found"))))
					(catch Exception e
						(log/error e "Failed to update manufacturer alias" {:alias-id (get-in request [:path-params :id])})
						(h/json-response {:error "Failed to update manufacturer alias"} 500))))
			(h/unauthorized-response))))

(defn delete-manufacturer-alias-handler
	"Delete manufacturer alias (admin/owner only)."
	[db]
	(fn [request]
		(if-let [_user-id (h/get-user-id request)]
			(if-let [forbidden (h/ensure-role request power-user-roles
													 "Only admins and owners can delete manufacturer aliases")]
				forbidden
				(try
					(let [alias-id (h/try-parse-uuid (get-in request [:path-params :id]))
								delete! (:delete! manufacturer-aliases/service)
								deleted? (when alias-id (delete! db alias-id))]
						(if deleted?
							(h/json-response {:success true})
							(h/not-found-response "Manufacturer alias not found")))
					(catch Exception e
						(log/error e "Failed to delete manufacturer alias" {:alias-id (get-in request [:path-params :id])})
						(h/json-response {:error "Failed to delete manufacturer alias"} 500))))
			(h/unauthorized-response))))
