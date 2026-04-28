(ns app.template.backend.security.privacy-subject-backfill
  "Backfill and cut over operational ownership columns to privacy subject refs.

   This namespace intentionally computes subject refs in application code, using
   `PRIVACY_SUBJECT_KEY_B64`, rather than in SQL. The mapping secret must not be
   stored in or derivable from the database."
  (:require
    [app.template.backend.security.privacy-subject :as privacy-subject]
    [clojure.java.io :as io]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.time Instant]))

(def ^:private expense-pairs
  [{:legacy :user_id
    :subject :subject_ref}
   {:legacy :created_by
    :subject :created_by_subject_ref}])

(def ^:private receipt-pairs
  [{:legacy :user_id
    :subject :subject_ref}
   {:legacy :created_by
    :subject :created_by_subject_ref}])

(def ^:private operational-table-specs
  [{:table :expenses
    :label :expenses
    :pairs expense-pairs}
   {:table :receipts
    :label :receipts
    :pairs receipt-pairs}])

(defn- update-count
  [result]
  (or (get result :next.jdbc/update-count)
    (get result :update-count)
    0))

(defn- candidate-clause
  [pairs cutover?]
  (let [pair-clauses (mapv (fn [{:keys [legacy subject]}]
                             (if cutover?
                               [:is-not legacy nil]
                               [:and
                                [:is-not legacy nil]
                                [:is subject nil]]))
                       pairs)]
    (into [:or] pair-clauses)))

