(ns app.domain.backend.expenses.services.countries
  "Countries CRUD service for the Expenses admin API.

  NOTE: The countries table uses a string PK (`country`), not a UUID id column.
  For admin list compatibility, this service computes an `id` field equal to `country`."
  (:require
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

(def ^:private jdbc-opts
  {:builder-fn rs/as-unqualified-lower-maps})

(defn- bad-request!
  [message & [data]]
  (throw (ex-info message (merge {:status 400} (or data {})))))

(defn- normalize-country
  [v]
  (some-> v str str/trim))

(defn- normalize-code
  [v]
  (some-> v str str/trim str/upper-case))

(defn- validate-country!
  [country]
  (let [c (normalize-country country)]
    (cond
      (str/blank? c)
      (bad-request! "Country cannot be blank" {:field :country})

      (> (count c) 255)
      (bad-request! "Country must be at most 255 characters" {:field :country
                                                              :max 255
                                                              :value c})

      :else c)))

(defn- validate-code!
  [code]
  (let [c (normalize-code code)]
    (cond
      (str/blank? c)
      (bad-request! "Code cannot be blank" {:field :code})

      (not= 2 (count c))
      (bad-request! "Code must be 2 characters" {:field :code
                                                 :expected-length 2
                                                 :value c})

      :else c)))

(defn- with-id
  [row]
  (when row
    (assoc row :id (:country row))))

(defn list-countries
  "Return countries list.

  Routes factory provides opts like:
  {:limit N :offset M :order-by :country :order-dir :asc}

  Returns DB-shaped rows with computed :id."
  [db {:keys [limit offset order-by order-dir]
       :or {limit 500 offset 0 order-by :country order-dir :asc}}]
  (let [order-by* (or order-by :country)
        order-dir* (or order-dir :asc)
        q {:select [:country :code :created_at :updated_at]
           :from [:countries]
           :order-by [[order-by* order-dir*]]
           :limit limit
           :offset offset}
        rows (jdbc/execute! db (sql/format q) jdbc-opts)]
    (mapv with-id rows)))

(defn get-country
  "Return a single country row by country name (string PK)."
  [db id]
  (let [country (validate-country! id)
        q {:select [:country :code :created_at :updated_at]
           :from [:countries]
           :where [:= :country country]
           :limit 1}
        row (jdbc/execute-one! db (sql/format q) jdbc-opts)]
    (with-id row)))

(defn create-country!
  "Insert a country row.

  Expects data keys in DB-shape: {:country <string> :code <string>}
  Returns inserted row with computed :id."
  [db data]
  (let [country (validate-country! (:country data))
        code (validate-code! (:code data))
        q {:insert-into :countries
           :values [{:country country
                     :code code
                     :created_at [:now]}]
           :returning [:country :code :created_at :updated_at]}
        row (jdbc/execute-one! db (sql/format q) jdbc-opts)]
    (with-id row)))

(defn update-country!
  "Update a country row.

  id is the existing country string. updates may include :country (rename) and/or :code.
  Returns updated row with computed :id."
  [db id updates]
  (let [country-id (validate-country! id)
        set-map (cond-> {}
                  (contains? updates :country)
                  (assoc :country (validate-country! (:country updates)))

                  (contains? updates :code)
                  (assoc :code (validate-code! (:code updates))))]
    (when (empty? set-map)
      (bad-request! "No valid fields to update" {:field :updates}))
    (let [q {:update :countries
             :set set-map
             :where [:= :country country-id]
             :returning [:country :code :created_at :updated_at]}
          row (jdbc/execute-one! db (sql/format q) jdbc-opts)]
      (with-id row))))

(defn delete-country!
  "Delete a country by country name (string PK). Returns boolean."
  [db id]
  (let [country (validate-country! id)
        q {:delete-from :countries
           :where [:= :country country]
           :returning [:country]}
        row (jdbc/execute-one! db (sql/format q) jdbc-opts)]
    (boolean row)))


