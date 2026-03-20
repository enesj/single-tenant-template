(ns app.template.backend.routes.admin.entities.backlog
  "Backlog-specific CRUD operations for admin entity routes."
  (:require
    [clojure.string :as str]
    [next.jdbc :as next-jdbc]))

;; ── Enum constants ────────────────────────────────────────────────────────────

(def ^:private backlog-status-options
  #{"Waiting" "In progress" "Completed" "Need improvements"})

(def ^:private backlog-type-options
  #{"Issue" "Feature" "Refactoring" "Review" "Improvement"})

(def ^:private backlog-status-aliases
  {"waiting" "Waiting"
   "in progres" "In progress"
   "in progress" "In progress"
   "completed" "Completed"
   "need improvments" "Need improvements"
   "need improvements" "Need improvements"})

(def ^:private backlog-type-aliases
  {"issue" "Issue"
   "feature" "Feature"
   "refactoring" "Refactoring"
   "review" "Review"
   "improvment" "Improvement"
   "improvement" "Improvement"})

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
   :type (backlog-row-value row :type)})

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
    ["SELECT number AS id, number, description, status::text AS status, type::text AS type
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
  (let [status-value (payload-value payload :status)]
    {:description (require-backlog-description! (payload-value payload :description))
     :status (if (or (nil? status-value)
                   (and (string? status-value) (str/blank? status-value)))
               "Waiting"
               (normalize-backlog-enum! :status backlog-status-options backlog-status-aliases status-value))
     :type (normalize-backlog-enum! :type backlog-type-options backlog-type-aliases (payload-value payload :type))}))

(defn normalize-backlog-update-payload
  "Validate and normalize an update payload map into a partial backlog update.
  Throws ex-info with :status 400 when no recognized fields are supplied."
  [payload]
  (let [has-key? (fn [k]
                   (or (contains? payload k)
                     (contains? payload (name k))))
        status-value (payload-value payload :status)
        type-value (payload-value payload :type)
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
                  (assoc :type (normalize-backlog-enum! :type backlog-type-options backlog-type-aliases type-value)))]
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
  "Return all backlog items ordered by status workflow position then number."
  [db]
  (next-jdbc/with-transaction [tx db]
    (next-jdbc/execute-one! tx ["SET LOCAL app.bypass_rls = true"])
    (next-jdbc/execute!
      tx
      ["SELECT number AS id, number, description, status::text AS status, type::text AS type
        FROM backlog
        ORDER BY CASE status::text
                   WHEN 'In progress' THEN 1
                   WHEN 'Waiting' THEN 2
                   WHEN 'Need improvements' THEN 3
                   WHEN 'Completed' THEN 4
                   ELSE 5
                 END ASC, number ASC"])))

(defn create-backlog-item!
  "Insert a new backlog row from a pre-normalized data map."
  [db data]
  (next-jdbc/with-transaction [tx db]
    (next-jdbc/execute-one! tx ["SET LOCAL app.bypass_rls = true"])
    (next-jdbc/execute-one!
      tx
      ["INSERT INTO backlog (description, status, type)
        VALUES (?, ?::backlog_status, ?::backlog_type)
        RETURNING number AS id, number, description, status::text AS status, type::text AS type"
       (:description data)
       (:status data)
       (:type data)])))

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
                type = ?::backlog_type
            WHERE number = ?
            RETURNING number AS id, number, description, status::text AS status, type::text AS type"
           (:description final-state)
           (:status final-state)
           (:type final-state)
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
