(ns app.domain.backend.expenses.services.stores
  "Compatibility facade for store services.

  New code should use focused namespaces under
  `app.domain.backend.expenses.services.stores.*`."
  (:require
    [app.domain.backend.expenses.services.stores.city :as stores-city]
    [app.domain.backend.expenses.services.stores.repo :as stores-repo]
    [app.domain.backend.expenses.services.stores.resolution :as stores-resolution]
    [app.domain.backend.expenses.services.stores.service :as stores-service]
    [app.domain.backend.expenses.services.stores.upsert :as stores-upsert]))

(def service stores-service/entity-service)

(defn extract-city-from-address
  ([address]
   (stores-city/extract-city-from-address address))
  ([address display-name]
   (stores-city/extract-city-from-address address display-name))
  ([address display-name place-id places-config]
   (stores-city/extract-city-from-address address display-name place-id places-config)))

(defn get-store [db store-id]
  (stores-repo/get-store db store-id))

(defn update-store!
  ([db store-id patch]
   (stores-upsert/update-store! db store-id patch))
  ([db store-id patch opts]
   (stores-upsert/update-store! db store-id patch opts)))

(defn find-or-create-store!
  ([db store-attrs]
   (stores-upsert/find-or-create-store! db store-attrs))
  ([db store-attrs opts]
   (stores-upsert/find-or-create-store! db store-attrs opts)))

(defn resolve-store-from-merchant
  ([db supplier-id merchant]
   (stores-resolution/resolve-store-from-merchant db supplier-id merchant))
  ([db supplier-id merchant opts]
   (stores-resolution/resolve-store-from-merchant db supplier-id merchant opts)))


