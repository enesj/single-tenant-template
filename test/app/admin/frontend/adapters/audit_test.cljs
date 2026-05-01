(ns app.admin.frontend.adapters.audit-test
  (:require
    [app.admin.frontend.adapters.audit :as audit-adapter]
    [app.admin.frontend.test-setup :as setup]
    [app.template.frontend.db.paths :as paths]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(deftest initialize-audit-ui-state-seeds-current-page-without-per-page
  (testing "initialize event seeds :current-page without hardcoding per-page"
    (setup/reset-db!)
    (rf/dispatch-sync [::audit-adapter/initialize-audit-ui-state])
    (let [db @rf-db/app-db
          base (paths/list-ui-state :audit-logs)]
      (is (= {:field :created-at :direction :desc}
            (get-in db (conj (paths/entity-metadata :audit-logs) :sort))))
      (is (= {:field :created-at :direction :desc}
            (get-in db (conj base :sort))))
      (is (= 1 (get-in db (conj base :pagination :current-page))))
      (is (nil? (get-in db (conj base :pagination :per-page)))
        "per-page should be left unset so list-view can seed it from configured defaults")
      (is (nil? (get-in db (conj base :per-page)))
        "legacy top-level per-page should not be initialized here")
      (is (= :server (get-in db (paths/list-pagination-mode :audit-logs))))
      (is (= [:admin/load-audit-logs] (get-in db (paths/list-refresh-event :audit-logs)))))))

(deftest initialize-audit-ui-state-preserves-existing-pagination
  (testing "initialize event preserves any existing pagination (incl per-page)"
    (setup/reset-db!)
    (let [base (paths/list-ui-state :audit-logs)]
      (swap! rf-db/app-db assoc-in base {:pagination {:current-page 3 :per-page 99}})
      (rf/dispatch-sync [::audit-adapter/initialize-audit-ui-state])
      (let [db @rf-db/app-db]
        (is (= 3 (get-in db (conj base :pagination :current-page)))
          "should not overwrite existing current-page")
        (is (= 99 (get-in db (conj base :pagination :per-page)))
          "should not overwrite existing per-page")))))

(deftest audit-log->template-entity-promotes-useful-display-fields
  (testing "system API failures expose actor, subject, and concise context fields"
    (let [result (audit-adapter/audit-log->template-entity
                   {:id "audit-1"
                    :actor-type "system"
                    :target-type "external_api"
                    :target-id "target-1"
                    :changes {:api-name "google-oauth"
                              :operation "token-exchange"
                              :http-status 400
                              :severity "error"
                              :error-message "OAuth token exchange non-200: 400"}})]
      (is (= "audit-1" (:id result)))
      (is (= "System" (:actor-display-name result)))
      (is (= "external_api" (:entity-type result)))
      (is (= "target-1" (:entity-id result)))
      (is (= "google-oauth" (:entity-name result)))
      (is (= "google-oauth • token-exchange • HTTP 400"
            (:context-summary result)))
      (is (= 400 (:http-status result)))
      (is (= "error" (:severity result)))
      (is (= "OAuth token exchange non-200: 400"
            (:error-message result))))))