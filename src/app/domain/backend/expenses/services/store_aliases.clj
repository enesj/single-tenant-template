(ns app.domain.backend.expenses.services.store-aliases
  "Store alias management (deduped raw store/address guesses -> canonical store mapping).

  Design:
  - store_aliases are globally unique by raw_label_normalized
  - receipts reference store_aliases via receipts.store_alias_id for cheap counts/joins"
  (:require
    [app.domain.backend.expenses.services.service-configs :as configs]
    [app.domain.backend.expenses.services.services-factory :as factory]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

(def config (configs/get-entity-config :store-alias))

(def service (factory/build-entity-service config))

(def ^:private min-alias-normalized-length
  2)

(defn find-or-create-alias!
  "Find or create a store_alias by raw_label (global uniqueness).

  Returns the alias row (with :id, :store_id, etc.)."
  [db raw-label]
  (when (str/blank? raw-label)
    (throw (ex-info "raw_label is required" {:status 400 :field :raw_label})))
  (let [raw-label* (str/trim (str raw-label))
        normalized (configs/normalize-store-key raw-label*)
        row {:id (UUID/randomUUID)
             :raw_label raw-label*
             :raw_label_normalized normalized
             :store_id nil
             :confidence 0
             :created_at [:now]}
        sql-map {:insert-into :store_aliases
                 :values [row]
                 :on-conflict [:raw_label_normalized]
                 :do-update-set {:raw_label :excluded/raw_label}
                 :returning [:*]}]
    (when (or (str/blank? normalized)
            (< (count normalized) min-alias-normalized-length))
      (throw (ex-info "raw_label normalizes to an invalid key"
               {:status 400
                :field :raw_label
                :raw_label raw-label*
                :raw_label_normalized normalized})))
    (jdbc/execute-one!
      db
      (sql/format sql-map)
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn map-alias-to-store!
  "Map a store alias to a canonical store.

  Sets store_aliases.store_id for the given alias."
  ([db alias-id store-id]
   (map-alias-to-store! db alias-id store-id 100))
  ([db alias-id store-id confidence]
   (jdbc/execute-one!
     db
     (sql/format {:update :store_aliases
                  :set {:store_id store-id
                        :confidence (or confidence 100)}
                  :where [:= :id alias-id]
                  :returning [:*]})
     {:builder-fn rs/as-unqualified-lower-maps})))

(def ^:private default-ocr-confidence
  "Confidence score used when OCR auto-maps an alias; lower than manual mappings (100)."
  25)

(defn map-alias-to-store-if-unmapped!
  "Map a store alias to a canonical store only if it is currently unmapped.

  Safe to run during automated ingestion (OCR) because it will NOT overwrite an
  existing manual mapping.

  Returns the updated alias row when an update happened, otherwise nil."
  ([db alias-id store-id]
   (map-alias-to-store-if-unmapped! db alias-id store-id default-ocr-confidence))
  ([db alias-id store-id confidence]
   (jdbc/execute-one!
     db
     (sql/format {:update :store_aliases
                  :set {:store_id store-id
                        :confidence (or confidence default-ocr-confidence)}
                  :where [:and
                          [:= :id alias-id]
                          [:is :store_id nil]]
                  :returning [:*]})
     {:builder-fn rs/as-unqualified-lower-maps})))


