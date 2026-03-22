(ns app.template.backend.services.onboarding.core
  "Workspace onboarding service — progress tracking and step completion.

   OnboardingProgress is keyed on (user_id, role), not per membership.
   A user who is 'owner' in two workspaces shares one onboarding record."
  (:require
    [app.template.backend.services.onboarding.config :as cfg]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log])
  (:import
    [java.util UUID]))

;; ============================================================================
;; Helpers
;; ============================================================================

(defn- ensure-uuid [v]
  (cond
    (instance? UUID v) v
    (string? v) (try (UUID/fromString v) (catch Exception _ nil))
    :else nil))

(defn- normalize-role [role]
  (if (keyword? role) (name role) (str role)))

(def ^:private query-opts {:builder-fn rs/as-unqualified-lower-maps})

(defn- query [db sql-map]
  (jdbc/execute! db (sql/format sql-map) query-opts))

(defn- query-one [db sql-map]
  (first (query db sql-map)))

;; ============================================================================
;; Read
;; ============================================================================

(defn get-progress
  "Return the onboarding progress for a (user-id, role) pair, or nil.
   Includes all steps ordered by step_order."
  [db user-id role]
  (let [uid  (ensure-uuid user-id)
        r    (normalize-role role)]
    (when-let [progress (query-one db
                          {:select [:*]
                           :from   [:onboarding_progress]
                           :where  [:and
                                    [:= :user_id uid]
                                    [:= :role [:cast r :membership_role]]]})]
      (let [steps (query db
                    {:select   [:*]
                     :from     [:onboarding_steps]
                     :where    [:= :progress_id (:id progress)]
                     :order-by [[:step_order :asc]]})]
        (assoc progress :steps (vec steps))))))

(defn get-progress-summary
  "Lightweight summary for auth-status / sidebar indicator.
   Returns {:active? bool :completed int :total int} or nil."
  [db user-id role]
  (let [uid (ensure-uuid user-id)
        r   (normalize-role role)]
    (when-let [progress (query-one db
                          {:select [:id :dismissed]
                           :from   [:onboarding_progress]
                           :where  [:and
                                    [:= :user_id uid]
                                    [:= :role [:cast r :membership_role]]]})]
      (let [counts (query-one db
                     {:select [[[:count :*] :total]
                               [[:count [:case
                                         [:= :status [:cast "completed" :onboarding_step_status]]
                                         [:inline 1]]]
                                :completed]
                               [[:count [:case
                                         [:!= :status [:cast "pending" :onboarding_step_status]]
                                         [:inline 1]]]
                                :non_pending]]
                      :from   [:onboarding_steps]
                      :where  [:= :progress_id (:id progress)]})]
        {:active?   (and (not (:dismissed progress))
                      (< (:completed counts 0) (:total counts 0)))
         :completed (:completed counts 0)
         :total     (:total counts 0)
         :dismissed (:dismissed progress)
         :redirect-to-onboarding? (and (not (:dismissed progress))
                                    (zero? (:non_pending counts 0)))}))))

(defn has-progress?
  "Does a (user-id, role) pair have an existing onboarding record?"
  [db user-id role]
  (let [uid (ensure-uuid user-id)
        r   (normalize-role role)]
    (some? (query-one db
             {:select [[:id]]
              :from   [:onboarding_progress]
              :where  [:and
                       [:= :user_id uid]
                       [:= :role [:cast r :membership_role]]]}))))

;; ============================================================================
;; Initialise
;; ============================================================================

