(ns app.template.frontend.db.flags)

(goog-define ^boolean ENABLE_APP_DB_SPEC true)
(goog-define ^boolean STRICT_APP_DB_SPEC false)

(defn validation-enabled?
  []
  ENABLE_APP_DB_SPEC)

(defn strict-validation-enabled?
  []
  (and (validation-enabled?) STRICT_APP_DB_SPEC))
