(ns automigrate.util.sql-validation
  "SQL syntax validation and auto-fixing utilities."
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as str]))

(def common-sql-issues
  "Common SQL syntax issues and their patterns"
  {:jsonb-array-syntax
   {:pattern #"(\w+)\s*=\s*ANY\s*\(\s*([^)]*jsonb[^)]*)\)"
    :description "Invalid JSONB array syntax. Use @> operator instead of ANY()"
    :fix-fn (fn [match]
              (let [[_ field jsonb-expr] match]
                (str jsonb-expr " @> to_jsonb(" field "::text)")))}

   :unquoted-dotted-identifiers
   {:pattern #"(?<!\")\b([a-zA-Z_][a-zA-Z0-9_]*\.[a-zA-Z0-9_]*\.[0-9]+)\b(?!\")"
    :description "Identifier with dots should be quoted"
    :fix-fn (fn [match]
              (let [[_ identifier] match]
                (str "\"" identifier "\"")))}

   :missing-space-in-function-calls
   {:pattern #"(\w+)\(\s*([^)]+)\s*\)\s*=\s*ANY\s*\("
    :description "Function call in ANY expression may need adjustment"
    :fix-fn (fn [match]
              (let [[full-match func-name args] match]
                ;; Keep the original for now, more complex logic needed
                full-match))}

   :malformed-drop-statements
   {:pattern #"DROP\s+FUNCTION\s+IF\s+EXISTS\s+(\w+)\(\s*,\s*([^)]*)\);"
    :description "Malformed DROP FUNCTION statement with leading comma"
    :fix-fn (fn [match]
              (let [[_ func-name args] match]
                (str "DROP FUNCTION IF EXISTS " func-name "(" args ");")))}})

