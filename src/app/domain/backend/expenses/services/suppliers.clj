(ns app.domain.backend.expenses.services.suppliers
  "Supplier CRUD services using factory pattern."
  (:require
    [app.domain.backend.expenses.services.places-api :as places-api]
    [app.domain.backend.expenses.services.service-configs :as configs]
    [app.domain.backend.expenses.services.services-factory :as factory]
    [app.domain.backend.expenses.services.suppliers.legacy-matching :as legacy]
    [app.domain.backend.expenses.services.suppliers.related-records :as related-records]
    [app.domain.backend.expenses.services.suppliers.repository :as repo]
    [app.domain.backend.expenses.services.suppliers.similarity :as similarity]
    [clojure.string :as str]))

(def config (configs/get-entity-config :supplier))

(def service (factory/build-entity-service config))

(def normalize-supplier-key configs/normalize-supplier-key)

(defn list-suppliers
  [db opts]
  (repo/list-suppliers db opts))

(defn count-suppliers
  [db opts]
  (repo/count-suppliers db opts))

(defn delete-supplier!
  [db supplier-id]
  (repo/delete-supplier! db supplier-id))

(defn find-by-normalized-key
  [db normalized-key]
  (repo/find-by-normalized-key db normalized-key))

(defn find-unique-descriptor-suffix-supplier
  [db normalized-key]
  (legacy/find-by-normalized-key-with-descriptor-suffix db normalized-key))

(defn- find-by-normalized-key-or-legacy
  [db normalized-key]
  (or (find-by-normalized-key db normalized-key)
    (legacy/find-by-canonical-key-with-legacy-suffix db normalized-key)
    (legacy/find-by-normalized-key-with-location-suffix db normalized-key)
    (legacy/find-by-normalized-key-with-descriptor-suffix db normalized-key)))

(defn- legacy-normalize-supplier-key-v0
  [name]
  (legacy/legacy-normalize-supplier-key-v0 name))

(defn- find-by-normalized-keys-or-legacy
  [db normalized-keys]
  (->> normalized-keys
    (map #(some-> % str str/trim not-empty))
    (remove nil?)
    distinct
    (some #(find-by-normalized-key-or-legacy db %))))

(defn- maybe-unescape-existing-display-name!
  [db {:keys [id display_name] :as supplier}]
  (let [dn (some-> display_name str)
        dn* (some-> dn configs/unescape-html-entities str str/trim not-empty)
        looks-escaped? (boolean (and dn (re-find #"&(#\d+|#x[0-9A-Fa-f]+|[A-Za-z]+);?" dn)))]
    (if (and looks-escaped? dn* (not= dn dn*))
      (or ((:update! service) db id {:display_name dn*}) supplier)
      supplier)))

(defn find-or-create-supplier!
  [db display-name & [{:keys [address]}]]
  (let [display-name (some-> display-name configs/unescape-html-entities str str/trim not-empty)
        display-name (legacy/strip-branch-suffix display-name)
        normalized (normalize-supplier-key display-name)]
    (if-let [existing (find-by-normalized-key-or-legacy db normalized)]
      {:existing? true
       :supplier (maybe-unescape-existing-display-name! db existing)}
      {:existing? false
       :supplier ((:create! service) db {:display_name display-name
                                         :address address})})))

(defn- unique-violation?
  [^java.sql.SQLException e]
  (= "23505" (.getSQLState e)))

(defn- create-supplier-idempotent!
  [db display-name]
  (let [display-name (some-> display-name str str/trim not-empty)
        display-name (legacy/strip-branch-suffix display-name)]
    (try
      ((:create! service) db {:display_name display-name})
      (catch java.sql.SQLException e
        (if (unique-violation? e)
          (or (find-by-normalized-key-or-legacy db (normalize-supplier-key display-name))
            (throw e))
          (throw e))))))

(defn- choose-create-display-name
  [ocr-display-name candidate-name]
  (let [ocr-display-name (some-> ocr-display-name configs/unescape-html-entities str str/trim not-empty)
        candidate-name (some-> candidate-name configs/unescape-html-entities str str/trim not-empty)
        ocr-normalized (normalize-supplier-key ocr-display-name)
        candidate-normalized (normalize-supplier-key candidate-name)]
    (cond
      (and (seq ocr-display-name)
        (seq candidate-name)
        (seq ocr-normalized)
        (seq candidate-normalized)
        (not= ocr-normalized candidate-normalized))
      candidate-name

      (seq ocr-display-name) ocr-display-name
      :else candidate-name)))

(defn resolve-or-create-supplier-with-places!
  [db ocr-guess & [opts]]
  (let [display-name (some-> ocr-guess configs/unescape-html-entities str str/trim not-empty)
        display-name (legacy/strip-branch-suffix display-name)]
    (if-not display-name
      {:supplier nil :source :ocr-fallback}
      (let [normalized (normalize-supplier-key display-name)
            legacy-normalized (legacy-normalize-supplier-key-v0 display-name)]
        (if-let [existing (find-by-normalized-keys-or-legacy db [normalized legacy-normalized])]
          {:supplier (maybe-unescape-existing-display-name! db existing) :source :db}
          (let [places-cfg (:places-cfg opts)
                input-norm (similarity/normalize-for-similarity display-name)
                use-bias? (and (<= (similarity/normalized-length input-norm) 4)
                            (get-in places-cfg [:location-bias]))
                search-opts {:region-code (or (:user-region opts) (:region-code places-cfg))
                             :language-code (:language-code places-cfg)
                             :max-results (:max-results places-cfg)
                             :location-bias (when use-bias? (:location-bias places-cfg))}
                places-res (when (and (map? places-cfg) (seq (:api-key places-cfg)))
                             (places-api/search-text! places-cfg display-name search-opts))
                candidate (similarity/best-place-candidate display-name (:places places-res))]
            (if-let [{:keys [place]} candidate]
              (let [candidate-name (some-> (:name place) configs/unescape-html-entities)
                    candidate-normalized (normalize-supplier-key candidate-name)
                    candidate-legacy-normalized (legacy-normalize-supplier-key-v0 candidate-name)
                    create-display-name (choose-create-display-name display-name candidate-name)]
                (if-let [existing (find-by-normalized-keys-or-legacy
                                    db
                                    [candidate-normalized candidate-legacy-normalized])]
                  {:supplier (maybe-unescape-existing-display-name! db existing) :source :places-api}
                  {:supplier (create-supplier-idempotent! db create-display-name)
                   :source :places-api}))
              {:supplier (create-supplier-idempotent! db display-name)
               :source :ocr-fallback})))))))

(defn list-related-records
  [db supplier-id opts]
  (related-records/list-related-records db supplier-id opts))
