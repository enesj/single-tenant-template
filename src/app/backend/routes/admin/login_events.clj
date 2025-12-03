(ns app.backend.routes.admin.login-events
  "Admin login events handlers"
  (:require
    [app.backend.routes.admin.utils :as utils]
    [app.backend.services.monitoring.login-events :as login-monitoring]
    [next.jdbc :as next-jdbc]
    [taoensso.timbre :as log]))

(defn get-login-events-handler
  "Get login events for admins and users with optional filtering."
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [params (:params request)
            pagination (utils/extract-pagination-params params
                         :default-limit 100)
            principal-type (some-> (:principal-type params) keyword)
            success (utils/parse-boolean-param params :success)
            options (merge pagination
                      {:principal-type principal-type
                       :success? success})
            events (login-monitoring/list-login-events db options)]
        (log/info "ADMIN LOGIN EVENTS: fetched events" {:count (count events)})
        (utils/json-response {:events events})))
    "Failed to retrieve login events"))

(defn delete-login-event-handler
  "Delete a single login event (admin action)"
  [db]
  (utils/with-error-handling
    (fn [request]
      (utils/handle-uuid-request request :id
        (fn [event-id request]
          (let [admin (:admin request)]
            (log/info "Admin" (:email admin) "attempting to delete login event" event-id)

            ;; Execute delete with admin context - set RLS bypass
            (let [result (next-jdbc/with-transaction [tx db]
                           ;; Set admin bypass context
                           (next-jdbc/execute-one! tx ["SET LOCAL app.bypass_rls = true"])
                           ;; Execute the delete
                           (next-jdbc/execute-one! tx
                             ["DELETE FROM login_events WHERE id = ?::uuid" event-id]))]
              (if (> (:next.jdbc/update-count result) 0)
                (do
                  (log/info "Successfully deleted login event" event-id "by admin" (:email admin))
                  (utils/log-admin-action "delete_login_event" (:id admin)
                    "login_event" event-id {})
                  (utils/success-response {:message "Login event deleted successfully"}))
                (utils/error-response "Login event not found" :status 404)))))))
    "Failed to delete login event"))

(defn bulk-delete-login-events-handler
  "Delete multiple login events (admin action)"
  [db]
  (utils/with-error-handling
    (fn [request]
      (let [admin (:admin request)
            body (:body request)
            ids (or (:ids body) [])
            ;; Parse UUIDs
            parsed-ids (keep utils/parse-uuid-custom ids)]
        (log/info "Admin" (:email admin) "attempting to bulk delete" (count parsed-ids) "login events")

        (if (empty? parsed-ids)
          (utils/error-response "No valid IDs provided" :status 400)
          (let [result (next-jdbc/with-transaction [tx db]
                         ;; Set admin bypass context
                         (next-jdbc/execute-one! tx ["SET LOCAL app.bypass_rls = true"])
                         ;; Execute the bulk delete using ANY for array matching
                         (next-jdbc/execute-one! tx
                           [(str "DELETE FROM login_events WHERE id = ANY(?::uuid[])")
                            (into-array String (map str parsed-ids))]))
                deleted-count (:next.jdbc/update-count result)]
            (log/info "Successfully bulk deleted" deleted-count "login events by admin" (:email admin))
            (utils/log-admin-action "bulk_delete_login_events" (:id admin)
              "login_events" nil {:count deleted-count :ids (map str parsed-ids)})
            (utils/success-response {:message (str deleted-count " login events deleted successfully")
                                     :deleted-count deleted-count})))))
    "Failed to bulk delete login events"))

(defn routes
  "Login events route definitions"
  [db]
  [""
   ["" {:get (get-login-events-handler db)}]
   ["/bulk" {:delete (bulk-delete-login-events-handler db)}]
   ["/:id" {:delete (delete-login-event-handler db)}]])
