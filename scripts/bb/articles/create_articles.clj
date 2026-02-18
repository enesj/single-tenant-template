#!/usr/bin/env bb

(ns scripts.bb.articles.create-articles
  (:require
    [clojure.edn :as edn]
    [clojure.string :as str]
    [articles.db :as db]))

(def article-option-keys
  [:canonical-name
   :normalized-key
   :link
   :legacy-category
   :manufacturer-name
   :manufacturer-key
   :category-name
   :category-description
   :subcategory-name
   :subcategory-description])

(def update-flag-keys
  [:update-manufacturer-name?
   :update-category-description?
   :update-subcategory-description?])

(defn- usage
  ([] (usage nil))
  ([msg]
   (when msg
     (binding [*out* *err*]
       (println msg)
       (println "")))
   (println "Create (or fetch) one or many article rows, optionally ensuring taxonomy first.")
   (println "")
   (println "Usage:")
   (println "  bb scripts/bb/articles/create_articles.clj [dev|test] --canonical-name NAME [options]")
   (println "  bb scripts/bb/articles/create_articles.clj [dev|test] --article-edn '{:canonical-name \"...\" ...}' [--article-edn ...]")
   (println "  bb scripts/bb/articles/create_articles.clj [dev|test] --articles-file /path/to/articles.edn")
   (println "")
   (println "Single-article options:")
   (println "  --canonical-name NAME")
   (println "  --normalized-key KEY              (defaults to normalized(canonical-name))")
   (println "  --link URL                        (optional)")
   (println "  --legacy-category TEXT            (optional; fills articles.category)")
   (println "")
   (println "  --manufacturer-name NAME          (optional)")
   (println "  --manufacturer-key KEY            (optional; defaults to normalized(NAME) when NAME present)")
   (println "")
   (println "  --category-name NAME              (optional)")
   (println "  --category-description TEXT       (optional)")
   (println "  --subcategory-name NAME           (optional; requires --category-name)")
   (println "  --subcategory-description TEXT    (optional)")
   (println "")
   (println "Batch options:")
   (println "  --article-edn EDN_MAP             (repeatable)")
   (println "  --articles-file PATH              (EDN vector/map; keys match single-article options)")
   (println "")
   (println "Conflict behavior flags (applied to all batch entries unless overridden in EDN):")
   (println "  --update-manufacturer-name        (allow updating display_name on conflict)")
   (println "  --update-category-description     (allow updating description on conflict)")
   (println "  --update-subcategory-description  (allow updating description on conflict)")
   (println "")
   (println "Output:")
   (println "  --dry-run                         (no DB writes)")
   (println "  --pretty                          (pretty EDN output)")
   (println "")
   (println "Notes:")
   (println "  - Writes are deterministic and idempotent by normalized_key.")
   (println "  - This script intentionally does NOT update an existing article on conflict.")
   (println "  - Existing taxonomy values are preserved by default unless update flags are used.")
   (println "  - Batch mode is faster because one bb process handles multiple articles.")
   (println "  - This script uses `psql` (no JDBC).")))

(defn- parse-args
  [args]
  (loop [args args
         parsed {:dry-run? false
                 :pretty? false
                 :update-manufacturer-name? false
                 :update-category-description? false
                 :update-subcategory-description? false
                 :article-edn-values []}]
    (let [[a b & more] args]
      (cond
        (nil? a) parsed

        (or (= a "--help") (= a "-h"))
        (do (usage) (System/exit 0))

        (= a "--dry-run")
        (recur (cons b more) (assoc parsed :dry-run? true))

        (= a "--pretty")
        (recur (cons b more) (assoc parsed :pretty? true))

        (= a "--update-manufacturer-name")
        (recur (cons b more) (assoc parsed :update-manufacturer-name? true))

        (= a "--update-category-description")
        (recur (cons b more) (assoc parsed :update-category-description? true))

        (= a "--update-subcategory-description")
        (recur (cons b more) (assoc parsed :update-subcategory-description? true))

        (= a "--article-edn")
        (recur more (update parsed :article-edn-values conj b))

        (= a "--articles-file")
        (recur more (assoc parsed :articles-file b))

        (= a "--canonical-name")
        (recur more (assoc parsed :canonical-name b))

        (= a "--normalized-key")
        (recur more (assoc parsed :normalized-key b))

        (= a "--link")
        (recur more (assoc parsed :link b))

        (= a "--legacy-category")
        (recur more (assoc parsed :legacy-category b))

        (= a "--manufacturer-name")
        (recur more (assoc parsed :manufacturer-name b))

        (= a "--manufacturer-key")
        (recur more (assoc parsed :manufacturer-key b))

        (= a "--category-name")
        (recur more (assoc parsed :category-name b))

        (= a "--category-description")
        (recur more (assoc parsed :category-description b))

        (= a "--subcategory-name")
        (recur more (assoc parsed :subcategory-name b))

        (= a "--subcategory-description")
        (recur more (assoc parsed :subcategory-description b))

        :else
        (do
          (usage (str "Unknown arg: " a))
          (System/exit 1))))))

