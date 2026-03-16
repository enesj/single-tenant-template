(ns app.domain.backend.expenses.services.cities-resolver
  "Orchestration logic for resolving city ids from free text."
  (:require
    [app.domain.backend.expenses.services.cities-normalize :as normalize]
    [app.domain.backend.expenses.services.cities-places :as places]
    [app.domain.backend.expenses.services.cities-repository :as repository]
    [clojure.string :as str]))

(defn resolve-city-id-from-text
  "Resolve city_id from free text with strict ZIP precedence.
   When ZIP is found but not in DB, tries all extracted name candidates,
   then falls back to ZIP prefix matching."
  ([db text]
   (resolve-city-id-from-text db normalize/default-country text))
  ([db country text]
   (if-let [zip (normalize/extract-zip-from-text text)]
     (or
       (some-> (repository/find-city-by-country-and-zip db country zip) :id)
       (some #(repository/find-city-id-by-candidate db %)
         (normalize/extract-city-fallback-candidates text))
       (some-> (repository/find-city-by-zip-prefix db zip) :id))
     (some-> (normalize/extract-city-fallback-candidate text)
       (#(repository/find-city-id-by-candidate db %))))))

(defn resolve-city-id-from-text!
  "Resolve city id from free text and optionally create missing city rows.
   When ZIP is found but not in DB, tries all extracted name candidates,
   then ZIP prefix matching, then Places API."
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
       (if-let [zip (normalize/extract-zip-from-text text*)]
         (or
           (some-> (repository/find-city-by-country-and-zip db country* zip) :id)
           (some #(repository/find-city-id-by-candidate db %)
             (normalize/extract-city-fallback-candidates text*))
           (some-> (repository/find-city-by-zip-prefix db zip) :id)
           (when-let [candidate (normalize/extract-city-fallback-candidate text*)]
             (when-let [confirmed-city (places/confirm-city-via-places zip candidate opts)]
               (repository/ensure-city-by-country-and-zip! db country* zip confirmed-city)))
           (when-let [{:keys [city-name]} (places/infer-city-and-zip-via-places text* opts :expected-zip zip)]
             (repository/ensure-city-by-country-and-zip! db country* zip city-name)))

         (or
           (when-let [candidate (normalize/extract-city-fallback-candidate text*)]
             (or
               (repository/find-city-id-by-candidate db candidate)
               (let [query-text (or (normalize/places-query-from-text text* candidate)
                                  candidate)]
                 (when-let [{:keys [zip city-name]} (places/infer-city-and-zip-via-places text* opts :candidate candidate :query-text query-text)]
                   (repository/ensure-city-by-country-and-zip! db country* zip city-name)))))
           (when-let [{:keys [zip city-name]} (places/infer-city-and-zip-via-places text* opts :query-text (normalize/places-query-from-text text* nil))]
             (repository/ensure-city-by-country-and-zip! db country* zip city-name))))))))
