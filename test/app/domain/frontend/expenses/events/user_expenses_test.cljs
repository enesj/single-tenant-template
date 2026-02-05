(ns app.domain.frontend.expenses.events.user-expenses-test
  (:require
    ;; Ensure events are registered
    [app.domain.frontend.expenses.events.user-expenses]
    ;; Ensure template list events are registered
    app.template.frontend.events.list.crud
    app.template.frontend.events.list.batch
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.helpers-test :as helpers]
    [cljs.test :refer [deftest is testing]]
    [goog.object :as gobj]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(defonce captured-http-requests (atom []))

(defn- install-fx-stubs! []
  ;; Keep tests deterministic: no real HTTP, no timers.
  ;; Note: other test setup namespaces also stub :http-xhrio; re-register here so
  ;; these tests always capture requests into this namespace-local atom.
  (rf/reg-fx :http-xhrio (fn [req]
                           (swap! captured-http-requests conj req)
                           nil))
  (rf/reg-fx :dispatch-later (fn [_] nil)))

(defn- reset-db! []
  (install-fx-stubs!)
  (reset! captured-http-requests [])
  (reset! rf-db/app-db helpers/valid-test-db-state))

(defn- last-http-request []
  (last @captured-http-requests))

(defn- req-method [req]
  (let [m (or (:method req) (gobj/get req "method"))]
    (cond-> m (string? m) keyword)))

(defn- req-uri [req]
  (or (:uri req) (gobj/get req "uri")))

(defn- req-body [req]
  (or (:body req) (gobj/get req "body")))

(defn- req-params [req]
  (or (:params req) (gobj/get req "params")))

(defn- req-ids [req]
  (or (get-in (req-params req) [:ids])
    (let [body (req-body req)]
      (when (string? body)
        (try
          (-> (js/JSON.parse body)
            (js->clj :keywordize-keys true)
            :ids)
          (catch :default _ nil))))))

(defn- req-format-content-type [req]
  (let [fmt (or (:format req) (gobj/get req "format"))]
    (or (get fmt :content-type) (gobj/get fmt "content-type"))))

(deftest modal-create-tracks-recently-created
  (testing "create-expense-modal-success tracks :expenses in :ui :recently-created"
    (reset-db!)
    (rf/dispatch-sync [:user-expenses/create-expense-modal-success
                       nil
                       {:expense {:id "exp-1"}}])
    (is (= #{"exp-1"}
          (get-in @rf-db/app-db [:ui :recently-created :expenses])))))

(deftest modal-update-tracks-recently-updated
  (testing "update-expense-modal-success tracks :expenses in :ui :recently-updated"
    (reset-db!)
    (rf/dispatch-sync [:user-expenses/update-expense-modal-success
                       "exp-2"
                       nil
                       {:expense {:id "exp-2"}}])
    (is (= #{"exp-2"}
          (get-in @rf-db/app-db [:ui :recently-updated :expenses])))))

(deftest template-delete-expenses-is-bridged
  (testing "template delete-entity for :expenses uses /api/v1/expenses/batch (not generic /api/v1/entities)"
    (reset-db!)
    ;; Seed minimal entity + list state so the delete-success bridge can update it.
    (swap! rf-db/app-db assoc-in (paths/entity-data :expenses) {"exp-1" {:id "exp-1"}
                                                                "exp-2" {:id "exp-2"}})
    (swap! rf-db/app-db assoc-in (paths/entity-ids :expenses) ["exp-1" "exp-2"])
    (swap! rf-db/app-db assoc-in (paths/entity-selected-ids :expenses) #{"exp-1"})
    (swap! rf-db/app-db assoc-in (paths/list-total-items :expenses) 2)

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-entity :expenses "exp-1"])

    (let [req (last-http-request)]
      (is (= :delete (req-method req)))
      (is (= "/api/v1/expenses/batch" (req-uri req)))
      (is (= ["exp-1"] (req-ids req))))

    ;; Simulate the HTTP success event that the template CRUD delete would dispatch.
    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-success :expenses "exp-1"])

    (is (nil? (get-in @rf-db/app-db (conj (paths/entity-data :expenses) "exp-1"))))
    (is (= ["exp-2"] (get-in @rf-db/app-db (paths/entity-ids :expenses))))
    (is (= #{} (get-in @rf-db/app-db (paths/entity-selected-ids :expenses))))
    (is (= 1 (get-in @rf-db/app-db (paths/list-total-items :expenses))))))

