#!/usr/bin/env bb

(ns scripts.bb.articles.map-aliases
  (:require
    [clojure.edn :as edn]
    [articles.db :as db]))

(defn- usage
  ([] (usage nil))
  ([msg]
   (when msg
     (binding [*out* *err*]
       (println msg)
       (println "")))
   (println "Map article_aliases rows to canonical articles by setting article_aliases.article_id.")
   (println "")
   (println "Usage:")
   (println "  bb scripts/bb/articles/map_aliases.clj [dev|test] (alias selector) (article selector) [--dry-run] [--allow-reassign] [--pretty]")
   (println "  bb scripts/bb/articles/map_aliases.clj [dev|test] --mappings-file /path/to/mappings.edn [--dry-run] [--allow-reassign] [--pretty]")
   (println "")
   (println "Single-mapping alias selector (choose exactly one mode):")
   (println "  --alias-id UUID                (repeat for many aliases to the same article)")
   (println "  --raw-label TEXT --supplier DISPLAY_NAME")
   (println "")
   (println "Single-mapping article selector (exactly one):")
   (println "  --article-id UUID")
   (println "  --article-key NORMALIZED_KEY")
   (println "  --canonical-name TEXT")
   (println "")
   (println "Batch mappings:")
   (println "  --mappings-file PATH           (EDN map/vector; each entry defines alias selector + article selector)")
   (println "")
   (println "Options:")
   (println "  --allow-reassign   Allow remapping aliases that already have article_id")
   (println "")
   (println "Output:")
   (println "  EDN: {:updated <n> :requested <n> :rows [...] :mapping_count <n> :mappings [...]}")
   (println "")
   (println "Notes:")
   (println "  - Default behavior only updates currently unmapped aliases (article_id IS NULL).")
   (println "  - For same article target, repeat --alias-id in one call.")
   (println "  - For many different targets, use --mappings-file in one call.")
   (println "  - This script uses `psql` (no JDBC).")))

(defn- parse-args
  [args]
  (loop [args args
         parsed {:dry-run? false
                 :allow-reassign? false
                 :pretty? false
                 :alias-ids []}]
    (let [[a b & more] args]
      (cond
        (nil? a) parsed

        (or (= a "--help") (= a "-h"))
        (do (usage) (System/exit 0))

        (= a "--dry-run")
        (recur (cons b more) (assoc parsed :dry-run? true))

        (= a "--allow-reassign")
        (recur (cons b more) (assoc parsed :allow-reassign? true))

        (= a "--pretty")
        (recur (cons b more) (assoc parsed :pretty? true))

        (= a "--mappings-file")
        (recur more (assoc parsed :mappings-file b))

        (= a "--alias-id")
        (recur more (update parsed :alias-ids conj b))

        (= a "--raw-label")
        (recur more (assoc parsed :raw-label b))

        (= a "--supplier")
        (recur more (assoc parsed :supplier-display-name b))

        (= a "--article-id")
        (recur more (assoc parsed :article-id b))

        (= a "--article-key")
        (recur more (assoc parsed :article-key b))

        (= a "--canonical-name")
        (recur more (assoc parsed :canonical-name b))

        :else
        (do
          (usage (str "Unknown arg: " a))
          (System/exit 1))))))

(defn- keywordize-k
  [k]
  (cond
    (keyword? k) (keyword (clojure.string/replace (name k) "_" "-"))
    (string? k) (keyword (clojure.string/replace k "_" "-"))
    :else k))

(defn- normalize-map-keys
  [m]
  (reduce-kv
    (fn [acc k v]
      (assoc acc (keywordize-k k) v))
    {}
    m))

(defn- normalize-alias-ids
  [value]
  (->> (cond
         (nil? value) []
         (sequential? value) value
         :else [value])
    (remove nil?)
    (map str)
    distinct
    vec))

(defn- read-mappings-file!
  [path]
  (let [raw (slurp path)
        parsed (try
                 (edn/read-string raw)
                 (catch Exception e
                   (throw (ex-info "Failed to parse --mappings-file EDN"
                            {:mappings-file path}
                            e))))]
    (cond
      (map? parsed)
      [(normalize-map-keys parsed)]

      (vector? parsed)
      (->> parsed
        (map-indexed
          (fn [idx row]
            (if (map? row)
              (normalize-map-keys row)
              (throw (ex-info "Each entry in --mappings-file must be a map"
                       {:mappings-file path
                        :mapping-index idx
                        :value row})))))
        vec)

      :else
      (throw (ex-info "--mappings-file must contain an EDN map or vector of maps"
               {:mappings-file path
                :value-type (type parsed)})))))

