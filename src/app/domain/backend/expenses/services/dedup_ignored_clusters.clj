(ns app.domain.backend.expenses.services.dedup-ignored-clusters
  "Persistence for admin-ignored false-positive duplicate clusters."
  (:require
    [app.domain.backend.expenses.services.receipts.storage :as receipts-storage]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

(defn list-ignored-cluster-ids
  "Return a set of ignored cluster_id strings for the given admin + entity-type."
  [db admin-id entity-type]
  (->> (jdbc/execute!
         db
         (sql/format {:select [:cluster_id]
                      :from [:dedup_ignored_clusters]
                      :where [:and
                              [:= :admin_id admin-id]
                              [:= :entity_type (name entity-type)]]})
         {:builder-fn rs/as-unqualified-lower-maps})
    (map :cluster_id)
    (remove nil?)
    set))

(defn ignore-cluster!
  "Insert/update an ignored cluster row.

  Idempotent by unique key (admin_id, entity_type, cluster_id)."
  [db {:keys [admin-id entity-type cluster-id member-ids note]}]
  (let [row {:id (UUID/randomUUID)
             :admin_id admin-id
             :entity_type (name entity-type)
             :cluster_id cluster-id
             :member_ids (receipts-storage/jsonb-value member-ids)
             :note note
             :created_at [:now]}]
    (jdbc/execute-one!
      db
      (sql/format
        {:insert-into :dedup_ignored_clusters
         :values [row]
         :on-conflict [:admin_id :entity_type :cluster_id]
         :do-update-set {:member_ids :excluded/member_ids
                         :note [:coalesce :excluded/note :dedup_ignored_clusters/note]}
         :returning [:*]})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn unignore-cluster!
  "Delete an ignored cluster row."
  [db admin-id entity-type cluster-id]
  (jdbc/execute-one!
    db
    (sql/format
      {:delete-from :dedup_ignored_clusters
       :where [:and
               [:= :admin_id admin-id]
               [:= :entity_type (name entity-type)]
               [:= :cluster_id cluster-id]]
       :returning [:id]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn list-ignored-clusters
  "List ignored cluster rows (for debugging/support UI)."
  [db admin-id {:keys [entity-type limit offset]
                :or {limit 100 offset 0}}]
  (jdbc/execute!
    db
    (sql/format
      (cond-> {:select [:cluster_id :entity_type :member_ids :note :created_at]
               :from [:dedup_ignored_clusters]
               :where [:= :admin_id admin-id]
               :order-by [[:created_at :desc]]
               :limit limit
               :offset offset}
        entity-type (assoc :where [:and
                                   [:= :admin_id admin-id]
                                   [:= :entity_type (name entity-type)]])))
    {:builder-fn rs/as-unqualified-lower-maps}))