(deftest template-delete-receipts-is-bridged
  (testing "template delete-entity for :receipts uses /api/v1/expenses/receipts/batch (not generic /api/v1/entities)"
    (reset-db!)

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-entity :receipts "rec-1"])

    (let [req (last-http-request)]
      (is (= :delete (req-method req)))
      (is (= "/api/v1/expenses/receipts/batch" (req-uri req)))
      (is (= ["rec-1"] (req-ids req))))))

(deftest template-delete-receipts-is-bridged-when-entity-type-is-string
  (testing "template delete-entity for \"receipts\" (string) is still bridged to /api/v1/expenses/receipts/batch"
    (reset-db!)

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-entity "receipts" "rec-1"])

    (let [req (last-http-request)]
      (is (= :delete (req-method req)))
      (is (= "/api/v1/expenses/receipts/batch" (req-uri req)))
      (is (= ["rec-1"] (req-ids req))))))

(deftest template-delete-receipts-not-bridged-in-admin-context
  (testing "in admin route context, receipts delete uses the admin receipts API (not the user receipts API)"
    (reset-db!)
    ;; Use the route name convention used by the admin router (e.g. :admin-users).
    (swap! rf-db/app-db assoc-in (paths/current-route-name) :admin-users)
    (swap! rf-db/app-db assoc :admin/token "test-token")

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-entity :receipts "rec-1"])

    (let [req (last-http-request)]
      (is (= :delete (req-method req)))
      (is (= "/admin/api/expenses/receipts/batch" (req-uri req)))
      (is (= ["rec-1"] (req-ids req))))))

(deftest template-delete-article-aliases-is-bridged
  (testing "template delete-entity for :article-aliases uses /api/v1/expenses/article-aliases/batch (not generic /api/v1/entities)"
    (reset-db!)

    ;; Seed minimal entity + list state so the delete-success bridge can update it.
    (swap! rf-db/app-db assoc-in (paths/entity-data :article-aliases)
      {"aa-1" {:id "aa-1"}
       "aa-2" {:id "aa-2"}})
    (swap! rf-db/app-db assoc-in (paths/entity-ids :article-aliases) ["aa-1" "aa-2"])
    (swap! rf-db/app-db assoc-in (paths/entity-selected-ids :article-aliases) #{"aa-1"})
    (swap! rf-db/app-db assoc-in (paths/list-total-items :article-aliases) 2)

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-entity :article-aliases "aa-1"])

    (let [req (last-http-request)]
      (is (= :delete (req-method req)))
      (is (= "/api/v1/expenses/article-aliases/batch" (req-uri req)))
      (is (= ["aa-1"] (req-ids req))))

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-success :article-aliases "aa-1"])

    (is (nil? (get-in @rf-db/app-db (conj (paths/entity-data :article-aliases) "aa-1"))))
    (is (= ["aa-2"] (get-in @rf-db/app-db (paths/entity-ids :article-aliases))))
    (is (= #{} (get-in @rf-db/app-db (paths/entity-selected-ids :article-aliases))))
    (is (= 1 (get-in @rf-db/app-db (paths/list-total-items :article-aliases))))))

(deftest template-delete-article-aliases-is-bridged-when-entity-type-is-string
  (testing "template delete-entity for \"article-aliases\" (string) is still bridged to /api/v1/expenses/article-aliases/batch"
    (reset-db!)

    ;; Seed state under the string entity-type key to simulate callers that
    ;; use route params/UI widget values before coercion.
    (swap! rf-db/app-db assoc-in (paths/entity-data "article-aliases")
      {"aa-1" {:id "aa-1"}
       "aa-2" {:id "aa-2"}})
    (swap! rf-db/app-db assoc-in (paths/entity-ids "article-aliases") ["aa-1" "aa-2"])
    (swap! rf-db/app-db assoc-in (paths/entity-selected-ids "article-aliases") #{"aa-1"})
    (swap! rf-db/app-db assoc-in (paths/list-total-items "article-aliases") 2)

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-entity "article-aliases" "aa-1"])

    (let [req (last-http-request)]
      (is (= :delete (req-method req)))
      (is (= "/api/v1/expenses/article-aliases/batch" (req-uri req)))
      (is (= ["aa-1"] (req-ids req))))

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-success "article-aliases" "aa-1"])

    (is (nil? (get-in @rf-db/app-db (conj (paths/entity-data "article-aliases") "aa-1"))))
    (is (= ["aa-2"] (get-in @rf-db/app-db (paths/entity-ids "article-aliases"))))
    (is (= #{} (get-in @rf-db/app-db (paths/entity-selected-ids "article-aliases"))))
    (is (= 1 (get-in @rf-db/app-db (paths/list-total-items "article-aliases"))))))