(defn- select-candidates
  [db {:keys [table pairs]} {:keys [cutover? limit]}]
  (let [fields (->> pairs
                 (mapcat (juxt :legacy :subject))
                 (cons :id)
                 distinct
                 vec)]
    (jdbc/execute!
      db
      (sql/format (cond-> {:select fields
                           :from [table]
                           :where (candidate-clause pairs cutover?)
                           :order-by [[:id :asc]]}
                    limit (assoc :limit limit)))
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- row-updates
  "Return update fields for one operational row.

   Without cutover, this only fills missing subject-ref columns. With cutover,
   it also nulls direct users.id columns once a subject ref exists or can be
   computed from the direct link."
  [pairs row {:keys [cutover?]}]
  (reduce
    (fn [updates {:keys [legacy subject]}]
      (let [legacy-value (get row legacy)
            existing-subject (get row subject)
            computed-subject (privacy-subject/user-subject-ref legacy-value)
            effective-subject (or existing-subject computed-subject)]
        (cond-> updates
          (and legacy-value (nil? existing-subject) computed-subject)
          (assoc subject computed-subject)

          (and cutover? legacy-value effective-subject)
          (assoc legacy nil))))
    {}
    pairs))

(defn- apply-operational-table!
  [db {:keys [table label pairs] :as spec} {:keys [dry-run?] :as opts}]
  (let [rows (select-candidates db spec opts)]
    (reduce
      (fn [acc row]
        (let [updates (row-updates pairs row opts)]
          (cond
            (empty? updates)
            (update acc :skipped inc)

            dry-run?
            (-> acc
              (update :would-update inc)
              (update :examples conj {:id (:id row)
                                      :updates (set (keys updates))}))

            :else
            (let [updated (update-count
                            (jdbc/execute-one!
                              db
                              (sql/format {:update table
                                           :set updates
                                           :where [:= :id (:id row)]})))]
              (-> acc
                (update :updated + updated)
                (update :examples conj {:id (:id row)
                                        :updates (set (keys updates))}))))))
      {:table label
       :scanned (count rows)
       :would-update 0
       :updated 0
       :skipped 0
       :examples []}
      rows)))

(defn- select-settings-candidates
  [db {:keys [cutover? limit]}]
  (jdbc/execute!
    db
    (sql/format (cond-> {:select [:id
                                  :tenant_id
                                  :user_id
                                  :subject_ref
                                  :default_payer_id
                                  :receipt_ocr_provider]
                         :from [:user_expense_settings]
                         :where (if cutover?
                                  [:is-not :user_id nil]
                                  [:and
                                   [:is-not :user_id nil]
                                   [:is :subject_ref nil]])
                         :order-by [[:tenant_id :asc]
                                    [:id :asc]]}
                  limit (assoc :limit limit)))
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- find-settings-by-subject
  [db tenant-id subject-ref row-id]
  (jdbc/execute-one!
    db
    (sql/format {:select [:id
                          :default_payer_id
                          :receipt_ocr_provider]
                 :from [:user_expense_settings]
                 :where [:and
                         [:= :tenant_id tenant-id]
                         [:= :subject_ref subject-ref]
                         [:<> :id row-id]]
                 :limit 1})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- merged-settings-fields
  "Merge a legacy settings row into an existing subject-ref row.

   Prefer the already-subject-owned row, because it may contain a newer explicit
   choice. Fill only missing values from the legacy row."
  [target source]
  {:default_payer_id (or (:default_payer_id target)
                       (:default_payer_id source))
   :receipt_ocr_provider (or (:receipt_ocr_provider target)
                           (:receipt_ocr_provider source)
                           "mistral")
   :updated_at [:now]})

(defn- apply-settings-row!
  [db row {:keys [dry-run? cutover?]}]
  (let [subject-ref (or (:subject_ref row)
                      (privacy-subject/user-subject-ref (:user_id row)))
        target (when subject-ref
                 (find-settings-by-subject db (:tenant_id row) subject-ref (:id row)))]
    (cond
      (nil? subject-ref)
      {:action :skipped
       :id (:id row)
       :reason :missing-subject-ref}

      target
      (if dry-run?
        {:action :would-merge-delete
         :id (:id row)
         :target-id (:id target)}
        (do
          (jdbc/execute-one!
            db
            (sql/format {:update :user_expense_settings
                         :set (merged-settings-fields target row)
                         :where [:= :id (:id target)]}))
          (jdbc/execute-one!
            db
            (sql/format {:delete-from :user_expense_settings
                         :where [:= :id (:id row)]}))
          {:action :merged-deleted
           :id (:id row)
           :target-id (:id target)}))

      :else
      (let [updates (cond-> {:subject_ref subject-ref
                             :updated_at [:now]}
                      cutover? (assoc :user_id nil))]
        (if dry-run?
          {:action :would-update
           :id (:id row)
           :updates (set (keys updates))}
          (do
            (jdbc/execute-one!
              db
              (sql/format {:update :user_expense_settings
                           :set updates
                           :where [:= :id (:id row)]}))
            {:action :updated
             :id (:id row)
             :updates (set (keys updates))}))))))

(defn- apply-settings!
  [db opts]
  (let [rows (select-settings-candidates db opts)]
    (reduce
      (fn [acc row]
        (let [{:keys [action] :as result} (apply-settings-row! db row opts)]
          (-> acc
            (update :results conj result)
            (update action (fnil inc 0)))))
      {:table :user-expense-settings
       :scanned (count rows)
       :results []}
      rows)))

(defn linkage-stats
  "Return counts showing remaining direct operational users.id links."
  [db]
  (jdbc/execute-one!
    db
    [(str
       "SELECT\n"
       "  (SELECT count(*) FROM expenses) AS expenses_total,\n"
       "  (SELECT count(*) FROM expenses WHERE user_id IS NOT NULL) AS expenses_user_id_links,\n"
       "  (SELECT count(*) FROM expenses WHERE user_id IS NOT NULL AND subject_ref IS NULL) AS expenses_missing_subject_ref,\n"
       "  (SELECT count(*) FROM expenses WHERE created_by IS NOT NULL) AS expenses_created_by_links,\n"
       "  (SELECT count(*) FROM expenses WHERE created_by IS NOT NULL AND created_by_subject_ref IS NULL) AS expenses_missing_created_by_subject_ref,\n"
       "  (SELECT count(*) FROM receipts) AS receipts_total,\n"
       "  (SELECT count(*) FROM receipts WHERE user_id IS NOT NULL) AS receipts_user_id_links,\n"
       "  (SELECT count(*) FROM receipts WHERE user_id IS NOT NULL AND subject_ref IS NULL) AS receipts_missing_subject_ref,\n"
       "  (SELECT count(*) FROM receipts WHERE created_by IS NOT NULL) AS receipts_created_by_links,\n"
       "  (SELECT count(*) FROM receipts WHERE created_by IS NOT NULL AND created_by_subject_ref IS NULL) AS receipts_missing_created_by_subject_ref,\n"
       "  (SELECT count(*) FROM user_expense_settings) AS settings_total,\n"
       "  (SELECT count(*) FROM user_expense_settings WHERE user_id IS NOT NULL) AS settings_user_id_links,\n"
       "  (SELECT count(*) FROM user_expense_settings WHERE user_id IS NOT NULL AND subject_ref IS NULL) AS settings_missing_subject_ref")]
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- write-report!
  [report]
  (let [file (str "tmp/privacy-subject-backfill-" (System/currentTimeMillis) ".edn")]
    (io/make-parents file)
    (spit file (pr-str report))
    file))

(defn backfill-privacy-subjects!
  "Backfill subject refs and optionally cut over direct operational user links.

   Options:
   - `:dry-run?` defaults to true and performs no writes.
   - `:cutover?` defaults to false. When true, direct `users.id` columns are
     nulled after their matching subject-ref columns are present.
   - `:limit` limits candidate rows per table for batch operation.
   - `:write-report?` defaults to true and writes an EDN report under `tmp/`."
  ([db]
   (backfill-privacy-subjects! db {}))
  ([db {:keys [dry-run? cutover? write-report?]
        :or {dry-run? true
             cutover? false
             write-report? true}
        :as opts}]
   (let [opts* (assoc opts
                 :dry-run? dry-run?
                 :cutover? cutover?)
         run! (fn [conn]
                (let [before (linkage-stats conn)
                      tables (mapv #(apply-operational-table! conn % opts*) operational-table-specs)
                      settings (apply-settings! conn opts*)
                      after (if dry-run?
                              before
                              (linkage-stats conn))]
                  {:timestamp (str (Instant/now))
                   :dry-run? dry-run?
                   :cutover? cutover?
                   :limit (:limit opts*)
                   :before before
                   :tables tables
                   :settings settings
                   :after after}))
         report (if dry-run?
                  (run! db)
                  (jdbc/with-transaction [tx db]
                    (run! tx)))
         report-file (when write-report?
                       (write-report! report))]
     (cond-> report
       report-file (assoc :report-file report-file)))))

(comment
  ;; Dry-run first, then apply in controlled batches if the configured
  ;; PRIVACY_SUBJECT_KEY_B64 is stable for the target environment.
  ;;
  ;; (require '[app.template.backend.migrations.simple-repl :as mig])
  ;; (def db (next.jdbc/get-datasource {:jdbcUrl (mig/get-jdbc-url :dev)}))
  ;; (backfill-privacy-subjects! db {:dry-run? true :cutover? true :limit 25})
  :rcf)
