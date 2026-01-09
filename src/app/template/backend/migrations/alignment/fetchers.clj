(ns app.template.backend.migrations.alignment.fetchers
  "DB fetchers and comparers for schema alignment."
  (:require
    [clojure.set :as set]
    [clojure.string :as str]
    [app.template.backend.migrations.alignment.utils :as utils]))

(defn fetch-tables
  [db]
  (->> (utils/q db ["SELECT table_name FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE' ORDER BY table_name"])
    (map :table_name)
    (remove utils/internal-tables)
    (set)))

(defn fetch-columns
  [db]
  (->> (utils/q db ["SELECT table_name, column_name, data_type, is_nullable, udt_name, character_maximum_length, numeric_precision, numeric_scale
             FROM information_schema.columns
             WHERE table_schema='public'
             ORDER BY table_name, ordinal_position"])
    (remove #(utils/internal-tables (:table_name %)))
    (reduce
      (fn [acc {:keys [table_name column_name data_type is_nullable udt_name character_maximum_length numeric_precision numeric_scale]}]
        (assoc-in acc [table_name column_name]
                  {:data-type (some-> data_type str/lower-case)
                   :is-nullable is_nullable
                   :udt-name (some-> udt_name str/lower-case)
                   :char-max character_maximum_length
                   :numeric-precision numeric_precision
                   :numeric-scale numeric_scale}))
      {})))

(defn fetch-indexes
  [db]
  (->> (utils/q db ["SELECT tablename, indexname FROM pg_indexes WHERE schemaname='public' ORDER BY tablename, indexname"])
    (remove #(utils/internal-tables (:tablename %)))
    (map :indexname)
    (map str/lower-case)
    (set)))

(defn fetch-enums
  [db]
  (let [rows (utils/q db ["SELECT t.typname AS type_name, e.enumlabel AS value
                   FROM pg_type t
                   JOIN pg_enum e ON t.oid = e.enumtypid
                   JOIN pg_namespace n ON n.oid = t.typnamespace
                   WHERE n.nspname='public'
                   ORDER BY t.typname, e.enumsortorder"])]
    (->> rows
      (group-by :type_name)
      (reduce-kv (fn [acc type-name xs]
                   (assoc acc (str/lower-case type-name)
                     (mapv :value xs)))
        {}))))

(defn compare-tables
  [{:keys [expected actual]}]
  {:missing (sort (set/difference expected actual))
   :extra (sort (set/difference actual expected))})

(defn compare-columns
  "Compare expected vs actual columns.

  Returns a map:
  {:missing {table [col...]}
   :extra {table [col...]}
   :mismatched {table [{:column .. :expected .. :actual ..} ...]}}"
  [{:keys [expected actual]}]
  (let [tables (sort (set/union (set (keys expected)) (set (keys actual))))]
    (reduce
      (fn [acc t]
        (let [exp-cols (get expected t {})
              act-cols (get actual t {})
              exp-names (set (keys exp-cols))
              act-names (set (keys act-cols))
              missing (sort (set/difference exp-names act-names))
              extra (sort (set/difference act-names exp-names))
              common (sort (set/intersection exp-names act-names))
              mismatched
              (->> common
                (keep (fn [c]
                        (let [e (get exp-cols c)
                              a (get act-cols c)
                              ok?
                              (case (:type-kind e)
                                :enum
                                (and (= (:is-nullable e) (:is-nullable a))
                                  (= (:udt-name e) (:udt-name a)))

                                :scalar
                                (and (= (:is-nullable e) (:is-nullable a))
                                  (= (:data-type e) (:data-type a))
                                  (or (nil? (:char-max e)) (= (:char-max e) (:char-max a)))
                                  (or (nil? (:numeric-precision e)) (= (:numeric-precision e) (:numeric-precision a)))
                                  (or (nil? (:numeric-scale e)) (= (:numeric-scale e) (:numeric-scale a))))

                                (= (:is-nullable e) (:is-nullable a)))]
                          (when-not ok?
                            {:column c :expected e :actual a}))))
                (vec))]
          (cond-> acc
            (seq missing) (assoc-in [:missing t] missing)
            (seq extra) (assoc-in [:extra t] extra)
            (seq mismatched) (assoc-in [:mismatched t] mismatched))))
      {:missing {} :extra {} :mismatched {}}
      tables)))

(defn compare-indexes
  [{:keys [expected actual]}]
  {:missing (sort (set/difference expected actual))})

(defn compare-enums
  [{:keys [expected actual]}]
  (let [expected-types (set (keys expected))
        actual-types (set (keys actual))
        missing-types (sort (set/difference expected-types actual-types))
        mismatched
        (->> (set/intersection expected-types actual-types)
          (keep (fn [t]
                  (let [exp (get expected t)
                        act (get actual t)
                        missing (seq (set/difference (set exp) (set act)))
                        extra (seq (set/difference (set act) (set exp)))]
                    (when (or missing extra)
                      {:type t
                       :missing-values (sort (or missing []))
                       :extra-values (sort (or extra []))}))))
          (sort-by :type)
          (vec))]
    {:missing-types missing-types
     :mismatched mismatched}))

(def ^:private re-create-function
  (re-pattern
    (str "(?is)\\bcreate\\s+(?:or\\s+replace\\s+)?"
      "function\\s+(?:public\\.)?([a-zA-Z0-9_]+)\\s*\\(")))

(def ^:private re-create-trigger
  (re-pattern "(?is)\\bcreate\\s+trigger\\s+([a-zA-Z0-9_]+)\\b"))

(def ^:private re-create-policy
  (re-pattern "(?is)\\bcreate\\s+policy\\s+([a-zA-Z0-9_]+)\\b"))

(def ^:private re-create-view
  (re-pattern
    (str "(?is)\\bcreate\\s+(?:or\\s+replace\\s+)?"
      "view\\s+(?:public\\.)?([a-zA-Z0-9_]+)\\b")))

(defn extract-sql-object-name
  "Extract an object name from an :up SQL string.

  kind: one of :function :trigger :policy :view

  Returns a normalized name string or nil if it can't parse."
  [kind sql]
  (let [sql (or sql "")
        re (case kind
             :function re-create-function
             :trigger re-create-trigger
             :policy re-create-policy
             :view re-create-view)]
    (when-let [[_ n] (re-find re sql)]
      (str/lower-case n))))

(defn expected-extended-object-names
  "Return {:expected #{...} :unparseable [{:key k :up ...} ...]}"
  [kind edn-map]
  (reduce-kv
    (fn [{:keys [expected unparseable]} k v]
      (if-let [n (extract-sql-object-name kind (:up v))]
        {:expected (conj expected n)
         :unparseable unparseable}
        {:expected expected
         :unparseable (conj unparseable {:key k :up (or (:up v) "")})}))
    {:expected #{} :unparseable []}
    (or edn-map {})))

(defn fetch-functions
  [db]
  (->> (utils/q db ["SELECT p.proname AS name
             FROM pg_proc p
             JOIN pg_namespace n ON p.pronamespace = n.oid
             WHERE n.nspname = 'public' AND p.prokind = 'f'
             ORDER BY p.proname"])
    (map :name)
    (map str/lower-case)
    (set)))

(defn fetch-triggers
  [db]
  (->> (utils/q db ["SELECT t.tgname AS name
             FROM pg_trigger t
             JOIN pg_class c ON t.tgrelid = c.oid
             JOIN pg_namespace n ON c.relnamespace = n.oid
             WHERE n.nspname = 'public' AND NOT t.tgisinternal
             ORDER BY c.relname, t.tgname"])
    (map :name)
    (map str/lower-case)
    (set)))

(defn fetch-views
  [db]
  (->> (utils/q db ["SELECT table_name AS name
             FROM information_schema.views
             WHERE table_schema='public'
             ORDER BY table_name"])
    (map :name)
    (map str/lower-case)
    (set)))

(defn fetch-policies
  [db]
  (->> (utils/q db ["SELECT pol.polname AS name
             FROM pg_policy pol
             JOIN pg_class c ON pol.polrelid = c.oid
             JOIN pg_namespace n ON c.relnamespace = n.oid
             WHERE n.nspname='public'
             ORDER BY c.relname, pol.polname"])
    (map :name)
    (map str/lower-case)
    (set)))
