(ns app.domain.frontend.expenses.admin.adapters.admin-crud-test
  (:require
    app.domain.frontend.expenses.admin.adapters.admin-crud
    [app.admin.frontend.utils.http :as admin-http]
    [app.domain.frontend.expenses.admin.adapters.admin-crud.common :as common]
    [app.domain.frontend.expenses.admin.adapters.admin-crud.entities :as entities]
    [app.domain.frontend.expenses.admin.adapters.admin-crud.register :as register]
    [app.template.frontend.shared.bridges.crud :as shared-bridges]
    [cljs.test :refer [deftest is testing]]))

(deftest build-admin-request-uri-shaping
  (testing "uses collection endpoint when id/ids are absent"
    (let [captured (atom nil)]
      (with-redefs [admin-http/admin-request (fn [opts]
                                               (reset! captured opts)
                                               opts)]
        (common/build-admin-request
          {:base-uri "/admin/api/expenses/suppliers"
           :log-prefix nil}
          {:method :get
           :params common/lookup-params
           :on-success [:ok]
           :on-failure [:err]})
        (is (= "/admin/api/expenses/suppliers" (:uri @captured)))
        (is (= common/lookup-params (:params @captured))))))

  (testing "uses /batch endpoint and stringifies ids"
    (let [captured (atom nil)]
      (with-redefs [admin-http/admin-request (fn [opts]
                                               (reset! captured opts)
                                               opts)]
        (common/build-admin-request
          {:base-uri "/admin/api/expenses/suppliers"
           :log-prefix nil}
          {:method :delete
           :ids [1 :two "three"]
           :on-success [:ok]
           :on-failure [:err]})
        (is (= "/admin/api/expenses/suppliers/batch" (:uri @captured)))
        (is (= {:ids ["1" ":two" "three"]} (:params @captured))))))

  (testing "encodes id for countries route"
    (let [captured (atom nil)]
      (with-redefs [admin-http/admin-request (fn [opts]
                                               (reset! captured opts)
                                               opts)]
        (common/build-admin-request
          {:base-uri "/admin/api/expenses/countries"
           :encode-id? true
           :log-prefix nil}
          {:method :put
           :id "BA/HR"
           :params {:country "Bosnia and Herzegovina/Croatia"}
           :on-success [:ok]
           :on-failure [:err]})
        (is (= "/admin/api/expenses/countries/BA%2FHR" (:uri @captured)))))))

(deftest crud-operations-admin-token-gate
  (let [operations (common/build-crud-operations
                     {:request-fn (fn [opts] opts)
                      :on-success-dispatch [:app.domain.frontend.expenses.events.suppliers/load-list {}]})
        fetch-request (get-in operations [:fetch :request])]
    (testing "redirects to login when admin token is missing"
      (is (= {:dispatch [:admin/redirect-to-login]}
            (fetch-request {:db {}} :suppliers {:db {}}))))

    (testing "issues request when admin token exists"
      (let [effect (fetch-request {:db {:admin/token "token"}} :suppliers {:db {:a 1}})]
        (is (= common/lookup-params (get-in effect [:http-xhrio :params])))
        (is (= [:app.template.frontend.events.list.crud/fetch-success :suppliers]
              (get-in effect [:http-xhrio :on-success])))))))

(deftest register-all-is-idempotent
  (register/register-all!)
  (register/register-all!)
  (doseq [{:keys [entity-key]} entities/admin-entities]
    (let [admin-bridges
          (->> (shared-bridges/get-bridges-for-entity entity-key)
            (filter #(= :admin (:bridge-id %)))
            vec)]
      (is (= 1 (count admin-bridges))
        (str "Expected one :admin bridge for " entity-key))
      (is (= #{:fetch :batch-delete :create :update}
            (-> admin-bridges first :operations keys set))
        (str "Unexpected operations for " entity-key)))))
