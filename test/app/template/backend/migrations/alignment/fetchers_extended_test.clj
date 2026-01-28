(ns app.template.backend.migrations.alignment.fetchers-extended-test
  (:require
    [app.template.backend.migrations.alignment.fetchers :as fetchers]
    [clojure.test :refer [deftest is testing]]))

(deftest expected-extended-object-definitions-parses-trigger-table
  (testing "expected triggers are keyed by table.trigger-name"
    (let [edn {:users-updated-at-trigger
               {:up "CREATE TRIGGER users_updated_at\n       BEFORE UPDATE ON users\n       FOR EACH ROW\n       EXECUTE FUNCTION update_updated_at_column();"}}
          res (fetchers/expected-extended-object-definitions :trigger edn)]
      (is (contains? (:expected res) "users.users_updated_at"))
      (is (= "users" (get-in res [:expected "users.users_updated_at" :table])))
      (is (= "users_updated_at" (get-in res [:expected "users.users_updated_at" :name]))))))

(deftest expected-extended-object-definitions-parses-function-args
  (testing "expected functions are keyed by name(identity-args)"
    (let [edn {:fn
               {:up "CREATE OR REPLACE FUNCTION foo(a uuid, b text) RETURNS void LANGUAGE sql AS $$ SELECT 1; $$;"}}
          res (fetchers/expected-extended-object-definitions :function edn)]
      (is (contains? (:expected res) "foo(a uuid, b text)"))
      (is (= "foo" (get-in res [:expected "foo(a uuid, b text)" :name])))
      (is (= "a uuid, b text" (get-in res [:expected "foo(a uuid, b text)" :identity-args]))))))

(deftest compare-extended-object-definitions-detects-missing-extra-mismatch
  (testing "missing and extra ids are detected"
    (let [expected {"a()" {:definition-normalized "x" :source-key :a}}
          actual {"b()" {:definition-normalized "y"}}
          diff (fetchers/compare-extended-object-definitions {:expected expected :actual actual})]
      (is (= ["a()"] (:missing diff)))
      (is (= ["b()"] (:extra diff)))))

  (testing "definition drift is detected for shared ids"
    (let [expected {"a()" {:definition-normalized "create function a()" :source-key :a}}
          actual {"a()" {:definition-normalized "create function a() returns int"}}
          diff (fetchers/compare-extended-object-definitions {:expected expected :actual actual})]
      (is (= [{:id "a()"
               :expected "create function a()"
               :actual "create function a() returns int"
               :expected-source-key :a}]
            (:mismatched diff))))))
