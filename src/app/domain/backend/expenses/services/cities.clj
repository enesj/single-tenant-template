(ns app.domain.backend.expenses.services.cities
  "Facade namespace for city ZIP-based lookup services."
  (:require
    [app.domain.backend.expenses.services.cities-normalize :as normalize]
    [app.domain.backend.expenses.services.cities-resolver :as resolver]
    [app.domain.backend.expenses.services.service-configs :as configs]
    [app.domain.backend.expenses.services.services-factory :as factory]))

(defn normalize-zip
  [zip-value]
  (normalize/normalize-zip zip-value))

(defn extract-zip-from-text
  [text]
  (normalize/extract-zip-from-text text))

(defn resolve-city-id-from-text
  ([db text]
   (resolver/resolve-city-id-from-text db text))
  ([db country text]
   (resolver/resolve-city-id-from-text db country text)))

(defn resolve-city-id-from-text!
  ([db text]
   (resolver/resolve-city-id-from-text! db text))
  ([db country-or-text text-or-opts]
   (resolver/resolve-city-id-from-text! db country-or-text text-or-opts))
  ([db country text opts]
   (resolver/resolve-city-id-from-text! db country text opts)))

(def config
  (configs/get-entity-config :city))

(def service
  (factory/build-entity-service config))
