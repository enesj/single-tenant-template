(ns scripts.bb.refactor
  (:require [babashka.fs :as fs]
            [babashka.process :refer [shell]]
            [clojure.string :as str]))


(defn file->ns [path]
  (let [base-path (str/replace path #"^src/" "")
        no-ext (cond
                 (str/ends-with? base-path ".clj") (subs base-path 0 (- (count base-path) 4))
                 (str/ends-with? base-path ".cljs") (subs base-path 0 (- (count base-path) 5))
                 (str/ends-with? base-path ".cljc") (subs base-path 0 (- (count base-path) 5))
                 :else base-path)]
    (-> no-ext
        (str/replace "_" "-")
        (str/replace "/" "."))))

(defn ns->path [ns ext]
  (str "src/" (-> ns
                  (str/replace "-" "_")
                  (str/replace "." "/"))
       ext))

(defn get-extension [path]
  (cond
    (str/ends-with? path ".clj") ".clj"
    (str/ends-with? path ".cljs") ".cljs"
    (str/ends-with? path ".cljc") ".cljc"
    :else nil))

;; Define rules as functions that take an old-ns and return new-ns or nil
(defn rename-rule [old-ns path]
  (cond
    ;; Admin: app.backend.services.admin -> app.admin.backend.services.admin
    (str/starts-with? old-ns "app.backend.services.admin")
    (str/replace-first old-ns "app.backend.services.admin" "app.admin.backend.services.admin")

    ;; Admin: app.backend.admin-setup -> app.admin.backend.setup
    (= old-ns "app.backend.admin-setup")
    "app.admin.backend.setup"

    ;; Domain: app.backend.services.user-expenses -> app.domain.backend.expenses.services.user-expenses
    (= old-ns "app.backend.services.user-expenses")
    "app.domain.backend.expenses.services.user-expenses"

    ;; Domain: expenses/frontend -> app.domain.frontend.expenses
    (str/starts-with? old-ns "app.domain.expenses.frontend")
    (str/replace-first old-ns "app.domain.expenses.frontend" "app.domain.frontend.expenses")

    ;; Domain: expenses/routes -> app.domain.backend.expenses.routes
    (str/starts-with? old-ns "app.domain.expenses.routes")
    (str/replace-first old-ns "app.domain.expenses.routes" "app.domain.backend.expenses.routes")

    ;; Domain: expenses/services -> app.domain.backend.expenses.services
    (str/starts-with? old-ns "app.domain.expenses.services")
    (str/replace-first old-ns "app.domain.expenses.services" "app.domain.backend.expenses.services")

    ;; Template: app.frontend -> app.template.frontend
    (str/starts-with? old-ns "app.frontend")
    (str/replace-first old-ns "app.frontend" "app.template.frontend")
    
    ;; Template: app.shared.frontend -> app.template.frontend.shared
    (str/starts-with? old-ns "app.shared.frontend")
    (str/replace-first old-ns "app.shared.frontend" "app.template.frontend.shared")

    ;; Template: app.migrations -> app.template.backend.migrations
    (str/starts-with? old-ns "app.migrations")
    (str/replace-first old-ns "app.migrations" "app.template.backend.migrations")

    ;; Template: app.backend (generic) -> app.template.backend
    ;; Must be last backend rule to avoid capturing admin/user-expenses
    (str/starts-with? old-ns "app.backend")
    (str/replace-first old-ns "app.backend" "app.template.backend")

    ;; Shared cleanup: .clj files in app.shared -> app.template.backend.utils
    (and (str/starts-with? old-ns "app.shared")
         (str/ends-with? path ".clj")) ;; Only .clj files, .cljc stays
    (str/replace-first old-ns "app.shared" "app.template.backend.utils")

    :else nil))

(defn scan-and-plan []
  (let [root "src/app"
        files (fs/glob root "**.{clj,cljs,cljc}")]
    (->> files
         (keep (fn [f]
                 (let [path (str f)
                       ns-name (file->ns path)
                       new-ns (rename-rule ns-name path)]
                   (when (and new-ns (not= ns-name new-ns))
                     {:path path
                      :old-ns ns-name
                      :new-ns new-ns}))))
         (sort-by :path))))

(defn -main []
  (println "🔎 Scanning for namespaces to rename...")
  (let [plan (scan-and-plan)]
    (if (empty? plan)
      (println "✅ No renames needed.")
      (do
        (println (str "📝 Found " (count plan) " namespaces to rename."))
        (doseq [{:keys [old-ns new-ns]} plan]
          (println (str "Move: " old-ns " -> " new-ns))
          (try
            (shell "clojure-lsp" "rename" "--from" old-ns "--to" new-ns)
            (catch Exception e
              (println (str "❌ Failed to rename " old-ns ": " (.getMessage e))))))
        (println "🎉 Refactoring complete!")))))

(-main)
