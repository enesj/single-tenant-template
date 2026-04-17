(ns app.admin.backend.services.admin.admins-test
  "Tests for admin service email projection and filtering behavior."
  (:require
    [app.admin.backend.services.admin.admins :as admins-svc]
    [app.template.backend.security.email :as email-privacy]
    [clojure.test :refer [deftest is testing]])
  (:import
    [java.util UUID]))

(deftest db-admin->app-resolves-full-email-before-normalization
  (testing "db-admin->app keeps full email visible while stripping persistence-only fields"
    (let [email "admins-svc-details@example.com"
          raw-admin (merge {:id (UUID/randomUUID)
                            :full_name "Admin Details"
                            :role "admin"
                            :status "active"
                            :password_hash "hashed-password"}
                      (email-privacy/email-storage email))
          admin (#'admins-svc/db-admin->app raw-admin)]
      (is (= email (:email admin)))
      (is (= (email-privacy/mask-email email) (:email-masked admin)))
      (is (nil? (:email-ciphertext admin)))
      (is (nil? (:email-lookup-hash admin)))
      (is (nil? (:password-hash admin))))))

(deftest build-admin-list-filter-clauses-includes-email-hash-match
  (testing "email filters are converted to the blind lookup hash clause"
    (let [email "filter-admin@example.com"
          clauses (#'admins-svc/build-admin-list-filter-clauses {:email email})]
      (is (= [(email-privacy/email-match-clause :a/email_lookup_hash :a/email email)]
            clauses)))))