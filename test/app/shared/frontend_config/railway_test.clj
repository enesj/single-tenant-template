(ns app.shared.frontend-config.railway-test
  (:require
    [app.shared.frontend-config.railway :as railway]
    [clojure.test :refer [deftest is testing]]))

(deftest railway-public-database-url-rewrites-internal-host-to-public-proxy
  (is (= "postgres://user:pass@gondola.proxy.rlwy.net:12386/railway"
        (railway/railway-public-database-url
          "postgres://user:pass@postgres.railway.internal:5432/railway"))))

(deftest ensure-prod-profile-args-only-applies-to-profiled-settings-tasks
  (testing "profile-aware Railway settings tasks default to prod"
    (is (= ["--profile" "prod" "--only" "expenses"]
          (railway/ensure-prod-profile-args
            "export-frontend-config-from-db"
            ["--only" "expenses"]))))
  (testing "existing profile flag is preserved"
    (is (= ["--profile" "production" "--only" "expenses"]
          (railway/ensure-prod-profile-args
            "migrate-and-sync-frontend-config"
            ["--profile" "production" "--only" "expenses"]))))
  (testing "non-profiled settings tasks are forwarded unchanged"
    (is (= ["--schema" "resources/db"]
          (railway/ensure-prod-profile-args
            "validate-frontend-config"
            ["--schema" "resources/db"]))))
  (testing "bb command assembly uses the normalized task arguments"
    (is (= ["bb" "export-frontend-config-from-db" "--profile" "prod" "--only" "expenses"]
          (railway/build-bb-command
            "export-frontend-config-from-db"
            ["--only" "expenses"])))))