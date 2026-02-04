(ns app.domain.backend.expenses.services.stores
  "Store (branch/location) services.

  Stores are owned by a supplier, and represent a specific physical location.

  This is intentionally kept separate from `suppliers` so that item aliasing can
  remain supplier-level (article_aliases.supplier_id) while expenses/receipts can
  carry store context."
  (:require
    [app.domain.backend.expenses.services.places-api :as places-api]
    [app.domain.backend.expenses.services.service-configs :as configs]
    [app.domain.backend.expenses.services.services-factory :as factory]
    [app.domain.backend.expenses.services.stores.matching :as store-matching]
    [app.domain.backend.expenses.services.stores.receipt-fingerprint :as receipt-fp]
    [app.shared.type-conversion :as type-conv]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.util UUID]))

(def config (configs/get-entity-config :store))

(def service (factory/build-entity-service config))

(def ^:private try-uuid type-conv/try-parse-uuid)

(defn get-store
  [db store-id]
  (jdbc/execute-one!
    db
    (sql/format {:select [:*]
                 :from [:stores]
                 :where [:= :id store-id]
                 :limit 1})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn find-by-supplier-and-normalized-key
  [db supplier-id normalized-key]
  (jdbc/execute-one!
    db
    (sql/format {:select [:*]
                 :from [:stores]
                 :where [:and
                         [:= :supplier_id supplier-id]
                         [:= :normalized_key normalized-key]]
                 :limit 1})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn find-by-supplier-and-place-id
  [db supplier-id place-id]
  (let [place-id* (some-> place-id str str/trim not-empty)]
    (when place-id*
      (jdbc/execute-one!
        db
        (sql/format {:select [:*]
                     :from [:stores]
                     :where [:and
                             [:= :supplier_id supplier-id]
                             [:= :place_id place-id*]]
                     :limit 1})
        {:builder-fn rs/as-unqualified-lower-maps}))))

(defn update-store!
  "Patch a store row and return the updated store.

  Accepts snake_case or kebab-case keys: display_name/display-name, address, place_id/place-id."
  [db store-id {:keys [display_name display-name address place_id place-id]}]
  (let [display-name* (some-> (or display_name display-name) str str/trim not-empty)
        address* (some-> address str str/trim not-empty)
        place-id* (some-> (or place_id place-id) str str/trim not-empty)
        set-map (cond-> {:updated_at [:now]}
                  (some? display-name*) (assoc :display_name display-name*)
                  (some? address*) (assoc :address address*)
                  (some? place-id*) (assoc :place_id place-id*))]
    (jdbc/execute-one!
      db
      (sql/format {:update :stores
                   :set set-map
                   :where [:= :id store-id]
                   :returning [:*]})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- merge-duplicate-stores-by-place-id!
  "Merge stores for a supplier that resolve to the same Places `place_id`.

  Keeps `keep-store-id`, rewires foreign keys, and deletes duplicates.

  This is safe to run during OCR ingestion."
  [db supplier-id place-id keep-store-id]
  (let [place-id* (some-> place-id str str/trim not-empty)]
    (when (and supplier-id (seq place-id*) keep-store-id)
      (let [rows (jdbc/execute!
                   db
                   (sql/format {:select [:id]
                                :from [:stores]
                                :where [:and
                                        [:= :supplier_id supplier-id]
                                        [:= :place_id place-id*]]})
                   {:builder-fn rs/as-unqualified-lower-maps})
            ids (->> rows (map :id) (remove nil?) distinct vec)
            keep-id keep-store-id
            drop-ids (->> ids (remove #(= % keep-id)) vec)]
        (when (seq drop-ids)
          ;; Update all dependent references first.
          (jdbc/execute!
            db
            (sql/format {:update :expenses
                         :set {:store_id keep-id}
                         :where [:in :store_id drop-ids]}))
          (jdbc/execute!
            db
            (sql/format {:update :store_aliases
                         :set {:store_id keep-id
                               :updated_at [:now]}
                         :where [:in :store_id drop-ids]}))
          ;; Finally remove duplicates.
          (jdbc/execute!
            db
            (sql/format {:delete-from :stores
                         :where [:in :id drop-ids]})))
        {:kept keep-id
         :merged (count drop-ids)}))))

(defn find-or-create-store!
  "Idempotently create a store row.

  Keys accepted (snake or kebab):
  - supplier_id / supplier-id (required)
  - display_name / display-name (required)
  - normalized_key / normalized-key (optional, used when :place_id is not present)
  - address (optional)
  - place_id / place-id (optional)

  Returns the store row."
  [db {:keys [supplier_id supplier-id
              display_name display-name
              normalized_key normalized-key
              address
              place_id place-id]}]
  (let [supplier-id* (try-uuid (or supplier_id supplier-id))
        display-name* (some-> (or display_name display-name) str str/trim not-empty)
        address* (some-> address str str/trim not-empty)
        place-id* (some-> (or place_id place-id) str str/trim not-empty)
        normalized* (some-> (or normalized_key normalized-key) str str/trim not-empty)]
    (when-not supplier-id*
      (throw (ex-info "supplier_id is required" {:status 400 :field :supplier_id})))
    (when-not display-name*
      (throw (ex-info "display_name is required" {:status 400 :field :display_name})))
    (let [normalized (cond
                       place-id*
                       (configs/normalize-store-key (str "place " place-id*))

                       normalized*
                       (configs/normalize-store-key normalized*)

                       :else
                       (configs/normalize-store-key (str display-name* " " (or address* ""))))
          row {:id (UUID/randomUUID)
               :supplier_id supplier-id*
               :display_name display-name*
               :normalized_key normalized
               :address address*
               :place_id place-id*
               :created_at [:now]
               :updated_at [:now]}
          sql-map {:insert-into :stores
                   :values [row]
                   :on-conflict [:supplier_id :normalized_key]
                   :do-update-set (cond-> {:display_name :excluded/display_name
                                           :updated_at [:now]}
                                    (some? address*) (assoc :address :excluded/address)
                                    (some? place-id*) (assoc :place_id :excluded/place_id))
                   :returning [:*]}]
      (jdbc/execute-one!
        db
        (sql/format sql-map)
        {:builder-fn rs/as-unqualified-lower-maps}))))

(defn- list-stores-for-supplier
  [db supplier-id]
  (jdbc/execute!
    db
    (sql/format {:select [:id :normalized_key :display_name :address :place_id]
                 :from [:stores]
                 :where [:= :supplier_id supplier-id]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- store-match-key
  [{:keys [normalized_key display_name address place_id] :as _store}]
  (let [normalized* (some-> normalized_key str str/trim not-empty)
        place-id* (some-> place_id str str/trim not-empty)
        address* (some-> address str str/trim not-empty)
        address-key (some-> address* configs/normalize-store-key)
        pj-or-places-key? (and (seq normalized*)
                            (or (str/starts-with? normalized* "pj-")
                              (str/starts-with? normalized* "place-")))
        display-name* (some-> display_name str str/trim not-empty)
        label (->> [display-name* address*]
                (remove str/blank?)
                (str/join " "))
        derived (some-> label not-empty configs/normalize-store-key)]
    (cond
      ;; For PJ/Places-keyed stores, prefer an address-derived key so we can still
      ;; match future OCR alias variants that don't include PJ/Places data.
      (and pj-or-places-key? (seq address-key))
      address-key

      ;; For normal stores, use the stored normalized_key as the primary match key.
      (seq normalized*)
      normalized*

      ;; Fallbacks.
      (seq address-key)
      address-key

      (and (seq place-id*) (seq derived))
      derived

      :else
      nil)))

(defn- store-candidates-for-supplier
  [db supplier-id]
  (mapv (fn [{:keys [id] :as row}]
          {:id id
           :key (store-match-key row)})
    (list-stores-for-supplier db supplier-id)))

(defn resolve-store-from-merchant
  "Best-effort store resolution from merchant data.

  `merchant` is expected to contain:
  - :name (supplier-level)
  - :store_name (branch label)
  - :address (store disambiguator)

  If Places is configured, we try to canonicalize stores by `place_id` so that
  multiple OCR variants (store_name/address noise) can converge on the same
  store row.

  Additionally:
  - when `:receipt-markdown` is present in opts, we try to derive a stable store
    key from the receipt header (e.g. `PJ` branch number) to avoid duplicates
  - when `:store-alias-normalized` is present in opts, treat it as the primary
    alias key for matching/heuristics (script-style)

  Returns {:store <row> :store-id <uuid> :store-alias-label <string>} or nil when
  it cannot infer a store label."
  ([db supplier-id merchant]
   (resolve-store-from-merchant db supplier-id merchant nil))
  ([db supplier-id {:keys [name store_name address] :as _merchant}
    {:keys [places-cfg user-region receipt-markdown
            store-alias-normalized store-alias-raw-label]
     :as _opts}]
   (let [supplier-id* (try-uuid supplier-id)
         supplier-name* (some-> name str str/trim not-empty)
         address* (some-> address str str/trim not-empty)
         store-name* (some-> store_name str str/trim not-empty)
         store-label (or address* store-name*)
         store-display-basic (or store-name* address* "Store")
         alias-key (or (some-> store-alias-normalized str str/trim not-empty)
                     (some-> store-label configs/normalize-store-key))
         pj-source (->> [receipt-markdown store-alias-raw-label store-name* address*]
                     (remove str/blank?)
                     (str/join "\n"))
         pj (some-> (receipt-fp/pj-number pj-source) str str/trim not-empty)
         pj-key (when pj
                  (configs/normalize-store-key (str "pj " pj)))
         store-key (or pj-key alias-key)
         places-enabled? (and (map? places-cfg)
                           (seq (:api-key places-cfg))
                           (not (seq pj-key))
                           (not (seq store-alias-normalized)))
         places-query (when (and places-enabled? (seq store-label))
                        (->> [supplier-name* store-name* address*]
                          (remove str/blank?)
                          (str/join " ")))
         places-opts {:region-code (or user-region (:region-code places-cfg))
                      :language-code (:language-code places-cfg)
                      :max-results (or (:max-results places-cfg) 3)
                      :location-bias (:location-bias places-cfg)
                      :field-mask "places.displayName,places.id,places.formattedAddress"}
         place (when (seq places-query)
                 (first (:places (places-api/search-text! places-cfg places-query places-opts))))
         place-id (some-> place :raw :id str str/trim not-empty)
         place-name (some-> place :name str str/trim not-empty)
         place-address (some-> place :raw :formattedAddress str str/trim not-empty)
         effective-alias-label (or address* place-address store-name*)
         effective-address (or place-address address*)
         effective-display (or store-name* place-name effective-address store-display-basic)]
     (when (and supplier-id* (seq effective-alias-label))
       (let [store (cond
                     (seq place-id)
                     (or
                       (when-let [existing (find-by-supplier-and-place-id db supplier-id* place-id)]
                         (update-store! db (:id existing)
                           {:display_name effective-display
                            :address effective-address
                            :place_id place-id}))

                       ;; If we already created a store without Places (script-style
                       ;; or PJ-key), attach place_id now.
                       (when-let [existing (and (seq store-key)
                                             (find-by-supplier-and-normalized-key db supplier-id* store-key))]
                         (update-store! db (:id existing)
                           {:display_name effective-display
                            :address effective-address
                            :place_id place-id}))

                       ;; Legacy key format (before script-style normalized_key).
                       (when-let [legacy (find-by-supplier-and-normalized-key
                                           db
                                           supplier-id*
                                           (configs/normalize-store-key (str store-display-basic " " (or address* ""))))]
                         (update-store! db (:id legacy)
                           {:display_name effective-display
                            :address effective-address
                            :place_id place-id}))

                       (find-or-create-store!
                         db
                         {:supplier_id supplier-id*
                          :display_name effective-display
                          :address effective-address
                          :place_id place-id}))

                     :else
                     (or
                       ;; Prefer stable store key (PJ branch number) when available.
                       (when (seq store-key)
                         (find-by-supplier-and-normalized-key db supplier-id* store-key))

                       ;; In case store-key came from PJ, also allow exact alias-key
                       ;; matches to reuse pre-existing stores.
                       (when (and (seq alias-key) (not= alias-key store-key))
                         (find-by-supplier-and-normalized-key db supplier-id* alias-key))

                       (when-let [store-id (receipt-fp/match-store-id db supplier-id* pj-source)]
                         (get-store db store-id))

                       (when-let [{:keys [store-id]} (store-matching/match-store
                                                       alias-key
                                                       (store-candidates-for-supplier db supplier-id*))]
                         (get-store db store-id))

                       (find-or-create-store!
                         db
                         {:supplier_id supplier-id*
                          :display_name (or store-display-basic
                                          (some-> store-alias-raw-label str str/trim not-empty)
                                          store-key
                                          alias-key
                                          "Store")
                          :address address*
                          :normalized_key store-key})))
             _ (when (and (seq place-id) (:id store))
                 (merge-duplicate-stores-by-place-id! db supplier-id* place-id (:id store)))]
         {:store store
          :store-id (:id store)
          :store-alias-label effective-alias-label})))))

(defn backfill-store-place-ids!
  "Backfill `stores.place_id` using Google Places, and merge duplicates by place_id.

  Intended for REPL/admin maintenance after enabling Places store canonicalization.

  opts:
  - :limit (default 200)
  - :region-code / :language-code

  Returns a summary map."
  ([db places-cfg]
   (backfill-store-place-ids! db places-cfg nil))
  ([db places-cfg {:keys [limit region-code language-code] :or {limit 200}}]
   (if-not (and (map? places-cfg) (seq (:api-key places-cfg)))
     {:scanned 0 :updated 0 :no-match 0 :failed 0 :reason :missing-places-api-key}
     (let [rows (jdbc/execute!
                  db
                  (sql/format {:select [[:st.id :store_id]
                                        [:st.supplier_id :supplier_id]
                                        [:st.display_name :store_display_name]
                                        [:st.address :store_address]
                                        [:sp.display_name :supplier_name]]
                               :from [[:stores :st]]
                               :join [[:suppliers :sp] [:= :sp.id :st.supplier_id]]
                               :where [:and
                                       [:is :st.place_id nil]
                                       [:is-not :st.address nil]
                                       [:<> :st.address ""]]
                               :limit limit})
                  {:builder-fn rs/as-unqualified-lower-maps})
           search-opts {:region-code (or region-code (:region-code places-cfg))
                        :language-code (or language-code (:language-code places-cfg))
                        :max-results (or (:max-results places-cfg) 3)
                        :location-bias (:location-bias places-cfg)
                        :field-mask "places.displayName,places.id,places.formattedAddress"}]
       (reduce
         (fn [acc {:keys [store_id supplier_id store_display_name store_address supplier_name]}]
           (try
             (let [query (->> [supplier_name store_display_name store_address]
                           (remove (fn [s] (str/blank? (str s))))
                           (map (fn [s] (str/trim (str s))))
                           (str/join " "))
                   place (when (seq query)
                           (first (:places (places-api/search-text! places-cfg query search-opts))))
                   place-id (some-> place :raw :id str str/trim not-empty)
                   place-address (some-> place :raw :formattedAddress str str/trim not-empty)]
               (if (seq place-id)
                 (do
                   (update-store!
                     db
                     store_id
                     {:place_id place-id
                      :address (or place-address store_address)
                      :display_name store_display_name})
                   (merge-duplicate-stores-by-place-id! db supplier_id place-id store_id)
                   (update acc :updated inc))
                 (update acc :no-match inc)))
             (catch Exception _
               (update acc :failed inc))))
         {:scanned (count rows)
          :updated 0
          :no-match 0
          :failed 0}
         rows)))))
