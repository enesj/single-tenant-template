(ns app.mobile.frontend.core-test
  (:require
    [app.mobile.frontend.auth-redirect :as sut]
    [cljs.test :refer [deftest is testing]]))

(deftest auth-status-navigation-path-preserves-and-restores-mobile-routes
  (testing "authenticated users stay on already matched app routes"
    (is (nil? (sut/auth-status-navigation-path true false :m/upload-manual "/m/upload/manual")))
    (is (nil? (sut/auth-status-navigation-path true false :m/upload "/m/upload")))
    (is (nil? (sut/auth-status-navigation-path true false :m/reports "/m/reports"))))

  (testing "hard reloads re-seed known in-app paths when current-view is still empty"
    (is (= "/m/upload/manual" (sut/auth-status-navigation-path true false nil "/m/upload/manual")))
    (is (= "/m/reports" (sut/auth-status-navigation-path true false nil "/m/reports")))))

(deftest auth-status-navigation-path-still-enforces-auth-guards
  (testing "auth and tenant guard routes still redirect as expected"
    (is (= "/m/dashboard" (sut/auth-status-navigation-path true false :m/login "/m/login")))
    (is (= "/m/dashboard" (sut/auth-status-navigation-path true false nil "/m")))
    (is (= "/m/dashboard" (sut/auth-status-navigation-path true false nil "/outside")))
    (is (= "/m/tenant-select" (sut/auth-status-navigation-path true true :m/upload-manual "/m/upload/manual")))
    (is (= "/m/login" (sut/auth-status-navigation-path false false :m/upload-manual "/m/upload/manual")))))
