(ns app.admin.backend.services.admin.users.bulk-test
  (:require
    [app.admin.backend.services.admin.users.bulk :as bulk]
    [app.template.backend.security.email :as email-privacy]
    [app.template.backend.utils.adapters.persistence :as persist]
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing]]))

(deftest export-users-csv-is-pseudonymous
  (testing "CSV export omits raw identity and relationship fields"
    (let [user-id #uuid "00000000-0000-0000-0000-000000000001"
          captured-query (atom nil)]
      (with-redefs [email-privacy/decrypt-email (fn [& _]
                                                  (throw (ex-info "CSV export must not decrypt emails" {})))
                    persist/execute-admin-query (fn [_db query normalize]
                                                  (reset! captured-query query)
                                                  (normalize [{:id user-id
                                                               :email "private@example.test"
                                                               :email_ciphertext "ciphertext"
                                                               :email_lookup_hash "lookup-hash"
                                                               :email_key_version "v1"
                                                               :full_name "Private Person"
                                                               :status "active"
                                                               :email_verified true
                                                               :auth_provider "password"
                                                               :created_at "2026-04-28T10:00:00Z"
                                                               :last_login_at "2026-04-28T11:00:00Z"
                                                               :tenant_name "Private Tenant"
                                                               :tenant_slug "private-tenant"}]))]
        (let [{:keys [success content]} (bulk/export-users-csv :db [user-id])]
          (is success)
          (is (= "User Ref,Status,Email Verified,Auth Provider,Created At,Last Login"
                (first (str/split-lines content))))
          (is (str/includes? content "User-00000000"))
          (is (not (str/includes? content "private@example.test")))
          (is (not (str/includes? content "ciphertext")))
          (is (not (str/includes? content "lookup-hash")))
          (is (not (str/includes? content "Private Person")))
          (is (not (str/includes? content "Private Tenant")))
          (is (not (contains? (:select @captured-query) :u.email_ciphertext)))
          (is (not (contains? (:select @captured-query) :u.full_name)))
          (is (nil? (:join @captured-query))))))))