(deftest template-delete-articles-is-bridged
  (testing "template delete-entity for :articles uses /api/v1/expenses/articles/batch (not generic /api/v1/entities)"
    (reset-db!)

    ;; Seed minimal entity + list state so the delete-success bridge can update it.
    (swap! rf-db/app-db assoc-in (paths/entity-data :articles)
      {"a-1" {:id "a-1"}
       "a-2" {:id "a-2"}})
    (swap! rf-db/app-db assoc-in (paths/entity-ids :articles) ["a-1" "a-2"])
    (swap! rf-db/app-db assoc-in (paths/entity-selected-ids :articles) #{"a-1"})
    (swap! rf-db/app-db assoc-in (paths/list-total-items :articles) 2)

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-entity :articles "a-1"])

    (let [req (last-http-request)]
      (is (= :delete (req-method req)))
      (is (= "/api/v1/expenses/articles/batch" (req-uri req)))
      (is (= ["a-1"] (req-ids req))))

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-success :articles "a-1"])

    (is (nil? (get-in @rf-db/app-db (conj (paths/entity-data :articles) "a-1"))))
    (is (= ["a-2"] (get-in @rf-db/app-db (paths/entity-ids :articles))))
    (is (= #{} (get-in @rf-db/app-db (paths/entity-selected-ids :articles))))
    (is (= 1 (get-in @rf-db/app-db (paths/list-total-items :articles))))))

(deftest template-delete-articles-is-bridged-when-entity-type-is-string
  (testing "template delete-entity for \"articles\" (string) is still bridged to /api/v1/expenses/articles/batch"
    (reset-db!)

    ;; Seed state under the string entity-type key to simulate callers that
    ;; use route params/UI widget values before coercion.
    (swap! rf-db/app-db assoc-in (paths/entity-data "articles")
      {"a-1" {:id "a-1"}
       "a-2" {:id "a-2"}})
    (swap! rf-db/app-db assoc-in (paths/entity-ids "articles") ["a-1" "a-2"])
    (swap! rf-db/app-db assoc-in (paths/entity-selected-ids "articles") #{"a-1"})
    (swap! rf-db/app-db assoc-in (paths/list-total-items "articles") 2)

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-entity "articles" "a-1"])

    (let [req (last-http-request)]
      (is (= :delete (req-method req)))
      (is (= "/api/v1/expenses/articles/batch" (req-uri req)))
      (is (= ["a-1"] (req-ids req))))

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-success "articles" "a-1"])

    (is (nil? (get-in @rf-db/app-db (conj (paths/entity-data "articles") "a-1"))))
    (is (= ["a-2"] (get-in @rf-db/app-db (paths/entity-ids "articles"))))
    (is (= #{} (get-in @rf-db/app-db (paths/entity-selected-ids "articles"))))
    (is (= 1 (get-in @rf-db/app-db (paths/list-total-items "articles"))))))

(deftest template-delete-price-observations-is-bridged
  (testing "template delete-entity for :price-observations uses /api/v1/expenses/price-observations/batch (not generic /api/v1/entities)"
    (reset-db!)

    ;; Seed minimal entity + list state so the delete-success bridge can update it.
    (swap! rf-db/app-db assoc-in (paths/entity-data :price-observations)
      {"po-1" {:id "po-1"}
       "po-2" {:id "po-2"}})
    (swap! rf-db/app-db assoc-in (paths/entity-ids :price-observations) ["po-1" "po-2"])
    (swap! rf-db/app-db assoc-in (paths/entity-selected-ids :price-observations) #{"po-1"})
    (swap! rf-db/app-db assoc-in (paths/list-total-items :price-observations) 2)

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-entity :price-observations "po-1"])

    (let [req (last-http-request)]
      (is (= :delete (req-method req)))
      (is (= "/api/v1/expenses/price-observations/batch" (req-uri req)))
      (is (= ["po-1"] (req-ids req))))

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/delete-success :price-observations "po-1"])

    (is (nil? (get-in @rf-db/app-db (conj (paths/entity-data :price-observations) "po-1"))))
    (is (= ["po-2"] (get-in @rf-db/app-db (paths/entity-ids :price-observations))))
    (is (= #{} (get-in @rf-db/app-db (paths/entity-selected-ids :price-observations))))
    (is (= 1 (get-in @rf-db/app-db (paths/list-total-items :price-observations))))))

