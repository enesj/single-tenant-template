#!/usr/bin/env bb

(ns scripts.bb.articles.backfill-brand-rule-manufacturers
  (:require
    [articles.db :as db]
    [articles.research.heuristics :as heuristics]
    [articles.research.validation :as validation]
    [clojure.string :as str]))

(defn- usage
  ([] (usage nil))
  ([msg]
   (when msg
     (binding [*out* *err*]
       (println msg)
       (println "")))
   (println "Backfill article manufacturers from known-brand taxonomy rules.")
   (println "")
   (println "Loads brand knowledge from:")
   (println "  - scripts/bb/articles/taxonomy/brand-rules.edn")
   (println "  - scripts/bb/articles/taxonomy/brand-parent-mappings.edn")
   (println "  - scripts/bb/articles/taxonomy/self-named-brands.edn")
   (println "")
   (println "Usage:")
   (println "  bb scripts/bb/articles/backfill_brand_rule_manufacturers.clj [dev|test|prod] [options]")
   (println "")
   (println "Options:")
   (println "  --apply                 Perform DB updates (default: dry-run)")
   (println "  --pretty                Pretty-print EDN output")
   (println "  --help                  Show this help")
   (println "")
   (println "Notes:")
   (println "  - Only articles with NULL manufacturer_id are considered.")
   (println "  - Only taxonomy rules with a non-nil, sanitized manufacturer are used.")
   (println "  - Exact-word brand rules are only trusted when corroborated by curated brand maps")
   (println "    or when the exact word normalizes to the manufacturer key.")
   (println "  - Missing manufacturers are reported and skipped; they are not auto-created.")))

(defn- parse-args
  [args]
  (loop [args args
         parsed {:apply? false
                 :pretty? false}]
    (let [[a & more] args]
      (cond
        (nil? a) parsed

        (or (= a "--help") (= a "-h"))
        (do (usage) (System/exit 0))

        (= a "--apply")
        (recur more (assoc parsed :apply? true))

        (= a "--pretty")
        (recur more (assoc parsed :pretty? true))

        :else
        (do
          (usage (str "Unknown arg: " a))
          (System/exit 1))))))

(defn- prefix-pattern
  [brand]
  (str "(?i)^" (java.util.regex.Pattern/quote (str brand)) "\\b"))

