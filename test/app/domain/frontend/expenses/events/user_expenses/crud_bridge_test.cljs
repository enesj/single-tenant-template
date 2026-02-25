(ns app.domain.frontend.expenses.events.user-expenses.crud-bridge-test
  (:require
    [app.domain.frontend.expenses.events.user-expenses.test-support :as sup]
    [app.template.frontend.db.paths :as paths]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(deftest modal-create-tracks-recently-created
  (testing "create-expense-modal-success tracks :expenses in :ui :recently-created"
    (sup/reset-db!)
    (rf/dispatch-sync [:user-expenses/create-expense-modal-success
                       nil
                       {:expense {:id "exp-1"}}])
    (is (= #{"exp-1"}
          (get-in @rf-db/app-db [:ui :recently-created :expenses])))))

(deftest modal-update-tracks-recently-updated
  (testing "update-expense-modal-success tracks :expenses in :ui :recently-updated"
    (sup/reset-db!)
    (rf/dispatch-sync [:user-expenses/update-expense-modal-success
                       "exp-2"
                       nil
                       {:expense {:id "exp-2"}}])
    (is (= #{"exp-2"}
          (get-in @rf-db/app-db [:ui :recently-updated :expenses])))))

(deftest template-delete-expenses-is-bridged
  (testing "template delete-entity for :expenses uses /api/v1/expenses/batch (not generic /api/v1/entities)"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/entity-data :expenses) {"exp-1" {:id "exp-1"}
                                                                "exp-2" {:id "exp-2"}})
    (swap! rf-db/app-db assoc-in (paths/entity-ids :expenses) ["exp-1" "exp-2"])
    (swap! rf-db/app-db assoc-in (paths/entity-selected-ids :expenses) #{"exp-1"})
    (swap! rf-db/app-db assoc-in (paths/list-total-items :expenses) 2)

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-entity :expenses "exp-1"])

    (let [req (sup/last-http-request)]
      (is (= :delete (sup/req-method req)))
      (is (= "/api/v1/expenses/batch" (sup/req-uri req)))
      (is (= ["exp-1"] (sup/req-ids req))))

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-success :expenses "exp-1"])

    (is (nil? (get-in @rf-db/app-db (conj (paths/entity-data :expenses) "exp-1"))))
    (is (= ["exp-2"] (get-in @rf-db/app-db (paths/entity-ids :expenses))))
    (is (= #{} (get-in @rf-db/app-db (paths/entity-selected-ids :expenses))))
    (is (= 1 (get-in @rf-db/app-db (paths/list-total-items :expenses))))))

(deftest template-delete-receipts-is-bridged
  (testing "template delete-entity for :receipts uses /api/v1/expenses/receipts/batch (not generic /api/v1/entities)"
    (sup/reset-db!)
    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-entity :receipts "rec-1"])
    (let [req (sup/last-http-request)]
      (is (= :delete (sup/req-method req)))
      (is (= "/api/v1/expenses/receipts/batch" (sup/req-uri req)))
      (is (= ["rec-1"] (sup/req-ids req))))))

(deftest template-delete-receipts-is-bridged-when-entity-type-is-string
  (testing "template delete-entity for \"receipts\" (string) is still bridged to /api/v1/expenses/receipts/batch"
    (sup/reset-db!)
    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-entity "receipts" "rec-1"])
    (let [req (sup/last-http-request)]
      (is (= :delete (sup/req-method req)))
      (is (= "/api/v1/expenses/receipts/batch" (sup/req-uri req)))
      (is (= ["rec-1"] (sup/req-ids req))))))

(deftest template-batch-delete-stores-is-routed-to-expenses-endpoint
  (testing "template batch-delete for :stores uses /api/v1/expenses/stores/batch (not generic /api/v1/entities/stores/batch)"
    (sup/reset-db!)
    (rf/dispatch-sync [:app.template.frontend.events.list.crud/batch-delete :stores ["store-1" "store-2"]])
    (let [req (sup/last-http-request)]
      (is (= :delete (sup/req-method req)))
      (is (= "/api/v1/expenses/stores/batch" (sup/req-uri req)))
      (is (= ["store-1" "store-2"] (sup/req-ids req))))))

