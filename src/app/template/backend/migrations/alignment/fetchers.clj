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

(defn- strip-outer-parens
  [s]
  (loop [s (str/trim (or s ""))]
    (if (and (str/starts-with? s "(")
          (str/ends-with? s ")"))
      (recur (subs s 1 (dec (count s))))
      s)))

(defn- normalize-sql-fragment
  "Best-effort normalization for SQL fragments returned by Postgres.

  This is intentionally conservative, but it does handle a common source of
  noise: implicit casts in predicates (e.g. 'password'::text).

  Normalizations:
  - collapse whitespace
  - strip outer parentheses
  - lowercase
  - strip common casts to text-like types"
  [s]
  (let [s (some-> s str str/trim)]
    (when (seq s)
      (-> s
        (str/replace #"\\s+" " ")
        (strip-outer-parens)
        (str/lower-case)
        (str/replace #"::\s*text" "")
        (str/replace #"::\s*character\s+varying" "")
        (str/replace #"::\s*varchar" "")
        (str/trim)))))

(defn- normalize-index-key
  [s]
  (-> (or s "")
    (str/replace "\"" "")
    (str/trim)
    (str/lower-case)))

(defn fetch-index-definitions
  "Fetch index definitions from the DB.

  Returns a map keyed by index name:
    {index-name {:table .. :method .. :unique? .. :keys [..] :predicate ..}}

  Notes:
  - Only considers public schema tables.
  - Excludes internal bookkeeping tables (see utils/internal-tables).
  - :keys are gathered in index column order using pg_get_indexdef(...)."
  [db]
  (let [rows
        (utils/q db
          ["SELECT t.relname AS table_name,
                   i.relname AS index_name,
                   am.amname AS method,
                   ix.indisunique AS is_unique,
                   pg_get_expr(ix.indpred, ix.indrelid) AS predicate,
                   k.n AS ord,
                   pg_get_indexdef(i.oid, k.n, true) AS key_def
            FROM pg_index ix
            JOIN pg_class i ON i.oid = ix.indexrelid
            JOIN pg_class t ON t.oid = ix.indrelid
            JOIN pg_namespace n ON n.oid = t.relnamespace
            JOIN pg_am am ON am.oid = i.relam
            JOIN generate_series(1, ix.indnatts) AS k(n) ON true
            WHERE n.nspname = 'public'
            ORDER BY t.relname, i.relname, k.n"])]
    (->> rows
      (remove #(utils/internal-tables (:table_name %)))
      (reduce
        (fn [acc {:keys [table_name index_name method is_unique predicate key_def]}]
          (let [idx (str/lower-case (or index_name ""))]
            (-> acc
              (assoc-in [idx :table] (str/lower-case (or table_name "")))
              (assoc-in [idx :method] (str/lower-case (or method "")))
              (assoc-in [idx :unique?] (boolean is_unique))
              (assoc-in [idx :predicate] predicate)
              (update-in [idx :keys] (fnil conj []) key_def))))
        {}))))

(defn compare-index-definitions
  "Compare expected vs actual index definitions.

  Input:
    expected: {index-name {:table .. :method .. :unique? .. :keys [..] :predicate ..}}
    actual:   {index-name {:table .. :method .. :unique? .. :keys [..] :predicate ..}}

  Returns:
    {:missing [index-name ...]
     :mismatched {table [{:index .. :expected .. :actual ..} ...]}}

  Notes:
  - We compare :method, :unique?, :keys (in order), and :predicate.
  - Predicate comparison is normalized best-effort; differences in redundant
    parentheses/whitespace/casing are ignored.
  - Expression indexes are supported on the DB side via pg_get_indexdef, but on
    the expected side we currently only model simple field indexes from
    models.edn's :indexes/:fields."
  [{:keys [expected actual]}]
  (let [expected (or expected {})
        actual (or actual {})
        exp-names (set (keys expected))
        act-names (set (keys actual))
        missing (sort (set/difference exp-names act-names))
        common (sort (set/intersection exp-names act-names))
        mismatched
        (reduce
          (fn [acc idx]
            (let [e (get expected idx)
                  a (get actual idx)
                  exp-keys (mapv utils/normalize-ident (or (:keys e) []))
                  act-keys (mapv normalize-index-key (or (:keys a) []))
                  exp-pred (normalize-sql-fragment (:predicate e))
                  act-pred (normalize-sql-fragment (:predicate a))
                  ok? (and (= (:table e) (:table a))
                        (= (:method e) (:method a))
                        (= (:unique? e) (:unique? a))
                        (= exp-keys act-keys)
                        (= exp-pred act-pred))]
              (if ok?
                acc
                (update-in acc [(:table e)] (fnil conj [])
                  {:index idx
                   :expected (assoc e :keys exp-keys :predicate exp-pred)
                   :actual (assoc (select-keys a [:table :method :unique? :keys :predicate])
                             :keys act-keys
                             :predicate act-pred)}))))
          {}
          common)]
    {:missing (vec missing)
     :mismatched mismatched}))

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

(defn- fk-action-code->kw
  "Convert Postgres FK action code (confdeltype/confupdtype) into a keyword.

  Postgres codes:
  - a: NO ACTION
  - r: RESTRICT
  - c: CASCADE
  - n: SET NULL
  - d: SET DEFAULT"
  [x]
  (case (some-> x str)
    "a" :no-action
    "r" :restrict
    "c" :cascade
    "n" :set-null
    "d" :set-default
    nil))

(defn fetch-foreign-keys
  "Fetch foreign key constraints from the DB.

  Returns a nested map:
    {table
      {column {:ref-table ..
               :ref-column ..
               :constraint-name ..
               :validated? ..
               :on-delete ..
               :on-update ..}}}

  Notes:
  - Only considers public schema tables.
  - Excludes internal bookkeeping tables (see utils/internal-tables).
  - Produces one entry per constrained column (multi-column FKs produce one
    row per column, preserving column order via ordinality)."
  [db]
  (let [rows
        (utils/q db
          ["SELECT c.relname AS table_name,
                   a.attname AS column_name,
                   rc.relname AS ref_table_name,
                   ra.attname AS ref_column_name,
                   con.conname AS constraint_name,
                   con.convalidated AS validated,
                   con.confdeltype AS on_delete,
                   con.confupdtype AS on_update
            FROM pg_constraint con
            JOIN pg_class c ON c.oid = con.conrelid
            JOIN pg_namespace n ON n.oid = c.relnamespace
            JOIN pg_class rc ON rc.oid = con.confrelid
            JOIN pg_namespace rn ON rn.oid = rc.relnamespace
            JOIN unnest(con.conkey) WITH ORDINALITY AS ck(attnum, ord) ON true
            JOIN pg_attribute a ON a.attrelid = c.oid AND a.attnum = ck.attnum
            JOIN unnest(con.confkey) WITH ORDINALITY AS fk(attnum, ord) ON fk.ord = ck.ord
            JOIN pg_attribute ra ON ra.attrelid = rc.oid AND ra.attnum = fk.attnum
            WHERE con.contype = 'f'
              AND n.nspname = 'public'
              AND rn.nspname = 'public'
            ORDER BY c.relname, con.conname, ck.ord"])]
    (->> rows
      (remove #(utils/internal-tables (:table_name %)))
      (reduce
        (fn [acc {:keys [table_name
                         column_name
                         ref_table_name
                         ref_column_name
                         constraint_name
                         validated
                         on_delete
                         on_update]}]
          (assoc-in acc [(str/lower-case table_name) (str/lower-case column_name)]
                    {:ref-table (str/lower-case ref_table_name)
                     :ref-column (str/lower-case ref_column_name)
                     :constraint-name (str/lower-case constraint_name)
                     :validated? (boolean validated)
                     :on-delete (fk-action-code->kw on_delete)
                     :on-update (fk-action-code->kw on_update)}))
        {}))))

(defn compare-foreign-keys
  "Compare expected vs actual foreign keys.

  Input shape:
    expected: {table {column {:ref-table .. :ref-column .. :on-delete? .. :on-update? ..}}}
    actual:   {table {column {:ref-table .. :ref-column .. :validated? .. :on-delete .. :on-update ..}}}

  Returns:
    {:missing {table [col ...]}
     :mismatched {table [{:column .. :expected .. :actual ..} ...]}
     :not-validated {table [col ...]}}

  Notes:
  - We always compare :ref-table/:ref-column.
  - We compare :on-delete/:on-update only when the expected FK specifies them
    (models.edn frequently does; omitting them keeps backwards-compatible
    behavior for older schemas/alignment assumptions)."
  [{:keys [expected actual]}]
  (let [tables (sort (set/union (set (keys expected)) (set (keys actual))))]
    (reduce
      (fn [acc t]
        (let [exp-cols (get expected t {})
              act-cols (get actual t {})
              exp-names (set (keys exp-cols))
              act-names (set (keys act-cols))
              missing (sort (set/difference exp-names act-names))
              common (sort (set/intersection exp-names act-names))
              mismatched
              (->> common
                (keep (fn [c]
                        (let [e (get exp-cols c)
                              a (get act-cols c)
                              ref-ok? (and (= (:ref-table e) (:ref-table a))
                                        (= (:ref-column e) (:ref-column a)))
                              on-delete-ok? (or (nil? (:on-delete e))
                                              (= (:on-delete e) (:on-delete a)))
                              on-update-ok? (or (nil? (:on-update e))
                                              (= (:on-update e) (:on-update a)))
                              ok? (and ref-ok? on-delete-ok? on-update-ok?)]
                          (when-not ok?
                            {:column c
                             :expected e
                             :actual (select-keys a
                                       [:ref-table
                                        :ref-column
                                        :on-delete
                                        :on-update
                                        :constraint-name
                                        :validated?])}))))
                (vec))
              not-validated
              (->> common
                (filter (fn [c]
                          (false? (get-in act-cols [c :validated?] true))))
                (sort)
                (vec))]
          (cond-> acc
            (seq missing) (assoc-in [:missing t] missing)
            (seq mismatched) (assoc-in [:mismatched t] mismatched)
            (seq not-validated) (assoc-in [:not-validated t] not-validated))))
      {:missing {} :mismatched {} :not-validated {}}
      tables)))

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
