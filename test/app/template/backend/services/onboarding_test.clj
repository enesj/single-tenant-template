(ns app.template.backend.services.onboarding-test
  "Tests for the workspace onboarding service.

  Covers: initialisation, step completion, skip, dismiss, delta onboarding,
  cross-role step cascade, and progress summary computation."
  (:require
    [app.backend.fixtures :as fixtures]
    [app.template.backend.services.onboarding.config :as cfg]
    [app.template.backend.services.onboarding.core :as onboarding]
    [clojure.test :refer [deftest is testing use-fixtures]]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

(use-fixtures :each fixtures/with-transaction-rollback)

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

(defn- create-test-user! [db email]
  (let [id (UUID/randomUUID)]
    (jdbc/execute-one! db
      ["INSERT INTO users (id, email, full_name, password_hash) VALUES (?, ?, ?, ?) RETURNING *"
       id email "Test User" "$2a$11$fakehashfortesting000000000000000000000000000000"]
      {:builder-fn rs/as-unqualified-lower-maps})))

;; ---------------------------------------------------------------------------
;; Config (pure)
;; ---------------------------------------------------------------------------

(deftest steps-for-role-test
  (testing "owner gets 7 steps"
    (is (= 7 (count (cfg/steps-for-role "owner")))))

  (testing "admin gets 3 steps"
    (is (= 3 (count (cfg/steps-for-role "admin")))))

  (testing "member gets 2 steps"
    (is (= 2 (count (cfg/steps-for-role "member")))))

  (testing "viewer gets 2 steps"
    (is (= 2 (count (cfg/steps-for-role "viewer")))))

  (testing "unknown role falls back to member steps"
    (is (= (count (cfg/steps-for-role "member"))
          (count (cfg/steps-for-role "unknown"))))))

(deftest steps-have-required-fields
  (testing "every step definition has :kind and :order"
    (doseq [role ["owner" "admin" "member" "viewer"]]
      (doseq [{:keys [kind order]} (cfg/steps-for-role role)]
        (is (string? kind) (str role " step missing :kind"))
        (is (pos-int? order) (str role " step " kind " has invalid :order"))))))

;; ---------------------------------------------------------------------------
;; Initialisation
;; ---------------------------------------------------------------------------

(deftest initialise-onboarding-test
  (let [db fixtures/*test-db*
        user (create-test-user! db "onboarding-init@test.com")
        uid (:id user)]

    (testing "creates onboarding with all role steps"
      (is (= :created (onboarding/initialise-onboarding! db uid "owner")))
      (let [progress (onboarding/get-progress db uid "owner")]
        (is (some? progress))
        (is (= 7 (count (:steps progress))))
        (is (every? #(= "pending" (:status %)) (:steps progress)))
        (is (false? (:dismissed progress)))))

    (testing "is idempotent — second call returns :already-exists"
      (is (= :already-exists (onboarding/initialise-onboarding! db uid "owner"))))

    (testing "different roles get independent progress"
      (is (= :created (onboarding/initialise-onboarding! db uid "member")))
      (let [member-progress (onboarding/get-progress db uid "member")]
        (is (= 2 (count (:steps member-progress))))))))

;; ---------------------------------------------------------------------------
;; Step Completion
;; ---------------------------------------------------------------------------

(deftest complete-step-test
  (let [db fixtures/*test-db*
        user (create-test-user! db "onboarding-complete@test.com")
        uid (:id user)]
    (onboarding/initialise-onboarding! db uid "owner")

    (testing "completing a pending step returns the updated step"
      (let [result (onboarding/complete-step! db uid "owner" "name_workspace")]
        (is (some? result))
        (is (= "completed" (:status result)))
        (is (some? (:completed_at result)))))

    (testing "completing an already-completed step returns nil"
      (is (nil? (onboarding/complete-step! db uid "owner" "name_workspace"))))

    (testing "completing a skipped step works (rescue)"
      (onboarding/skip-step! db uid "owner" "setup_profile")
      (let [result (onboarding/complete-step! db uid "owner" "setup_profile")]
        (is (some? result))
        (is (= "completed" (:status result)))))))

;; ---------------------------------------------------------------------------
;; Skip
;; ---------------------------------------------------------------------------

(deftest skip-step-test
  (let [db fixtures/*test-db*
        user (create-test-user! db "onboarding-skip@test.com")
        uid (:id user)]
    (onboarding/initialise-onboarding! db uid "owner")

    (testing "skipping a pending step returns updated step"
      (let [result (onboarding/skip-step! db uid "owner" "configure_categories")]
        (is (some? result))
        (is (= "skipped" (:status result)))
        (is (nil? (:completed_at result)))))

    (testing "skipping an already-skipped step returns nil"
      (is (nil? (onboarding/skip-step! db uid "owner" "configure_categories"))))

    (testing "skipping a completed step returns nil"
      (onboarding/complete-step! db uid "owner" "name_workspace")
      (is (nil? (onboarding/skip-step! db uid "owner" "name_workspace"))))))

(deftest skip-all-pending-test
  (let [db fixtures/*test-db*
        user (create-test-user! db "onboarding-skipall@test.com")
        uid (:id user)]
    (onboarding/initialise-onboarding! db uid "owner")

    ;; Complete one step first
    (onboarding/complete-step! db uid "owner" "name_workspace")

    (testing "skips all remaining pending steps"
      (let [skipped-count (onboarding/skip-all-pending! db uid "owner")]
        (is (= 6 skipped-count) "should skip 6 out of 7 (1 already completed)")))

    (testing "no pending steps remain"
      (let [progress (onboarding/get-progress db uid "owner")]
        (is (every? #(not= "pending" (:status %)) (:steps progress)))))))

;; ---------------------------------------------------------------------------
;; Dismiss
;; ---------------------------------------------------------------------------

(deftest dismiss-test
  (let [db fixtures/*test-db*
        user (create-test-user! db "onboarding-dismiss@test.com")
        uid (:id user)]
    (onboarding/initialise-onboarding! db uid "owner")

    (testing "dismiss sets dismissed flag"
      (let [result (onboarding/dismiss! db uid "owner")]
        (is (some? result))
        (is (true? (:dismissed result)))))

    (testing "dismissing again returns nil (already dismissed)"
      (is (nil? (onboarding/dismiss! db uid "owner"))))))

;; ---------------------------------------------------------------------------
;; Progress Summary
;; ---------------------------------------------------------------------------

(deftest progress-summary-test
  (let [db fixtures/*test-db*
        user (create-test-user! db "onboarding-summary@test.com")
        uid (:id user)]

    (testing "nil when no onboarding exists"
      (is (nil? (onboarding/get-progress-summary db uid "owner"))))

    (testing "fresh onboarding: active, redirect true"
      (onboarding/initialise-onboarding! db uid "owner")
      (let [summary (onboarding/get-progress-summary db uid "owner")]
        (is (:active? summary))
        (is (true? (:redirect-to-onboarding? summary)))
        (is (= 0 (:completed summary)))
        (is (= 7 (:total summary)))
        (is (false? (:dismissed summary)))))

    (testing "after one step: still active, redirect false"
      (onboarding/complete-step! db uid "owner" "name_workspace")
      (let [summary (onboarding/get-progress-summary db uid "owner")]
        (is (:active? summary))
        (is (false? (:redirect-to-onboarding? summary)))
        (is (= 1 (:completed summary)))))

    (testing "after dismiss: not active"
      (onboarding/dismiss! db uid "owner")
      (let [summary (onboarding/get-progress-summary db uid "owner")]
        (is (false? (:active? summary)))
        (is (true? (:dismissed summary)))))))

;; ---------------------------------------------------------------------------
;; Delta Onboarding (role promotion)
;; ---------------------------------------------------------------------------

(deftest delta-onboarding-test
  (let [db fixtures/*test-db*
        user (create-test-user! db "onboarding-delta@test.com")
        uid (:id user)]

    ;; Start as member
    (onboarding/initialise-onboarding! db uid "member")
    (onboarding/complete-step! db uid "member" "setup_profile")

    (testing "delta onboarding pre-marks steps completed in prior roles"
      (is (= :created (onboarding/initialise-delta-onboarding! db uid "admin")))
      (let [progress (onboarding/get-progress db uid "admin")
            steps-by-kind (into {} (map (juxt :kind identity) (:steps progress)))]
        (is (= 3 (count (:steps progress))) "admin has 3 steps")
        (is (= "completed" (get-in steps-by-kind ["setup_profile" :status]))
          "setup_profile should be pre-completed from member role")
        (is (= "pending" (get-in steps-by-kind ["management_intro" :status]))
          "management_intro should be pending (new for admin)")
        (is (= "pending" (get-in steps-by-kind ["upload_first_receipt" :status]))
          "upload_first_receipt should be pending (not yet done in member)")))

    (testing "delta onboarding is idempotent"
      (is (= :already-exists (onboarding/initialise-delta-onboarding! db uid "admin"))))))

;; ---------------------------------------------------------------------------
;; Cross-Role Step Cascade
;; ---------------------------------------------------------------------------

(deftest complete-step-for-all-roles-test
  (let [db fixtures/*test-db*
        user (create-test-user! db "onboarding-cascade@test.com")
        uid (:id user)]

    ;; Create onboarding for two roles that share setup_profile
    (onboarding/initialise-onboarding! db uid "owner")
    (onboarding/initialise-onboarding! db uid "member")

    (testing "completes the step across all active roles"
      (let [results (onboarding/complete-step-for-all-roles! db uid "setup_profile")]
        (is (= 2 (count results)) "should complete in both owner and member")))

    (testing "verify both roles show setup_profile as completed"
      (let [owner-steps  (:steps (onboarding/get-progress db uid "owner"))
            member-steps (:steps (onboarding/get-progress db uid "member"))
            owner-profile  (first (filter #(= "setup_profile" (:kind %)) owner-steps))
            member-profile (first (filter #(= "setup_profile" (:kind %)) member-steps))]
        (is (= "completed" (:status owner-profile)))
        (is (= "completed" (:status member-profile)))))

    (testing "does not cascade to dismissed onboarding"
      ;; Create a third role and dismiss it
      (onboarding/initialise-onboarding! db uid "viewer")
      (onboarding/dismiss! db uid "viewer")
      ;; Complete browse_intro which only viewer has — should not touch dismissed
      (let [results (onboarding/complete-step-for-all-roles! db uid "browse_intro")]
        (is (zero? (count results))
          "viewer is dismissed so browse_intro should not be updated")))))

;; ---------------------------------------------------------------------------
;; try-complete-step! (fire-and-forget)
;; ---------------------------------------------------------------------------

(deftest try-complete-step-fire-and-forget-test
  (let [db fixtures/*test-db*
        user (create-test-user! db "onboarding-tryc@test.com")
        uid (:id user)]

    (testing "returns nil when no onboarding exists (no error)"
      (is (nil? (onboarding/try-complete-step! db uid "owner" "name_workspace"))))

    (testing "completes step when onboarding exists"
      (onboarding/initialise-onboarding! db uid "owner")
      (let [result (onboarding/try-complete-step! db uid "owner" "name_workspace")]
        (is (some? result))
        (is (= "completed" (:status result)))))))

;; ---------------------------------------------------------------------------
;; has-progress?
;; ---------------------------------------------------------------------------

(deftest has-progress-test
  (let [db fixtures/*test-db*
        user (create-test-user! db "onboarding-has@test.com")
        uid (:id user)]

    (testing "false before initialisation"
      (is (false? (onboarding/has-progress? db uid "owner"))))

    (testing "true after initialisation"
      (onboarding/initialise-onboarding! db uid "owner")
      (is (true? (onboarding/has-progress? db uid "owner"))))

    (testing "false for uninitialised role"
      (is (false? (onboarding/has-progress? db uid "admin"))))))