(defn validate-sql-syntax
  "Validate SQL syntax and detect common issues"
  [sql-content]
  (let [issues (atom [])]
    ;; Check for each common issue
    (doseq [[issue-key issue-config] common-sql-issues]
      (let [pattern (:pattern issue-config)
            description (:description issue-config)
            matches (re-seq pattern sql-content)]
        (when (seq matches)
          (swap! issues conj {:type issue-key
                              :description description
                              :matches matches
                              :count (count matches)}))))

    ;; Additional specific validations
    (when (and (str/includes? sql-content "JSONB")
            (str/includes? sql-content "ANY("))
      (swap! issues conj {:type :jsonb-any-operator
                          :description "JSONB fields should use @> operator instead of ANY()"
                          :severity :error}))

    (when (re-find #"shared_with_tenants.*ANY" sql-content)
      (swap! issues conj {:type :jsonb-tenant-sharing
                          :description "shared_with_tenants is JSONB, use @> operator"
                          :severity :error}))

    @issues))

(defn fix-common-sql-issues
  "Auto-fix common SQL syntax issues"
  [sql-content]
  (reduce
    (fn [sql [issue-key issue-config]]
      (let [pattern (:pattern issue-config)
            fix-fn (:fix-fn issue-config)]
        (str/replace sql pattern
          (fn [match]
            (try
              (fix-fn (if (string? match) [match] match))
              (catch Exception e
                          ;; If fix fails, return original
                (if (string? match) match (first match))))))))
    sql-content
    common-sql-issues))

(defn fix-jsonb-array-syntax
  "Specifically fix JSONB array syntax issues"
  [sql-content]
  (-> sql-content
      ;; Fix tenant sharing JSONB syntax
    (str/replace #"current_tenant_id\(\)\s*=\s*ANY\s*\(\s*shared_with_tenants\s*\)"
      "shared_with_tenants @> to_jsonb(current_tenant_id()::text)")
      ;; Fix general JSONB array syntax
    (str/replace #"(\w+)\s*=\s*ANY\s*\(\s*([^)]*jsonb[^)]*)\)"
      "$2 @> to_jsonb($1::text)")))

(defn fix-quoted-identifiers
  "Fix unquoted identifiers that contain dots"
  [sql-content]
  (str/replace sql-content
    #"(?<!\")\b([a-zA-Z_][a-zA-Z0-9_]*\.[a-zA-Z0-9_]*\.[0-9]+)\b(?!\")"
    "\"$1\""))

(defn fix-drop-function-syntax
  "Fix malformed DROP FUNCTION statements"
  [sql-content]
  (str/replace sql-content
    #"DROP\s+FUNCTION\s+IF\s+EXISTS\s+(\w+)\(\s*,\s*([^)]*)\);"
    "DROP FUNCTION IF EXISTS $1($2);"))

(defn comprehensive-sql-fix
  "Apply all common SQL fixes"
  [sql-content]
  (-> sql-content
    fix-jsonb-array-syntax
    fix-quoted-identifiers
    fix-drop-function-syntax
    fix-common-sql-issues))

(defn validate-and-fix-sql
  "Validate SQL and optionally auto-fix issues"
  [sql-content & {:keys [auto-fix? verbose?] :or {auto-fix? true verbose? false}}]
  (let [original-issues (validate-sql-syntax sql-content)
        fixed-sql (if auto-fix? (comprehensive-sql-fix sql-content) sql-content)
        final-issues (if auto-fix? (validate-sql-syntax fixed-sql) original-issues)]

    (when verbose?
      (when (seq original-issues)
        (println "🔍 SQL validation issues found:")
        (doseq [issue original-issues]
          (println (str "  ⚠️  " (:description issue) " (" (:count issue 1) " occurrences)"))))

      (when (and auto-fix? (< (count final-issues) (count original-issues)))
        (println (str "✅ Auto-fixed " (- (count original-issues) (count final-issues)) " issues"))))

    {:sql fixed-sql
     :original-issues original-issues
     :final-issues final-issues
     :fixed-count (- (count original-issues) (count final-issues))
     :has-errors? (boolean (seq final-issues))}))

;; SQL validation specs
(s/def ::sql-content string?)
(s/def ::issue-type keyword?)
(s/def ::description string?)
(s/def ::severity #{:error :warning :info})
(s/def ::count pos-int?)

(s/def ::sql-issue
  (s/keys :req-un [::issue-type ::description]
    :opt-un [::severity ::count]))

(s/def ::validation-result
  (s/keys :req-un [::sql-content]
    :opt-un [::original-issues ::final-issues ::fixed-count ::has-errors?]))

;; Validation predicates for common patterns
(defn has-jsonb-any-issue?
  "Check if SQL has JSONB ANY() syntax issues"
  [sql-content]
  (and (str/includes? sql-content "jsonb")
    (re-find #"=\s*ANY\s*\(" sql-content)))

(defn has-unquoted-dotted-identifiers?
  "Check if SQL has unquoted dotted identifiers"
  [sql-content]
  (re-find #"(?<!\")\b[a-zA-Z_][a-zA-Z0-9_]*\.[a-zA-Z0-9_]*\.[0-9]+\b(?!\")" sql-content))

(defn has-malformed-drop-statements?
  "Check if SQL has malformed DROP statements"
  [sql-content]
  (re-find #"DROP\s+FUNCTION.*\(\s*," sql-content))

;; Migration-specific validation
(defn validate-function-migration
  "Validate function migration syntax"
  [migration-content]
  (let [issues (atom [])]

    ;; Check for C language functions that might be extension functions
    (when (and (str/includes? migration-content "LANGUAGE c")
            (or (str/includes? migration-content "$libdir/uuid-ossp")
              (str/includes? migration-content "$libdir/hstore")))
      (swap! issues conj {:type :extension-function
                          :description "Function appears to be provided by PostgreSQL extension"
                          :severity :warning}))

    ;; Check for proper FORWARD/BACKWARD sections
    (when-not (and (str/includes? migration-content "-- FORWARD")
                (str/includes? migration-content "-- BACKWARD"))
      (swap! issues conj {:type :missing-sections
                          :description "Migration should have FORWARD and BACKWARD sections"
                          :severity :error}))

    @issues))

(defn validate-policy-migration
  "Validate policy migration syntax"
  [migration-content]
  (let [issues (atom [])]

    ;; Check for JSONB tenant sharing issues
    (when (re-find #"shared_with_tenants.*ANY" migration-content)
      (swap! issues conj {:type :jsonb-policy-syntax
                          :description "Policy uses incorrect JSONB syntax for tenant sharing"
                          :severity :error}))

    ;; Check for proper RLS policy structure
    (when-not (re-find #"CREATE POLICY.*ON.*USING" migration-content)
      (swap! issues conj {:type :invalid-policy-structure
                          :description "Policy migration should contain CREATE POLICY statement"
                          :severity :error}))

    @issues))

(defn validate-view-migration
  "Validate view migration syntax"
  [migration-content]
  (let [issues (atom [])]

    ;; Check for unquoted view names with dots
    (when (re-find #"DROP VIEW.*[a-zA-Z_][a-zA-Z0-9_]*\.[a-zA-Z0-9_]*\.[0-9]+" migration-content)
      (swap! issues conj {:type :unquoted-view-name
                          :description "View name with dots should be quoted"
                          :severity :error}))

    @issues))

(defn validate-migration-by-type
  "Validate migration based on its type"
  [migration-content file-extension]
  (case file-extension
    ".fn" (validate-function-migration migration-content)
    ".pol" (validate-policy-migration migration-content)
    ".view" (validate-view-migration migration-content)
    ".sql" (validate-sql-syntax migration-content)
    []))
