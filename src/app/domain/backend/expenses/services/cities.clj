(ns app.domain.backend.expenses.services.cities
  "Facade namespace for city ZIP-based lookup services."
  (:require
    [app.domain.backend.expenses.services.cities-normalize :as normalize]
    [app.domain.backend.expenses.services.cities-places :as places]
    [app.domain.backend.expenses.services.cities-repository :as repository]
    [clojure.string :as str]
    [app.domain.backend.expenses.services.service-configs :as configs]
    [app.domain.backend.expenses.services.services-factory :as factory]))

(defn find-city-by-normalized-key
  [db normalized-key]
  (repository/find-city-by-normalized-key db normalized-key))

(defn normalize-zip
  [zip-value]
  (normalize/normalize-zip zip-value))

(defn extract-zip-from-text
  [text]
  (normalize/extract-zip-from-text text))

(defn find-city-by-country-and-zip
  [db country zip]
  (repository/find-city-by-country-and-zip db country zip))

(defn- extract-city-fallback-candidate
  [text]
  (normalize/extract-city-fallback-candidate text))

(defn- places-query-from-text
  [text candidate]
  (normalize/places-query-from-text text candidate))

(defn- find-city-id-by-candidate
  [db candidate]
  (some (fn [normalized-key]
          (some-> (find-city-by-normalized-key db normalized-key)
            :id))
    (normalize/candidate-normalized-keys candidate)))

(defn- ensure-city-by-country-and-zip!
  [db country zip city-name]
  (repository/ensure-city-by-country-and-zip! db country zip city-name))

(defn resolve-city-id-from-text
  ([db text]
   (resolve-city-id-from-text db normalize/default-country text))
  ([db country text]
   (if-let [zip (extract-zip-from-text text)]
     (some-> (find-city-by-country-and-zip db country zip)
       :id)
     (some-> (extract-city-fallback-candidate text)
       (#(find-city-id-by-candidate db %))))))

(defn- confirm-city-via-places
  [zip candidate opts]
  (places/confirm-city-via-places zip candidate opts))

(defn- infer-city-and-zip-via-places
  [text opts & kwargs]
  (apply places/infer-city-and-zip-via-places text opts kwargs))

(defn resolve-city-id-from-text!
  ([db text]
   (resolve-city-id-from-text! db normalize/default-country text nil))
  ([db country-or-text text-or-opts]
   (if (or (map? text-or-opts) (nil? text-or-opts))
     (resolve-city-id-from-text! db normalize/default-country country-or-text text-or-opts)
     (resolve-city-id-from-text! db country-or-text text-or-opts nil)))
  ([db country text opts]
   (let [country* (or (some-> country str str/trim not-empty)
                    normalize/default-country)
         text* (some-> text str str/trim not-empty)]
     (when (seq text*)
       (if-let [zip (extract-zip-from-text text*)]
         (or
           (some-> (find-city-by-country-and-zip db country* zip)
             :id)
           (when-let [candidate (extract-city-fallback-candidate text*)]
             (when-let [confirmed-city (confirm-city-via-places zip candidate opts)]
               (ensure-city-by-country-and-zip! db country* zip confirmed-city)))
           (when-let [{:keys [city-name]} (infer-city-and-zip-via-places text* opts :expected-zip zip)]
             (ensure-city-by-country-and-zip! db country* zip city-name)))

         (or
           (when-let [candidate (extract-city-fallback-candidate text*)]
             (or
               (find-city-id-by-candidate db candidate)
               (let [query-text (or (places-query-from-text text* candidate)
                                  candidate)]
                 (when-let [{:keys [zip city-name]} (infer-city-and-zip-via-places text* opts :candidate candidate :query-text query-text)]
                   (ensure-city-by-country-and-zip! db country* zip city-name)))))
           (when-let [{:keys [zip city-name]} (infer-city-and-zip-via-places text* opts :query-text (places-query-from-text text* nil))]
             (ensure-city-by-country-and-zip! db country* zip city-name))))))))

(def config
  (configs/get-entity-config :city))

(def service
  (factory/build-entity-service config))
