(ns articles.db
  "Shared helpers for the `scripts/bb/articles/*` Babashka scripts.

  IMPORTANT: These helpers are intentionally `psql`-based (shelling out) instead of
  using JDBC, because Babashka does not support all of the Java interop that
  `next.jdbc` transitively requires in this repo.

  All query helpers:
  - use `psql -X` to ignore user `~/.psqlrc` (determinism)
  - enable `ON_ERROR_STOP=1` to fail fast on SQL errors
  - return data via `row_to_json` + `json_agg` to avoid delimiter parsing issues."
  (:refer-clojure :exclude [parse-long])
  (:require
    [aero.core :as aero]
    [clojure.data.json :as json]
    [clojure.java.shell :as sh]
    [clojure.pprint :as pprint]
    [clojure.string :as str])
  (:import
    [java.text Normalizer Normalizer$Form]
    [java.util UUID]))

(defn read-config
  "Read `config/base.edn` for a given Aero profile keyword (typically `:dev` or `:test`)."
  [profile]
  (aero/read-config "config/base.edn" {:profile profile}))

(defn db-config
  "Return the `:database` map (host/port/dbname/user/password) for a profile."
  [profile]
  (:database (read-config profile)))

(defn uuid
  "Generate a random UUID string (lowercase)."
  []
  (str (UUID/randomUUID)))

(defn parse-long
  [s]
  (try
    (Long/parseLong (str s))
    (catch Exception _ nil)))

(defn parse-profile
  "Parse the first CLI arg as a profile, supporting `dev|test` and `--dev|--test`.

  Returns {:profile <kw> :args <remaining-args>}.

  Defaults to `:dev` when no explicit profile is provided."
  [args]
  (let [[a & more] args]
    (cond
      (nil? a) {:profile :dev :args []}

      (#{"dev" "test"} a)
      {:profile (if (= a "dev") :dev :test)
       :args more}

      (= a "--dev") {:profile :dev :args more}
      (= a "--test") {:profile :test :args more}

      :else {:profile :dev :args args})))

(defn- strip-diacritics
  [s]
  (let [nfd (Normalizer/normalize s Normalizer$Form/NFD)]
    (str/replace nfd #"\p{InCombiningDiacriticalMarks}+" "")))

(defn normalize-key
  "Conservatively normalize a human name into a stable key.

  Rules:
  - strip diacritics (Š → S)
  - lowercase
  - replace any run of non [a-z0-9] with a single '-'
  - trim '-' from ends

  This is intended for keys like `manufacturers.normalized_key` and
  `articles.normalized_key`."
  [s]
  (when (some? s)
    (-> s
      str
      strip-diacritics
      str/lower-case
      (str/replace #"[^a-z0-9]+" "-")
      (str/replace #"(^-+)|(-+$)" "")
      (str/replace #"-+" "-"))))

(defn sql-literal
  "Render a *value* as a SQL literal.

  This is a minimal escape helper for building deterministic scripts without JDBC
  parameterization.

  - nil -> NULL
  - strings -> single-quoted with ' escaped to ''
  - numbers -> emitted as-is
  - booleans -> TRUE/FALSE

  Do not use this for identifiers (table/column names)."
  [v]
  (cond
    (nil? v) "NULL"
    (true? v) "TRUE"
    (false? v) "FALSE"
    (number? v) (str v)
    (keyword? v) (sql-literal (name v))
    :else (str "'" (str/replace (str v) "'" "''") "'")))

(defn sql-in-list
  "Render a seq of values into an IN list: (v1, v2, ...)."
  [xs]
  (str "(" (str/join ", " (map sql-literal xs)) ")"))

(defn- run-psql
  [{:keys [host port dbname user password] :as db} sql]
  (when-not (and host port dbname user)
    (throw (ex-info "Invalid db config (expected :host :port :dbname :user)" {:db (dissoc db :password)})))
  (let [{:keys [out err exit]}
        (sh/sh "psql"
          "-X"
          "-v" "ON_ERROR_STOP=1"
          "-h" host
          "-p" (str port)
          "-U" user
          "-d" dbname
          "-t" "-A"
          "-c" sql
          :env (merge (into {} (System/getenv))
                 (when password {"PGPASSWORD" password})))]
    (when (not= 0 exit)
      (throw (ex-info "psql command failed" {:exit exit :err (str/trim err)})))
    out))

(defn query
  "Run SQL (a SELECT or a DML with RETURNING) and return a vector of maps.

  The SQL should NOT end with a semicolon.
  DML statements (INSERT/UPDATE/DELETE) are wrapped in a CTE;
  plain SELECTs are wrapped in a FROM subquery."
  [db sql]
  (let [dml? (boolean (re-find #"(?i)^\s*(INSERT|UPDATE|DELETE)\b" sql))
        wrapper (if dml?
                  (str
                    "WITH t AS (\n" sql "\n)\n"
                    "SELECT COALESCE(json_agg(row_to_json(t)), '[]'::json) FROM t")
                  (str
                    "SELECT COALESCE(json_agg(row_to_json(t)), '[]'::json)\n"
                    "FROM (\n" sql "\n) t"))
        out (run-psql db wrapper)
        json-str (str/trim out)]
    (when (str/blank? json-str)
      (throw (ex-info "psql returned empty output (unexpected)" {:sql (subs wrapper 0 (min 200 (count wrapper)))})))
    (vec (json/read-str json-str :key-fn keyword))))

(defn query1
  "Like `query`, but return the first row (or nil)."
  [db sql]
  (first (query db sql)))

(defn pprint-edn
  "Pretty-print EDN to stdout."
  [x]
  (pprint/pprint x)
  (flush))

(defn prn-edn
  "Print EDN to stdout as a single line."
  [x]
  (prn x)
  (flush))
