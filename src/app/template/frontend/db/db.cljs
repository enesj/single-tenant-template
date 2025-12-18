(ns app.template.frontend.db.db
  (:require
    [app.template.frontend.db.defaults :as defaults]
    [app.template.frontend.db.flags :as flags]
    [app.template.frontend.db.interceptors :as interceptors]
    [app.template.frontend.db.schemas :as schemas]
    [app.template.frontend.db.validation :as validation]))

;; Flags
(def ENABLE_APP_DB_SPEC flags/ENABLE_APP_DB_SPEC)
(def STRICT_APP_DB_SPEC flags/STRICT_APP_DB_SPEC)
(def validation-enabled? flags/validation-enabled?)
(def strict-validation-enabled? flags/strict-validation-enabled?)

;; Schema helpers
(def models-data->map schemas/models-data->map)

;; Defaults
(def default-session-state defaults/default-session-state)
(def make-default-list-state defaults/make-default-list-state)
(def default-db defaults/default-db)
(def make-db-with-models-data defaults/make-db-with-models-data)

;; Validation
(def validate-db validation/validate-db)

;; Interceptors
(def check-spec-interceptor interceptors/check-spec-interceptor)
(def common-interceptors interceptors/common-interceptors)
