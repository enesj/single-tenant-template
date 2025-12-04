# Backend Test Patterns

Common testing patterns used throughout the backend test suite.

## Handler Testing Patterns

### Pattern 1: Mock Handler Factory

When testing route handlers created by factory functions:

```clojure
(deftest create-handler-test
  (testing "handler creation"
    (let [deps {:db (h/mock-db {})}
          handler-fn (my-routes/create-handler deps)]
      (is (fn? handler-fn))
      
      (testing "handles valid request"
        (let [response (handler-fn (h/mock-admin-request :get "/endpoint"))]
          (is (= 200 (:status response))))))))
```

### Pattern 2: Direct Handler Testing

For handlers that don't need factory setup:

```clojure
(deftest direct-handler-test
  (with-redefs [service/get-data (constantly {:items [1 2 3]})]
    (let [response (my-handler (h/mock-admin-request :get "/data"))]
      (is (= 200 (:status response)))
      (is (= [1 2 3] (-> response h/parse-response-body :items))))))
```

### Pattern 3: Route Structure Verification

Verify that routes are properly defined:

```clojure
(deftest routes-structure-test
  (let [routes (my-routes/routes {:db mock-db})]
    (is (vector? routes) "routes should be a vector")
    (is (= "/my-endpoint" (first routes)) "first element is path")
    (is (map? (second routes)) "second element is handler map")
    (is (contains? (second routes) :get) "has GET handler")))
```

## Service Mocking Patterns

### Pattern 4: Full Service Mock

Mock entire service namespace:

```clojure
(deftest service-integration-test
  (with-redefs [user-service/find-by-id (fn [_ id] {:id id :name "Test"})
                user-service/update! (fn [_ id data] (assoc data :id id))
                user-service/delete! (fn [_ id] {:deleted id})]
    ;; Tests here see mocked service
    ))
```

### Pattern 5: Conditional Mock

Mock that behaves differently based on input:

```clojure
(deftest conditional-behavior-test
  (with-redefs [service/find-by-id 
                (fn [_ id]
                  (case id
                    1 {:id 1 :name "Found"}
                    2 nil
                    (throw (ex-info "Unexpected" {:id id}))))]
    
    (testing "found case"
      (let [response (handler (h/mock-admin-request :get "/items/1"))]
        (is (= 200 (:status response)))))
    
    (testing "not found case"
      (let [response (handler (h/mock-admin-request :get "/items/2"))]
        (is (= 404 (:status response)))))))
```

### Pattern 6: Assertion Inside Mock

Verify mock was called with expected args:

```clojure
(deftest mock-assertion-test
  (let [call-log (atom [])]
    (with-redefs [service/process 
                  (fn [db data]
                    (swap! call-log conj {:db db :data data})
                    {:processed true})]
      
      (handler (h/mock-admin-request :post "/process" {:value 42}))
      
      (is (= 1 (count @call-log)))
      (is (= {:value 42} (:data (first @call-log)))))))
```

## Response Testing Patterns

### Pattern 7: Status Code Verification

```clojure
(deftest status-codes-test
  (testing "success responses"
    (is (= 200 (:status (handler get-request))))
    (is (= 201 (:status (handler post-request))))
    (is (= 204 (:status (handler delete-request)))))
  
  (testing "error responses"
    (is (= 400 (:status (handler invalid-request))))
    (is (= 404 (:status (handler not-found-request))))
    (is (= 500 (:status (handler error-request))))))
```

### Pattern 8: Response Body Structure

```clojure
(deftest response-structure-test
  (let [body (h/parse-response-body response)]
    (testing "has required fields"
      (is (contains? body :data))
      (is (contains? body :meta)))
    
    (testing "data structure"
      (is (vector? (:data body)))
      (is (every? #(contains? % :id) (:data body))))
    
    (testing "meta structure"
      (is (number? (get-in body [:meta :total])))
      (is (number? (get-in body [:meta :page]))))))
```

### Pattern 9: Error Response Format

```clojure
(deftest error-format-test
  (with-redefs [service/validate (constantly {:valid false :errors ["Invalid email"]})]
    (let [response (handler (h/mock-admin-request :post "/users" {:email "bad"}))
          body (h/parse-response-body response)]
      (is (= 400 (:status response)))
      (is (contains? body :error))
      (is (string? (:error body))))))
```

## CRUD Testing Patterns

### Pattern 10: List Endpoint

```clojure
(deftest list-endpoint-test
  (with-redefs [service/list-all (constantly [{:id 1} {:id 2} {:id 3}])]
    (let [response (handler (h/mock-admin-request :get "/items"))
          body (h/parse-response-body response)]
      (is (= 200 (:status response)))
      (is (= 3 (count (:data body)))))))
```

### Pattern 11: Get By ID Endpoint

```clojure
(deftest get-by-id-test
  (testing "existing item"
    (with-redefs [service/find-by-id (constantly {:id 1 :name "Test"})]
      (let [response (handler (h/mock-admin-request :get "/items/1"))]
        (is (= 200 (:status response)))
        (is (= 1 (-> response h/parse-response-body :id))))))
  
  (testing "non-existing item"
    (with-redefs [service/find-by-id (constantly nil)]
      (let [response (handler (h/mock-admin-request :get "/items/999"))]
        (is (= 404 (:status response)))))))
```

### Pattern 12: Create Endpoint

