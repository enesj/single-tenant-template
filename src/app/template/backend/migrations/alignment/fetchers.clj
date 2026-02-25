(ns app.template.backend.migrations.alignment.fetchers
  "Facade namespace for schema alignment fetchers."
  (:require
    [app.template.backend.migrations.alignment.fetchers-base :as base]
    [app.template.backend.migrations.alignment.fetchers-extended :as extended]))

(defn fetch-tables
  [db]
  (base/fetch-tables db))

(defn fetch-columns
  [db]
  (base/fetch-columns db))

(defn fetch-indexes
  [db]
  (base/fetch-indexes db))

(defn fetch-index-definitions
  [db]
  (base/fetch-index-definitions db))

(defn compare-index-definitions
  [{:keys [expected actual]}]
  (base/compare-index-definitions {:expected expected :actual actual}))

(defn fetch-enums
  [db]
  (base/fetch-enums db))

(defn fetch-foreign-keys
  [db]
  (base/fetch-foreign-keys db))

(defn compare-foreign-keys
  [{:keys [expected actual]}]
  (base/compare-foreign-keys {:expected expected :actual actual}))

(defn compare-tables
  [{:keys [expected actual]}]
  (base/compare-tables {:expected expected :actual actual}))

(defn compare-columns
  [{:keys [expected actual]}]
  (base/compare-columns {:expected expected :actual actual}))

(defn compare-indexes
  [{:keys [expected actual]}]
  (base/compare-indexes {:expected expected :actual actual}))

(defn compare-enums
  [{:keys [expected actual]}]
  (base/compare-enums {:expected expected :actual actual}))

(defn extract-sql-object-name
  [kind sql]
  (extended/extract-sql-object-name kind sql))

(defn expected-extended-object-names
  [kind edn-map]
  (extended/expected-extended-object-names kind edn-map))

(defn expected-extended-object-definitions
  [kind edn-map]
  (extended/expected-extended-object-definitions kind edn-map))

(defn compare-extended-object-definitions
  [{:keys [expected actual]}]
  (extended/compare-extended-object-definitions {:expected expected :actual actual}))

(defn fetch-function-definitions
  [db]
  (extended/fetch-function-definitions db))

(defn fetch-trigger-definitions
  [db]
  (extended/fetch-trigger-definitions db))

(defn fetch-view-definitions
  [db]
  (extended/fetch-view-definitions db))

(defn fetch-policy-definitions
  [db]
  (extended/fetch-policy-definitions db))

(defn fetch-functions
  [db]
  (extended/fetch-functions db))

(defn fetch-triggers
  [db]
  (extended/fetch-triggers db))

(defn fetch-views
  [db]
  (extended/fetch-views db))

(defn fetch-policies
  [db]
  (extended/fetch-policies db))