(deftest template-batch-delete-stores-uses-admin-endpoint-in-admin-context
  (testing "in admin route context, stores batch-delete uses /admin/api/expenses/stores/batch"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/current-route-name) :admin-users)
    (swap! rf-db/app-db assoc :admin/token "test-token")
    (rf/dispatch-sync [:app.template.frontend.events.list.crud/batch-delete :stores ["store-1"]])
    (let [req (sup/last-http-request)]
      (is (= :delete (sup/req-method req)))
      (is (= "/admin/api/expenses/stores/batch" (sup/req-uri req)))
      (is (= ["store-1"] (sup/req-ids req))))))

(deftest template-delete-receipts-not-bridged-in-admin-context
  (testing "in admin route context, receipts delete uses the admin receipts API (not the user receipts API)"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/current-route-name) :admin-users)
    (swap! rf-db/app-db assoc :admin/token "test-token")
    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-entity :receipts "rec-1"])
    (let [req (sup/last-http-request)]
      (is (= :delete (sup/req-method req)))
      (is (= "/admin/api/expenses/receipts/batch" (sup/req-uri req)))
      (is (= ["rec-1"] (sup/req-ids req))))))

(deftest template-delete-article-aliases-is-bridged
  (testing "template delete-entity for :article-aliases uses /api/v1/expenses/article-aliases/batch"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/entity-data :article-aliases) {"aa-1" {:id "aa-1"}
                                                                       "aa-2" {:id "aa-2"}})
    (swap! rf-db/app-db assoc-in (paths/entity-ids :article-aliases) ["aa-1" "aa-2"])
    (swap! rf-db/app-db assoc-in (paths/entity-selected-ids :article-aliases) #{"aa-1"})
    (swap! rf-db/app-db assoc-in (paths/list-total-items :article-aliases) 2)

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-entity :article-aliases "aa-1"])

    (let [req (sup/last-http-request)]
      (is (= :delete (sup/req-method req)))
      (is (= "/api/v1/expenses/article-aliases/batch" (sup/req-uri req)))
      (is (= ["aa-1"] (sup/req-ids req))))

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-success :article-aliases "aa-1"])

    (is (nil? (get-in @rf-db/app-db (conj (paths/entity-data :article-aliases) "aa-1"))))
    (is (= ["aa-2"] (get-in @rf-db/app-db (paths/entity-ids :article-aliases))))
    (is (= #{} (get-in @rf-db/app-db (paths/entity-selected-ids :article-aliases))))
    (is (= 1 (get-in @rf-db/app-db (paths/list-total-items :article-aliases))))))

(deftest template-delete-article-aliases-is-bridged-when-entity-type-is-string
  (testing "template delete-entity for \"article-aliases\" (string) is still bridged"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/entity-data "article-aliases") {"aa-1" {:id "aa-1"}
                                                                         "aa-2" {:id "aa-2"}})
    (swap! rf-db/app-db assoc-in (paths/entity-ids "article-aliases") ["aa-1" "aa-2"])
    (swap! rf-db/app-db assoc-in (paths/entity-selected-ids "article-aliases") #{"aa-1"})
    (swap! rf-db/app-db assoc-in (paths/list-total-items "article-aliases") 2)

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-entity "article-aliases" "aa-1"])

    (let [req (sup/last-http-request)]
      (is (= :delete (sup/req-method req)))
      (is (= "/api/v1/expenses/article-aliases/batch" (sup/req-uri req)))
      (is (= ["aa-1"] (sup/req-ids req))))

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-success "article-aliases" "aa-1"])

    (is (nil? (get-in @rf-db/app-db (conj (paths/entity-data "article-aliases") "aa-1"))))
    (is (= ["aa-2"] (get-in @rf-db/app-db (paths/entity-ids "article-aliases"))))
    (is (= #{} (get-in @rf-db/app-db (paths/entity-selected-ids "article-aliases"))))
    (is (= 1 (get-in @rf-db/app-db (paths/list-total-items "article-aliases"))))))

