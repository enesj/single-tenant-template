(ns app.backend.mock-data-helper
  "Helper namespace to provide auto-generated mock data for tests.
   All tests now use comprehensive auto-generated data from models.edn."
  (:require
    [app.backend.mock-data-auto :as auto]
    [clojure.spec.alpha :as s]))

(defn get-mock-data
  "Get auto-generated mock data"
  []
  @auto/entities-mock-data)

(defn validate-mock-data
  "Validate that the mock data conforms to the specs"
  []
  (let [data (get-mock-data)]
    (doseq [[entity-name entity-data] data]
      (let [entity-spec (keyword (str (name entity-name) "/entity"))]
        ;; Validate new-data
        (when-let [new-data (:new-data entity-data)]
          (when-not (s/valid? entity-spec new-data)
            (throw (ex-info (str "Invalid new-data for " entity-name)
                     {:explanation (s/explain-str entity-spec new-data)}))))
        ;; Validate update-data
        (when-let [update-data (:update-data entity-data)]
          (when-not (s/valid? entity-spec update-data)
            (throw (ex-info (str "Invalid update-data for " entity-name)
                     {:explanation (s/explain-str entity-spec update-data)}))))
        ;; Validate seed-data
        (when-let [seed-data (:seed-data entity-data)]
          (doseq [item seed-data]
            (when-not (s/valid? entity-spec item)
              (throw (ex-info (str "Invalid seed-data item for " entity-name)
                       {:explanation (s/explain-str entity-spec item)})))))))))