(defn- keywordize-k
  [k]
  (cond
    (keyword? k) (keyword (str/replace (name k) "_" "-"))
    (string? k) (keyword (str/replace k "_" "-"))
    :else k))

(defn- normalize-map-keys
  [m]
  (reduce-kv
    (fn [acc k v]
      (assoc acc (keywordize-k k) v))
    {}
    m))

(defn- parse-article-edn!
  [s context]
  (try
    (let [parsed (edn/read-string s)]
      (if (map? parsed)
        (normalize-map-keys parsed)
        (throw (ex-info "Article EDN must be a map"
                 {:context context
                  :value parsed}))))
    (catch Exception e
      (throw (ex-info "Failed to parse article EDN"
               {:context context
                :edn s}
               e)))))

(defn- read-articles-file!
  [path]
  (let [raw (slurp path)
        parsed (try
                 (edn/read-string raw)
                 (catch Exception e
                   (throw (ex-info "Failed to parse --articles-file EDN"
                            {:articles-file path}
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
              (throw (ex-info "Each entry in --articles-file must be a map"
                       {:articles-file path
                        :article-index idx
                        :value row})))))
        vec)

      :else
      (throw (ex-info "--articles-file must contain an EDN map or vector of maps"
               {:articles-file path
                :value-type (type parsed)})))))

(defn- compact-map
  [m]
  (into {} (remove (comp nil? val) m)))

(defn- single-article-from-cli
  [opts]
  (let [article (compact-map (select-keys opts article-option-keys))]
    (when (seq article) article)))

