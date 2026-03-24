(ns app.domain.backend.expenses.services.receipts.janitor
  "Receipt file janitor: purge finalized receipt binaries after a retention window
   and clean up orphaned files from local receipt storage."
  (:require
    [app.domain.backend.expenses.services.receipts.storage :as receipt-storage]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log])
  (:import
    [java.nio.file Files]
    [java.time Instant]
    [java.sql Timestamp]))

(def ^:private default-storage-base-dir
  "upload/stripes")

(def ^:private default-older-than-days 60)
(def ^:private default-limit 200)
(def ^:private default-orphan-limit 200)

(defn- normalize-positive-long
  [value default-value]
  (let [n (long (or value default-value))]
    (if (neg? n) default-value n)))

(defn- cutoff-instant
  [older-than-days now]
  (.minusSeconds ^Instant now (* 86400 (normalize-positive-long older-than-days default-older-than-days))))

(defn list-purge-candidates
  "Return posted receipts whose original file can be safely purged.

  Current safety rule:
  - receipt status is `posted`
  - the receipt links to an expense id
  - the linked expense row exists and points back to this receipt
  - `file_purged_at` is still null
  - the receipt has aged past the retention window

  Options:
  - :older-than-days (default 60)
  - :limit (default 200)
  - :now (default Instant/now)"
  ([db]
   (list-purge-candidates db nil))
  ([db {:keys [older-than-days limit now]}]
   (let [older-than-days (normalize-positive-long older-than-days default-older-than-days)
         limit (max 1 (normalize-positive-long limit default-limit))
         now (or now (Instant/now))
         cutoff (cutoff-instant older-than-days now)
         query {:select [:r.id
                         :r.storage_key
                         :r.original_filename
                         :r.status
                         :r.expense_id
                         :r.updated_at
                         :r.file_size
                         :r.file_purged_at]
                :from [[:receipts :r]]
                :where [:and
                        [:= :r.status (receipt-storage/receipt-status-cast "posted")]
                        [:is :r.file_purged_at nil]
                        [:is-not :r.storage_key nil]
                        [:is-not :r.expense_id nil]
                        [:<= :r.updated_at cutoff]
                        [:exists {:select [1]
                                  :from [[:expenses :e]]
                                  :where [:and
                                          [:= :e.id :r.expense_id]
                                          [:= :e.receipt_id :r.id]]}]]
                :order-by [[:r.updated_at :asc]
                           [:r.id :asc]]
                :limit limit}]
     (jdbc/execute!
       db
       (sql/format query)
       {:builder-fn rs/as-unqualified-lower-maps}))))

(defn mark-file-purged!
  "Persist the timestamp that records an intentional receipt file purge.

  Returns the updated row (or nil when it was already marked)."
  ([db receipt-id]
   (mark-file-purged! db receipt-id nil))
  ([db receipt-id {:keys [at]}]
   (jdbc/execute-one!
     db
     (sql/format {:update :receipts
                  :set {:file_purged_at (Timestamp/from ^Instant (or at (Instant/now)))}
                  :where [:and
                          [:= :id receipt-id]
                          [:is :file_purged_at nil]]
                  :returning [:id :storage_key :file_purged_at]})
     {:builder-fn rs/as-unqualified-lower-maps})))

(defn purge-receipt-file!
  "Delete a receipt file from local storage and mark the DB row as intentionally purged.

  Options:
  - :storage-base-dir (default upload/stripes)
  - :dry-run? (default false)
  - :purge-at (default Instant/now when persisted)"
  ([db receipt]
   (purge-receipt-file! db receipt nil))
  ([db receipt {:keys [storage-base-dir dry-run? purge-at]
                :or {storage-base-dir default-storage-base-dir
                     dry-run? false}}]
   (let [receipt-id (:id receipt)
         storage-key (:storage_key receipt)
         file (receipt-storage/resolve-local-receipt-file storage-base-dir storage-key)
         file-existed? (some? file)
         base-result {:receipt-id receipt-id
                      :storage-key storage-key
                      :file-existed? file-existed?
                      :dry-run? (boolean dry-run?)}]
     (if dry-run?
       (assoc base-result :result :would-purge)
       (do
         (when file
           (Files/deleteIfExists (.toPath file)))
         (let [row (mark-file-purged! db receipt-id {:at purge-at})
               result (if file-existed? :purged :already-missing)]
           (log/info "Receipt file janitor processed receipt"
             {:receipt-id receipt-id
              :storage-key storage-key
              :result result
              :file-existed? file-existed?})
           (assoc base-result
             :result result
             :file-purged-at (:file_purged_at row))))))))