(defn initialise-onboarding!
  "Create onboarding progress + steps for a (user-id, role) pair.
   No-op if progress already exists. Returns :created or :already-exists.
   `db-or-tx` can be a transaction connection for atomicity with provisioning."
  [db-or-tx user-id role]
  (let [uid       (ensure-uuid user-id)
        r         (normalize-role role)
        step-defs (cfg/steps-for-role r)]
    (if (has-progress? db-or-tx uid r)
      :already-exists
      (let [progress-id (UUID/randomUUID)
            now         (java.time.LocalDateTime/now)]
        (jdbc/execute-one! db-or-tx
          (sql/format {:insert-into [:onboarding_progress]
                       :values [{:id         progress-id
                                 :user_id    uid
                                 :role       [:cast r :membership_role]
                                 :dismissed  false
                                 :created_at now
                                 :updated_at now}]}))
        (doseq [{:keys [kind order]} step-defs]
          (jdbc/execute-one! db-or-tx
            (sql/format {:insert-into [:onboarding_steps]
                         :values [{:id          (UUID/randomUUID)
                                   :progress_id progress-id
                                   :kind        [:cast kind :onboarding_step_kind]
                                   :step_order  order
                                   :status      [:cast "pending" :onboarding_step_status]
                                   :completed_at nil
                                   :created_at  now
                                   :updated_at  now}]})))
        (log/info "Initialised onboarding" {:user-id uid :role r :steps (count step-defs)})
        :created))))

(defn initialise-delta-onboarding!
  "Create onboarding for a new role, pre-marking steps that are already
   completed in any prior role's onboarding. Used after role promotion."
  [db user-id new-role]
  (let [uid       (ensure-uuid user-id)
        r         (normalize-role new-role)
        step-defs (cfg/steps-for-role r)]
    (if (has-progress? db uid r)
      :already-exists
      (let [;; Collect step kinds completed in ANY prior role
            completed-kinds (->> (query db
                                   {:select-distinct [:kind]
                                    :from [:onboarding_steps]
                                    :where [:and
                                            [:in :progress_id
                                             {:select [:id]
                                              :from [:onboarding_progress]
                                              :where [:= :user_id uid]}]
                                            [:= :status [:cast "completed" :onboarding_step_status]]]})
                              (map :kind)
                              (map str)
                              set)
            progress-id (UUID/randomUUID)
            now         (java.time.LocalDateTime/now)]
        (jdbc/execute-one! db
          (sql/format {:insert-into [:onboarding_progress]
                       :values [{:id         progress-id
                                 :user_id    uid
                                 :role       [:cast r :membership_role]
                                 :dismissed  false
                                 :created_at now
                                 :updated_at now}]}))
        (doseq [{:keys [kind order]} step-defs]
          (let [already-done? (contains? completed-kinds kind)
                status        (if already-done? "completed" "pending")]
            (jdbc/execute-one! db
              (sql/format {:insert-into [:onboarding_steps]
                           :values [{:id           (UUID/randomUUID)
                                     :progress_id  progress-id
                                     :kind         [:cast kind :onboarding_step_kind]
                                     :step_order   order
                                     :status       [:cast status :onboarding_step_status]
                                     :completed_at (when already-done? now)
                                     :created_at   now
                                     :updated_at   now}]}))))
        (log/info "Initialised delta onboarding" {:user-id uid :role r
                                                  :pre-completed (count completed-kinds)})
        :created))))

;; ============================================================================
;; Step Completion / Skip
;; ============================================================================

(defn complete-step!
  "Mark a step as completed. Works from pending or skipped status.
   Returns the updated step or nil if not found / already completed."
  [db user-id role step-kind]
  (let [uid (ensure-uuid user-id)
        r   (normalize-role role)
        k   (if (keyword? step-kind) (name step-kind) (str step-kind))
        now (java.time.LocalDateTime/now)]
    (query-one db
      {:update :onboarding_steps
       :set    {:status       [:cast "completed" :onboarding_step_status]
                :completed_at now
                :updated_at   now}
       :where  [:and
                [:in :progress_id
                 {:select [:id]
                  :from   [:onboarding_progress]
                  :where  [:and
                           [:= :user_id uid]
                           [:= :role [:cast r :membership_role]]]}]
                [:= :kind [:cast k :onboarding_step_kind]]
                [:!= :status [:cast "completed" :onboarding_step_status]]]
       :returning [:*]})))

