(ns app.admin.frontend.events.users.utils
  "Shared utilities for user events - state management, API helpers, error handling"
  (:require
    [ajax.core :as ajax]
    [clojure.string :as str]
    [taoensso.timbre :as log]))

;; ============================================================================
;; State Update Utilities
;; ============================================================================

(defn update-user-in-admin-state
  "Updates a user in the :admin/users array"
  [db user-id update-fn]
  (update db :admin/users
    (fn [users]
      (mapv #(if (= (:users/id %) user-id)
               (update-fn %)
               %)
        users))))

(defn update-user-in-entity-store
  "Updates a user in the entity store that the template system uses"
  [db user-id update-fn]
  (update-in db [:entities :users :data]
    (fn [users-by-id]
      (if-let [user (get users-by-id user-id)]
        (assoc users-by-id user-id (update-fn user))
        users-by-id))))

(defn sync-admin-and-entity-stores
  "Updates user data in both admin and entity stores"
  [db user-id update-fn]
  (-> db
    (update-user-in-admin-state user-id update-fn)
    (update-user-in-entity-store user-id update-fn)))

(defn bulk-update-users-in-admin-state
  "Updates multiple users in the :admin/users array"
  [db user-ids update-fn]
  (update db :admin/users
    (fn [users]
      (mapv #(if (some #{(:users/id %)} user-ids)
               (update-fn %)
               %)
        users))))

(defn bulk-update-users-in-entity-store
  "Updates multiple users in the entity store"
  [db user-ids update-fn]
  (update-in db [:entities :users :data]
    (fn [users-by-id]
      (reduce (fn [acc user-id]
                (if-let [user (get acc user-id)]
                  (assoc acc user-id (update-fn user))
                  acc))
        users-by-id
        user-ids))))

(defn sync-bulk-admin-and-entity-stores
  "Updates multiple users in both admin and entity stores"
  [db user-ids update-fn]
  (-> db
    (bulk-update-users-in-admin-state user-ids update-fn)
    (bulk-update-users-in-entity-store user-ids update-fn)))

;; ============================================================================
;; HTTP Request Utilities
;; ============================================================================

(defn create-user-http-request
  "Creates standard HTTP request configuration for user operations.

   Automatically includes the admin token from localStorage when available,
   unless an explicit `x-admin-token` header is already provided."
  [method uri & {:keys [params on-success on-failure headers]
                 :or {params {}
                      headers {"Content-Type" "application/json"}}}]
  (let [token (.getItem js/localStorage "admin-token")
        headers' (cond-> headers
                   (and token (not (contains? headers "x-admin-token")))
                   (assoc "x-admin-token" token))]
    {:method method
     :uri uri
     :params params
     :headers headers'
     :format (ajax/json-request-format)
     :response-format (ajax/json-response-format {:keywords? true})
     :on-success on-success
     :on-failure on-failure}))

(defn create-loading-db-state
  "Sets loading state in database"
  [db loading-key]
  (assoc db loading-key true))

(defn clear-loading-db-state
  "Clears loading state in database"
  [db loading-key]
  (assoc db loading-key false))

;; ============================================================================
;; Error Handling Utilities
;; ============================================================================

(defn handle-user-api-error
  "Standard error handling for user API operations"
  [db error loading-key error-key operation-name]
  (log/error (str "Failed " operation-name ":") error)
  (-> db
    (assoc loading-key false)
    (assoc error-key (str error))))

(defn log-user-operation
  "Standard logging for user operations"
  [operation-name & args]
  (log/info (str operation-name ":") (clojure.string/join " " (map str args))))

;; ============================================================================
;; Success Message Utilities
;; ============================================================================

(defn show-user-success-message
  "Shows success message and optionally dispatches additional events"
  [db message & {:keys [dispatch-events]
                 :or {dispatch-events []}}]
  (let [db' (assoc db :admin/success-message message)]
    (if (empty? dispatch-events)
      {:db db'}
      {:db db'
       :dispatch-n dispatch-events})))

;; ============================================================================
;; File Download Utilities
;; ============================================================================

(defn create-download-link
  "Creates and triggers download of blob response"
  [blob-response filename]
  (let [url (.createObjectURL js/URL blob-response)
        link (.createElement js/document "a")]
    (.setAttribute link "href" url)
    (.setAttribute link "download" filename)
    (.click link)
    (.revokeObjectURL js/URL url)))

(defn generate-export-filename
  "Generates standardized export filename"
  [base-name & {:keys [user-id timestamp]
                :or {timestamp (.toISOString (js/Date.))}}]
  (if user-id
    (str base-name "-" user-id "-" timestamp ".csv")
    (str base-name "-" timestamp ".csv")))

;; Shared utilities will be extracted here
;; - update-user-in-admin-state
;; - update-user-in-entity-store
;; - sync-admin-and-entity-stores
;; - create-user-http-request
;; - handle-user-api-error
;; - show-user-success-message
