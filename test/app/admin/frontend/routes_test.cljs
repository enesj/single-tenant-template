(ns app.admin.frontend.routes-test
  (:require
    [app.admin.frontend.routes :as routes]
    [cljs.test :refer [deftest is testing]]
    [clojure.set :as set]
    [re-frame.core :as rf]))

(defn- route-maps [route-tree]
  (->> (tree-seq coll? seq route-tree)
    (filter map?)))

(defn- find-route [route-tree route-name]
  (some #(when (= (:name %) route-name) %)
    (route-maps route-tree)))

(deftest admin-routes-include-expected-pages
  (testing "admin routes expose expected named pages (single-tenant)"
    (let [names (set (keep :name (route-maps routes/admin-routes)))
          expected #{:admin-login
                     :admin-forgot-password
                     :admin-reset-password
                     :admin-dashboard
                     :admin-dashboard-alt
                     :admin-users
                     :admin-audit
                     :admin-login-events
                     :admin-admins
                     :admin-settings}]
      (is (set/subset? expected names)))))

(deftest guarded-start-wraps-dispatch
  (testing "guarded-start delegates to auth gate"
    (let [captured (atom nil)
          controller (routes/guarded-start [:admin/load-dashboard])]
      (with-redefs [rf/dispatch (fn [event] (reset! captured event))]
        ((:start controller) {:path {}}))
      (is (= [:admin/check-auth-protected [[:admin/load-dashboard]]] @captured)))))

(deftest users-route-triggers-loaders-through-guard
  (testing "users controller dispatches guarded load events (single-tenant)"
    (let [route (find-route routes/admin-routes :admin-users)
          start-fn (-> route :controllers first :start)
          captured (atom nil)]
      (with-redefs [rf/dispatch (fn [event] (reset! captured event))]
        (start-fn {:path {}}))
      (is (= [:admin/check-auth-protected
              [[:admin/load-users]]]
            @captured)))))
