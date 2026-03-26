(ns app.domain.backend.expenses.routes.articles-routes-test
	(:require
		[app.domain.backend.expenses.routes.articles :as articles-routes]
		[app.domain.backend.expenses.routes.routes-factory :as factory]
		[app.domain.backend.expenses.services.article-aliases :as aliases]
		[app.template.backend.routes.admin.utils :as utils]
		[clojure.test :refer [deftest is testing]]))

(defn- unmapped-aliases-handler
	[]
	(->> (rest (articles-routes/routes :mock-db))
		(some (fn [route]
						(when (= "/unmapped-aliases" (first route))
							(get-in route [1 :get]))))))

(deftest unmapped-aliases-admin-route-includes-pagination-envelope
	(testing "admin unmapped aliases route returns rows plus total/limit/offset"
		(let [list-opts (atom nil)
					count-opts (atom nil)
					handler (unmapped-aliases-handler)]
			(with-redefs [aliases/list-unmapped-aliases (fn [_db opts]
																										(reset! list-opts opts)
																										[{:id "alias-1"}])
										aliases/count-unmapped-aliases (fn [_db opts]
																										 (reset! count-opts opts)
																										 193)
										factory/to-app identity
										utils/success-response (fn [body & [_status]]
																						 {:status 200
																							:body body})]
				(let [response (handler {:query-params {"limit" "25"
																								"offset" "50"}})]
					(is (= 200 (:status response)))
					(is (= {:unmapped-aliases [{:id "alias-1"}]
									:total 193
									:limit 25
									:offset 50}
								(:body response)))
					(is (= {:limit 25 :offset 50} @list-opts))
					(is (= @list-opts @count-opts)))))))