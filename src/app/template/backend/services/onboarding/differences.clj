(ns app.template.backend.services.onboarding.differences
  "Compute workspace differences for the role-difference notice.

   When a user enters a new workspace but already onboarded for this role,
   a brief notice highlights what is different about this workspace."
  (:require
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

(defn- ensure-uuid [v]
  (cond
    (instance? UUID v) v
    (string? v) (try (UUID/fromString v) (catch Exception _ nil))
    :else nil))

(def ^:private query-opts {:builder-fn rs/as-unqualified-lower-maps})

(defn compute-role-differences
  "Return a list of difference maps for a workspace the user is entering.
   Each difference has :capability (description) and :detail (value).
   Returns empty list if there's nothing noteworthy."
  [db tenant-id role]
  (let [tid (ensure-uuid tenant-id)
        r   (if (keyword? role) (name role) (str role))]
    (when tid
      (let [cat-count (-> (jdbc/execute-one! db
                            (sql/format {:select [[[:count :*] :cnt]]
                                         :from   [:expense_categories]
                                         :where  [:= :tenant_id tid]})
                            query-opts)
                        :cnt)
            member-count (-> (jdbc/execute-one! db
                               (sql/format {:select [[[:count :*] :cnt]]
                                            :from   [:tenant_memberships]
                                            :where  [:and
                                                     [:= :tenant_id tid]
                                                     [:= :status [:cast "active" :membership_status]]]})
                               query-opts)
                           :cnt)
            tenant (jdbc/execute-one! db
                     (sql/format {:select [:name]
                                  :from   [:tenants]
                                  :where  [:= :id tid]})
                     query-opts)]
        (cond-> []
          true
          (conj {:capability "workspace_name"
                 :detail     (:name tenant)})

          (pos? (or member-count 0))
          (conj {:capability "team_size"
                 :detail     member-count})

          (pos? (or cat-count 0))
          (conj {:capability "category_count"
                 :detail     cat-count})

          (= r "viewer")
          (conj {:capability "read_only"
                 :detail     true}))))))