(defn- collect-article-inputs!
  [opts]
  (let [global-defaults (select-keys opts update-flag-keys)
        from-file (if-let [path (:articles-file opts)]
                    (read-articles-file! path)
                    [])
        from-flags (->> (:article-edn-values opts)
                     (map-indexed
                       (fn [idx value]
                         (parse-article-edn! value (str "--article-edn #" (inc idx)))))
                     vec)
        single (single-article-from-cli opts)]
    (->> (concat from-file from-flags (when single [single]))
      (mapv #(merge global-defaults %)))))

(defn- prepare-article!
  [idx opts]
  (let [canonical-name (some-> (:canonical-name opts) str str/trim)
        _ (when (str/blank? canonical-name)
            (throw (ex-info "Each article requires :canonical-name"
                     {:article-index idx
                      :article opts})))
        category-name (:category-name opts)
        subcategory-name (:subcategory-name opts)
        _ (when (and subcategory-name (not category-name))
            (throw (ex-info "--subcategory-name requires --category-name"
                     {:article-index idx
                      :article opts})))
        normalized-key (some-> (or (:normalized-key opts)
                                 (db/normalize-key canonical-name))
                         str
                         str/trim)
        _ (when (str/blank? normalized-key)
            (throw (ex-info "Article normalized key cannot be blank"
                     {:article-index idx
                      :canonical-name canonical-name
                      :normalized-key (:normalized-key opts)})))
        manufacturer-normalized-key (cond
                                      (:manufacturer-name opts)
                                      (some-> (or (:manufacturer-key opts)
                                                (db/normalize-key (:manufacturer-name opts)))
                                        str
                                        str/trim)

                                      (:manufacturer-key opts)
                                      (some-> (:manufacturer-key opts) str str/trim)

                                      :else
                                      nil)
        _ (when (and (or (:manufacturer-name opts) (:manufacturer-key opts))
                  (str/blank? manufacturer-normalized-key))
            (throw (ex-info "Manufacturer normalized key cannot be blank"
                     {:article-index idx
                      :manufacturer-name (:manufacturer-name opts)
                      :manufacturer-key (:manufacturer-key opts)})))
        normalized-opts (cond-> (assoc opts
                                  :canonical-name canonical-name
                                  :normalized-key normalized-key)
                          manufacturer-normalized-key
                          (assoc :manufacturer-key manufacturer-normalized-key))
        planned {:canonical_name canonical-name
                 :normalized_key normalized-key
                 :legacy_category (:legacy-category normalized-opts)
                 :link (:link normalized-opts)
                 :manufacturer (cond
                                 (:manufacturer-name normalized-opts)
                                 {:display_name (:manufacturer-name normalized-opts)
                                  :normalized_key manufacturer-normalized-key}

                                 (:manufacturer-key normalized-opts)
                                 {:normalized_key manufacturer-normalized-key}

                                 :else
                                 nil)
                 :category (when (:category-name normalized-opts)
                             {:name (:category-name normalized-opts)
                              :description (:category-description normalized-opts)})
                 :subcategory (when (:subcategory-name normalized-opts)
                                {:category_name (:category-name normalized-opts)
                                 :name (:subcategory-name normalized-opts)
                                 :description (:subcategory-description normalized-opts)})
                 :update_existing {:manufacturer_name (:update-manufacturer-name? normalized-opts)
                                   :category_description (:update-category-description? normalized-opts)
                                   :subcategory_description (:update-subcategory-description? normalized-opts)}}]
    {:opts normalized-opts
     :planned planned}))

(defn- cached-call!
  [cache cache-key f]
  (if (contains? @cache cache-key)
    (get @cache cache-key)
    (let [result (f)]
      (swap! cache assoc cache-key result)
      result)))

(defn- ensure-manufacturer!
  [db {:keys [manufacturer-name manufacturer-key update-manufacturer-name?]}]
  (cond
    manufacturer-name
    (let [k (some-> (or manufacturer-key (db/normalize-key manufacturer-name)) str str/trim)
          _ (when (str/blank? k)
              (throw (ex-info "Manufacturer normalized key cannot be blank"
                       {:manufacturer-name manufacturer-name
                        :manufacturer-key manufacturer-key})))
          id (db/uuid)
          inserted (db/query1
                     db
                     (str
                       "INSERT INTO manufacturers (id, display_name, normalized_key)\n"
                       "VALUES (" (db/sql-literal id) ", " (db/sql-literal manufacturer-name) ", " (db/sql-literal k) ")\n"
                       "ON CONFLICT (normalized_key)\n"
                       (if update-manufacturer-name?
                         "DO UPDATE SET display_name = EXCLUDED.display_name\n"
                         "DO NOTHING\n")
                       "RETURNING id, display_name, normalized_key"))]
      (or inserted
        (db/query1
          db
          (str
            "SELECT id, display_name, normalized_key\n"
            "FROM manufacturers\n"
            "WHERE normalized_key = " (db/sql-literal k) "\n"
            "LIMIT 1"))))

    manufacturer-key
    (let [k (some-> manufacturer-key str str/trim)
          _ (when (str/blank? k)
              (throw (ex-info "--manufacturer-key cannot be blank" {:manufacturer-key manufacturer-key})))]
      (db/query1
        db
        (str
          "SELECT id, display_name, normalized_key\n"
          "FROM manufacturers\n"
          "WHERE normalized_key = " (db/sql-literal k) "\n"
          "LIMIT 1")))

    :else
    nil))

(defn- ensure-category!
  [db {:keys [category-name category-description update-category-description?]}]
  (when category-name
    (let [id (db/uuid)
          inserted (db/query1
                     db
                     (str
                       "INSERT INTO categories (id, name, description)\n"
                       "VALUES (" (db/sql-literal id) ", " (db/sql-literal category-name) ", " (db/sql-literal category-description) ")\n"
                       "ON CONFLICT (name)\n"
                       (if update-category-description?
                         "DO UPDATE SET description = COALESCE(EXCLUDED.description, categories.description)\n"
                         "DO NOTHING\n")
                       "RETURNING id, name, description"))]
      (or inserted
        (db/query1
          db
          (str
            "SELECT id, name, description\n"
            "FROM categories\n"
            "WHERE name = " (db/sql-literal category-name) "\n"
            "LIMIT 1"))))))

(defn- ensure-subcategory!
  [db {:keys [category-name subcategory-name subcategory-description update-subcategory-description?]}]
  (when subcategory-name
    (let [category-id
          (:id
           (db/query1
             db
             (str
               "SELECT id\n"
               "FROM categories\n"
               "WHERE name = " (db/sql-literal category-name) "\n"
               "LIMIT 1")))
          _ (when-not category-id
              (throw
                (ex-info
                  "Category not found (did you pass --category-name / ensure it first)?"
                  {:category-name category-name})))
          id (db/uuid)
          inserted (db/query1
                     db
                     (str
                       "INSERT INTO subcategories (id, category_id, name, description)\n"
                       "VALUES (" (db/sql-literal id) ", " (db/sql-literal category-id) ", " (db/sql-literal subcategory-name) ", " (db/sql-literal subcategory-description) ")\n"
                       "ON CONFLICT (category_id, name)\n"
                       (if update-subcategory-description?
                         "DO UPDATE SET description = COALESCE(EXCLUDED.description, subcategories.description)\n"
                         "DO NOTHING\n")
                       "RETURNING id, category_id, name, description"))]
      (or inserted
        (db/query1
          db
          (str
            "SELECT id, category_id, name, description\n"
            "FROM subcategories\n"
            "WHERE category_id = " (db/sql-literal category-id) "\n"
            "  AND name = " (db/sql-literal subcategory-name) "\n"
            "LIMIT 1"))))))

(defn- find-article-by-key!
  [db normalized-key]
  (db/query1
    db
    (str
      "SELECT id, canonical_name, normalized_key, subcategory_id, link, manufacturer_id\n"
      "FROM articles\n"
      "WHERE normalized_key = " (db/sql-literal normalized-key) "\n"
      "LIMIT 1")))

(defn- create-article!
  [db {:keys [canonical-name normalized-key link manufacturer-id subcategory-id]}]
  (let [id (db/uuid)]
    (db/query1
      db
      (str
        "INSERT INTO articles (id, canonical_name, normalized_key, subcategory_id, link, manufacturer_id)\n"
        "VALUES ("
        (db/sql-literal id) ", "
        (db/sql-literal canonical-name) ", "
        (db/sql-literal normalized-key) ", "
        (db/sql-literal subcategory-id) ", "
        (db/sql-literal link) ", "
        (db/sql-literal manufacturer-id)
        ")\n"
        "ON CONFLICT (normalized_key) DO NOTHING\n"
        "RETURNING id, canonical_name, normalized_key, subcategory_id, link, manufacturer_id"))))

(defn- process-article!
  [db caches opts]
  (let [manufacturer-cache-key [(:manufacturer-name opts)
                                (:manufacturer-key opts)
                                (:update-manufacturer-name? opts)]
        category-cache-key [(:category-name opts)
                            (:category-description opts)
                            (:update-category-description? opts)]
        subcategory-cache-key [(:category-name opts)
                               (:subcategory-name opts)
                               (:subcategory-description opts)
                               (:update-subcategory-description? opts)]
        manufacturer (cached-call!
                       (:manufacturer caches)
                       manufacturer-cache-key
                       #(ensure-manufacturer! db opts))
        manufacturer-id (:id manufacturer)
        _ (when (and (:manufacturer-key opts) (nil? manufacturer-id))
            (throw (ex-info "Manufacturer not found for --manufacturer-key"
                     {:manufacturer-key (:manufacturer-key opts)})))
        category (cached-call!
                   (:category caches)
                   category-cache-key
                   #(ensure-category! db opts))
        subcategory (cached-call!
                      (:subcategory caches)
                      subcategory-cache-key
                      #(ensure-subcategory! db opts))
        subcategory-id (:id subcategory)
        inserted (create-article! db {:canonical-name (:canonical-name opts)
                                      :normalized-key (:normalized-key opts)
                                      :link (:link opts)
                                      :manufacturer-id manufacturer-id
                                      :subcategory-id subcategory-id})
        created? (some? inserted)
        article (or inserted (find-article-by-key! db (:normalized-key opts)))
        _ (when-not article
            (throw (ex-info "Article not found after insert/select"
                     {:normalized-key (:normalized-key opts)})))]
    {:created? created?
     :article article
     :manufacturer manufacturer
     :category category
     :subcategory subcategory}))

(defn -main
  [& args]
  (let [{:keys [profile args]} (db/parse-profile args)
        opts (parse-args args)
        {:keys [dry-run? pretty?]} opts
        article-inputs (collect-article-inputs! opts)
        _ (when (empty? article-inputs)
            (usage "Provide one article via CLI flags, one or more --article-edn, or --articles-file.")
            (System/exit 1))
        prepared (->> article-inputs
                   (map-indexed prepare-article!)
                   vec)
        planned (mapv :planned prepared)]
    (if dry-run?
      (if pretty?
        (db/pprint-edn {:dry_run true
                        :requested (count planned)
                        :planned planned})
        (db/prn-edn {:dry_run true
                     :requested (count planned)
                     :planned planned}))
      (let [config (db/read-config profile)
            db (:database config)
            caches {:manufacturer (atom {})
                    :category (atom {})
                    :subcategory (atom {})}
            results (->> prepared
                      (map-indexed
                        (fn [idx {:keys [opts]}]
                          (try
                            (assoc (process-article! db caches opts) :article_index idx)
                            (catch Exception e
                              (throw (ex-info "Failed processing article in batch"
                                       {:article-index idx
                                        :article (select-keys opts article-option-keys)}
                                       e))))))
                      vec)
            created (count (filter :created? results))
            existing (- (count results) created)
            result {:requested (count results)
                    :created created
                    :existing existing
                    :results results}]
        (if pretty?
          (db/pprint-edn result)
          (db/prn-edn result))))))

(apply -main *command-line-args*)
