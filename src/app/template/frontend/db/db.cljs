(ns app.template.frontend.db.db
  "App-db schema/initial state; update for new state keys."
  (:require
    [app.template.frontend.db.defaults :as defaults]
    [app.template.frontend.db.interceptors :as interceptors]
    [app.template.frontend.db.schemas :as schemas]
    [app.template.frontend.db.validation :as validation]))

;; Flags

;; Schema helpers
(def models-data->map schemas/models-data->map)

;; Defaults

(def make-default-list-state defaults/make-default-list-state)
(def default-db defaults/default-db)
(def make-db-with-models-data defaults/make-db-with-models-data)

;; Validation
(def validate-db validation/validate-db)

;; Interceptors

(def common-interceptors interceptors/common-interceptors)
