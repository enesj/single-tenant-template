(ns app.template.backend.services.monitoring.login-events
  "Simple login events recording and querying for admins and users."
  (:require
    [app.shared.adapters.database :as shared-db]
    [app.shared.query-builders :as shared-qb]
    [app.shared.type-conversion :as tc]
    [app.template.backend.security.email :as email-privacy]
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

(declare update-principal-last-login!)

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
  (let [principal-type-str (name principal-type)
        principal-id-uuid (cond
                            (instance? java.util.UUID principal-id) principal-id
                            (some? principal-id) (tc/try-parse-uuid principal-id)
                            :else (do
                                    (log/warn "record-login-event! called without principal-id"
                                      {:principal-type principal-type :success success})
                                    nil))]
    (when (and (some? principal-id) (nil? principal-id-uuid))
      (log/warn "record-login-event! called with invalid principal-id"
        {:principal-type principal-type
         :principal-id principal-id
         :success success}))
    (when principal-id-uuid
      (let [logged-at (time/instant)]
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
                       :created_at logged-at}]}))
        (when success
          (update-principal-last-login! db principal-type principal-id-uuid logged-at))))))

(defn- update-principal-last-login!
  [db principal-type principal-id logged-at]
  (let [target-table (case (keyword (name principal-type))
                       :user :users
                       :admin :admins
                       nil)]
    (when target-table
      (jdbc/execute-one! db
        (hsql/format {:update target-table
                      :set {:last_login_at logged-at}
                      :where [:= :id principal-id]})))))

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

(def ^:private allowed-login-events-order-by
  {:created-at :login_events.created_at
   :principal-type :login_events.principal_type
   :success :login_events.success
   :reason :login_events.reason
   :principal-ref :login_events.principal_id
   :principal-name [:raw "COALESCE(admins.full_name, users.full_name)"]})

(defn- build-login-events-conditions
  [{:keys [principal-type success?]}]
  (let [principal-type-db (principal-type->db principal-type)]
    (cond-> []
      principal-type-db (conj [:= :login_events.principal_type principal-type-db])
      (some? success?) (conj [:= :login_events.success success?]))))

(defn- build-login-events-list-query
  [{:keys [limit offset sorts order-by order-dir] :as opts}]
  (let [limit (or limit 100)
        offset (or offset 0)
        conditions (build-login-events-conditions opts)
        order-clauses (shared-qb/resolve-order-by-clauses
                        {:sorts sorts
                         :order-by order-by
                         :order-dir order-dir
                         :allowed-order-by allowed-login-events-order-by
                         :default-order-by :login_events.created_at
                         :default-order-dir :desc
                         :tie-breaker [:login_events.id :asc]})]
    (cond-> {:select [[:login_events.id :id]
                      :login_events.principal_type
                      :login_events.principal_id
                      :login_events.success
                      :login_events.reason
                      :login_events.ip
                      :login_events.user_agent
                      :login_events.created_at
                      [:admins.full_name :admin_name]
                      [:users.full_name :user_name]]
             :from [[:login_events]]
             :left-join [[:admins :admins] [:= :admins.id :login_events.principal_id]
                         [:users :users] [:= :users.id :login_events.principal_id]]
             :order-by order-clauses
             :limit limit
             :offset offset}
      (seq conditions) (assoc :where (into [:and] conditions)))))

(defn- build-login-events-count-query
  [opts]
  (let [conditions (build-login-events-conditions opts)]
    (cond-> {:select [[[:count :*] :total]]
             :from [[:login_events]]}
      (seq conditions) (assoc :where (into [:and] conditions)))))

(defn count-login-events
  "Count login events using the same filters as `list-login-events`."
  [db opts]
  (let [row (jdbc/execute-one! db (hsql/format (build-login-events-count-query opts)))
        total (or (:total row) (some-> row vals first) 0)]
    (long total)))

(defn list-login-events
  "List login events for admins and users for global admin monitoring.

   Options map:
   - :principal-type keyword or string (:admin or :user)
   - :success?       boolean to filter by success flag
   - :limit          integer, default 100
   - :offset         integer, default 0"
  [db {:keys [principal-type success? limit offset] :as opts}]
  (let [limit (or limit 100)
        offset (or offset 0)
        rows (jdbc/execute! db (hsql/format (build-login-events-list-query opts)))]
    (log/info "LOGIN-MONITOR: listed login events"
      {:principal-type principal-type
       :success? success?
       :limit limit
       :offset offset
       :row-count (count rows)})
    (mapv (fn [row]
            (let [converted (shared-db/to-app row)
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
                  admin-name (or (:admin-name converted)
                               (:admins/admin-name converted))
                  user-name (or (:user-name converted)
                              (:users/user-name converted))
                  principal-type-key (some-> principal-type-val name keyword)
                  principal-ref (case principal-type-key
                                  :admin (email-privacy/admin-ref principal-id-val)
                                  :user (email-privacy/user-ref principal-id-val)
                                  nil)
                  principal-name (or admin-name user-name principal-ref)]
              (-> converted
                ;; Normalize core fields to flat, non-namespaced keys
                (assoc :id id-val
                  :principal-id principal-id-val
                  :principal-type principal-type-val
                  :success success-val
                  :reason reason-val)
                (cond-> created-at (assoc :created-at (->millis created-at))
                  ip (assoc :ip-address ip)
                  ua (assoc :user-agent ua)
                  principal-ref (assoc :principal-ref principal-ref))
                (assoc :principal-name principal-name)
                (dissoc :admin-email :admin-name :user-email :user-name
                  :admins/admin-email :admins/admin-name
                  :users/user-email :users/user-name
                  :login-events/id :login-events/principal-id
                  :login-events/ip :login-events/user-agent
                  :login-events/created-at :login-events/principal-type
                  :login-events/success :login-events/reason))))
      rows)))

(defn list-login-events-page
  "List login events and include pagination metadata for server-backed pagination."
  [db {:keys [limit offset] :as opts}]
  (let [limit* (or limit 100)
        offset* (or offset 0)
        events (list-login-events db (assoc opts :limit limit* :offset offset*))
        total (count-login-events db opts)]
    {:events events
     :total total
     :limit limit*
     :offset offset*}))
