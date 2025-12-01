(ns app.admin.frontend.events.users.core-test
  (:require
    [app.admin.frontend.events.users.core]          ;; ensure handlers are registered
    [app.admin.frontend.events.users.template.messages]
    [app.admin.frontend.test-setup :as setup]
    [app.template.frontend.db.paths :as paths]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

 ;; Helpers to ensure subs used by security wrappers exist if pulled transitively
(when-not (get-in @rf-db/app-db [:test :admin-session-registered?])
  (swap! rf-db/app-db assoc-in [:test :admin-session-registered?] true))

(deftest fetch-entities-users-produces-correct-request
  (testing ":admin/fetch-entities generates admin GET request to users endpoint"
    (setup/reset-db!)
    (setup/install-http-stub!)
    (setup/put-token! "token-abc")
    (rf/dispatch-sync [:admin/fetch-entities :users])
    (let [req (setup/last-http-request)]
      (is (= :get (:method req)))
      (is (= "/admin/api/users" (:uri req)))
      (is (= [:admin/fetch-entities-success :users] (take 2 (:on-success req))))
      (is (= [:admin/fetch-entities-failure :users] (take 2 (:on-failure req)))))))

(deftest fetch-entities-success-normalizes-into-template-state
  (testing "success handler normalizes and stores data"
    (setup/reset-db!)
    (setup/install-http-stub!)
    (setup/put-token! "token-abc")
    (rf/dispatch-sync [:admin/fetch-entities :users])
     ;; Simulate API success with payload {users: [...]}
    (setup/respond-success! {:users [{:id 1 :email "a@test.com"}
                                     {:id 2 :email "b@test.com"}]})
    (let [db @rf-db/app-db]
      (is (= [1 2] (get-in db (paths/entity-ids :users))))
      (is (= false (get-in db (paths/entity-loading? :users))))
      (is (= [{:id 1 :email "a@test.com"}
              {:id 2 :email "b@test.com"}]
            (:admin/users db))))))

(deftest fetch-entities-failure-sets-error
  (testing "failure handler sets error and clears loading"
    (setup/reset-db!)
    (setup/install-http-stub!)
    (setup/put-token! "token-abc")
    (rf/dispatch-sync [:admin/fetch-entities :users])
    (setup/respond-failure! {:status 500 :response {:error "boom"}})
    (let [db @rf-db/app-db]
      (is (false? (get-in db (paths/entity-loading? :users))))
      (is (some? (get-in db (paths/entity-error :users)))))))

(deftest fetch-entities-failure-with-data-recovers
  (testing "failure payload with original-text data is recovered"
    (setup/reset-db!)
    (setup/install-http-stub!)
    (setup/put-token! "token-abc")
    (rf/dispatch-sync [:admin/fetch-entities :users])
    (let [payload (clj->js {:users [{:id 10} {:id 20}]})]
      (setup/respond-failure! {:status 200 :original-text payload}))
    (let [db @rf-db/app-db]
      (is (= [10 20] (get-in db (paths/entity-ids :users))))
      (is (nil? (get-in db (paths/entity-error :users)))))))

(deftest delete-user-dispatches-admin-entity-request
  (testing ":admin/delete-user issues admin entity delete request and syncs state on success"
    (setup/reset-db!)
    (setup/install-http-stub!)
    (let [user-id "11111111-2222-3333-4444-555555555555"]
      (swap! rf-db/app-db
        (fn [db]
          (-> db
            (assoc :admin/users [{:users/id user-id :email "user@example.com"}])
            (assoc-in (paths/entity-data :users) {user-id {:id user-id :email "user@example.com"}})
            (assoc-in (paths/entity-ids :users) [user-id])
            (assoc-in (paths/entity-selected-ids :users) #{user-id}))))

      (rf/dispatch-sync [:admin/delete-user user-id])

      (let [req (setup/last-http-request)]
        (is (= :delete (:method req)))
        (is (= (str "/admin/api/entities/users/" user-id) (:uri req)))
        (is (= [:admin/delete-user-success user-id] (:on-success req)))
        (is (= [:admin/delete-user-failure user-id] (:on-failure req))))

      (setup/respond-success! {:success true :message "Deleted"})

      (let [db @rf-db/app-db]
        (is (= [] (:admin/users db)))
        (is (= {} (get-in db (paths/entity-data :users))))
        (is (= [] (get-in db (paths/entity-ids :users))))
        (is (or (nil? (get-in db (paths/entity-selected-ids :users)))
              (empty? (get-in db (paths/entity-selected-ids :users)))))
        (is (false? (:admin/updating-user db)))
        (is (nil? (:admin/user-update-error db)))
        (is (nil? (:admin/delete-confirmation-visible? db)))))))

(deftest delete-user-failure-surfaces-error
  (testing ":admin/delete-user-failure clears loading state and records error message"
    (setup/reset-db!)
    (setup/install-http-stub!)
    (let [user-id "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"]
      (swap! rf-db/app-db assoc :admin/users [{:users/id user-id :email "fail@example.com"}])

      (rf/dispatch-sync [:admin/delete-user user-id])

      (setup/respond-failure! {:status 500 :response {:message "Boom"}})

      (let [db @rf-db/app-db]
        (is (false? (:admin/updating-user db)))
        (is (some? (:admin/user-update-error db)))
        (is (= [{:users/id user-id :email "fail@example.com"}] (:admin/users db)))))))

(deftest view-user-details-lifecycle
  (testing ":admin/view-user-details opens modal and stores details on success"
    (setup/reset-db!)
    (setup/install-http-stub!)
    (rf/dispatch-sync [:admin/view-user-details 42])
    (let [req (setup/last-http-request)]
      (is (= :get (:method req)))
      (is (= "/admin/api/users/42" (:uri req))))
    (setup/respond-success! {:user {:id 42 :email "x@test.com"}})
    (let [db @rf-db/app-db]
      (is (true? (:admin/user-details-modal-open? db)))
      (is (= {:id 42 :email "x@test.com"} (:admin/current-user-details db)))))

  (testing "failure keeps modal open and sets error"
    (setup/reset-db!)
    (setup/install-http-stub!)
    (rf/dispatch-sync [:admin/view-user-details 51])
    (setup/respond-failure! {:status 500 :response {:message "nope"}})
    (let [db @rf-db/app-db]
      (is (true? (:admin/user-details-modal-open? db)))
      (is (some? (:admin/user-details-error db))))))
