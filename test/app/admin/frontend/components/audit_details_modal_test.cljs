(ns app.admin.frontend.components.audit-details-modal-test
  (:require
    [app.admin.frontend.components.audit-details-modal :as audit-modal]
    [app.admin.frontend.test-setup :as setup]
    [app.template.frontend.utils.test-utils :as test-utils]
    [cljs.test :refer [deftest is testing]]
    [clojure.string :as str]
    [uix.core :refer [$]]))

(test-utils/setup-test-environment!)

(defn render-markup [audit-log]
  (test-utils/enhanced-render-to-static-markup
    ($ audit-modal/audit-details-body {:audit-log audit-log})))

(deftest audit-details-body-renders-promoted-audit-context
  (testing "detail view surfaces target fallback fields and API failure metadata"
    (setup/reset-db!)
    (let [markup (render-markup
                   {:id "audit-1"
                    :action "external_api_failure"
                    :actor-display-name "System"
                    :target-type "external_api"
                    :target-id "target-1"
                    :entity-name "google-oauth"
                    :context-summary "google-oauth • token-exchange • HTTP 400"
                    :http-status 400
                    :severity "error"
                    :error-message "OAuth token exchange non-200: 400"
                    :operation "token-exchange"
                    :changes {:api-name "google-oauth"
                              :operation "token-exchange"
                              :http-status 400
                              :severity "error"
                              :error-message "OAuth token exchange non-200: 400"}})]
      (is (str/includes? markup "Actor Information"))
      (is (str/includes? markup "System"))
      (is (str/includes? markup "Event Details"))
      (is (str/includes? markup "google-oauth"))
      (is (str/includes? markup "token-exchange"))
      (is (str/includes? markup "HTTP Status"))
      (is (str/includes? markup "400"))
      (is (str/includes? markup "external_api"))
      (is (str/includes? markup "target-1"))
      (is (str/includes? markup "OAuth token exchange non-200: 400")))))