(defn- mapping-from-cli
  [opts]
  {:alias-ids (normalize-alias-ids (:alias-ids opts))
   :raw-label (:raw-label opts)
   :supplier-display-name (:supplier-display-name opts)
   :article-id (:article-id opts)
   :article-key (:article-key opts)
   :canonical-name (:canonical-name opts)
   :allow-reassign? (:allow-reassign? opts)})

(defn- normalize-mapping
  [mapping default-allow-reassign?]
  (let [base (normalize-map-keys mapping)
        alias-ids (normalize-alias-ids (if-let [alias-id (:alias-id base)]
                                         (conj (vec (:alias-ids base)) alias-id)
                                         (:alias-ids base)))
        allow-reassign? (if (contains? base :allow-reassign?)
                          (boolean (:allow-reassign? base))
                          default-allow-reassign?)
        supplier-display-name (or (:supplier-display-name base)
                                (:supplier base))]
    (-> base
      (assoc :alias-ids alias-ids
        :supplier-display-name supplier-display-name
        :allow-reassign? allow-reassign?)
      (dissoc :alias-id :supplier))))

(defn- resolve-supplier-id!
  [db supplier-display-name]
  (when supplier-display-name
    (:id
     (db/query1
       db
       (str
         "SELECT id\n"
         "FROM suppliers\n"
         "WHERE display_name = " (db/sql-literal supplier-display-name) "\n"
         "LIMIT 1")))))

(defn- resolve-article-id!
  [db {:keys [article-id article-key canonical-name]}]
  (cond
    article-id article-id

    article-key
    (:id
     (db/query1
       db
       (str
         "SELECT id\n"
         "FROM articles\n"
         "WHERE normalized_key = " (db/sql-literal article-key) "\n"
         "LIMIT 1")))

    canonical-name
    (:id
     (db/query1
       db
       (str
         "SELECT id\n"
         "FROM articles\n"
         "WHERE canonical_name = " (db/sql-literal canonical-name) "\n"
         "LIMIT 1")))

    :else
    nil))

(defn- update-by-alias-ids!
  [db alias-ids article-id allow-reassign?]
  (db/query
    db
    (str
      "UPDATE article_aliases\n"
      "SET article_id = " (db/sql-literal article-id) "\n"
      "WHERE id IN " (db/sql-in-list alias-ids) "\n"
      (when-not allow-reassign?
        "  AND article_id IS NULL\n")
      "RETURNING id, raw_label, supplier_id, article_id")))

(defn- update-by-raw-label-and-supplier!
  [db {:keys [raw-label supplier-id article-id allow-reassign?]}]
  (db/query
    db
    (str
      "UPDATE article_aliases\n"
      "SET article_id = " (db/sql-literal article-id) "\n"
      "WHERE raw_label = " (db/sql-literal raw-label) "\n"
      "  AND supplier_id = " (db/sql-literal supplier-id) "\n"
      (when-not allow-reassign?
        "  AND article_id IS NULL\n")
      "RETURNING id, raw_label, supplier_id, article_id")))

(defn- alias-selector-count
  [{:keys [alias-ids raw-label supplier-display-name]}]
  (+ (if (seq alias-ids) 1 0)
    (if (and raw-label supplier-display-name) 1 0)))

(defn- article-selector-count
  [{:keys [article-id article-key canonical-name]}]
  (+ (if article-id 1 0)
    (if article-key 1 0)
    (if canonical-name 1 0)))

(defn- mapping-summary
  [{:keys [alias-ids raw-label supplier-display-name article-id article-key canonical-name allow-reassign?]}]
  {:alias (if (seq alias-ids)
            {:alias_ids alias-ids}
            {:raw_label raw-label
             :supplier_display_name supplier-display-name})
   :article (cond-> {}
              article-id (assoc :article-id article-id)
              article-key (assoc :article-key article-key)
              canonical-name (assoc :canonical-name canonical-name))
   :allow_reassign allow-reassign?})

