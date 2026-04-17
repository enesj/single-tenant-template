(ns app.template.backend.migrations.alignment.schema-test
  (:require
    [app.template.backend.migrations.alignment.schema :as schema]
    [app.template.backend.migrations.alignment.fetchers :as fetchers]
    [clojure.string :as str]
    [clojure.test :refer [deftest is testing]]))

(deftest models->expected-includes-foreign-keys-test
  (testing "models->expected extracts expected FK references from models"
    (let [models {:article_aliases
                  {:fields [[:id :uuid {:primary-key true}]
                            [:supplier_id
                             :uuid
                             {:foreign-key :suppliers/id
                              :on-delete :cascade}]
                            [:article_id :uuid {:foreign-key :articles/id}]
                            [:raw_label_normalized [:varchar 255] {:null false}]]}
                  :suppliers {:fields [[:id :uuid {:primary-key true}]]}
                  :articles {:fields [[:id :uuid {:primary-key true}]]}}
          expected (schema/models->expected models)]
      (is (= {"article_aliases" {"supplier_id" {:ref-table "suppliers"
                                                :ref-column "id"
                                                :on-delete :cascade}
                                 "article_id" {:ref-table "articles" :ref-column "id"}}}
            (:foreign-keys expected))))))

(deftest compare-foreign-keys-test
  (testing "missing FK columns are detected"
    (let [expected {"article_aliases" {"supplier_id" {:ref-table "suppliers" :ref-column "id"}
                                       "article_id" {:ref-table "articles" :ref-column "id"}}}
          actual {"article_aliases" {"article_id" {:ref-table "articles" :ref-column "id" :validated? true}}}
          diff (fetchers/compare-foreign-keys {:expected expected :actual actual})]
      (is (= {"article_aliases" ["supplier_id"]}
            (:missing diff)))))

  (testing "mismatched references are detected"
    (let [expected {"x" {"a_id" {:ref-table "a" :ref-column "id"}}}
          actual {"x" {"a_id" {:ref-table "b" :ref-column "id" :validated? true :constraint-name "x_a_id_fkey"}}}
          diff (fetchers/compare-foreign-keys {:expected expected :actual actual})]
      (is (= {"x" [{:column "a_id"
                    :expected {:ref-table "a" :ref-column "id"}
                    :actual {:ref-table "b" :ref-column "id" :constraint-name "x_a_id_fkey" :validated? true}}]}
            (:mismatched diff)))))

  (testing "mismatched on-delete is detected when expected specifies it"
    (let [expected {"x" {"a_id" {:ref-table "a" :ref-column "id" :on-delete :cascade}}}
          actual {"x" {"a_id" {:ref-table "a"
                               :ref-column "id"
                               :on-delete :restrict
                               :validated? true
                               :constraint-name "x_a_id_fkey"}}}
          diff (fetchers/compare-foreign-keys {:expected expected :actual actual})]
      (is (= {"x" [{:column "a_id"
                    :expected {:ref-table "a" :ref-column "id" :on-delete :cascade}
                    :actual {:ref-table "a"
                             :ref-column "id"
                             :on-delete :restrict
                             :constraint-name "x_a_id_fkey"
                             :validated? true}}]}
            (:mismatched diff)))))

  (testing "not-validated constraints are reported"
    (let [expected {"x" {"a_id" {:ref-table "a" :ref-column "id"}}}
          actual {"x" {"a_id" {:ref-table "a" :ref-column "id" :validated? false :constraint-name "x_a_id_fkey"}}}
          diff (fetchers/compare-foreign-keys {:expected expected :actual actual})]
      (is (= {"x" ["a_id"]}
            (:not-validated diff))))))

(deftest models->expected-includes-index-definitions-test
  (testing "models->expected extracts index definitions (keys/unique/where)"
    (let [models {:payers
                  {:fields [[:id :uuid {:primary-key true}]
                            [:is_default :boolean {:null false}]]
                   :indexes [[:uniq_payers_default
                              :btree
                              {:fields [:is_default]
                               :unique true
                               :where [:= :is_default true]}]]}}
          expected (schema/models->expected models)
          idx (get-in expected [:index-definitions "uniq_payers_default"])]
      (is (= {:table "payers"
              :method "btree"
              :unique? true
              :keys ["is_default"]}
            (select-keys idx [:table :method :unique? :keys])))
      (is (= "is_default = true"
            (some-> (:predicate idx) str/lower-case))))))

(deftest compare-index-definitions-test
  (testing "mismatched keys are detected"
    (let [expected {"idx_x" {:table "x" :method "btree" :unique? false :keys ["a"] :predicate nil}}
          actual {"idx_x" {:table "x" :method "btree" :unique? false :keys ["b"] :predicate nil}}
          diff (fetchers/compare-index-definitions {:expected expected :actual actual})]
      (is (= 1 (count (get-in diff [:mismatched "x"]))))))

  (testing "mismatched predicate is detected"
    (let [expected {"idx_x" {:table "x" :method "btree" :unique? false :keys ["a"] :predicate "a = TRUE"}}
          actual {"idx_x" {:table "x" :method "btree" :unique? false :keys ["a"] :predicate "(a)"}}
          diff (fetchers/compare-index-definitions {:expected expected :actual actual})]
      (is (= 1 (count (get-in diff [:mismatched "x"])))))))
