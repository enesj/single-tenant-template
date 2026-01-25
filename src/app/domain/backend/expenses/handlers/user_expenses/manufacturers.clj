(ns app.domain.backend.expenses.handlers.user-expenses.manufacturers
	"User-facing manufacturer handlers (admin/owner only)."
	(:require
		[app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
		[app.domain.backend.expenses.services.manufacturers :as manufacturers]
		[clojure.string :as str]
		[taoensso.timbre :as log]))

(def ^:private power-user-roles
	#{"admin" "owner"})

(defn list-manufacturers-handler
	"List manufacturers for power users.

	Query params:
	- limit (default 50)
	- offset (default 0)
	- search (string, optional)"
	[db]
	(fn [request]
		(if-let [_user-id (h/get-user-id request)]
			(if-let [forbidden (h/ensure-role request power-user-roles
													 "Only admins and owners can view manufacturers")]
				forbidden
				(try
					(let [params (:query-params request)
								limit (or (some-> (h/get-param params :limit) parse-long) 50)
								offset (or (some-> (h/get-param params :offset) parse-long) 0)
								search (h/get-param params :search)
								opts (cond-> {:limit limit :offset offset}
											 (some? search) (assoc :search search))
								list! (:list manufacturers/service)
								rows (vec (list! db opts))]
						(h/json-response {:data rows
															:limit limit
															:offset offset}))
					(catch Exception e
						(log/error e "Failed to list manufacturers" {:query-params (:query-params request)})
						(h/json-response {:error "Failed to list manufacturers"} 500))))
			(h/unauthorized-response))))

(defn create-manufacturer-handler
	"Create manufacturer (admin/owner only)."
	[db]
	(fn [request]
		(if-let [_user-id (h/get-user-id request)]
			(if-let [forbidden (h/ensure-role request power-user-roles
													 "Only admins and owners can create manufacturers")]
				forbidden
				(try
					(let [body (h/read-body-params request)
								display-name (some-> (or (:display_name body) (:display-name body)) str str/trim)
								_ (when (str/blank? display-name)
										(throw (ex-info "display_name is required" {:status 400 :field :display_name})))
								create! (:create! manufacturers/service)
								manufacturer (create! db {:display_name display-name})]
						(h/json-response {:data manufacturer} 201))
					(catch clojure.lang.ExceptionInfo e
						(let [status (or (:status (ex-data e)) 400)]
							(h/json-response {:error (ex-message e)} status)))
					(catch Exception e
						(log/error e "Failed to create manufacturer")
						(h/json-response {:error "Failed to create manufacturer"} 500))))
			(h/unauthorized-response))))

(defn update-manufacturer-handler
	"Update manufacturer fields (admin/owner only)."
	[db]
	(fn [request]
		(if-let [_user-id (h/get-user-id request)]
			(if-let [forbidden (h/ensure-role request power-user-roles
													 "Only admins and owners can update manufacturers")]
				forbidden
				(try
					(let [manufacturer-id (h/try-parse-uuid (get-in request [:path-params :id]))
								body (h/read-body-params request)
								display-name (some-> (or (:display_name body) (:display-name body)) str str/trim)
								archived-at (or (:archived_at body) (:archived-at body))
								archived-at-provided? (or (contains? body :archived_at)
																					(contains? body :archived-at))
								payload (cond-> {}
													(some? display-name) (assoc :display_name display-name)
													archived-at-provided? (assoc :archived_at archived-at))
								update! (:update! manufacturers/service)]
						(cond
							(nil? manufacturer-id)
							(h/not-found-response "Manufacturer not found")

							(empty? payload)
							(h/json-response {:error "No manufacturer fields provided"} 400)

							:else
							(if-let [updated (update! db manufacturer-id payload)]
								(h/json-response {:data updated})
								(h/not-found-response "Manufacturer not found"))))
					(catch clojure.lang.ExceptionInfo e
						(let [status (or (:status (ex-data e)) 400)]
							(h/json-response {:error (ex-message e)} status)))
					(catch Exception e
						(log/error e "Failed to update manufacturer" {:manufacturer-id (get-in request [:path-params :id])})
						(h/json-response {:error "Failed to update manufacturer"} 500))))
			(h/unauthorized-response))))

(defn delete-manufacturer-handler
	"Delete manufacturer (admin/owner only)."
	[db]
	(fn [request]
		(if-let [_user-id (h/get-user-id request)]
			(if-let [forbidden (h/ensure-role request power-user-roles
													 "Only admins and owners can delete manufacturers")]
				forbidden
				(try
					(let [manufacturer-id (h/try-parse-uuid (get-in request [:path-params :id]))
								delete! (:delete! manufacturers/service)
								deleted? (when manufacturer-id (delete! db manufacturer-id))]
						(if deleted?
							(h/json-response {:success true})
							(h/not-found-response "Manufacturer not found")))
					(catch Exception e
						(log/error e "Failed to delete manufacturer" {:manufacturer-id (get-in request [:path-params :id])})
						(h/json-response {:error "Failed to delete manufacturer"} 500))))
			(h/unauthorized-response))))
