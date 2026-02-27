(ns app.domain.backend.expenses.services.stores.upsert
  (:require
    [app.domain.backend.expenses.services.cities :as cities]
    [app.domain.backend.expenses.services.service-configs :as configs]
    [app.domain.backend.expenses.services.stores.repo :as repo]
    [app.shared.type-conversion :as type-conv]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log])
  (:import
    [java.util UUID]))

(def ^:private try-uuid type-conv/try-parse-uuid)

(defn update-store!
  "Patch a store row and return the updated store."
  ([db store-id patch]
   (update-store! db store-id patch nil))
  ([db store-id {:keys [display_name display-name
                        address
                        normalized_key normalized-key
                        place_id place-id]} opts]
   (let [display-name* (some-> (or display_name display-name) str str/trim not-empty)
         address* (some-> address str str/trim not-empty)
         explicit-normalized* (some-> (or normalized_key normalized-key) str str/trim not-empty)
         place-normalized* (repo/place-id->normalized-key (or place_id place-id))
         normalized* (or explicit-normalized* place-normalized*)
         city-source-text (some->> [display-name* address*]
                            (remove str/blank?)
                            (str/join "\n")
                            (not-empty))
         city-id (when address*
                   (try
                     (cities/resolve-city-id-from-text! db (or city-source-text address*) opts)
                     (catch Exception e
                       (log/warn e "City resolution failed during store update; skipping city"
                         {:store-id store-id
                          :display-name display-name*})
                       nil)))
         set-map (cond-> {}
                   (some? display-name*) (assoc :display_name display-name*)
                   (some? address*) (assoc :address address*)
                   (some? normalized*) (assoc :normalized_key normalized*)
                   (some? address*) (assoc :city_id city-id))]
     (if (seq set-map)
       (jdbc/execute-one!
         db
         (sql/format {:update :stores
                      :set set-map
                      :where [:= :id store-id]
                      :returning [:*]})
         {:builder-fn rs/as-unqualified-lower-maps})
       (repo/get-store db store-id)))))

(defn merge-duplicate-stores-by-place-id!
  "Merge stores for a supplier that resolve to the same Places place_id."
  [db supplier-id place-id keep-store-id]
  (when-let [normalized-key (repo/place-id->normalized-key place-id)]
    (when (and supplier-id (seq normalized-key) keep-store-id)
      (let [rows (jdbc/execute!
                   db
                   (sql/format {:select [:id]
                                :from [:stores]
                                :where [:and
                                        [:= :supplier_id supplier-id]
                                        [:= :normalized_key normalized-key]]})
                   {:builder-fn rs/as-unqualified-lower-maps})
            ids (->> rows (map :id) (remove nil?) distinct vec)
            keep-id keep-store-id
            drop-ids (->> ids (remove #(= % keep-id)) vec)]
        (when (seq drop-ids)
          (jdbc/execute!
            db
            (sql/format {:update :expenses
                         :set {:store_id keep-id}
                         :where [:in :store_id drop-ids]}))
          (jdbc/execute!
            db
            (sql/format {:update :store_aliases
                         :set {:store_id keep-id}
                         :where [:in :store_id drop-ids]}))
          (jdbc/execute!
            db
            (sql/format {:delete-from :stores
                         :where [:in :id drop-ids]})))
        {:kept keep-id
         :merged (count drop-ids)}))))

(defn find-or-create-store!
  "Idempotently create or update store row."
  ([db store-attrs]
   (find-or-create-store! db store-attrs nil))
  ([db {:keys [supplier_id supplier-id
               display_name display-name
               normalized_key normalized-key
               address
               place_id place-id]}
    opts]
   (let [supplier-id* (try-uuid (or supplier_id supplier-id))
         display-name* (some-> (or display_name display-name) str str/trim not-empty)
         address* (some-> address str str/trim not-empty)
         place-key (repo/place-id->normalized-key (or place_id place-id))
         normalized* (some-> (or normalized_key normalized-key) str str/trim not-empty)]
     (when-not supplier-id*
       (throw (ex-info "supplier_id is required" {:status 400 :field :supplier_id})))
     (when-not display-name*
       (throw (ex-info "display_name is required" {:status 400 :field :display_name})))
     (let [normalized (cond
                        place-key place-key
                        normalized* (configs/normalize-store-key normalized*)
                        :else (configs/normalize-store-key (str display-name* " " (or address* ""))))
           city-source-text (some->> [display-name* address*]
                              (remove str/blank?)
                              (str/join "\n")
                              (not-empty))
           city-id (when address*
                     (try
                       (cities/resolve-city-id-from-text! db (or city-source-text address*) opts)
                       (catch Exception e
                         (log/warn e "City resolution failed during store creation; continuing without city"
                           {:supplier-id supplier-id*
                            :display-name display-name*})
                         nil)))
           row {:id (UUID/randomUUID)
                :supplier_id supplier-id*
                :display_name display-name*
                :normalized_key normalized
                :address address*
                :city_id city-id
                :created_at [:now]}
           sql-map {:insert-into :stores
                    :values [row]
                    :on-conflict [:supplier_id :normalized_key]
                    :do-update-set (cond-> {:display_name :excluded/display_name}
                                     (some? address*) (assoc :address :excluded/address
                                                        :city_id :excluded/city_id))
                    :returning [:*]}]
       (jdbc/execute-one!
         db
         (sql/format sql-map)
         {:builder-fn rs/as-unqualified-lower-maps})))))