(deftest template-batch-update-expenses-is-bridged
  (testing "template batch update for :expenses uses /api/v1/expenses/batch (not generic /api/v1/entities/expenses/batch)"
    (reset-db!)

    (rf/dispatch-sync
      [:app.template.frontend.events.list.batch/batch-update
       {:entity-name :expenses
        :item-ids ["exp-1"]
        :values {:notes "hello"}}])

    (let [req (last-http-request)
          items (get-in req [:params :items])
          item (first items)]
      (is (= :put (req-method req)))
      (is (= "/api/v1/expenses/batch" (req-uri req)))
      (is (= 1 (count items)))
      (is (= "exp-1" (:id item)))
      (is (= "hello" (:notes item)))
      (is (instance? js/Date (:updated-at item))))))

(deftest template-fetch-lookups-is-bridged
  (testing "template fetch for payers/suppliers/receipts uses /api/v1/expenses/* (not generic /api/v1/entities/*)"
    (reset-db!)

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/fetch-entities :payers])
    (let [req (last-http-request)]
      (is (= :get (req-method req)))
      (is (= "/api/v1/expenses/payers?limit=500&offset=0" (req-uri req))))

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/fetch-entities :suppliers])
    (let [req (last-http-request)]
      (is (= :get (req-method req)))
      (is (= "/api/v1/expenses/suppliers?limit=500&offset=0" (req-uri req))))

    (rf/dispatch-sync [:app.template.frontend.events.list.crud/fetch-entities :receipts])
    (let [req (last-http-request)]
      (is (= :get (req-method req)))
      (is (= "/api/v1/expenses/receipts?limit=500&offset=0" (req-uri req))))))

