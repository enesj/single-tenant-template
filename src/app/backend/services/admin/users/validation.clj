(ns app.backend.services.admin.users.validation
  "User validation logic and business rule constraints.

   This namespace handles:
   - User update validation
   - User deletion constraint checking
   - Batch constraint validation
   - Business rule enforcement"
  (:require
    [app.shared.field-metadata :as field-meta]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [honey.sql :as hsql]
    [next.jdbc :as jdbc]))

;; ============================================================================
;; User Update Validation
;; ============================================================================

(defonce ^:private models-data-cache
  (delay
    (try
      (let [models-resource (io/resource "db/models.edn")]
        (when models-resource
          (-> models-resource slurp edn/read-string)))
      (catch Exception _
        ;; In case models cannot be loaded (e.g., during certain tests),
        ;; fall back to nil and use hardcoded defaults where necessary.
        nil))))

(defn validate-user-updates
  "Validate user update data against business rules and constraints"
  [updates]
  (let [allowed-keys #{:email :full_name :avatar_url :auth_provider :provider_user_id
                       :role :status :last_login_at}
        models @models-data-cache
        role-enum-set (if-let [choices (and models (field-meta/get-enum-choices models :users :role))]
                        (set choices)
                        #{"admin" "member" "viewer" "unassigned"})
        status-enum-set (if-let [choices (and models (field-meta/get-enum-choices models :users :status))]
                          (set choices)
                          #{"active" "inactive" "suspended"})
        enum-sets {:role role-enum-set
                   :status status-enum-set
                   :auth_provider #{"google" "email" "apple"}}]

    ;; Check for disallowed keys
    (let [invalid-keys (remove allowed-keys (keys updates))]
      (when (seq invalid-keys)
        (throw (ex-info (str "Invalid update keys: " (vec invalid-keys))
                 {:status 400 :invalid-keys invalid-keys :allowed-keys (vec allowed-keys)}))))

    ;; Enum validation (only when those keys are present)
    (doseq [[k allowed] enum-sets]
      (when (contains? updates k)
        (when-not (contains? allowed (str (get updates k)))
          (throw (ex-info (str "Invalid value for " (name k))
                   {:status 400 :field k :allowed (vec allowed) :value (get updates k)})))))

    ;; Additional business rule validations
    (when (and (contains? updates :email)
            (not (re-matches #"^[^\s@]+@[^\s@]+\.[^\s@]+$" (:email updates))))
      (throw (ex-info "Invalid email format"
               {:status 400 :field :email :value (:email updates)})))

    ;; Return sanitized updates
    (select-keys updates allowed-keys)))

;; ============================================================================
;; User Deletion Constraint Checking
;; ============================================================================

(defn check-user-deletion-constraints
  "Check if user can be safely deleted (single-tenant, minimal dependencies)."
  [db user-id]
  (let [user (jdbc/execute-one! db
               (hsql/format {:select [:*]
                             :from [:users]
                             :where [:= :id user-id]}))]
    (when-not user
      (throw (ex-info "User not found for deletion"
               {:status 404 :user-id user-id})))
    {:user user
     :can-delete true
     :impact-summary {:cascade-deletions {}
                      :set-null-updates {}
                      :total-cascade 0
                      :total-set-null 0}}))

;; ============================================================================
;; Batch Validation
;; ============================================================================

(defn check-users-deletion-constraints-batch
  "Batch-check deletion constraints for multiple users in a single request.

   Returns a map with :success, :dry-run, and :results (a vector of per-user
   results). Each result contains:
   - :user-id        UUID of the user
   - :can-delete?    boolean flag
   - :constraints    vector of {:type error| warning :code ... :message ...}
   - :warnings       vector (currently unused; reserved for future use)
   - :impact-analysis impact summary when available
  "
  [db user-ids]
  (let [ids (->> user-ids (filter some?) vec)
        results (mapv (fn [uid]
                        (try
                          (let [r (check-user-deletion-constraints db uid)]
                            {:user-id uid
                             :can-delete? true
                             :constraints []
                             :warnings []
                             :impact-analysis (:impact-summary r)})
                          (catch clojure.lang.ExceptionInfo e
                            (let [d (ex-data e)]
                              {:user-id uid
                               :can-delete? false
                               :constraints [{:type "error"
                                              :code (some-> (:reason d) name)
                                              :message (.getMessage e)}]
                               :warnings []
                               :impact-analysis (select-keys d [:total-cascade-deletions :cascade-details])}))
                          (catch Exception e
                            {:user-id uid
                             :can-delete? false
                             :constraints [{:type "error"
                                            :code "unexpected-error"
                                            :message (.getMessage e)}]
                             :warnings []})))
                  ids)]
    {:success true
     :dry-run true
     :results results}))
