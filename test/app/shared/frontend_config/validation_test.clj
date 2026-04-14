(ns app.shared.frontend-config.validation-test
	(:require
		[app.shared.frontend-config.validation :as validation]
		[clojure.test :refer [deftest is testing]]))

(deftest validate-bundles-allows-frontend-only-entities-via-allowlist
	(let [schema-index {:entities #{"receipts"}
											:entity->fields {"receipts" {:raw ["id"]
																										:canonical #{"id"}}}}
				allowlist {:unmapped-aliases ["supplier_display_name"
																			"raw_label"
																			"occurrence_count"
																			"supplier_id"]
									 :tenant-members ["member_name"
																		"member_email"
																		"joined_on"]}
				bundles [{:scope :domain
									:domain "expenses"
									:paths {:entities "tmp/domain-entities.edn"
													:table-columns "tmp/domain-table-columns.edn"
													:view-options "tmp/domain-view-options.edn"}
									:data {:entities {:unmapped-aliases {:title "Unmapped aliases"}}
												 :table-columns {:unmapped-aliases {:available-columns ["supplier_display_name"
																																								"raw_label"
																																								"occurrence_count"]
																														 :default-visible-columns ["raw_label"]
																														 :filterable-columns ["supplier_display_name"]
																														 :sortable-columns ["occurrence_count"]
																														 :always-visible ["raw_label"]
																														 :column-config {:supplier_id {:type "text"}}}}
												 :view-options {:unmapped-aliases {:column-defaults {}
																													 :column-locks {}}}}}
								 {:scope :domain
									:domain "template"
									:paths {:entities "tmp/template-entities.edn"
													:view-options "tmp/template-view-options.edn"}
									:data {:entities {:tenant-members {:title "Tenant members"}}
												 :view-options {:tenant-members {:column-defaults {}
																												 :column-locks {}}}}}]
				results (validation/validate-bundles bundles schema-index allowlist)]
		(testing "synthetic frontend entities validate successfully when allowlisted"
			(is (every? :valid? results))
			(is (every? (comp empty? :errors) results))
			(is (every? (comp empty? :unknown-entities :semantic) results)))))