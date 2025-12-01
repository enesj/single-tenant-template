(ns automigrate.templates
  "Enhanced migration file generation templates."
  (:require
   [automigrate.util.extensions :as extensions]
   [clojure.string :as str]))

;; Template configuration
(def template-config
  {:function {:use-extension true
              :include-comments true
              :validate-syntax true}
   :policy {:fix-jsonb-syntax true
            :include-examples true}
   :view {:quote-identifiers true
          :include-recreation-sql false}
   :trigger {:include-table-reference true}})

(defn extract-function-signature
  "Extract function signature from function definition"
  [function-body]
  (let [create-match (re-find #"CREATE\s+(?:OR\s+REPLACE\s+)?FUNCTION\s+([^(]+\([^)]*\))" function-body)]
    (if create-match
      (second create-match)
      "unknown_function()")))

(defn comment-out-sql
  "Comment out SQL for template use"
  [sql]
  (->> (str/split-lines sql)
    (map #(str "-- " %))
    (str/join "\n")))


(defn fix-policy-jsonb-syntax
  "Fix common JSONB syntax issues in policy definitions"
  [policy-sql]
  (-> policy-sql
      ;; Fix tenant sharing JSONB syntax
    (str/replace #"current_tenant_id\(\)\s*=\s*ANY\s*\(\s*shared_with_tenants\s*\)"
      "shared_with_tenants @> to_jsonb(current_tenant_id()::text)")
      ;; Fix general JSONB array syntax
    (str/replace #"(\w+)\s*=\s*ANY\s*\(\s*([^)]*jsonb[^)]*)\)"
      "$2 @> to_jsonb($1::text)")))

(defn quote-identifier-if-needed
  "Quote SQL identifier if it contains special characters"
  [identifier]
  (if (re-find #"[^a-zA-Z0-9_]" identifier)
    (str "\"" identifier "\"")
    identifier))

(defn generate-view-recreation-sql
  "Generate SQL to recreate a view (basic attempt)"
  [view-name view-sql]
  (if (str/includes? view-sql "CREATE")
    (str "-- Original view definition:\n" (comment-out-sql view-sql))
    (str "-- Unable to generate recreation SQL automatically")))


;; Template functions

(defn render-template
  "Render a template with variables"
  [template variables]
  (reduce-kv
    (fn [content k v]
      (str/replace content (str "{{" (name k) "}}") (str v)))
    template
    variables))

(defn function-migration-template
  "Enhanced function migration template with extension support"
  [function-name function-body extension-name use-extension?]
  (let [template (if use-extension?
                   ;; Extension-based template
                   "-- FORWARD
-- Function provided by {{extension-name}} extension
CREATE EXTENSION IF NOT EXISTS \"{{extension-name}}\";

-- The following function is provided by the extension:
-- {{function-signature}}
-- If you need custom behavior, uncomment and modify:
-- {{custom-function-sql}}

-- BACKWARD
-- Extension function, no cleanup needed
-- If you uncommented custom function above, use:
-- DROP FUNCTION IF EXISTS {{function-signature}};"

                   ;; Custom function template
                   "-- FORWARD
{{function-sql}}

-- BACKWARD
DROP FUNCTION IF EXISTS {{function-signature}};")]

    (render-template template
      {:extension-name extension-name
       :function-signature (extract-function-signature function-body)
       :function-sql function-body
       :custom-function-sql (comment-out-sql function-body)})))

(defn policy-migration-template
  "Enhanced policy migration template with JSONB fixes"
  [policy-name table-name policy-sql]
  (let [fixed-sql (fix-policy-jsonb-syntax policy-sql)
        template "-- FORWARD
{{policy-sql}}

-- Common JSONB patterns for tenant sharing:
-- Example: shared_with_tenants @> to_jsonb(current_tenant_id()::text)
-- Example: permissions @> '[\"read\", \"write\"]'::jsonb

-- BACKWARD
DROP POLICY IF EXISTS {{policy-name}} ON {{table-name}};"]

    (render-template template
      {:policy-sql fixed-sql
       :policy-name policy-name
       :table-name table-name})))

(defn view-migration-template
  "Enhanced view migration template with proper identifier quoting"
  [view-name view-sql include-recreation?]
  (let [quoted-view-name (quote-identifier-if-needed view-name)
        template (if include-recreation?
                   "-- FORWARD
{{view-sql}}

-- BACKWARD
-- Recreation SQL (auto-generated, may need adjustment):
{{recreation-sql}}
DROP VIEW IF EXISTS {{quoted-view-name}};"

                   "-- FORWARD
{{view-sql}}

-- BACKWARD
-- Cannot recreate view automatically - manual recreation required
DROP VIEW IF EXISTS {{quoted-view-name}};")]

    (render-template template
      {:view-sql view-sql
       :quoted-view-name quoted-view-name
       :recreation-sql (generate-view-recreation-sql view-name view-sql)})))

(defn trigger-migration-template
  "Enhanced trigger migration template"
  [trigger-name table-name trigger-sql]
  (let [template "-- FORWARD
{{trigger-sql}}

-- Trigger will be active on table: {{table-name}}
-- Remember to create the trigger function first if it doesn't exist

-- BACKWARD
DROP TRIGGER IF EXISTS {{trigger-name}} ON {{table-name}};"]

    (render-template template
      {:trigger-sql trigger-sql
       :trigger-name trigger-name
       :table-name table-name})))

;; Utility functions for template processing



;; Enhanced template generation functions

(defn generate-function-migration
  "Generate function migration with intelligence about extensions"
  [function-name function-body config]
  (let [required-extension (extensions/get-required-extension function-name)
        use-extension? (and required-extension
                         (get-in config [:function :use-extension]))
        extension-based? (and (str/includes? function-body "LANGUAGE c")
                           (or (str/includes? function-body "$libdir/uuid-ossp")
                             (str/includes? function-body "$libdir/hstore")))]

    (if (and use-extension? extension-based?)
      {:type :extension-function
       :content (function-migration-template function-name function-body
                  required-extension true)
       :extension required-extension
       :skip-if-exists true}

      {:type :custom-function
       :content (function-migration-template function-name function-body
                  nil false)
       :extension nil
       :skip-if-exists false})))

(defn generate-policy-migration
  "Generate policy migration with JSONB syntax fixes"
  [policy-name table-name policy-sql config]
  (let [needs-jsonb-fix? (and (str/includes? policy-sql "shared_with_tenants")
                           (str/includes? policy-sql "ANY"))
        fixed-content (if (get-in config [:policy :fix-jsonb-syntax])
                        (policy-migration-template policy-name table-name policy-sql)
                        (str "-- FORWARD\n" policy-sql
                          "\n\n-- BACKWARD\nDROP POLICY IF EXISTS "
                          policy-name " ON " table-name ";"))]

    {:type :policy
     :content fixed-content
     :needs-jsonb-fix needs-jsonb-fix?
     :table-name table-name}))

(defn generate-view-migration
  "Generate view migration with proper identifier handling"
  [view-name view-sql config]
  (let [has-dotted-name? (str/includes? view-name ".")
        include-recreation? (get-in config [:view :include-recreation-sql])
        content (view-migration-template view-name view-sql include-recreation?)]

    {:type :view
     :content content
     :has-dotted-name has-dotted-name?
     :needs-quoting (and has-dotted-name?
                      (get-in config [:view :quote-identifiers]))}))

(defn generate-trigger-migration
  "Generate trigger migration with table context"
  [trigger-name table-name trigger-sql config]
  {:type :trigger
   :content (trigger-migration-template trigger-name table-name trigger-sql)
   :table-name table-name
   :include-table-ref (get-in config [:trigger :include-table-reference])})

;; Template validation

(defn validate-template-output
  "Validate generated template for common issues"
  [template-content template-type]
  (let [issues (atom [])]

    ;; Check for missing FORWARD/BACKWARD sections
    (when-not (and (str/includes? template-content "-- FORWARD")
                (str/includes? template-content "-- BACKWARD"))
      (swap! issues conj {:type :missing-sections
                          :description "Template missing FORWARD or BACKWARD section"}))

    ;; Type-specific validations
    (case template-type
      :function (when (and (str/includes? template-content "LANGUAGE c")
                        (not (str/includes? template-content "EXTENSION")))
                  (swap! issues conj {:type :c-function-without-extension
                                      :description "C language function without extension check"}))

      :policy (when (str/includes? template-content "ANY(")
                (swap! issues conj {:type :jsonb-syntax-issue
                                    :description "Policy may have JSONB syntax issues"}))

      :view (when (and (str/includes? template-content ".")
                    (not (str/includes? template-content "\"")))
              (swap! issues conj {:type :unquoted-identifier
                                  :description "View name with dots should be quoted"}))

      nil)

    @issues))

(defn generate-migration-with-validation
  "Generate migration template with automatic validation"
  [migration-type migration-name migration-data config]
  (let [result (case migration-type
                 :function (generate-function-migration
                             migration-name
                             (:body migration-data)
                             config)
                 :policy (generate-policy-migration
                           migration-name
                           (:table migration-data)
                           (:sql migration-data)
                           config)
                 :view (generate-view-migration
                         migration-name
                         (:sql migration-data)
                         config)
                 :trigger (generate-trigger-migration
                            migration-name
                            (:table migration-data)
                            (:sql migration-data)
                            config))

        validation-issues (validate-template-output (:content result) migration-type)]

    (assoc result
      :validation-issues validation-issues
      :valid? (empty? validation-issues))))

;; Configuration helpers

(defn create-template-config
  "Create template configuration with sensible defaults"
  [overrides]
  (merge-with merge template-config overrides))

(defn extension-aware-config
  "Create configuration that's aware of available extensions"
  [db-connection user-overrides]
  (let [installed-extensions (extensions/get-installed-extensions db-connection)
        base-config (create-template-config user-overrides)
        extension-aware-updates {:function {:use-extension (and (contains? installed-extensions "uuid-ossp")
                                                             (contains? installed-extensions "hstore"))}}]
    (merge-with merge base-config extension-aware-updates)))

;; Template registry for extensibility

(def template-registry (atom {}))

(defn register-template
  "Register a custom template for a migration type"
  [migration-type template-fn]
  (swap! template-registry assoc migration-type template-fn))

(defn get-template
  "Get template function for migration type"
  [migration-type]
  (get @template-registry migration-type
    (case migration-type
      :function generate-function-migration
      :policy generate-policy-migration
      :view generate-view-migration
      :trigger generate-trigger-migration
      (fn [& args] {:type :unknown :content "-- Unknown migration type"}))))

;; Template application

(defn apply-template
  "Apply template with full configuration and validation"
  [migration-type migration-name migration-data & {:keys [config db-connection]
                                                   :or {config {}}}]
  (let [effective-config (if db-connection
                           (extension-aware-config db-connection config)
                           (create-template-config config))
        template-fn (get-template migration-type)
        result (template-fn migration-name migration-data effective-config)]

    (when (:valid? result false)
      (println (str "✅ Generated valid " (name migration-type) " migration for " migration-name)))

    (when (seq (:validation-issues result))
      (println (str "⚠️  Template validation issues for " migration-name ":"))
      (doseq [issue (:validation-issues result)]
        (println (str "   - " (:description issue)))))

    result))
