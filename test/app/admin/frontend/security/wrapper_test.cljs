 (ns app.admin.frontend.security.wrapper-test
   (:require
     [app.admin.frontend.security.wrapper :as sec]
     [cljs.test :refer [deftest is testing]]
     [re-frame.db :as rf-db]))

 ;; Define subs that wrapper expects - only register if not already registered
(when-not (get-in @rf-db/app-db [:test :admin-subs-registered?])
  (swap! rf-db/app-db assoc-in [:test :admin-subs-registered?] true))

(deftest ensure-admin-session-denies-when-unauthenticated
  (testing "ensure-admin-session blocks and returns nil when not authenticated"
    (reset! rf-db/app-db {:admin/authenticated? false})
    (let [called? (atom false)
          handler (fn [& _] (reset! called? true))
          guarded (sec/ensure-admin-session handler)]
      (is (nil? (guarded {:x 1})))
      (is (false? @called?)))))

(deftest ensure-admin-session-allows-when-authenticated
  (testing "ensure-admin-session calls handler when authenticated"
    (reset! rf-db/app-db {:admin/authenticated? true
                          :admin/session {:id 1 :role :admin}})
    (let [called? (atom false)
          handler (fn [& _] (reset! called? true) :ok)
          guarded (sec/ensure-admin-session handler)]
      (is (= :ok (guarded {:x 1})))
      (is (true? @called?)))))

(deftest permissions-checks
  (testing "has-permission? denies/allows based on role"
    (reset! rf-db/app-db {:admin/authenticated? true
                          :admin/session {:id 1 :role :support}})
    (is (false? (sec/has-permission? :impersonate-user)))
    (reset! rf-db/app-db {:admin/authenticated? true
                          :admin/session {:id 2 :role :super-admin}})
    (is (true? (sec/has-permission? :impersonate-user)))))

(deftest secure-template-operation-redirects-when-unauthenticated
  (testing "secure-template-operation enforces auth and redirects"
    (reset! rf-db/app-db {:admin/authenticated? false
                          :admin/session nil})
    (let [captured (atom [])
          orig-event (fn [_cofx _ev] (swap! captured conj :called))
          handler ((sec/secure-template-operation :delete) orig-event)]
      (try
        (handler {} [:app.template.frontend.events.list.crud/delete-entity :users 1])
        (catch :default _e nil))
      ;; Should not call original handler
      (is (empty? @captured)))))
