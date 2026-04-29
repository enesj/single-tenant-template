(ns app.shared.frontend-config.template-user
  "Shared path definitions for template-owned user UI config bundles.")

(def root-dir
  "Source-controlled template user UI config root."
  "src/app/template/frontend/config")

(def paths
  {:entities (str root-dir "/entities.edn")
   :view-options (str root-dir "/view-options.edn")
   :form-fields (str root-dir "/form-fields.edn")
  :table-columns (str root-dir "/table-columns.edn")
  :navigation (str root-dir "/navigation.edn")})
