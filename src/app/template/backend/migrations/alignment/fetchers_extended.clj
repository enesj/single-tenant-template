(ns app.template.backend.migrations.alignment.fetchers-extended
  "Extended object and DDL comparison helpers for schema alignment."
  (:require
    [clojure.set :as set]
    [clojure.string :as str]
    [app.template.backend.migrations.alignment.utils :as utils]))

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

(defn- normalize-ddl-sql
  [value]
  (let [string-value (some-> value str str/trim)]
    (when (seq string-value)
      (-> string-value
        (str/replace #";+$" "")
        (str/replace #"\s+" " ")
        (str/replace #"\$[a-zA-Z0-9_]*\$" "\\$\\$")
        (str/replace #"(?i)\bpublic\." "")
        (str/lower-case)
        (str/trim)))))

(defn- normalize-function-definition
  [sql]
  (let [sql (or sql "")
        body (when-let [[_ _tag extracted-body] (re-find #"(?is)\bas\s+(\$[a-zA-Z0-9_]*\$)(.*?)\1" sql)]
               (some-> extracted-body str/trim (str/replace #"\s+" " ")))
        language (some-> (re-find #"(?is)\blanguage\s+([a-zA-Z0-9_]+)" sql)
                   second
                   str/lower-case)
        returns (some-> (re-find #"(?is)\breturns\s+(.+?)\s+(?:language\b|as\b)" sql)
                  second
                  str/trim
                  str/lower-case)]
    (if (and body language returns)
      (-> (str "returns " returns
            " language " language
            " as $$ " body " $$")
        (normalize-ddl-sql))
      (normalize-ddl-sql sql))))

(defn- extract-create-view-body
  [sql]
  (when-let [[_ body] (re-find #"(?is)\bas\s+(.*)$" (or sql ""))]
    (str/trim body)))

(defn- roles->vec
  [roles]
  (cond
    (nil? roles) []
    (instance? java.sql.Array roles) (mapv str (seq (.getArray ^java.sql.Array roles)))
    (sequential? roles) (mapv str roles)
    (string? roles)
    (let [string-value (-> roles str/trim (str/replace #"^\{" "") (str/replace #"\}$" ""))]
      (if (str/blank? string-value)
        []
        (->> (str/split string-value #",")
          (mapv str/trim)
          (remove str/blank?)
          (vec))))
    :else [(str roles)]))

(defn- normalize-policy-definition
  [{:keys [permissive cmd roles qual with-check]}]
  (let [roles (->> (roles->vec roles)
                (map str/lower-case)
                (sort)
                (vec))
        cmd (some-> cmd str str/lower-case)
        qual (some-> qual str)
        with-check (some-> with-check str)]
    (normalize-ddl-sql
      (str "permissive=" (boolean permissive)
        " cmd=" (or cmd "")
        " roles=" (pr-str roles)
        " using=" (or qual "")
        " with-check=" (or with-check "")))))

(defn- normalize-arglist
  [s]
  (-> (or s "")
    (str/trim)
    (str/replace #"\s+" " ")
    (str/lower-case)
    (str/trim)))

(def ^:private re-create-function+args
  (re-pattern
    (str "(?is)\\bcreate\\s+(?:or\\s+replace\\s+)?"
      "function\\s+(?:public\\.)?([a-zA-Z0-9_]+)\\s*\\(([^)]*)\\)")))

(def ^:private re-create-trigger+table
  (re-pattern
    "(?is)\\bcreate\\s+trigger\\s+([a-zA-Z0-9_]+)\\b.*?\\bon\\s+(?:only\\s+)?(?:public\\.)?([a-zA-Z0-9_]+)\\b"))

(def ^:private re-create-policy+table
  (re-pattern
    "(?is)\\bcreate\\s+policy\\s+([a-zA-Z0-9_]+)\\b\\s+on\\s+(?:public\\.)?([a-zA-Z0-9_]+)\\b"))

(def ^:private re-create-view+name
  (re-pattern
    (str "(?is)\\bcreate\\s+(?:or\\s+replace\\s+)?"
      "view\\s+(?:public\\.)?([a-zA-Z0-9_]+)\\b")))

(defn- extract-expected-extended-object
  [kind sql]
  (let [sql (or sql "")]
    (case kind
      :function
      (when-let [[_ fname args] (re-find re-create-function+args sql)]
        (let [fname (str/lower-case fname)
              args (normalize-arglist args)
              id (str fname "(" args ")")]
          {:id id
           :name fname
           :identity-args args
           :definition-normalized (normalize-function-definition sql)}))

      :trigger
      (when-let [[_ tname table] (re-find re-create-trigger+table sql)]
        (let [tname (str/lower-case tname)
              table (str/lower-case table)
              id (str table "." tname)]
          {:id id
           :name tname
           :table table
           :definition-normalized (normalize-ddl-sql sql)}))

      :policy
      (when-let [[_ pname table] (re-find re-create-policy+table sql)]
        (let [pname (str/lower-case pname)
              table (str/lower-case table)
              id (str table "." pname)]
          {:id id
           :name pname
           :table table
           :definition-normalized
           (let [s (str/lower-case sql)
                 permissive (not (boolean (re-find #"(?is)\bas\s+restrictive\b" s)))
                 cmd (or (some-> (re-find #"(?is)\bfor\s+(all|select|insert|update|delete)\b" s) second)
                       "all")
                 roles (or (some-> (re-find #"(?is)\bto\s+(.+?)(?:\s+using\b|\s+with\s+check\b|$)" s) second)
                         "public")
                 qual (some-> (re-find #"(?is)\busing\s*\((.*?)\)" sql) second)
                 with-check (some-> (re-find #"(?is)\bwith\s+check\s*\((.*?)\)" sql) second)]
             (normalize-policy-definition {:permissive permissive
                                           :cmd cmd
                                           :roles roles
                                           :qual qual
                                           :with-check with-check}))}))

      :view
      (when-let [[_ vname] (re-find re-create-view+name sql)]
        (let [vname (str/lower-case vname)]
          {:id vname
           :name vname
           :definition-normalized (or (some-> (extract-create-view-body sql) normalize-ddl-sql)
                                   (normalize-ddl-sql sql))}))

      nil)))

(defn expected-extended-object-definitions
  [kind edn-map]
  (reduce-kv
    (fn [{:keys [expected unparseable]} k v]
      (let [up (:up v)]
        (if-let [obj (extract-expected-extended-object kind up)]
          {:expected (assoc expected (:id obj) (assoc obj :source-key k :up (or up "")))
           :unparseable unparseable}
          {:expected expected
           :unparseable (conj unparseable {:key k :up (or up "")})})))
    {:expected {} :unparseable []}
    (or edn-map {})))

(defn compare-extended-object-definitions
  [{:keys [expected actual]}]
  (let [expected (or expected {})
        actual (or actual {})
        exp-ids (set (keys expected))
        act-ids (set (keys actual))
        missing (sort (set/difference exp-ids act-ids))
        extra (sort (set/difference act-ids exp-ids))
        common (sort (set/intersection exp-ids act-ids))
        mismatched
        (->> common
          (keep (fn [id]
                  (let [e (get expected id)
                        a (get actual id)
                        e* (:definition-normalized e)
                        a* (:definition-normalized a)
                        ok? (= e* a*)]
                    (when-not ok?
                      {:id id
                       :expected e*
                       :actual a*
                       :expected-source-key (:source-key e)}))))
          (vec))]
    {:missing (vec missing)
     :extra (vec extra)
     :mismatched mismatched}))

(defn fetch-function-definitions
  [db]
  (->> (utils/q db
         ["SELECT p.oid AS oid,
                  p.proname AS name,
                  pg_get_function_identity_arguments(p.oid) AS identity_args,
                  pg_get_functiondef(p.oid) AS definition,
                  e.extname AS extension
           FROM pg_proc p
           JOIN pg_namespace n ON p.pronamespace = n.oid
           LEFT JOIN pg_depend d
             ON d.classid = 'pg_proc'::regclass
            AND d.objid = p.oid
            AND d.refclassid = 'pg_extension'::regclass
            AND d.deptype = 'e'
           LEFT JOIN pg_extension e ON e.oid = d.refobjid
           WHERE n.nspname = 'public'
             AND p.prokind = 'f'
             AND e.extname IS NULL
           ORDER BY p.proname"])
    (reduce
      (fn [acc {:keys [name identity_args definition]}]
        (let [fname (str/lower-case (or name ""))
              args (normalize-arglist identity_args)
              id (str fname "(" args ")")]
          (assoc acc id
            {:id id
             :name fname
             :identity-args args
             :definition (or definition "")
             :definition-normalized (normalize-function-definition definition)})))
      {})))

(defn fetch-trigger-definitions
  [db]
  (->> (utils/q db
         ["SELECT c.relname AS table_name,
                  t.tgname AS trigger_name,
                  pg_get_triggerdef(t.oid, true) AS definition
           FROM pg_trigger t
           JOIN pg_class c ON t.tgrelid = c.oid
           JOIN pg_namespace n ON c.relnamespace = n.oid
           WHERE n.nspname = 'public' AND NOT t.tgisinternal
           ORDER BY c.relname, t.tgname"])
    (remove #(utils/internal-tables (:table_name %)))
    (reduce
      (fn [acc {:keys [table_name trigger_name definition]}]
        (let [table (str/lower-case (or table_name ""))
              tname (str/lower-case (or trigger_name ""))
              id (str table "." tname)]
          (assoc acc id
            {:id id
             :table table
             :name tname
             :definition (or definition "")
             :definition-normalized (normalize-ddl-sql definition)})))
      {})))

(defn fetch-view-definitions
  [db]
  (->> (utils/q db
         ["SELECT c.relname AS view_name,
                  pg_get_viewdef(c.oid, true) AS definition
           FROM pg_class c
           JOIN pg_namespace n ON c.relnamespace = n.oid
           WHERE n.nspname = 'public' AND c.relkind = 'v'
           ORDER BY c.relname"])
    (reduce
      (fn [acc {:keys [view_name definition]}]
        (let [vname (str/lower-case (or view_name ""))
              body (or definition "")
              normalized (normalize-ddl-sql body)]
          (assoc acc vname
            {:id vname
             :name vname
             :definition body
             :definition-normalized normalized})))
      {})))

(defn fetch-policy-definitions
  [db]
  (->> (utils/q db
         ["SELECT tablename AS table_name,
          policyname AS policy_name,
          permissive,
          roles,
          cmd,
          qual,
          with_check
        FROM pg_policies
        WHERE schemaname = 'public'
        ORDER BY tablename, policyname"])
    (remove #(utils/internal-tables (:table_name %)))
    (reduce
      (fn [acc {:keys [table_name policy_name permissive roles cmd qual with_check]}]
        (let [table (str/lower-case (or table_name ""))
              pname (str/lower-case (or policy_name ""))
              id (str table "." pname)
              normalized (normalize-policy-definition {:permissive permissive
                                                      :cmd cmd
                                                      :roles roles
                                                      :qual qual
                                                      :with-check with_check})
              repr (str "permissive=" (boolean permissive)
                     " cmd=" (some-> cmd str/lower-case)
                     " roles=" (pr-str (roles->vec roles))
                     " using=" (str qual)
                     " with-check=" (str with_check))]
          (assoc acc id
            {:id id
             :table table
             :name pname
             :definition repr
             :definition-normalized normalized})))
      {})))

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
