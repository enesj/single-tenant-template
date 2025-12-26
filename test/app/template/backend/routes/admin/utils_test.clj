(ns app.template.backend.routes.admin.utils-test
  (:require
    [app.template.backend.routes.admin.utils :as utils]
    [cheshire.core :as json]
    [clojure.test :refer [deftest is testing]]))

(defn- parse-body [response]
  (json/parse-string (:body response) true))

(deftest with-error-handling-passes-through-4xx
  (testing "passes through explicit client errors (status + message)"
    (let [handler (utils/with-error-handling
                    (fn [_]
                      (throw (ex-info "Cannot delete a posted receipt" {:status 409})))
                    "Failed to delete receipt")
          response (handler {})]
      (is (= 409 (:status response)))
      (is (= "Cannot delete a posted receipt" (:error (parse-body response)))))))

(deftest with-error-handling-hides-unexpected-errors
  (testing "does not leak exception messages for unexpected errors"
    (let [handler (utils/with-error-handling
                    (fn [_]
                      (throw (Exception. "Database is on fire")))
                    "Failed to delete receipt")
          response (handler {})]
      (is (= 500 (:status response)))
      (is (= "Failed to delete receipt" (:error (parse-body response)))))))