```clojure
(deftest create-endpoint-test
  (testing "valid creation"
    (with-redefs [service/create! (fn [_ data] (assoc data :id 1))]
      (let [response (handler (h/mock-admin-request :post "/items" {:name "New"}))]
        (is (= 201 (:status response)))
        (is (= 1 (-> response h/parse-response-body :id))))))
  
  (testing "invalid data"
    (with-redefs [service/validate (constantly {:valid false})]
      (let [response (handler (h/mock-admin-request :post "/items" {}))]
        (is (= 400 (:status response)))))))
```

### Pattern 13: Update Endpoint

```clojure
(deftest update-endpoint-test
  (with-redefs [service/find-by-id (constantly {:id 1 :name "Old"})
                service/update! (fn [_ id data] (assoc data :id id))]
    (let [response (handler (h/mock-admin-request :put "/items/1" {:name "New"}))]
      (is (= 200 (:status response)))
      (is (= "New" (-> response h/parse-response-body :name))))))
```

### Pattern 14: Delete Endpoint

```clojure
(deftest delete-endpoint-test
  (testing "successful deletion"
    (with-redefs [service/find-by-id (constantly {:id 1})
                  service/delete! (constantly true)]
      (let [response (handler (h/mock-admin-request :delete "/items/1"))]
        (is (= 204 (:status response))))))
  
  (testing "delete non-existing"
    (with-redefs [service/find-by-id (constantly nil)]
      (let [response (handler (h/mock-admin-request :delete "/items/999"))]
        (is (= 404 (:status response)))))))
```

## Pagination Testing Patterns

### Pattern 15: Pagination Parameters

```clojure
(deftest pagination-test
  (with-redefs [service/list-paginated 
                (fn [_ {:keys [limit offset]}]
                  {:data (repeat limit {:id 1})
                   :total 100
                   :limit limit
                   :offset offset})]
    
    (testing "default pagination"
      (let [response (handler (h/mock-admin-request :get "/items"))
            body (h/parse-response-body response)]
        (is (= 20 (count (:data body))))  ; default limit
        (is (= 100 (:total body)))))
    
    (testing "custom limit"
      (let [response (handler (h/mock-admin-request :get "/items?limit=5"))
            body (h/parse-response-body response)]
        (is (= 5 (count (:data body))))))
    
    (testing "offset"
      (let [response (handler (h/mock-admin-request :get "/items?offset=50"))
            body (h/parse-response-body response)]
        (is (= 50 (:offset body)))))))
```

## Filter Testing Patterns

### Pattern 16: Query Filters

```clojure
(deftest filter-test
  (let [captured-filters (atom nil)]
    (with-redefs [service/list-filtered 
                  (fn [_ filters]
                    (reset! captured-filters filters)
                    [])]
      
      (testing "status filter"
        (handler (h/mock-admin-request :get "/items?status=active"))
        (is (= "active" (:status @captured-filters))))
      
      (testing "date range filter"
        (handler (h/mock-admin-request :get "/items?from=2024-01-01&to=2024-12-31"))
        (is (= "2024-01-01" (:from @captured-filters)))
        (is (= "2024-12-31" (:to @captured-filters))))
      
      (testing "multiple filters"
        (handler (h/mock-admin-request :get "/items?status=active&type=admin"))
        (is (= "active" (:status @captured-filters)))
        (is (= "admin" (:type @captured-filters)))))))
```

## Protocol Testing Patterns

### Pattern 17: Protocol Existence

```clojure
(deftest protocol-existence-test
  (testing "protocol is defined"
    (is (some? MyProtocol)))
  
  (testing "protocol methods exist"
    (is (fn? my-method))
    (is (fn? other-method))))
```

### Pattern 18: Protocol Implementation

```clojure
(deftest protocol-implementation-test
  (let [impl (reify MyProtocol
               (my-method [this arg] {:result arg}))]
    (is (satisfies? MyProtocol impl))
    (is (= {:result 42} (my-method impl 42)))))
```

## Async Testing Patterns

### Pattern 19: Future Results

```clojure
(deftest async-operation-test
  (with-redefs [service/async-process 
                (fn [data]
                  (future {:processed data}))]
    (let [result @(service/async-process {:value 42})]
      (is (= {:processed {:value 42}} result)))))
```

## Negative Testing Patterns

### Pattern 20: Exception Handling

```clojure
(deftest exception-handling-test
  (testing "database error"
    (with-redefs [service/get-data 
                  (fn [_] (throw (ex-info "DB connection failed" {:type :db-error})))]
      (let [response (handler (h/mock-admin-request :get "/data"))]
        (is (= 500 (:status response))))))
  
  (testing "validation error"
    (with-redefs [service/validate 
                  (fn [_] (throw (ex-info "Invalid" {:type :validation})))]
      (let [response (handler (h/mock-admin-request :post "/data" {}))]
        (is (= 400 (:status response)))))))
```

### Pattern 21: Edge Cases

```clojure
(deftest edge-cases-test
  (testing "empty result"
    (with-redefs [service/list-all (constantly [])]
      (let [body (h/parse-response-body (handler request))]
        (is (empty? (:data body))))))
  
  (testing "null values"
    (with-redefs [service/find-by-id (constantly {:id 1 :name nil})]
      (let [body (h/parse-response-body (handler request))]
        (is (nil? (:name body))))))
  
  (testing "special characters"
    (with-redefs [service/create! identity]
      (let [response (handler (h/mock-admin-request :post "/items" 
                               {:name "Test <script>alert('xss')</script>"}))]
        (is (= 200 (:status response)))))))
```
