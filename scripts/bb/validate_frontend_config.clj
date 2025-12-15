#!/usr/bin/env bb

(ns validate-frontend-config
  (:require
    [clojure.edn :as edn]
    [app.shared.specs.entities :as entities-spec]
    [app.shared.specs.form-fields :as form-fields-spec]
    [app.shared.specs.table-columns :as table-columns-spec]
    [app.shared.specs.view-options :as view-options-spec]))

(defn- die!
  [msg]
  (binding [*out* *err*]
    (println msg))
  (System/exit 2))

(defn- read-edn!
  [path]
  (try
    (edn/read-string (slurp path))
    (catch Exception e
      (die! (str "Failed to read EDN: " path "\n" (.getMessage e))))))

(defn- print-result!
  [{:keys [label path valid? errors warnings]}]
  (if valid?
    (println "✓" label "valid" (str "(" path ")"))
    (do
      (println "✗" label "INVALID" (str "(" path ")"))
      (when (seq errors)
        (println "  errors:" (pr-str errors)))
      (when (seq warnings)
        (println "  warnings:" (pr-str warnings))))))

(def ^:private checks
  [{:label "admin entities.edn"
    :path "src/app/admin/frontend/config/entities.edn"
    :validate entities-spec/validate-admin-entities-strict}

   {:label "admin view-options.edn"
    :path "src/app/admin/frontend/config/view-options.edn"
    :validate view-options-spec/validate-view-options-strict}

   {:label "admin form-fields.edn"
    :path "src/app/admin/frontend/config/form-fields.edn"
    :validate form-fields-spec/validate-form-fields-strict}

   {:label "admin table-columns.edn"
    :path "src/app/admin/frontend/config/table-columns.edn"
    :validate table-columns-spec/validate-table-columns-strict}

   ;; Domain-owned (user-facing)
   {:label "domain entities.edn"
    :path "src/app/domain/frontend/expenses/config/entities.edn"
    :validate entities-spec/validate-user-entities}

   {:label "domain view-options.edn"
    :path "src/app/domain/frontend/expenses/config/view-options.edn"
    :validate view-options-spec/validate-view-options-strict}

   {:label "domain form-fields.edn"
    :path "src/app/domain/frontend/expenses/config/form-fields.edn"
    :validate form-fields-spec/validate-form-fields-strict}

   {:label "domain table-columns.edn"
    :path "src/app/domain/frontend/expenses/config/table-columns.edn"
    :validate table-columns-spec/validate-table-columns-strict}])

(defn -main
  [& _args]
  (println "=== Validating frontend config EDNs ===")
  (let [results
        (for [{:keys [label path validate]} checks
              :let [data (read-edn! path)
                    res (validate data)
                    res' (merge {:label label :path path} res)]]
          (do
            (print-result! res')
            res'))
        invalid (filter (comp not :valid?) results)]
    (if (seq invalid)
      (die! (str "\nConfig validation failed (" (count invalid) " invalid file(s))."))
      (println "\n✅ All frontend config EDNs are valid."))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
