(ns app.template.backend.routes.admin.entities.backlog
  "Backlog-specific CRUD operations for admin entity routes."
  (:require
    [clojure.string :as str]
    [next.jdbc :as next-jdbc]))

;; ── Enum constants ────────────────────────────────────────────────────────────

(def ^:private backlog-status-options
  #{"Waiting" "In progres" "Completed" "Need improvments"})

(def ^:private backlog-type-options
  #{"Issue" "Feature" "Refactoring" "Review" "Improvment"})

(def ^:private backlog-status-aliases
  {"waiting" "Waiting"
   "in progres" "In progres"
   "in progress" "In progres"
   "completed" "Completed"
   "need improvments" "Need improvments"
   "need improvements" "Need improvments"})

(def ^:private backlog-type-aliases
  {"issue" "Issue"
   "feature" "Feature"
   "refactoring" "Refactoring"
   "review" "Review"
   "improvment" "Improvment"
   "improvement" "Improvment"})

;; ── Private helpers ───────────────────────────────────────────────────────────

(defn- backlog-option-value
  [value]
  (if (map? value)
    (or (get value :value)
      (get value "value")
      value)
    value))

(defn- canonical-backlog-value
  [aliases value]
  (let [normalized (some-> value backlog-option-value str str/trim)
        lowered (some-> normalized str/lower-case)]
    (or (get aliases lowered) normalized)))

(defn- payload-value
  [payload k]
  (or (get payload k)
    (get payload (name k))))

(defn- backlog-row-value
  [row k]
  (or (get row k)
    (get row (keyword "backlog" (name k)))
    (get row (name k))))

(defn- normalize-fetched-backlog-item
  [row]
  {:id (backlog-row-value row :id)
   :number (backlog-row-value row :number)
   :description (backlog-row-value row :description)
   :status (backlog-row-value row :status)
   :type (backlog-row-value row :type)
   :priority (backlog-row-value row :priority)})

(defn- parse-backlog-priority!
  [value]
  (let [priority (try
                   (Integer/parseInt (str value))
                   (catch Exception _ nil))]
    (when-not (some? priority)
      (throw (ex-info "Backlog priority must be an integer"
               {:status 400
                :field :priority
                :value value})))
    (when-not (<= 1 priority 5)
      (throw (ex-info "Backlog priority must be between 1 and 5"
               {:status 400
                :field :priority
                :value value
                :allowed [1 2 3 4 5]})))
    priority))

(defn- normalize-backlog-enum!
  [field allowed aliases value]
  (let [canonical (canonical-backlog-value aliases value)]
    (when-not (contains? allowed canonical)
      (throw (ex-info
               (str "Invalid backlog " (name field))
               {:status 400
                :field field
                :value value
                :allowed (vec allowed)})))
    canonical))

(defn- require-backlog-description!
  [value]
  (let [description (some-> value str str/trim)]
    (when (or (nil? description) (str/blank? description))
      (throw (ex-info "Backlog description is required"
               {:status 400
                :field :description})))
    description))

(defn- fetch-backlog-item
  "Fetch a single backlog item by number using an existing transaction."
  [tx number]
  (next-jdbc/execute-one!
    tx
    ["SELECT number AS id, number, description, status::text AS status, type::text AS type, priority
      FROM backlog
      WHERE number = ?"
     number]))

;; ── Public API ────────────────────────────────────────────────────────────────

(defn parse-backlog-number!
  "Parse and validate a backlog number from a string or integer value.
  Throws ex-info with :status 400 on invalid input."
  [value]
  (let [parsed (try
                 (Integer/parseInt (str value))
                 (catch Exception _ nil))]
    (when-not (some? parsed)
      (throw (ex-info "Backlog number must be an integer"
               {:status 400
                :field :number
                :value value})))
    parsed))

(defn normalize-backlog-create-payload
  "Validate and normalize a create payload map into canonical backlog fields."
  [payload]
  (let [status-value (payload-value payload :status)
        priority-value (payload-value payload :priority)]
    {:description (require-backlog-description! (payload-value payload :description))
     :status (if (or (nil? status-value)
                   (and (string? status-value) (str/blank? status-value)))
               "Waiting"
               (normalize-backlog-enum! :status backlog-status-options backlog-status-aliases status-value))
     :type (normalize-backlog-enum! :type backlog-type-options backlog-type-aliases (payload-value payload :type))
     :priority (if (or (nil? priority-value)
                     (and (string? priority-value) (str/blank? priority-value)))
                 1
                 (parse-backlog-priority! priority-value))}))

(defn normalize-backlog-update-payload
  "Validate and normalize an update payload map into a partial backlog update.
  Throws ex-info with :status 400 when no recognized fields are supplied."
  [payload]
  (let [has-key? (fn [k]
                   (or (contains? payload k)
                     (contains? payload (name k))))
        status-value (payload-value payload :status)
        type-value (payload-value payload :type)
        priority-value (payload-value payload :priority)
        present-update-value? (fn [value]
                                (not (or (nil? value)
                                       (and (string? value)
                                         (str/blank? value)))))
        updates (cond-> {}
                  (has-key? :description)
                  (assoc :description (require-backlog-description! (payload-value payload :description)))

                  (and (has-key? :status) (present-update-value? status-value))
                  (assoc :status (normalize-backlog-enum! :status backlog-status-options backlog-status-aliases status-value))

                  (and (has-key? :type) (present-update-value? type-value))
                  (assoc :type (normalize-backlog-enum! :type backlog-type-options backlog-type-aliases type-value))

                  (and (has-key? :priority) (present-update-value? priority-value))
                  (assoc :priority (parse-backlog-priority! priority-value)))]
    (when (empty? updates)
      (throw (ex-info "No backlog fields supplied for update"
               {:status 400
                :field :payload})))
    updates))

(defn get-backlog-item
  "Fetch a single backlog item by number, opening its own transaction."
  [db number]
  (next-jdbc/with-transaction [tx db]
    (next-jdbc/execute-one! tx ["SET LOCAL app.bypass_rls = true"])
    (fetch-backlog-item tx number)))

(defn list-backlog-items
  "Return all backlog items ordered by priority then number."
  [db]
  (next-jdbc/with-transaction [tx db]
    (next-jdbc/execute-one! tx ["SET LOCAL app.bypass_rls = true"])
    (next-jdbc/execute!
      tx
      ["SELECT number AS id, number, description, status::text AS status, type::text AS type, priority
        FROM backlog
        ORDER BY priority ASC, number ASC"])))

(defn create-backlog-item!
  "Insert a new backlog row from a pre-normalized data map."
  [db data]
  (next-jdbc/with-transaction [tx db]
    (next-jdbc/execute-one! tx ["SET LOCAL app.bypass_rls = true"])
    (next-jdbc/execute-one!
      tx
      ["INSERT INTO backlog (description, status, type, priority)
        VALUES (?, ?::backlog_status, ?::backlog_type, ?)
        RETURNING number AS id, number, description, status::text AS status, type::text AS type, priority"
       (:description data)
       (:status data)
       (:type data)
       (:priority data)])))

(defn update-backlog-item!
  "Apply a partial update map to the backlog item identified by number.
  Returns nil when the item does not exist."
  [db number updates]
  (next-jdbc/with-transaction [tx db]
    (next-jdbc/execute-one! tx ["SET LOCAL app.bypass_rls = true"])
    (if-let [existing (fetch-backlog-item tx number)]
      (let [normalized-existing (normalize-fetched-backlog-item existing)
            final-state (merge normalized-existing updates)]
        (next-jdbc/execute-one!
          tx
          ["UPDATE backlog
            SET description = ?,
                status = ?::backlog_status,
                type = ?::backlog_type,
                priority = ?
            WHERE number = ?
            RETURNING number AS id, number, description, status::text AS status, type::text AS type, priority"
           (:description final-state)
           (:status final-state)
           (:type final-state)
           (:priority final-state)
           number]))
      nil)))

(defn delete-backlog-item!
  "Delete the backlog item identified by number. Returns the deleted row or nil."
  [db number]
  (next-jdbc/with-transaction [tx db]
    (next-jdbc/execute-one! tx ["SET LOCAL app.bypass_rls = true"])
    (next-jdbc/execute-one!
      tx
      ["DELETE FROM backlog
        WHERE number = ?
        RETURNING number AS id, number"
       number])))

(defn batch-delete-backlog-items!
  "Delete backlog items by a seq of numbers. Returns the count of deleted rows."
  [db numbers]
  (if (empty? numbers)
    0
    (next-jdbc/with-transaction [tx db]
      (next-jdbc/execute-one! tx ["SET LOCAL app.bypass_rls = true"])
      (let [placeholders (str/join "," (repeat (count numbers) "?"))
            sql (str "DELETE FROM backlog WHERE number IN (" placeholders ")")
            params (into [sql] numbers)
            result (next-jdbc/execute-one! tx params)]
        (:next.jdbc/update-count result 0)))))