(defn complete-step-for-all-roles!
  "Complete a step kind across ALL active onboarding records for a user.
   Used for user-level steps like setup_profile."
  [db user-id step-kind]
  (let [uid (ensure-uuid user-id)
        k   (if (keyword? step-kind) (name step-kind) (str step-kind))
        now (java.time.LocalDateTime/now)]
    (query db
      {:update :onboarding_steps
       :set    {:status       [:cast "completed" :onboarding_step_status]
                :completed_at now
                :updated_at   now}
       :where  [:and
                [:in :progress_id
                 {:select [:id]
                  :from   [:onboarding_progress]
                  :where  [:and
                           [:= :user_id uid]
                           [:= :dismissed false]]}]
                [:= :kind [:cast k :onboarding_step_kind]]
                [:!= :status [:cast "completed" :onboarding_step_status]]]
       :returning [:*]})))

(defn try-complete-step!
  "Attempt to complete a step. No-op if the step doesn't exist or is already
   completed. Used by trigger integrations that don't know if onboarding is active."
  [db user-id role step-kind]
  (try
    (complete-step! db user-id role step-kind)
    (catch Exception e
      (log/debug "Onboarding step completion skipped" {:step step-kind :error (.getMessage e)})
      nil)))

(defn skip-step!
  "Mark a step as skipped. Only works from pending status.
   Returns the updated step or nil."
  [db user-id role step-kind]
  (let [uid (ensure-uuid user-id)
        r   (normalize-role role)
        k   (if (keyword? step-kind) (name step-kind) (str step-kind))
        now (java.time.LocalDateTime/now)]
    (query-one db
      {:update :onboarding_steps
       :set    {:status     [:cast "skipped" :onboarding_step_status]
                :updated_at now}
       :where  [:and
                [:in :progress_id
                 {:select [:id]
                  :from   [:onboarding_progress]
                  :where  [:and
                           [:= :user_id uid]
                           [:= :role [:cast r :membership_role]]]}]
                [:= :kind [:cast k :onboarding_step_kind]]
                [:= :status [:cast "pending" :onboarding_step_status]]]
       :returning [:*]})))

(defn skip-all-pending!
  "Mark all pending steps as skipped. Used for 'skip entire onboarding'.
   Returns the count of steps skipped."
  [db user-id role]
  (let [uid (ensure-uuid user-id)
        r   (normalize-role role)
        now (java.time.LocalDateTime/now)]
    (let [result (jdbc/execute-one! db
                   (sql/format
                     {:update :onboarding_steps
                      :set    {:status     [:cast "skipped" :onboarding_step_status]
                               :updated_at now}
                      :where  [:and
                               [:in :progress_id
                                {:select [:id]
                                 :from   [:onboarding_progress]
                                 :where  [:and
                                          [:= :user_id uid]
                                          [:= :role [:cast r :membership_role]]
                                          [:= :dismissed false]]}]
                               [:= :status [:cast "pending" :onboarding_step_status]]]}))]
      (or (:next.jdbc/update-count result) 0))))

;; ============================================================================
;; Dismiss
;; ============================================================================

(defn dismiss!
  "Dismiss onboarding for a (user-id, role). Final — no undo.
   Returns the updated progress or nil."
  [db user-id role]
  (let [uid (ensure-uuid user-id)
        r   (normalize-role role)
        now (java.time.LocalDateTime/now)]
    (query-one db
      {:update :onboarding_progress
       :set    {:dismissed  true
                :updated_at now}
       :where  [:and
                [:= :user_id uid]
                [:= :role [:cast r :membership_role]]
                [:= :dismissed false]]
       :returning [:*]})))

(comment
  ;; REPL testing
  ;; (require 'app.template.backend.services.onboarding.core :reload)
  ;; (def db (:db-pool (system.core/system)))
  ;; (initialise-onboarding! db "user-uuid" "owner")
  ;; (get-progress db "user-uuid" "owner")
  ;; (complete-step! db "user-uuid" "owner" "setup_profile")
  :rcf)
