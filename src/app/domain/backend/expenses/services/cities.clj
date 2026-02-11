(ns app.domain.backend.expenses.services.cities
  "City normalization and lookup services.
  
  Provides idempotent city upsert and backfill operations for the cities table.
  Used to normalize store city data during Phase 2 of cities table migration."
  (:require
    [app.domain.backend.expenses.services.service-configs :as configs]
    [app.domain.backend.expenses.services.services-factory :as factory]
    [app.domain.backend.expenses.services.stores :as stores]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.text Normalizer Normalizer$Form]
    [java.time Instant]))

(defn normalize-city-key
  "Normalize city name to a stable key for uniqueness checks.
  
  Applies:
  - ASCII normalization (strip diacritics, handle Đ/đ)
  - Lowercase conversion
  - Whitespace trimming and collapsing
  
  Similar to normalize-store-key but tailored for city names.
  
  Examples:
    (normalize-city-key \"Sarajevo\") 
    ;; => \"sarajevo\"
    
    (normalize-city-key \"Banja Luka\") 
    ;; => \"banja luka\"
    
    (normalize-city-key \"MOSTAR\") 
    ;; => \"mostar\"
    
    (normalize-city-key \"  Tuzla  \") 
    ;; => \"tuzla\""
  [city-name]
  (when-let [name* (some-> city-name str str/trim not-empty)]
    (-> name*
      (Normalizer/normalize Normalizer$Form/NFD)
      (str/replace #"\p{M}+" "")
      (str/replace #"Đ" "D")
      (str/replace #"đ" "d")
      str/lower-case
      (str/replace #"\s+" " ")
      str/trim)))

(defn find-city-by-normalized-key
  "Query cities table by normalized_key.
  
  Returns city row (map with :id, :name, :normalized_key, timestamps) or nil.
  
  Example:
    (find-city-by-normalized-key db \"sarajevo\")
    ;; => {:id #uuid \"...\", :name \"Sarajevo\", :normalized_key \"sarajevo\", ...}"
  [db normalized-key]
  (when-let [key* (some-> normalized-key str str/trim not-empty)]
    (jdbc/execute-one!
      db
      (sql/format {:select [:*]
                   :from [:cities]
                   :where [:= :normalized_key key*]
                   :limit 1})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn ensure-city!
  "Upsert city by normalized_key, return city id.
  
  Computes normalized_key from city-name and uses INSERT ... ON CONFLICT
  to ensure idempotency. Safe for concurrent use.
  
  Algorithm:
  1. Normalize city-name to compute normalized_key
  2. INSERT with ON CONFLICT (normalized_key) DO UPDATE SET updated_at = now()
  3. Return city id
  
  Examples:
    (ensure-city! db \"Sarajevo\")
    ;; => #uuid \"...\"  (creates or returns existing)
    
    (ensure-city! db \"sarajevo\")
    ;; => #uuid \"...\"  (same id, normalized to \"sarajevo\")
    
    (ensure-city! db \"SARAJEVO\")
    ;; => #uuid \"...\"  (same id, normalized)"
  [db city-name]
  (when-let [name* (some-> city-name str str/trim not-empty)]
    (let [normalized (normalize-city-key name*)
          row (jdbc/execute-one!
                db
                (sql/format {:insert-into :cities
                             :values [{:name name*
                                       :normalized_key normalized
                                       :created_at [:now]
                                       :updated_at [:now]}]
                             :on-conflict [:normalized_key]
                             :do-update-set {:updated_at [:now]}
                             :returning [:id]})
                {:builder-fn rs/as-unqualified-lower-maps})]
      (:id row))))

(defn backfill-store-cities!
  "Idempotent backfill function to populate cities and set stores.city_id.
  
  Processes stores where city_id IS NULL and address/display_name/city is present.
  For each store:
  1. Extract city name using stores/extract-city-from-address (address, display-name)
  2. If extraction yields nil/blank, fallback to legacy stores.city value
  3. If city-name is non-blank:
     - Call ensure-city! to get/create city-id
     - Update stores.city_id = city-id
  
  Safe to re-run (only processes stores with NULL city_id).
  
  Options:
    :dry-run?  - When true, log what would be updated without writing (default: false)
    :limit     - Max rows to process per run (default: nil, process all)
  
  Returns:
    {:scanned N
     :updated M
     :skipped K
     :failed J
     :report-file \"tmp/backfill-city-ids-<timestamp>.edn\"}
  
  Examples:
    ;; Dry run first (see what will happen)
    (backfill-store-cities! db :dry-run? true :limit 10)
    
    ;; Run actual backfill on small sample
    (backfill-store-cities! db :limit 20)
    
    ;; Full backfill (all stores with NULL city_id)
    (backfill-store-cities! db)"
  [db & {:keys [dry-run? limit] :or {dry-run? false}}]
  (let [query {:select [:id :address :display_name :city]
               :from [:stores]
               :where [:and
                       [:is :city_id nil]
                       [:or
                        [:is-not :address nil]
                        [:is-not :display_name nil]
                        [:is-not :city nil]]]}
        query-with-limit (if limit
                           (assoc query :limit limit)
                           query)
        rows (jdbc/execute!
               db
               (sql/format query-with-limit)
               {:builder-fn rs/as-unqualified-lower-maps})
        total-scanned (count rows)
        timestamp (str (Instant/now))
        report-file (str "tmp/backfill-city-ids-" timestamp ".edn")]

    (println (format "\n=== Backfill stores.city_id ==="))
    (println (format "Mode: %s" (if dry-run? "DRY-RUN" "LIVE")))
    (println (format "Scanned: %d stores with city_id = NULL" total-scanned))
    (println (format "Limit: %s\n" (or limit "none")))

    (let [result (reduce
                   (fn [acc {:keys [id address display_name city]}]
                     (try
                       (let [;; Step 1: Extract city from address/display_name
                             extracted-city (stores/extract-city-from-address address display_name)
                             ;; Step 2: Fallback to legacy city column if extraction failed
                             city-name (or (some-> extracted-city str str/trim not-empty)
                                         (some-> city str str/trim not-empty))
                             ;; Determine source for logging
                             source (cond
                                      (and extracted-city (seq extracted-city)) :extracted
                                      (and city (seq city)) :legacy-fallback
                                      :else :none)]
                         (if (seq city-name)
                           (do
                             (when-not dry-run?
                               (let [city-id (ensure-city! db city-name)]
                                 (jdbc/execute-one!
                                   db
                                   (sql/format {:update :stores
                                                :set {:city_id city-id
                                                      :updated_at [:now]}
                                                :where [:= :id id]}))))
                             (when dry-run?
                               (println (format "[%s] Store %s: would set city=\"%s\" (source: %s)"
                                          (:updated acc)
                                          id
                                          city-name
                                          (name source))))
                             (update acc :updated inc))
                           (do
                             (when dry-run?
                               (println (format "[SKIP] Store %s: no city extractable" id)))
                             (update acc :skipped inc))))
                       (catch Exception e
                         (println (format "[ERROR] Store %s: %s" id (.getMessage e)))
                         (update acc :failed inc))))
                   {:scanned total-scanned
                    :updated 0
                    :skipped 0
                    :failed 0}
                   rows)
          report (assoc result
                   :timestamp timestamp
                   :dry-run? dry-run?
                   :limit (or limit :all))]

      ;; Save report to tmp/
      (io/make-parents report-file)
      (spit report-file (pr-str report))

      (println (format "\n=== Summary ==="))
      (println (format "Scanned:  %d stores" (:scanned report)))
      (println (format "Updated:  %d stores" (:updated report)))
      (println (format "Skipped:  %d stores (no city)" (:skipped report)))
      (println (format "Failed:   %d stores" (:failed report)))
      (println (format "Report:   %s\n" report-file))

      (assoc report :report-file report-file))))

;; ============================================================================
;; Service Registration
;; ============================================================================

(def config
  (configs/get-entity-config :city))

;; ============================================================================
;; Generated CRUD Operations
;; ============================================================================

(def service
  (factory/build-entity-service config))

(comment
  ;; REPL validation examples

  ;; 1. Test city normalization
  (normalize-city-key "Sarajevo")
  ;; => "sarajevo"

  (normalize-city-key "Banja Luka")
  ;; => "banja luka"

  (normalize-city-key "MOSTAR")
  ;; => "mostar"

  ;; 2. Test city upsert (requires db connection)
  (require '[system.state :as system-state])
  (def db (:database @system-state/state))

  (ensure-city! db "Sarajevo")
  ;; => #uuid "..."

  (ensure-city! db "sarajevo")
  ;; => #uuid "..." (same id)

  (ensure-city! db "SARAJEVO")
  ;; => #uuid "..." (same id)

  ;; 3. Test city lookup
  (find-city-by-normalized-key db "sarajevo")
  ;; => {:id #uuid "...", :name "Sarajevo", :normalized_key "sarajevo", ...}

  ;; 4. Dry-run backfill (safe, no writes)
  (backfill-store-cities! db :dry-run? true :limit 10)
  ;; => {:scanned 10, :updated N, :skipped M, :failed 0, :report-file "tmp/..."}

  ;; 5. Real backfill (small limit first)
  (backfill-store-cities! db :limit 20)
  ;; => {:scanned 20, :updated N, :skipped M, :failed 0, :report-file "tmp/..."}

  ;; 6. Full backfill (all stores with NULL city_id)
  (backfill-store-cities! db)
  ;; => {:scanned N, :updated M, :skipped K, :failed 0, :report-file "tmp/..."}

  :rcf)