(deftest template-delete-articles-is-bridged
  (testing "template delete-entity for :articles uses /api/v1/expenses/articles/batch"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/entity-data :articles) {"a-1" {:id "a-1"}
                                                                 "a-2" {:id "a-2"}})
    (swap! rf-db/app-db assoc-in (paths/entity-ids :articles) ["a-1" "a-2"])
    (swap! rf-db/app-db assoc-in (paths/entity-selected-ids :articles) #{"a-1"})
    (swap! rf-db/app-db assoc-in (paths/list-total-items :articles) 2)

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-entity :articles "a-1"])
    (let [req (sup/last-http-request)]
      (is (= :delete (sup/req-method req)))
      (is (= "/api/v1/expenses/articles/batch" (sup/req-uri req)))
      (is (= ["a-1"] (sup/req-ids req))))

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-success :articles "a-1"])
    (is (nil? (get-in @rf-db/app-db (conj (paths/entity-data :articles) "a-1"))))
    (is (= ["a-2"] (get-in @rf-db/app-db (paths/entity-ids :articles))))
    (is (= #{} (get-in @rf-db/app-db (paths/entity-selected-ids :articles))))
    (is (= 1 (get-in @rf-db/app-db (paths/list-total-items :articles))))))

(deftest template-delete-articles-is-bridged-when-entity-type-is-string
  (testing "template delete-entity for \"articles\" (string) is still bridged"
    (sup/reset-db!)
    (swap! rf-db/app-db assoc-in (paths/entity-data "articles") {"a-1" {:id "a-1"}
                                                                  "a-2" {:id "a-2"}})
    (swap! rf-db/app-db assoc-in (paths/entity-ids "articles") ["a-1" "a-2"])
    (swap! rf-db/app-db assoc-in (paths/entity-selected-ids "articles") #{"a-1"})
    (swap! rf-db/app-db assoc-in (paths/list-total-items "articles") 2)

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-entity "articles" "a-1"])
    (let [req (sup/last-http-request)]
      (is (= :delete (sup/req-method req)))
      (is (= "/api/v1/expenses/articles/batch" (sup/req-uri req)))
      (is (= ["a-1"] (sup/req-ids req))))

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-success "articles" "a-1"])
    (is (nil? (get-in @rf-db/app-db (conj (paths/entity-data "articles") "a-1"))))
    (is (= ["a-2"] (get-in @rf-db/app-db (paths/entity-ids "articles"))))
    (is (= #{} (get-in @rf-db/app-db (paths/entity-selected-ids "articles"))))
    (is (= 1 (get-in @rf-db/app-db (paths/list-total-items "articles"))))))

(deftest template-batch-update-expenses-is-bridged
  (testing "template batch-update for :expenses hits expenses endpoint"
    (sup/reset-db!)
    (rf/dispatch-sync
      [:app.template.frontend.events.list.batch/batch-update
       {:entity-name :expenses
        :item-ids ["exp-1"]
        :values {:notes "hello"}}])
    (let [req (sup/last-http-request)
          items (get-in req [:params :items])
          item (first items)]
      (is (= :put (sup/req-method req)))
      (is (= "/api/v1/expenses/batch" (sup/req-uri req)))
      (is (= 1 (count items)))
      (is (= "exp-1" (:id item)))
      (is (= "hello" (:notes item)))
      (is (instance? js/Date (:updated-at item))))))

(deftest template-fetch-lookups-is-bridged
  (testing "template fetch for payers/suppliers/receipts uses /api/v1/expenses/* (not generic /api/v1/entities/*)"
    (sup/reset-db!)

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/fetch-entities :payers])
    (let [req (sup/last-http-request)]
      (is (= :get (sup/req-method req)))
      (is (= "/api/v1/expenses/payers?limit=500&offset=0" (sup/req-uri req))))

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/fetch-entities :suppliers])
    (let [req (sup/last-http-request)]
      (is (= :get (sup/req-method req)))
      (is (= "/api/v1/expenses/suppliers?limit=500&offset=0" (sup/req-uri req))))

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/fetch-entities :receipts])
    (let [req (sup/last-http-request)]
      (is (= :get (sup/req-method req)))
      (is (= "/api/v1/expenses/receipts?limit=500&offset=0" (sup/req-uri req))))))
