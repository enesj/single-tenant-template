(ns code-quality.check-unused-reframe.data
  (:require
    [babashka.fs :as fs]
    [clojure.edn :as edn]))

;; Keywords flagged as unused from clojure-lsp diagnostics
(def unused-keywords
  (let [p "scripts/bb/code_quality/check_unused_reframe/unused_keywords.edn"]
    (if (fs/exists? p)
      (try
        (edn/read-string (slurp p))
        (catch Exception e
          (println "Warning: failed to load unused keywords from EDN:" (.-message e))
          []))
      [])))


(defn- admin-entity-keys
  "Load admin entity keys from entities.edn so we can detect dynamic subscription usage."
  []
  (let [p "src/app/admin/frontend/config/entities.edn"]
    (when (fs/exists? p)
      (try
        (let [m (edn/read-string (slurp p))]
          (->> (keys m)
            (filter keyword?)
            set))
        (catch Exception _
          #{})))))

(defn admin-dynamic-subscription-keyword-strings
  "Subscriptions referenced dynamically by app.template.frontend.utils.shared/use-entity-state.

  use-entity-state builds (keyword admin (str (name entity) -loading?)) and similarly for -error.
  We treat those as used for all entities declared in admin entities.edn."
  []
  (let [entities (admin-entity-keys)
        suffixes ["loading?" "error"]]
    (into #{}
      (for [e entities
            s suffixes]
        (str ":admin/" (name e) "-" s)))))
