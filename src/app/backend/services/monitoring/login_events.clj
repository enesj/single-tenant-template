(ns app.backend.services.monitoring.login-events
  "Simple login events recording and querying for admins and users."
  (:require
    [app.shared.adapters.database :as db-adapter]
    [app.shared.type-conversion :as tc]
    [honey.sql :as hsql]
    [java-time.api :as time]
    [next.jdbc :as jdbc]
    [taoensso.timbre :as log])
  (:import
    [java.util UUID]))

;; Helper to cast principal-type to the database enum type used by :login_events.principal_type
(defn- principal-type->db
  [principal-type]
  (when principal-type
    (tc/cast-for-database :login-principal-type (name principal-type))))

(defn record-login-event!
  "Record a login attempt for an admin or user.

   Params map keys:
   - :principal-type  keyword or string, e.g. :admin or :user
   - :principal-id    UUID or string UUID
   - :success         boolean, true for successful login
   - :reason          optional failure reason string
   - :ip              optional client IP string
   - :user-agent      optional user-agent string"
  [db {:keys [principal-type principal-id success reason ip user-agent]
       :or {success false}}]
  (try
    (let [principal-type-str (name principal-type)
          principal-id-uuid (cond
                              (instance? java.util.UUID principal-id) principal-id
                              (some? principal-id) (java.util.UUID/fromString (str principal-id))
                              :else (do
                                      (log/warn "record-login-event! called without principal-id"
                                        {:principal-type principal-type :success success})
                                      nil))]
      (when principal-id-uuid
        (log/info "LOGIN-MONITOR: recording login event"
          {:principal-type principal-type-str
           :principal-id (str principal-id-uuid)
           :success success
           :reason reason
           :ip ip})
        (jdbc/execute-one! db
          (hsql/format
            {:insert-into :login_events
             :values [{:id (UUID/randomUUID)
                       :principal_type (tc/cast-for-database :login-principal-type principal-type-str)
                       :principal_id principal-id-uuid
                       :success success
                       :reason reason
                       :ip ip
                       :user_agent user-agent
                       :created_at (time/instant)}]}))))
    (catch Exception e
      (log/error e "Failed to record login event"
        {:principal-type principal-type
         :principal-id principal-id
         :success success
         :reason reason})
      nil)))

(defn count-recent-login-events
  "Count login events since a given instant.

   Options map may contain:
   - :since           java.time.Instant cutoff (required)
   - :principal-type  optional keyword/string to filter by :admin or :user
   - :success?        optional boolean to filter by success flag"
  [db {:keys [since principal-type success?]}]
  (when since
    (let [base-conditions [[:>= :created_at since]]
          principal-type-db (principal-type->db principal-type)
          conditions (cond-> base-conditions
                       principal-type-db (conj [:= :principal_type principal-type-db])
                       (some? success?) (conj [:= :success success?]))
          where-clause (into [:and] conditions)
          row (jdbc/execute-one! db
                (hsql/format {:select [[[:count :*] :count]]
                              :from [:login_events]
                              :where where-clause}))]
      (or (:count row) 0))))

(defn get-login-history
  "Get recent login events for a principal.

   - principal-type: keyword/string :admin or :user
   - principal-id:   UUID of the principal
   Options map may include :limit and :offset."
  [db principal-type principal-id {:keys [limit offset] :or {limit 50 offset 0}}]
  (let [principal-type-db (principal-type->db principal-type)
        sql-map {:select [:*]
                 :from [:login_events]
                 :where [:and
                         [:= :principal_type principal-type-db]
                         [:= :principal_id principal-id]]
                 :order-by [[:created_at :desc]]
                 :limit limit
                 :offset offset}
        rows (jdbc/execute! db (hsql/format sql-map))]
    (log/info "LOGIN-MONITOR: fetched login history"
      {:principal-type principal-type
       :principal-id (str principal-id)
       :row-count (count rows)})
    rows))

