(ns app.shared.frontend-config.schema-test
  (:require
    [app.shared.frontend-config.schema :as schema]
    [clojure.test :refer [deftest is testing]])
  (:import
    (java.nio.file Files Path)
    (java.nio.file.attribute FileAttribute)))

(defn- temp-dir
  ^Path
  []
  (Files/createTempDirectory "frontend-config-schema-test" (make-array FileAttribute 0)))

(defn- p
  ^String
  [^Path dir ^String rel]
  (str (.toString dir) "/" rel))

(defn- mkdirp!
  [^Path dir ^String rel]
  (Files/createDirectories (.resolve dir rel) (make-array FileAttribute 0)))

(defn- write-edn!
  [path data]
  (spit path (pr-str data) :encoding "UTF-8"))

(deftest models-index-accepts-hierarchical-directory
  (let [dir (temp-dir)
        base (.toString dir)]
    (mkdirp! dir "template")
    (mkdirp! dir "shared")
    (mkdirp! dir "domain")
    (mkdirp! dir "domain/alpha")

    ;; template → domain → shared (shared wins on conflicts)
    (write-edn! (p dir "template/models.edn")
      {:things {:fields [[:id :uuid {:primary-key true}]
                         [:template_only :text]]}})

    (write-edn! (p dir "domain/models.edn")
      {:direct_domain {:fields [[:id :uuid] [:direct_only :text]]}
       :expense_items {:fields [[:id :uuid] [:created_at :timestamptz]]}})

    (write-edn! (p dir "domain/alpha/models.edn")
      {:alpha_domain {:fields [[:id :uuid] [:alpha_only :text]]}})

    (write-edn! (p dir "shared/models.edn")
      {:things {:fields [[:id :uuid {:primary-key true}]
                         [:shared_only :text]]}})

    (testing "directory path merges hierarchical models and normalizes IDs"
      (let [idx (schema/models-index base)]
        (is (contains? (:entities idx) "things"))
        (is (contains? (:entities idx) "direct-domain"))
        (is (contains? (:entities idx) "alpha-domain"))
        (is (contains? (:entities idx) "expense-items"))

        ;; Merge order: shared wins on conflicts
        (is (= ["id" "shared_only"]
              (get-in idx [:entity->fields "things" :raw])))
        (is (= #{"id" "shared-only"}
              (get-in idx [:entity->fields "things" :canonical])))

        ;; Field name normalization: created_at is canonicalized to created-at
        (is (contains?
              (get-in idx [:entity->fields "expense-items" :canonical])
              "created-at"))))))