(defn list-referenced-storage-keys
  "Return the set of storage keys still referenced by receipts rows."
  [db]
  (->> (jdbc/execute!
         db
         (sql/format {:select [:storage_key]
                      :from [:receipts]
                      :where [:is-not :storage_key nil]})
         {:builder-fn rs/as-unqualified-lower-maps})
    (keep :storage_key)
    set))

(defn- hidden-file?
  [f]
  (or (.isHidden ^java.io.File f)
    (str/starts-with? (.getName ^java.io.File f) ".")))

(defn- file->storage-key
  [base-dir file]
  (-> (str (.relativize (.toPath ^java.io.File base-dir) (.toPath ^java.io.File file)))
    (str/replace "\\" "/")))

(defn- list-storage-files
  [storage-base-dir]
  (let [base-dir (io/file storage-base-dir)]
    (if-not (.exists base-dir)
      []
      (->> (file-seq base-dir)
        (filter #(.isFile ^java.io.File %))
        (remove hidden-file?)
        (map (fn [file]
               {:storage-key (file->storage-key base-dir file)
                :file file}))
        (sort-by :storage-key)
        vec))))

(defn list-orphaned-files
  "Return local storage files that are no longer referenced by any receipts row.

  Options:
  - :storage-base-dir (default upload/stripes)
  - :orphan-limit (default 200)"
  ([db]
   (list-orphaned-files db nil))
  ([db {:keys [storage-base-dir orphan-limit]
        :or {storage-base-dir default-storage-base-dir}}]
   (let [orphan-limit (max 1 (normalize-positive-long orphan-limit default-orphan-limit))
         referenced-keys (list-referenced-storage-keys db)]
     (->> (list-storage-files storage-base-dir)
       (remove (fn [{:keys [storage-key]}]
                 (contains? referenced-keys storage-key)))
       (take orphan-limit)
       vec))))

(defn delete-orphan-file!
  "Delete an orphaned file from local receipt storage."
  [{:keys [storage-key file dry-run?]}]
  (if dry-run?
    {:storage-key storage-key
     :result :would-delete-orphan
     :dry-run? true}
    (do
      (Files/deleteIfExists (.toPath ^java.io.File file))
      (log/info "Receipt file janitor deleted orphaned file" {:storage-key storage-key})
      {:storage-key storage-key
       :result :deleted-orphan
       :dry-run? false})))

(defn run-janitor!
  "Run one janitor pass.

  Options:
  - :storage-base-dir (default upload/stripes)
  - :older-than-days (default 60)
  - :limit (default 200)
  - :orphan-limit (default 200)
  - :delete-orphans? (default true)
  - :dry-run? (default false)
  - :now (mainly for tests)
  - :purge-at (mainly for tests)"
  [db {:keys [storage-base-dir older-than-days limit orphan-limit delete-orphans? dry-run? now purge-at]
       :or {storage-base-dir default-storage-base-dir
            delete-orphans? true
            dry-run? false}}]
  (let [opts {:storage-base-dir storage-base-dir
              :older-than-days older-than-days
              :limit limit
              :orphan-limit orphan-limit
              :dry-run? dry-run?
              :now now
              :purge-at purge-at}
        purge-candidates (list-purge-candidates db opts)
        purge-results (mapv (fn [receipt]
                              (try
                                (purge-receipt-file! db receipt opts)
                                (catch Exception e
                                  (log/error e "Receipt file janitor failed to purge candidate"
                                    {:receipt-id (:id receipt)
                                     :storage-key (:storage_key receipt)})
                                  {:receipt-id (:id receipt)
                                   :storage-key (:storage_key receipt)
                                   :result :error
                                   :error (.getMessage e)})))
                        purge-candidates)
        orphan-candidates (if delete-orphans?
                            (list-orphaned-files db opts)
                            [])
        orphan-results (mapv (fn [entry]
                               (try
                                 (delete-orphan-file! (assoc entry :dry-run? dry-run?))
                                 (catch Exception e
                                   (log/error e "Receipt file janitor failed to delete orphan"
                                     {:storage-key (:storage-key entry)})
                                   {:storage-key (:storage-key entry)
                                    :result :error
                                    :error (.getMessage e)})))
                         orphan-candidates)
        summary {:purge-candidates (count purge-candidates)
                 :purged (count (filter #(contains? #{:purged :already-missing :would-purge} (:result %)) purge-results))
                 :purge-errors (count (filter #(= :error (:result %)) purge-results))
                 :orphan-candidates (count orphan-candidates)
                 :orphan-deletions (count (filter #(contains? #{:deleted-orphan :would-delete-orphan} (:result %)) orphan-results))
                 :orphan-errors (count (filter #(= :error (:result %)) orphan-results))
                 :dry-run? (boolean dry-run?)}]
    (log/info "Receipt file janitor pass complete" summary)
    {:summary summary
     :purge-results purge-results
     :orphan-results orphan-results}))