(ns app.template.backend.utils.adapters.normalization
  "Shared database normalization utilities for admin and frontend APIs.

  Backward-compatible wrapper around `app.shared.adapters.normalization`."
  (:require
    [app.shared.adapters.normalization :as shared-norm]))

(def convert-db-keys->app-keys shared-norm/convert-db-keys->app-keys)
(def app-keyword->camel shared-norm/app-keyword->camel)
(def convert-app-keys->camel-keys shared-norm/convert-app-keys->camel-keys)
(def db-keyword->app-with-aliases shared-norm/db-keyword->app-with-aliases)
(def normalize-admin-result shared-norm/normalize-admin-result)
