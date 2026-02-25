(ns app.domain.backend.expenses.services.stores
  "Compatibility facade for store services.

  New code should use focused namespaces under
  `app.domain.backend.expenses.services.stores.*`."
  (:require
    [app.domain.backend.expenses.services.stores.city :as stores-city]
    [app.domain.backend.expenses.services.stores.related :as stores-related]
    [app.domain.backend.expenses.services.stores.repo :as stores-repo]
    [app.domain.backend.expenses.services.stores.resolution :as stores-resolution]
    [app.domain.backend.expenses.services.stores.service :as stores-service]
    [app.domain.backend.expenses.services.stores.upsert :as stores-upsert]))

(def config stores-service/config)

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

(defn find-by-supplier-and-normalized-key [db supplier-id normalized-key]
  (stores-repo/find-by-supplier-and-normalized-key db supplier-id normalized-key))

(defn find-by-supplier-and-place-id [db supplier-id place-id]
  (stores-repo/find-by-supplier-and-place-id db supplier-id place-id))

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

(defn list-related-records [db store-id opts]
  (stores-related/list-related-records db store-id opts))
