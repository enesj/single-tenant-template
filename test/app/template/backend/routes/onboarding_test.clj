(ns app.template.backend.routes.onboarding-test
  "Tests for onboarding API route handlers.

  These tests invoke handlers directly (no HTTP), verifying request/response
  shape, auth guards, and integration with the onboarding service."
  (:require
    [app.backend.fixtures :as fixtures]
    [app.template.backend.routes.onboarding :as onboarding-routes]
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
       id email "Route Test User" "$2a$11$fakehashfortesting000000000000000000000000000000"]
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- auth-request
  "Build a fake authenticated request."
  [{:keys [user tenant-id role path-params]}]
  (cond-> {:session {:auth-session {:user {:id (:id user)
                                           :email (:email user)}
                                    :tenant {:id tenant-id}
                                    :membership {:id (UUID/randomUUID)
                                                 :role role}}}}
    path-params (assoc :path-params path-params)))

(defn- unauth-request []
  {:session {}})

(defn- no-role-request [user]
  {:session {:auth-session {:user {:id (:id user) :email (:email user)}}}})

(defn- parse-body [resp]
  (let [body (:body resp)]
    (cond
      (map? body) body
      :else body)))

;; We need to access the private handler constructors via their route tree.
;; Instead, we'll use the public `onboarding-routes` to build handlers.
(defn- make-handlers [db]
  (let [routes (onboarding-routes/onboarding-routes db identity)
        ;; routes = ["/onboarding" {:middleware ...} ["/progress" ...] ...]
        children (drop 2 routes)]
    (into {}
      (for [[path method-map] children
            :let [method (first (keys method-map))
                  handler (get-in method-map [method :handler])]]
        [path handler]))))

;; ---------------------------------------------------------------------------
;; GET /onboarding/progress
;; ---------------------------------------------------------------------------

(deftest progress-handler-test
  (let [db fixtures/*test-db*
        user (create-test-user! db "onb-route-progress@test.com")
        handlers (make-handlers db)
        get-progress (get handlers "/progress")]

    (testing "returns progress when onboarding exists"
      (onboarding/initialise-onboarding! db (:id user) "owner")
      (let [resp (get-progress (auth-request {:user user :tenant-id (UUID/randomUUID) :role "owner"}))
            body (parse-body resp)]
        (is (= 200 (:status resp)))
        (is (some? (:progress body)))
        (is (= 7 (count (get-in body [:progress :steps]))))))

    (testing "returns nil progress when no onboarding"
      (let [resp (get-progress (auth-request {:user user :tenant-id (UUID/randomUUID) :role "admin"}))
            body (parse-body resp)]
        (is (= 200 (:status resp)))
        (is (nil? (:progress body)))))))

;; ---------------------------------------------------------------------------
;; POST /onboarding/steps/:step-kind/complete
;; ---------------------------------------------------------------------------

(deftest complete-step-handler-test
  (let [db fixtures/*test-db*
        user (create-test-user! db "onb-route-complete@test.com")
        handlers (make-handlers db)
        complete-step (get handlers "/steps/:step-kind/complete")]
    (onboarding/initialise-onboarding! db (:id user) "owner")

    (testing "completes a pending step"
      (let [resp (complete-step (auth-request {:user user
                                               :tenant-id (UUID/randomUUID)
                                               :role "owner"
                                               :path-params {:step-kind "name_workspace"}}))
            body (parse-body resp)]
        (is (= 200 (:status resp)))
        (is (true? (:success body)))
        (is (= "completed" (get-in body [:step :status])))))

    (testing "returns success=false for already completed step"
      (let [resp (complete-step (auth-request {:user user
                                               :tenant-id (UUID/randomUUID)
                                               :role "owner"
                                               :path-params {:step-kind "name_workspace"}}))
            body (parse-body resp)]
        (is (= 200 (:status resp)))
        (is (false? (:success body)))))))

;; ---------------------------------------------------------------------------
;; POST /onboarding/steps/:step-kind/skip
;; ---------------------------------------------------------------------------

(deftest skip-step-handler-test
  (let [db fixtures/*test-db*
        user (create-test-user! db "onb-route-skip@test.com")
        handlers (make-handlers db)
        skip-step (get handlers "/steps/:step-kind/skip")]
    (onboarding/initialise-onboarding! db (:id user) "owner")

    (testing "skips a pending step"
      (let [resp (skip-step (auth-request {:user user
                                           :tenant-id (UUID/randomUUID)
                                           :role "owner"
                                           :path-params {:step-kind "configure_categories"}}))
            body (parse-body resp)]
        (is (= 200 (:status resp)))
        (is (true? (:success body)))
        (is (= "skipped" (get-in body [:step :status])))))

    (testing "returns success=false for non-pending step"
      (let [resp (skip-step (auth-request {:user user
                                           :tenant-id (UUID/randomUUID)
                                           :role "owner"
                                           :path-params {:step-kind "configure_categories"}}))
            body (parse-body resp)]
        (is (= 200 (:status resp)))
        (is (false? (:success body)))))))

;; ---------------------------------------------------------------------------
;; POST /onboarding/skip-all
;; ---------------------------------------------------------------------------

(deftest skip-all-handler-test
  (let [db fixtures/*test-db*
        user (create-test-user! db "onb-route-skipall@test.com")
        handlers (make-handlers db)
        skip-all (get handlers "/skip-all")]
    (onboarding/initialise-onboarding! db (:id user) "owner")

    (testing "skips all pending steps"
      (let [resp (skip-all (auth-request {:user user :tenant-id (UUID/randomUUID) :role "owner"}))
            body (parse-body resp)]
        (is (= 200 (:status resp)))
        (is (true? (:success body)))
        (is (= 7 (:skipped-count body)))))))

;; ---------------------------------------------------------------------------
;; POST /onboarding/dismiss
;; ---------------------------------------------------------------------------

(deftest dismiss-handler-test
  (let [db fixtures/*test-db*
        user (create-test-user! db "onb-route-dismiss@test.com")
        handlers (make-handlers db)
        dismiss (get handlers "/dismiss")]
    (onboarding/initialise-onboarding! db (:id user) "owner")

    (testing "dismisses onboarding"
      (let [resp (dismiss (auth-request {:user user :tenant-id (UUID/randomUUID) :role "owner"}))
            body (parse-body resp)]
        (is (= 200 (:status resp)))
        (is (true? (:success body)))
        (is (true? (get-in body [:progress :dismissed])))))

    (testing "returns success=false when already dismissed"
      (let [resp (dismiss (auth-request {:user user :tenant-id (UUID/randomUUID) :role "owner"}))
            body (parse-body resp)]
        (is (= 200 (:status resp)))
        (is (false? (:success body)))))))

;; ---------------------------------------------------------------------------
;; Auth guards
;; ---------------------------------------------------------------------------

(deftest auth-guard-test
  (let [db fixtures/*test-db*
        user (create-test-user! db "onb-route-guard@test.com")
        handlers (make-handlers db)
        get-progress (get handlers "/progress")]

    (testing "unauthenticated request returns 401"
      (let [resp (get-progress (unauth-request))]
        (is (= 401 (:status resp)))))

    (testing "no-role request returns 403"
      (let [resp (get-progress (no-role-request user))]
        (is (= 403 (:status resp)))))))
