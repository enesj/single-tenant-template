(ns app.domain.backend.expenses.services.cities-backfill
  "Backfill helpers for setting `stores.city_id`."
  (:require
    [app.domain.backend.expenses.services.cities-normalize :as normalize]
    [app.domain.backend.expenses.services.cities-resolver :as resolver]
    [app.domain.backend.expenses.services.places-api :as places-api]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.time Instant]))

(defn backfill-store-cities!
  "Backfill stores.city_id using ZIP-only city resolution."
  [db & {:keys [dry-run? limit] :or {dry-run? false}}]
  (let [query {:select [:id :address :display_name]
               :from [:stores]
               :where [:and
                       [:is :city_id nil]
                       [:or
                        [:is-not :address nil]
                        [:is-not :display_name nil]]]}
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
                   (fn [acc {:keys [id address display_name]}]
                     (try
                       (let [;; Try address alone first (avoids token-count inflation from
                             ;; appending display_name when address already contains the city)
                             source-text (or (when-not (str/blank? address) address)
                                           (->> [address display_name]
                                             (remove str/blank?)
                                             (str/join " ")))
                             zip (normalize/extract-zip-from-text source-text)
                             city-id (or (resolver/resolve-city-id-from-text db source-text)
                                       (when (and (str/blank? address) (not (str/blank? display_name)))
                                         (resolver/resolve-city-id-from-text db display_name)))]
                         (if city-id
                           (do
                             (when-not dry-run?
                               (jdbc/execute-one!
                                 db
                                 (sql/format {:update :stores
                                              :set {:city_id city-id}
                                              :where [:= :id id]})))
                             (when dry-run?
                               (println (format "[%s] Store %s: would set city_id=%s (zip=%s)"
                                          (:updated acc)
                                          id
                                          city-id
                                          zip)))
                             (update acc :updated inc))
                           (do
                             (when dry-run?
                               (println (format "[SKIP] Store %s: unresolved ZIP (zip=%s)" id (or zip "none"))))
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

      (io/make-parents report-file)
      (spit report-file (pr-str report))

      (println (format "\n=== Summary ==="))
      (println (format "Scanned:  %d stores" (:scanned report)))
      (println (format "Updated:  %d stores" (:updated report)))
      (println (format "Skipped:  %d stores (unresolved ZIP)" (:skipped report)))
      (println (format "Failed:   %d stores" (:failed report)))
      (println (format "Report:   %s\n" report-file))

      (assoc report :report-file report-file))))

(defn backfill-store-cities-with-places!
  "Backfill stores.city_id and optionally create missing `cities` rows via Places.

  `places-cfg` may be either the raw `:places` sub-map from the app config or the
  fully-built map returned by `places-api/build-config` — the function normalises it
  either way so callers don't need to remember which form to pass."
  [db places-cfg & {:keys [dry-run? limit country user-region]
                    :or {dry-run? true
                         limit 25}}]
  (let [places-cfg* (places-api/build-config {:places places-cfg})
        country* (or (some-> country str str/trim not-empty) normalize/default-country)
        query {:select [:id :address :display_name]
               :from [:stores]
               :where [:and
                       [:is :city_id nil]
                       [:or
                        [:is-not :address nil]
                        [:is-not :display_name nil]]]
               :limit limit}
        rows (jdbc/execute!
               db
               (sql/format query)
               {:builder-fn rs/as-unqualified-lower-maps})
        total-scanned (count rows)
        timestamp (str (Instant/now))
        report-file (str "tmp/backfill-city-ids-with-places-" timestamp ".edn")
        opts {:places-cfg places-cfg*
              :user-region user-region}]

    (println (format "\n=== Backfill stores.city_id (with Places) ==="))
    (println (format "Mode: %s" (if dry-run? "DRY-RUN" "LIVE")))
    (println (format "Scanned: %d stores with city_id = NULL" total-scanned))
    (println (format "Limit: %s\n" (or limit "none")))

    (let [result (reduce
                   (fn [acc {:keys [id address display_name]}]
                     (try
                       (let [source-text (->> [display_name address]
                                           (remove str/blank?)
                                           (str/join "\n"))
                             city-id (resolver/resolve-city-id-from-text! db country* source-text opts)]
                         (if city-id
                           (do
                             (when-not dry-run?
                               (jdbc/execute-one!
                                 db
                                 (sql/format {:update :stores
                                              :set {:city_id city-id}
                                              :where [:= :id id]})))
                             (when dry-run?
                               (println (format "[OK] Store %s: would set city_id=%s" id city-id)))
                             (update acc :updated inc))
                           (do
                             (when dry-run?
                               (println (format "[SKIP] Store %s: unresolved" id)))
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
                   :limit (or limit :all)
                   :country country*)]

      (io/make-parents report-file)
      (spit report-file (pr-str report))

      (println (format "\n=== Summary ==="))
      (println (format "Scanned:  %d stores" (:scanned report)))
      (println (format "Updated:  %d stores" (:updated report)))
      (println (format "Skipped:  %d stores" (:skipped report)))
      (println (format "Failed:   %d stores" (:failed report)))
      (println (format "Report:   %s\n" report-file))

      (assoc report :report-file report-file))))