(defn- ->millis
  "Normalize Java time instances to epoch millis for frontend consumption."
  [v]
  (cond
    (instance? java.time.Instant v) (.toEpochMilli ^java.time.Instant v)
    (instance? java.time.OffsetDateTime v) (.toEpochMilli (.toInstant ^java.time.OffsetDateTime v))
    :else v))

(defn list-login-events
  "List login events for admins and users for global admin monitoring.

   Options map:
   - :principal-type keyword or string (:admin or :user)
   - :success?       boolean to filter by success flag
   - :limit          integer, default 100
   - :offset         integer, default 0"
  [db {:keys [principal-type success? limit offset]}]
  (let [limit (or limit 100)
        offset (or offset 0)
        principal-type-db (principal-type->db principal-type)
        base {:select [[:login_events.id :id]
                       :login_events.principal_type
                       :login_events.principal_id
                       :login_events.success
                       :login_events.reason
                       :login_events.ip
                       :login_events.user_agent
                       :login_events.created_at
                       [:admins.email :admin_email]
                       [:admins.full_name :admin_name]
                       [:users.email :user_email]
                       [:users.full_name :user_name]]
              :from [[:login_events]]
              :left-join [[:admins :admins] [:= :admins.id :login_events.principal_id]
                          [:users :users] [:= :users.id :login_events.principal_id]]
              :order-by [[:login_events.created_at :desc]]
              :limit limit
              :offset offset}
        conditions (cond-> []
                     principal-type-db (conj [:= :login_events.principal_type principal-type-db])
                     (some? success?) (conj [:= :login_events.success success?]))
        sql-map (cond-> base
                  (seq conditions) (assoc :where (into [:and] conditions)))
        rows (jdbc/execute! db (hsql/format sql-map))]
    (log/info "LOGIN-MONITOR: listed login events"
      {:principal-type principal-type
       :success? success?
       :limit limit
       :offset offset
       :row-count (count rows)})
    (mapv (fn [row]
            (let [converted (-> row
                              db-adapter/convert-pg-objects
                              db-adapter/convert-db-keys->app-keys)
                  id-val (or (:id converted)
                             (:login-events/id converted))
                  principal-id-val (or (:principal-id converted)
                                       (:login-events/principal-id converted))
                  ;; Support both plain and namespaced keys from the DB adapter
                  created-at (or (:created-at converted)
                               (:login-events/created-at converted))
                  ip (or (:ip converted)
                       (:login-events/ip converted))
                  ua (or (:user-agent converted)
                       (:login-events/user-agent converted))
                  principal-type-val (or (:principal-type converted)
                                       (:login-events/principal-type converted))
                  success-val (or (:success converted)
                                (:login-events/success converted))
                  reason-val (or (:reason converted)
                               (:login-events/reason converted))
                  admin-email (or (:admin-email converted)
                                (:admins/admin-email converted))
                  admin-name (or (:admin-name converted)
                               (:admins/admin-name converted))
                  user-email (or (:user-email converted)
                               (:users/user-email converted))
                  user-name (or (:user-name converted)
                              (:users/user-name converted))
                  principal-email (or admin-email user-email)
                  principal-name (or admin-name user-name)]
              (-> converted
                ;; Normalize core fields to flat, non-namespaced keys
                (assoc :id id-val
                  :principal-id principal-id-val
                  :principal-type principal-type-val
                  :success success-val
                  :reason reason-val)
                (cond-> created-at (assoc :created-at (->millis created-at))
                  ip (assoc :ip-address ip)
                  ua (assoc :user-agent ua))
                (assoc :principal-email principal-email
                  :principal-name principal-name)
                (dissoc :admin-email :admin-name :user-email :user-name
                  :admins/admin-email :admins/admin-name
                  :users/user-email :users/user-name
                  :login-events/id :login-events/principal-id
                  :login-events/ip :login-events/user-agent
                  :login-events/created-at :login-events/principal-type
                  :login-events/success :login-events/reason))))
      rows)))