(defn- normalized-token-string
  [s]
  (some-> s db/normalize-key (#(str "-" % "-"))))

(defn- normalized-brand-contained?
  [canonical-name brand-key]
  (let [canonical* (normalized-token-string canonical-name)
        brand* (some-> brand-key (#(str "-" % "-")))]
    (and canonical* brand* (str/includes? canonical* brand*))))

(defn- quoted-prefix-word
  [pattern]
  (when-let [[_ word] (re-matches #"(?i)\(\?i\)\^\\Q(.+?)\\E\\b" pattern)]
    word))

(defn- curated-brand->manufacturer-map
  []
  (let [parent-mappings (or (heuristics/load-taxonomy "brand-parent-mappings.edn") {})
        self-named-brands (or (heuristics/load-taxonomy "self-named-brands.edn") [])]
    (merge
      (->> parent-mappings
        (keep (fn [[brand manufacturer-name]]
                (when-let [clean-mfr (validation/sanitize-manufacturer manufacturer-name)]
                  [(db/normalize-key brand) (db/normalize-key clean-mfr)])))
        (into {}))
      (->> self-named-brands
        (keep (fn [brand]
                (when-let [clean-mfr (validation/sanitize-manufacturer brand)]
                  [(db/normalize-key brand) (db/normalize-key clean-mfr)])))
        (into {})))))

(defn- trusted-exact-word-rule?
  [pattern manufacturer-key curated-brand->manufacturer]
  (when-let [word (quoted-prefix-word pattern)]
    (let [word-key (db/normalize-key word)]
      (or (= word-key manufacturer-key)
        (= manufacturer-key (get curated-brand->manufacturer word-key))))))

(defn- sanitized-brand-rules
  [curated-brand->manufacturer]
  (->> (or (heuristics/load-taxonomy "brand-rules.edn") [])
    (remove #(or (heuristics/generic-pattern? %)
               (heuristics/bad-manufacturer? %)))
    (keep (fn [[pattern manufacturer-name _category-name _subcategory-name]]
            (when-let [clean-mfr (validation/sanitize-manufacturer manufacturer-name)]
              (let [manufacturer-key (db/normalize-key clean-mfr)]
                (when (or (nil? (quoted-prefix-word pattern))
                        (trusted-exact-word-rule? pattern manufacturer-key curated-brand->manufacturer))
                  {:pattern pattern
                   :manufacturer-name clean-mfr
                   :manufacturer-key manufacturer-key
                   :source :brand-rules})))))))

(defn- parent-mapping-rules
  []
  (->> (or (heuristics/load-taxonomy "brand-parent-mappings.edn") {})
    seq
    (sort-by (comp - count key))
    (keep (fn [[brand manufacturer-name]]
            (when-let [clean-mfr (validation/sanitize-manufacturer manufacturer-name)]
              (let [brand-key (db/normalize-key brand)]
                {:pattern (prefix-pattern brand)
                 :brand-key brand-key
                 :manufacturer-name clean-mfr
                 :manufacturer-key (db/normalize-key clean-mfr)
                 :source :brand-parent-mappings
                 :brand brand
                 :match-type :normalized-contains}))))))

(defn- self-named-brand-rules
  []
  (->> (or (heuristics/load-taxonomy "self-named-brands.edn") [])
    (sort-by (comp - count))
    (keep (fn [brand]
            (when-let [clean-mfr (validation/sanitize-manufacturer brand)]
              (let [brand-key (db/normalize-key brand)]
                {:pattern (prefix-pattern brand)
                 :brand-key brand-key
                 :manufacturer-name clean-mfr
                 :manufacturer-key (db/normalize-key clean-mfr)
                 :source :self-named-brands
                 :brand brand
                 :match-type :normalized-contains}))))))

(defn- dedupe-rules
  [rules]
  (reduce (fn [{:keys [seen rules]} rule]
            (if (contains? seen (:pattern rule))
              {:seen seen :rules rules}
              {:seen (conj seen (:pattern rule))
               :rules (conj rules rule)}))
    {:seen #{}
     :rules []}
    rules))

(defn- load-known-brand-rules
  []
  (let [curated-brand->manufacturer (curated-brand->manufacturer-map)]
    (:rules
     (dedupe-rules
       (concat (sanitized-brand-rules curated-brand->manufacturer)
         (parent-mapping-rules)
         (self-named-brand-rules))))))

(defn- fetch-null-manufacturer-articles
  [db]
  (db/query
    db
    (str
      "SELECT id, canonical_name, normalized_key\n"
      "FROM articles\n"
      "WHERE manufacturer_id IS NULL\n"
      "ORDER BY canonical_name")))

(defn- fetch-manufacturers-by-key
  [db]
  (->> (db/query
         db
         (str
           "SELECT id, display_name, normalized_key\n"
           "FROM manufacturers\n"
           "ORDER BY display_name"))
    (map (fn [manufacturer]
           [(:normalized_key manufacturer) manufacturer]))
    (into {})))

(defn- find-matching-rule
  [rules canonical-name]
  (let [matches-rule? (fn [{:keys [match-type pattern brand-key]}]
                        (case match-type
                          :normalized-contains (normalized-brand-contained? canonical-name brand-key)
                          (when (and canonical-name pattern)
                            (re-find (re-pattern pattern) canonical-name))))]
    (->> rules
      (filter matches-rule?)
      (sort-by (fn [{:keys [match-type brand-key pattern]}]
                 [(case match-type
                    :normalized-contains 0
                    1)
                  (- (count (or brand-key pattern "")))]))
      first)))

(defn- match-article
  [rules manufacturers-by-key {:keys [id canonical_name normalized_key]}]
  (when-let [{:keys [manufacturer-name manufacturer-key source pattern]} (find-matching-rule rules canonical_name)]
    (let [manufacturer (get manufacturers-by-key manufacturer-key)]
      {:id id
       :canonical-name canonical_name
       :normalized-key normalized_key
       :manufacturer-name manufacturer-name
       :manufacturer-key manufacturer-key
       :manufacturer-id (:id manufacturer)
       :manufacturer-exists? (boolean manufacturer)
       :matched-by source
       :pattern pattern})))

(defn- apply-updates!
  [db matches]
  (->> matches
    (filter :manufacturer-id)
    (group-by (juxt :manufacturer-id :manufacturer-name :manufacturer-key))
    (mapcat (fn [[[manufacturer-id manufacturer-name manufacturer-key] items]]
              (let [updated (db/query
                              db
                              (str
                                "UPDATE articles\n"
                                "SET manufacturer_id = " (db/sql-literal manufacturer-id) "\n"
                                "WHERE id IN " (db/sql-in-list (map :id items)) "\n"
                                "RETURNING id, canonical_name, normalized_key"))]
                (mapv (fn [row]
                        (assoc row
                          :manufacturer-name manufacturer-name
                          :manufacturer-key manufacturer-key))
                  updated))))
    vec))

(defn- summarize-by-manufacturer
  [matches]
  (->> matches
    (group-by (juxt :manufacturer-name :manufacturer-key :manufacturer-exists?))
    (map (fn [[[manufacturer-name manufacturer-key manufacturer-exists?] items]]
           {:manufacturer-name manufacturer-name
            :manufacturer-key manufacturer-key
            :manufacturer-exists? manufacturer-exists?
            :article-count (count items)
            :examples (->> items (map :canonical-name) (take 5) vec)}))
    (sort-by (juxt (comp - :article-count) :manufacturer-name))
    vec))

(defn -main
  [& args]
  (let [{:keys [profile args]} (db/parse-profile args)
        {:keys [apply? pretty?]} (parse-args args)
        config (db/read-config profile)
        db (:database config)
        rules (load-known-brand-rules)
        manufacturers-by-key (fetch-manufacturers-by-key db)
        null-manufacturer-articles (fetch-null-manufacturer-articles db)
        matches (->> null-manufacturer-articles
                  (keep #(match-article rules manufacturers-by-key %))
                  (sort-by :canonical-name)
                  vec)
        updatable-matches (filterv :manufacturer-id matches)
        missing-manufacturer-matches (filterv (comp not :manufacturer-id) matches)
        updated-rows (if apply?
                       (apply-updates! db updatable-matches)
                       [])
        result {:profile (name profile)
                :apply? apply?
                :null-manufacturer-articles (count null-manufacturer-articles)
                :known-brand-rules (count rules)
                :matched-articles (count matches)
                :updatable-articles (count updatable-matches)
                :updated-articles (count updated-rows)
                :missing-manufacturer-count (count missing-manufacturer-matches)
                :missing-manufacturers (->> missing-manufacturer-matches
                                         (map (fn [{:keys [manufacturer-name manufacturer-key]}]
                                                {:manufacturer-name manufacturer-name
                                                 :manufacturer-key manufacturer-key}))
                                         distinct
                                         (sort-by :manufacturer-name)
                                         vec)
                :matches-by-manufacturer (summarize-by-manufacturer matches)
                :planned-updates (mapv #(select-keys % [:canonical-name
                                                        :normalized-key
                                                        :manufacturer-name
                                                        :manufacturer-key
                                                        :matched-by
                                                        :pattern
                                                        :manufacturer-exists?])
                                   matches)}]
    (if pretty?
      (db/pprint-edn result)
      (db/prn-edn result))))

(apply -main *command-line-args*)