(deftest upload-receipt-sends-file-in-formdata
  (testing "upload-receipt sends multipart file (guards against trim-v arg loss)"
    (reset-db!)
    (let [file (js/File. #js ["abc"] "r.jpg" #js {:type "image/jpeg"})]
      (rf/dispatch-sync [:user-expenses/set-upload-payer-id "payer-1"])
      (rf/dispatch-sync [:user-expenses/upload-receipt file])
      (let [req (last-http-request)
            body (req-body req)
            uploaded (.get body "file")
            payer-id (.get body "payer_id")]
        (is (= :post (req-method req)))
        (is (string? (req-uri req)))
        (is (instance? js/FormData body))
        (is (instance? js/File uploaded))
        (is (= "r.jpg" (.-name uploaded)))
        (is (= "payer-1" payer-id))
        (is (not (true? (req-format-content-type req))))))))

(deftest upload-receipt-success-queues-ocr
  (testing "upload-receipt-success automatically triggers OCR"
    (reset-db!)
    (rf/dispatch-sync [:user-expenses/upload-receipt-success {:data {:id "rec-1"}}])
    (is (= 1 (count @captured-http-requests)))
    (let [req (last-http-request)]
      (is (= :post (req-method req)))
      (is (= "/api/v1/expenses/receipts/rec-1/ocr" (req-uri req)))
      (is (nil? (req-params req))))))

(deftest upload-receipt-duplicate-shows-notice-and-skips-ocr
  (testing "duplicate uploads show a notice and do not queue OCR"
    (reset-db!)
    (rf/dispatch-sync [:user-expenses/upload-receipt-success
                       {:data {:id "rec-1" :original_filename "r.jpg"}
                        :duplicate? true}])
    (is (= 0 (count @captured-http-requests)))
    (let [notice (get-in @rf-db/app-db [:user-expenses :upload :notice])]
      (is (seq notice))
      (is (some #(re-find #"Already uploaded" (str %)) notice)))))

(deftest upload-receipts-batch-sends-multiple-requests
  (testing "upload-receipts queues files and uploads sequentially"
    (reset-db!)
    (let [f1 (js/File. #js ["a"] "a.jpg" #js {:type "image/jpeg"})
          f2 (js/File. #js ["b"] "b.jpg" #js {:type "image/jpeg"})]
      (rf/dispatch-sync [:user-expenses/set-upload-payer-id "payer-xyz"])
      (rf/dispatch-sync [:user-expenses/upload-receipts [f1 f2]])

      (is (= 1 (count @captured-http-requests)))
      (let [req1 (last-http-request)
            body1 (req-body req1)
            uploaded1 (.get body1 "file")
            payer1 (.get body1 "payer_id")]
        (is (= :post (req-method req1)))
        (is (instance? js/FormData body1))
        (is (instance? js/File uploaded1))
        (is (= "payer-xyz" payer1))
        (is (= "a.jpg" (.-name uploaded1))))

      ;; simulate success -> triggers next request
      (rf/dispatch-sync [:user-expenses/upload-receipts-success [f2] {:data {:id "rec-1"}}])

      (is (= 2 (count @captured-http-requests)))
      (let [req2 (last-http-request)
            body2 (req-body req2)
            uploaded2 (.get body2 "file")
            payer2 (.get body2 "payer_id")]
        (is (instance? js/FormData body2))
        (is (instance? js/File uploaded2))
        (is (= "payer-xyz" payer2))
        (is (= "b.jpg" (.-name uploaded2))))

      (rf/dispatch-sync [:user-expenses/upload-receipts-success [] {:data {:id "rec-2"}}])
      ;; Final success should queue OCR for each uploaded receipt.
      (is (= 4 (count @captured-http-requests)))
      (let [req3 (nth @captured-http-requests 2)
            req4 (nth @captured-http-requests 3)]
        (is (= :post (req-method req3)))
        (is (= "/api/v1/expenses/receipts/rec-1/ocr" (req-uri req3)))

        (is (= :post (req-method req4)))
        (is (= "/api/v1/expenses/receipts/rec-2/ocr" (req-uri req4))))
      (is (false? (get-in @rf-db/app-db [:user-expenses :upload :loading?]))))))

(deftest upload-receipts-batch-continues-after-failure
  (testing "upload-receipts continues queue after a failure"
    (reset-db!)
    (let [f1 (js/File. #js ["a"] "a.jpg" #js {:type "image/jpeg"})
          f2 (js/File. #js ["b"] "b.jpg" #js {:type "image/jpeg"})]
      (rf/dispatch-sync [:user-expenses/upload-receipts [f1 f2]])

      (rf/dispatch-sync [:user-expenses/upload-receipts-failure
                         [f2]
                         "a.jpg"
                         {:response {:error "Boom"}}])

      (is (= 2 (count @captured-http-requests)))
      (let [req2 (last-http-request)
            body2 (req-body req2)
            uploaded2 (.get body2 "file")]
        (is (instance? js/FormData body2))
        (is (instance? js/File uploaded2))
        (is (= "b.jpg" (.-name uploaded2))))

      (is (= 1 (get-in @rf-db/app-db [:user-expenses :upload :batch :failed])))
      (let [msg (get-in @rf-db/app-db [:user-expenses :upload :error])]
        (is (string? msg))
        (is (.startsWith msg "a.jpg:")))

      ;; simulate success of the remaining file -> should queue OCR for receipts that did upload
      (rf/dispatch-sync [:user-expenses/upload-receipts-success [] {:data {:id "rec-2"}}])
      (is (= 3 (count @captured-http-requests)))
      (let [req3 (last-http-request)]
        (is (= :post (req-method req3)))
        (is (= "/api/v1/expenses/receipts/rec-2/ocr" (req-uri req3)))
        (is (nil? (req-params req3)))))))

(deftest update-settings-sends-json-params
  (testing "update-settings sends JSON payload in :params (not :body)"
    (reset-db!)
    (let [settings {:default-currency "EUR"
                    :default-payer-id ""
                    :notifications-enabled true
                    :receipt-refine-enabled true}]
      (rf/dispatch-sync [:user-expenses/update-settings settings])
      (let [req (last-http-request)]
        (is (= :put (req-method req)))
        (is (= "/api/v1/expenses/settings" (req-uri req)))
        (is (= settings (req-params req)))
        (is (nil? (req-body req)))))))