(defn- validate-mapping!
  [mapping context]
  (let [{:keys [alias-ids raw-label supplier-display-name]} mapping
        _ (when (or (and raw-label (not supplier-display-name))
                  (and supplier-display-name (not raw-label)))
            (throw (ex-info "--raw-label and --supplier must be provided together"
                     {:context context
                      :mapping (mapping-summary mapping)})))
        alias-selector-count* (alias-selector-count mapping)
        article-selector-count* (article-selector-count mapping)]
    (when (not= alias-selector-count* 1)
      (throw (ex-info "Choose exactly one alias selector: one or more --alias-id OR (--raw-label and --supplier)"
               {:context context
                :mapping (mapping-summary mapping)})))
    (when (not= article-selector-count* 1)
      (throw (ex-info "Choose exactly one article selector: --article-id, --article-key, or --canonical-name"
               {:context context
                :mapping (mapping-summary mapping)})))
    mapping))

(defn- collect-mappings!
  [opts]
  (if-let [path (:mappings-file opts)]
    (do
      (when (or (seq (:alias-ids opts))
              (:raw-label opts)
              (:supplier-display-name opts)
              (:article-id opts)
              (:article-key opts)
              (:canonical-name opts))
        (throw (ex-info "When --mappings-file is used, do not pass direct alias/article selectors"
                 {:selectors (select-keys opts [:alias-ids :raw-label :supplier-display-name :article-id :article-key :canonical-name])})))
      (->> (read-mappings-file! path)
        (map-indexed (fn [idx mapping]
                       (-> (normalize-mapping mapping (:allow-reassign? opts))
                         (validate-mapping! (str "mappings-file index " idx)))))
        vec))
    [(-> (mapping-from-cli opts)
       (validate-mapping! "cli options"))]))

(defn- execute-mapping!
  [db mapping]
  (let [{:keys [alias-ids raw-label supplier-display-name allow-reassign?]} mapping
        supplier-id (when (and (empty? alias-ids) supplier-display-name)
                      (resolve-supplier-id! db supplier-display-name))
        _ (when (and (empty? alias-ids) (nil? supplier-id))
            (throw (ex-info "Supplier not found" {:supplier-display-name supplier-display-name})))
        resolved-article-id (resolve-article-id! db mapping)
        _ (when-not resolved-article-id
            (throw (ex-info
                     "Article not found (provide --article-id, --article-key, or --canonical-name)"
                     {:mapping (mapping-summary mapping)})))
        rows (if (seq alias-ids)
               (update-by-alias-ids! db alias-ids resolved-article-id allow-reassign?)
               (update-by-raw-label-and-supplier!
                 db
                 {:raw-label raw-label
                  :supplier-id supplier-id
                  :article-id resolved-article-id
                  :allow-reassign? allow-reassign?}))
        updated-alias-id-set (set (map :id rows))
        not-updated-alias-ids (if (seq alias-ids)
                                (->> alias-ids
                                  (remove updated-alias-id-set)
                                  vec)
                                [])
        _ (when (empty? rows)
            (throw (ex-info (if allow-reassign?
                              "No alias rows updated"
                              "No alias rows updated. Alias may already be mapped; rerun with --allow-reassign to override.")
                     {:mapping (mapping-summary mapping)
                      :supplier-id supplier-id
                      :allow-reassign? allow-reassign?})))]
    (cond-> {:updated (count rows)
             :requested (if (seq alias-ids) (count alias-ids) 1)
             :article_id resolved-article-id
             :rows rows}
      (seq not-updated-alias-ids)
      (assoc :not_updated_alias_ids not-updated-alias-ids))))

(defn -main
  [& args]
  (let [{:keys [profile args]} (db/parse-profile args)
        opts (parse-args args)
        {:keys [dry-run? pretty?]} opts
        mappings (collect-mappings! opts)
        config (db/read-config profile)
        db (:database config)
        planned {:mapping_count (count mappings)
                 :mappings (mapv mapping-summary mappings)}]
    (if dry-run?
      (if pretty?
        (db/pprint-edn {:dry_run true :planned planned})
        (db/prn-edn {:dry_run true :planned planned}))
      (let [results (->> mappings
                      (mapv #(execute-mapping! db %)))
            not-updated (->> results
                          (mapcat :not_updated_alias_ids)
                          distinct
                          vec)
            result (cond-> {:updated (reduce + (map :updated results))
                            :requested (reduce + (map :requested results))
                            :mapping_count (count results)
                            :rows (vec (mapcat :rows results))}
                     (= 1 (count results))
                     (assoc :article_id (:article_id (first results)))

                     (> (count results) 1)
                     (assoc :mappings (mapv #(dissoc % :rows) results))

                     (seq not-updated)
                     (assoc :not_updated_alias_ids not-updated))]
        (if pretty?
          (db/pprint-edn result)
          (db/prn-edn result))))))

(apply -main *command-line-args*)
