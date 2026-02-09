#!/usr/bin/env clj

(ns codex.skills.create-stores.scripts.create-stores
  "Create/link stores from unmapped store_aliases when supplier_id can be inferred unambiguously.

  Safety:
  - Default is dry-run (no DB writes)
  - Requires --apply to perform writes
  - Prompts for a confirmation phrase unless --yes is provided

  Usage:
    clj -M .codex/skills/create-stores/scripts/create_stores.clj [--dev|--test|dev|test] [--apply] [--yes] [--reset-stores] [--dedupe-existing] [--limit N] [--min-receipts N]"
  (:require
    [aero.core :as aero]
    [clojure.set :as set]
    [clojure.string :as str]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs])
  (:import
    [java.time Instant]))

(defn- usage
  ([] (usage nil))
  ([msg]
   (when msg
     (binding [*out* *err*]
       (println msg)
       (println "")))
   (println "Usage:")
   (println "  clj -M .codex/skills/create-stores/scripts/create_stores.clj [--dev|--test|dev|test] [--apply] [--yes] [--reset-stores] [--dedupe-existing] [--limit N] [--min-receipts N]")
   (println "")
   (println "Default is dry-run (no DB writes).")
   (println "")
   (println "Examples:")
   (println "  clj -M .codex/skills/create-stores/scripts/create_stores.clj --dev")
   (println "  clj -M .codex/skills/create-stores/scripts/create_stores.clj --dev --apply")
   (println "  clj -M .codex/skills/create-stores/scripts/create_stores.clj --dev --reset-stores")
   (println "  clj -M .codex/skills/create-stores/scripts/create_stores.clj --dev --dedupe-existing")
   (println "  clj -M .codex/skills/create-stores/scripts/create_stores.clj --dev --dedupe-existing --apply --yes")
   (println "  clj -M .codex/skills/create-stores/scripts/create_stores.clj test --apply --yes --limit 25")))

(defn- parse-long-safe
  [s]
  (try
    (Long/parseLong (str s))
    (catch Exception _ nil)))

(defn- parse-args
  [args]
  (loop [args args
         parsed {:profile :dev
                 :apply? false
                 :yes? false
                 :dedupe-existing? false
                 :reset-stores? false
                 :limit nil
                 :min-receipts 1}]
    (let [[a b & more] args]
      (cond
        (nil? a) parsed

        (#{"dev" "test"} a)
        (recur (cons b more) (assoc parsed :profile (keyword a)))

        (= a "--dev")
        (recur (cons b more) (assoc parsed :profile :dev))

        (= a "--test")
        (recur (cons b more) (assoc parsed :profile :test))

        (= a "--apply")
        (recur (cons b more) (assoc parsed :apply? true))

        (or (= a "--yes") (= a "--force"))
        (recur (cons b more) (assoc parsed :yes? true))

        (or (= a "--reset-stores") (= a "--reset"))
        (recur (cons b more) (assoc parsed :reset-stores? true))

        (or (= a "--dedupe-existing") (= a "--dedupe"))
        (recur (cons b more) (assoc parsed :dedupe-existing? true))

        (= a "--limit")
        (let [n (parse-long-safe b)]
          (when-not n
            (usage (str "Invalid --limit: " (pr-str b)))
            (System/exit 1))
          (recur more (assoc parsed :limit n)))

        (= a "--min-receipts")
        (let [n (parse-long-safe b)]
          (when-not n
            (usage (str "Invalid --min-receipts: " (pr-str b)))
            (System/exit 1))
          (recur more (assoc parsed :min-receipts n)))

        (or (= a "--help") (= a "-h"))
        (do (usage) (System/exit 0))

        :else
        (do
          (usage (str "Unknown arg: " a))
          (System/exit 1))))))

(defn- datasource-from-config
  [config]
  (let [{:keys [host port dbname user password]} (:database config)]
    (jdbc/get-datasource {:dbtype "postgresql"
                          :host host
                          :port port
                          :dbname dbname
                          :user user
                          :password password})))

(defn- stats
  [ds]
  (jdbc/execute-one!
    ds
    [(str
       "SELECT\n"
       "  (SELECT count(*) FROM stores) AS stores,\n"
       "  (SELECT count(*) FROM store_aliases) AS store_aliases,\n"
       "  (SELECT count(*) FROM store_aliases WHERE store_id IS NULL) AS store_aliases_unmapped,\n"
       "  (SELECT count(*) FROM store_aliases WHERE store_id IS NOT NULL) AS store_aliases_mapped,\n"
       "  (SELECT count(*) FROM receipts WHERE store_alias_id IS NOT NULL) AS receipts_with_store_alias,\n"
       "  (SELECT count(*) FROM supplier_aliases WHERE supplier_id IS NOT NULL) AS supplier_aliases_mapped")]
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- update-count
  [result]
  (or (get result :next.jdbc/update-count)
    (get result :update-count)
    0))

(defn- confirm!
  [{:keys [profile dbname action phrases]}]
  (let [phrases (set phrases)
        prompt (cond
                 (empty? phrases) "CONFIRM"
                 (= 1 (count phrases)) (first phrases)
                 :else (str/join " / " (sort phrases)))]
    (println (str "⚠️  DANGER: " action))
    (println (str "🎯 Target DB: " dbname " (" (name profile) ")"))
    (println "")
    (print (str "Type '" prompt "' to confirm: "))
    (flush)
    (contains? phrases (some-> (read-line) str/trim))))

(def ^:private fuzzy-similarity-threshold 0.92)
(def ^:private fuzzy-similarity-margin 0.03)

(def ^:private pj-name-sim-threshold 0.8)

(defn- normalize-for-match
  [s]
  (-> (or s "")
    str/trim
    str/lower-case
    (str/replace #"[^a-z0-9]+" "-")
    (str/replace #"-{2,}" "-")
    (str/replace #"(^-)|(-$)" "")))

(defn- loose-key
  [s]
  (-> (normalize-for-match s)
    (str/replace #"^ul-" "")
    (str/replace #"-broj-" "-br-")
    (str/replace #"-\d{4,5}(?=-|$)" "")
    (str/replace #"-{2,}" "-")
    (str/replace #"(^-)|(-$)" "")))

(defn- store-number
  "Extract store number from a loose key (e.g. ...-br-12-...)."
  [loose]
  (second (re-find #"(?:^|-)br-(\d{1,4})(?:-|$)" (str loose))))

(defn- city-from-loose
  "Best-effort: detect city suffixes to avoid cross-city fuzzy merges."
  [k]
  (let [k (str k)]
    (cond
      (re-find #"(?:^|-)sarajevo(?:-centar|-novi-grad)?$" k) "sarajevo"
      (re-find #"(?:^|-)mostar(?:-centar)?$" k) "mostar"
      :else nil)))

(defn- core-key
  "A more-stable key for fuzzy matching: drop zip codes (already done in `loose-key`)
   and strip common trailing city/district suffixes without stripping store-name tokens
   like `shopping-centar`."
  [k]
  (let [k (loose-key k)]
    (-> k
      (str/replace #"-sarajevo-centar$" "")
      (str/replace #"-sarajevo-novi-grad$" "")
      (str/replace #"-sarajevo$" "")
      (str/replace #"-mostar$" "")
      (str/replace #"-{2,}" "-")
      (str/replace #"(^-)|(-$)" ""))))

(defn- first-line-matching
  [s re]
  (some (fn [line]
          (when (re-find re line) line))
    (str/split-lines (or s ""))))

(defn- parse-pj
  [markdown]
  (some-> (re-find #"(?i)\bPJ\b\s*(?:br\.|broj)?\s*\.?\s*(\d{1,4})" (or markdown ""))
    second))

(defn- parse-pj-name
  [markdown]
  (when-let [line (first-line-matching markdown #"(?i)\bPJ\b")]
    (let [after-num (-> line
                      (str/replace #"(?i)^.*\bPJ\b\s*(?:br\.|broj)?\s*\.?\s*\d{1,4}\s*" "")
                      (str/replace #"\"" "")
                      (str/replace #",+" " ")
                      (str/replace #"\s+" " ")
                      str/trim)
          no-city (-> after-num
                    (str/replace #"(?i)\s*(sarajevo|mostar)\b.*$" "")
                    str/trim)
          normalized (some-> no-city not-empty normalize-for-match)]
      normalized)))

(defn- parse-bfm
  [markdown]
  (some-> (re-find #"(?i)\b[IT]BFM:\s*([A-Z0-9]+)" (or markdown ""))
    second))

(defn- receipt->fp
  [{:keys [parsed_markdown store_guess]}]
  (let [md (or parsed_markdown "")
        pj (parse-pj md)
        pj-name (parse-pj-name md)
        bfm (parse-bfm md)
        guess (some-> store_guess normalize-for-match not-empty)]
    {:pj (cond-> #{} pj (conj pj))
     :pj_name (cond-> #{} pj-name (conj pj-name))
     :bfm (cond-> #{} bfm (conj bfm))
     :store_guess (cond-> #{} guess (conj guess))}))

(defn- merge-fp
  [a b]
  (merge-with set/union (or a {}) (or b {})))

(defn- fp-intersects?
  [a b k]
  (boolean (seq (set/intersection (or (get a k) #{}) (or (get b k) #{})))))

(defn- select-receipts-for-store-alias
  [ds store-alias-id]
  (jdbc/execute!
    ds
    ["SELECT parsed_markdown, store_guess FROM receipts WHERE store_alias_id = ? AND parsed_markdown IS NOT NULL" store-alias-id]
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- store-alias-fp
  [ds store-alias-id]
  (reduce merge-fp {} (map receipt->fp (select-receipts-for-store-alias ds store-alias-id))))

(defn- select-store-receipts-for-supplier
  [ds supplier-id]
  (jdbc/execute!
    ds
    [(str
       "SELECT\n"
       "  s.id AS store_id,\n"
       "  r.parsed_markdown,\n"
       "  r.store_guess\n"
       "FROM stores s\n"
       "JOIN store_aliases sa ON sa.store_id = s.id\n"
       "JOIN receipts r ON r.store_alias_id = sa.id\n"
       "WHERE s.supplier_id = ?\n"
       "  AND r.parsed_markdown IS NOT NULL")
     supplier-id]
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- store-fp-by-id-for-supplier
  [ds supplier-id]
  (reduce
    (fn [acc {:keys [store_id] :as row}]
      (update acc store_id merge-fp (receipt->fp row)))
    {}
    (select-store-receipts-for-supplier ds supplier-id)))

(declare select-stores-for-supplier similarity)

(defn- build-store-candidates-for-supplier
  [ds supplier-id]
  (let [stores (select-stores-for-supplier ds supplier-id)
        fp-by-id (store-fp-by-id-for-supplier ds supplier-id)]
    (mapv (fn [{:keys [id] :as s}]
            (assoc s :fp (get fp-by-id id {})))
      stores)))

(defn- select-supplier-ids-with-stores
  [ds]
  (mapv :supplier_id
    (jdbc/execute!
      ds
      ["SELECT DISTINCT supplier_id FROM stores ORDER BY supplier_id"]
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- store-stats-for-supplier
  [ds supplier-id]
  (jdbc/execute!
    ds
    [(str
       "SELECT\n"
       "  s.id AS store_id,\n"
       "  count(DISTINCT sa.id) AS aliases_cnt,\n"
       "  count(r.id) AS receipts_cnt\n"
       "FROM stores s\n"
       "LEFT JOIN store_aliases sa ON sa.store_id = s.id\n"
       "LEFT JOIN receipts r ON r.store_alias_id = sa.id\n"
       "WHERE s.supplier_id = ?\n"
       "GROUP BY s.id")
     supplier-id]
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- only-one
  [xs]
  (when (= 1 (count xs))
    (first xs)))

(defn- store-pj
  [store]
  (only-one (get-in store [:fp :pj])))

(defn- store-pj-name
  [store]
  (only-one (get-in store [:fp :pj_name])))

(defn- choose-canonical-store
  [stats-by-id stores]
  (first
    (sort-by
      (fn [{:keys [id normalized_key]}]
        (let [{:keys [receipts_cnt aliases_cnt]} (get stats-by-id id)]
          [(- (long (or receipts_cnt 0)))
           (- (long (or aliases_cnt 0)))
           (or normalized_key "")]))
      stores)))

(defn- dedupe-plan-for-supplier
  "Return vector of {:pj .. :canonical .. :duplicates [...] :reason ...} to merge.
   Conservative: only groups stores by single PJ number; skips when PJ names clearly disagree."
  [ds supplier-id]
  (let [stores (build-store-candidates-for-supplier ds supplier-id)
        stats-by-id (into {}
                      (map (fn [{:keys [store_id] :as row}] [store_id row])
                        (store-stats-for-supplier ds supplier-id)))
        with-pj (keep (fn [s]
                        (when-let [pj (store-pj s)]
                          (assoc s :pj pj)))
                  stores)
        groups (->> with-pj
                 (group-by :pj)
                 (filter (fn [[_ ss]] (> (count ss) 1))))]
    (->> groups
      (map (fn [[pj ss]]
             (let [names (->> ss (map store-pj-name) (remove nil?) distinct)
                   names-ok? (or (< (count names) 2)
                               (every? (fn [n] (>= (double (similarity (first names) n)) 0.8))
                                 (rest names)))]
               (when names-ok?
                 (let [canonical (choose-canonical-store stats-by-id ss)
                       duplicates (->> ss
                                    (remove (fn [s] (= (:id s) (:id canonical))))
                                    (map :id)
                                    vec)]
                   {:pj pj
                    :canonical (:id canonical)
                    :duplicates duplicates
                    :reason (if (seq names) "pj+name" "pj-only")})))))
      (remove nil?)
      vec)))

(defn- merge-stores!
  [tx canonical-id duplicate-id]
  (jdbc/execute-one! tx ["UPDATE store_aliases SET store_id = ? WHERE store_id = ?" canonical-id duplicate-id])
  (jdbc/execute-one! tx ["UPDATE expenses SET store_id = ? WHERE store_id = ?" canonical-id duplicate-id])
  (jdbc/execute-one! tx ["DELETE FROM stores WHERE id = ?" duplicate-id]))

(defn- dedupe-existing-stores
  "When apply? is false, returns a plan only. When true, applies merges in tx.

   Returns {:planned n :merged n :deleted n :skipped n}."
  [ds {:keys [apply?]}]
  (let [supplier-ids (select-supplier-ids-with-stores ds)
        plans (mapcat (fn [supplier-id]
                        (map (fn [p] (assoc p :supplier_id supplier-id))
                          (dedupe-plan-for-supplier ds supplier-id)))
                supplier-ids)]
    (if-not apply?
      {:planned (count plans)
       :plan (vec plans)}
      (do
        (doseq [{:keys [canonical duplicates]} plans
                duplicate-id duplicates]
          (merge-stores! ds canonical duplicate-id))
        {:planned (count plans)
         :merged (reduce + 0 (map (comp count :duplicates) plans))
         :deleted (reduce + 0 (map (comp count :duplicates) plans))}))))

(defn- levenshtein
  "Classic Levenshtein edit distance (inserts/deletes/substitutions)."
  [a b]
  (let [^String a (str a)
        ^String b (str b)]
    (cond
      (= a b) 0
      (zero? (.length a)) (.length b)
      (zero? (.length b)) (.length a)
      :else
      (let [alen (.length a)
            blen (.length b)
            prev (int-array (inc blen))
            curr (int-array (inc blen))]
        (dotimes [j (inc blen)]
          (aset-int prev j j))
        (dotimes [i alen]
          (aset-int curr 0 (inc i))
          (dotimes [j blen]
            (let [cost (if (= (.charAt a i) (.charAt b j)) 0 1)
                  deletion (inc (aget prev (inc j)))
                  insertion (inc (aget curr j))
                  substitution (+ (aget prev j) cost)]
              (aset-int curr (inc j) (min deletion insertion substitution))))
          (System/arraycopy curr 0 prev 0 (alength curr)))
        (aget prev blen)))))

(defn- similarity
  "Return similarity in [0..1], based on normalized Levenshtein distance."
  [a b]
  (let [a (normalize-for-match a)
        b (normalize-for-match b)
        maxlen (max (.length ^String a) (.length ^String b))]
    (if (zero? maxlen)
      1.0
      (- 1.0 (/ (double (levenshtein a b)) (double maxlen))))))

(defn- match-store
  "Given a target normalized_key + receipt fingerprint and store candidates
   ({:id .. :normalized_key .. :fp ..}), return a match map:

     {:id .. :match :exact|:receipt|:loose|:fuzzy :score n}

   Safety:
   - Prefer exact matches.
   - Prefer receipt-based matches when unambiguous (PJ + optional name).
   - Allow a single loose-key match (zip code removed, common tokens normalized).
   - Allow a high-confidence fuzzy match only when unambiguous.
   - Do not fuzzy-match when store numbers conflict (br-1 vs br-2).
   - Avoid cross-city fuzzy merges when both sides have detectable cities."
  [target-key target-fp candidates]
  (let [target-key (some-> target-key str/trim)
        exact (some (fn [{:keys [id normalized_key]}]
                      (when (= normalized_key target-key)
                        {:id id :match :exact :score 1.0}))
                candidates)]
    (or exact
      (when (seq candidates)
        (let [pj-matches
              (->> candidates
                (filter (fn [{:keys [fp]}]
                          (fp-intersects? target-fp fp :pj))))

              receipt-matches
              (->> pj-matches
                (filter (fn [{:keys [fp]}]
                          (let [target-names (:pj_name target-fp)
                                cand-names (:pj_name fp)
                                sim-ok? (when (and (seq target-names) (seq cand-names))
                                          (>= (apply max 0.0 (for [t target-names
                                                                   c cand-names]
                                                               (double (similarity t c))))
                                            pj-name-sim-threshold))
                                name-ok (or (empty? target-names)
                                          (empty? cand-names)
                                          (fp-intersects? target-fp fp :pj_name)
                                          sim-ok?)]
                            name-ok))))

              receipt-match
              (cond
                ;; PJ is effectively a branch identifier; if it maps to exactly one
                ;; candidate, match it even if OCR mangled the PJ-name.
                (= 1 (count pj-matches))
                (first pj-matches)

                (= 1 (count receipt-matches))
                (first receipt-matches)

                :else
                nil)]
          (or (when receipt-match
                {:id (:id receipt-match) :match :receipt :score 1.0})
            (let [target-loose (loose-key target-key)
                  target-core (core-key target-key)
                  target-city (city-from-loose target-loose)
                  target-num (store-number target-loose)
                  loose-matches (filter (fn [{:keys [normalized_key]}]
                                          (= (loose-key normalized_key) target-loose))
                                  candidates)]
              (cond
                (= 1 (count loose-matches))
                {:id (:id (first loose-matches)) :match :loose :score 1.0}

                (> (count loose-matches) 1)
                nil

                :else
                (let [scored (->> candidates
                               (map (fn [{:keys [id normalized_key]}]
                                      (let [cand-loose (loose-key normalized_key)
                                            cand-core (core-key normalized_key)
                                            cand-city (city-from-loose cand-loose)
                                            cand-num (store-number cand-loose)
                                            bad-city? (and target-city cand-city (not= target-city cand-city))
                                            bad-num? (and target-num cand-num (not= target-num cand-num))
                                            score (cond
                                                    bad-city? 0.0
                                                    bad-num? 0.0
                                                    :else (similarity target-core cand-core))
                                            threshold (if (and target-num cand-num)
                                                        fuzzy-similarity-threshold
                                                        0.98)]
                                        {:id id
                                         :normalized_key normalized_key
                                         :score score
                                         :threshold threshold})))
                               (sort-by (comp - :score)))
                      best (first scored)
                      second-best (second scored)
                      best-score (double (or (:score best) 0.0))
                      best-threshold (double (or (:threshold best) 1.0))
                      second-score (double (or (:score second-best) 0.0))
                      margin (- best-score second-score)]
                  (when (and best
                          (>= best-score best-threshold)
                          (or (nil? second-best)
                            (>= margin fuzzy-similarity-margin)))
                    {:id (:id best)
                     :match :fuzzy
                     :score best-score
                     :margin margin}))))))))))

(defn- select-unmapped-aliases
  "Select store_aliases to process.

   By default only includes unmapped aliases (store_id IS NULL). When include-mapped?
   is true (used for --reset-stores planning), includes all aliases."
  [ds {:keys [limit include-mapped?]}]
  (let [sql (str
              "SELECT id, raw_label, raw_label_normalized\n"
              "FROM store_aliases\n"
              (when-not include-mapped? "WHERE store_id IS NULL\n")
              "ORDER BY updated_at DESC, created_at DESC"
              (when limit (str "\nLIMIT " (long limit))))]
    (jdbc/execute! ds [sql] {:builder-fn rs/as-unqualified-lower-maps})))

(defn- supplier-candidates
  "Per store_alias, count supplier candidates seen on receipts.

   By default only considers unmapped store_aliases. When include-mapped?
   is true (used for --reset-stores planning), includes all aliases."
  [ds {:keys [include-mapped?] :or {include-mapped? false}}]
  (let [where (if include-mapped?
                "WHERE ssa.supplier_id IS NOT NULL\n"
                (str
                  "WHERE sa.store_id IS NULL\n"
                  "  AND ssa.supplier_id IS NOT NULL\n"))]
    (jdbc/execute!
      ds
      [(str
         "SELECT\n"
         "  sa.id AS store_alias_id,\n"
         "  ssa.supplier_id AS supplier_id,\n"
         "  count(*) AS receipts_cnt\n"
         "FROM store_aliases sa\n"
         "JOIN receipts r ON r.store_alias_id = sa.id\n"
         "JOIN supplier_aliases ssa ON ssa.id = r.supplier_alias_id\n"
         where
         "GROUP BY sa.id, ssa.supplier_id")]
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn- choose-suppliers
  "Return a map store_alias_id -> {:supplier_id uuid :receipts_cnt n} for unambiguous best candidates.
   Return a set of ambiguous store_alias_ids (ties) as :ambiguous."
  [{:keys [min-receipts]} rows]
  (let [grouped (group-by :store_alias_id rows)]
    (reduce-kv
      (fn [{:keys [chosen ambiguous] :as acc} alias-id candidates]
        (let [sorted (sort-by (comp - :receipts_cnt) candidates)
              best (first sorted)
              second-best (second sorted)
              best-cnt (long (or (:receipts_cnt best) 0))
              second-cnt (long (or (:receipts_cnt second-best) 0))]
          (cond
            (< best-cnt (long min-receipts))
            acc

            (and second-best (= best-cnt second-cnt))
            (assoc acc :ambiguous (conj ambiguous alias-id))

            :else
            (assoc acc :chosen (assoc chosen alias-id {:supplier_id (:supplier_id best)
                                                       :receipts_cnt best-cnt})))))
      {:chosen {} :ambiguous #{}}
      grouped)))

(defn- select-stores-for-supplier
  [ds supplier-id]
  (jdbc/execute!
    ds
    ["SELECT id, normalized_key FROM stores WHERE supplier_id = ?" supplier-id]
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- find-store-id
  [ds supplier-id normalized-key]
  (:id
   (jdbc/execute-one!
     ds
     ["SELECT id FROM stores WHERE supplier_id = ? AND normalized_key = ? LIMIT 1"
      supplier-id
      normalized-key]
     {:builder-fn rs/as-unqualified-lower-maps})))

(defn- insert-store!
  "Insert a store (id generated in DB). Returns {:store_id .. :created? bool}."
  [ds {:keys [supplier-id display-name normalized-key]}]
  (let [inserted (:id
                  (jdbc/execute-one!
                    ds
                    [(str
                       "INSERT INTO stores (id, supplier_id, display_name, normalized_key, address, place_id)\n"
                       "VALUES (gen_random_uuid(), ?, ?, ?, NULL, NULL)\n"
                       "ON CONFLICT (supplier_id, normalized_key) DO NOTHING\n"
                       "RETURNING id")
                     supplier-id
                     display-name
                     normalized-key]
                    {:builder-fn rs/as-unqualified-lower-maps}))]
    (if inserted
      {:store_id inserted :created? true}
      {:store_id (find-store-id ds supplier-id normalized-key) :created? false})))

(defn- map-alias!
  [ds store-alias-id store-id]
  (jdbc/execute-one!
    ds
    ["UPDATE store_aliases SET store_id = ? WHERE id = ? AND store_id IS NULL"
     store-id
     store-alias-id]))

(defn- reset-stores!
  "Reset store mappings by deleting ALL stores and clearing any references.

   Safety: Only call this when the user explicitly requested `--reset-stores`.

   Returns {:aliases_unmapped n :expenses_unmapped n :stores_deleted n}."
  [tx]
  {:aliases_unmapped (update-count (jdbc/execute-one! tx ["UPDATE store_aliases SET store_id = NULL WHERE store_id IS NOT NULL"]))
   :expenses_unmapped (update-count (jdbc/execute-one! tx ["UPDATE expenses SET store_id = NULL WHERE store_id IS NOT NULL"]))
   :stores_deleted (update-count (jdbc/execute-one! tx ["DELETE FROM stores"]))})

(defn -main
  [& args]
  (let [{:keys [profile apply? yes? dedupe-existing? reset-stores? limit min-receipts]} (parse-args args)
        config (aero/read-config "config/base.edn" {:profile profile})
        ds (datasource-from-config config)
        dbname (get-in config [:database :dbname])
        before (stats ds)
        aliases (select-unmapped-aliases ds {:limit limit :include-mapped? reset-stores?})
        cand-rows (supplier-candidates ds {:include-mapped? reset-stores?})
        {:keys [chosen ambiguous]} (choose-suppliers {:min-receipts min-receipts} cand-rows)
        supplier-ids (->> chosen vals (map :supplier_id) distinct)
        stores-by-supplier (into {}
                             (map (fn [supplier-id]
                                    [supplier-id (if reset-stores?
                                                   []
                                                   (build-store-candidates-for-supplier ds supplier-id))])
                               supplier-ids))]
    (println (str "[" (Instant/now) "]"))
    (println "Create stores from store_aliases (infer supplier via receipts)")
    (println "  profile:" (name profile))
    (println "  dbname:  " dbname)
    (println "  dry-run?:" (not apply?))
    (println "  limit:   " (or limit "none"))
    (println "  min-receipts:" min-receipts)
    (println "  dedupe-existing?:" dedupe-existing?)
    (println "  reset-stores?:" reset-stores?)
    (println "")
    (println "Before:")
    (println "  stores:" (:stores before))
    (println "  store_aliases:" (:store_aliases before))
    (println "  store_aliases (unmapped):" (:store_aliases_unmapped before))
    (println "  receipts (with store_alias_id):" (:receipts_with_store_alias before))
    (println "  supplier_aliases (mapped):" (:supplier_aliases_mapped before))
    (println "")
    (println "Candidates:")
    (println "  unmapped aliases loaded:" (count aliases))
    (println "  inferable (unambiguous):" (count chosen))
    (println "  ambiguous (ties):" (count ambiguous))
    (println "")

    (when-not apply?
      (let [max-print 50
            {:keys [would-create would-reuse-exact would-reuse-receipt would-reuse-heuristic would-map skipped-ambiguous skipped-no-supplier]}
            (loop [idx 0
                   remaining aliases
                   stores-by-supplier stores-by-supplier
                   would-create 0
                   would-reuse-exact 0
                   would-reuse-receipt 0
                   would-reuse-heuristic 0
                   would-map 0
                   skipped-ambiguous 0
                   skipped-no-supplier 0]
              (if (empty? remaining)
                {:would-create would-create
                 :would-reuse-exact would-reuse-exact
                 :would-reuse-receipt would-reuse-receipt
                 :would-reuse-heuristic would-reuse-heuristic
                 :would-map would-map
                 :skipped-ambiguous skipped-ambiguous
                 :skipped-no-supplier skipped-no-supplier}
                (let [{:keys [id raw_label raw_label_normalized]} (first remaining)
                      {:keys [supplier_id receipts_cnt]} (get chosen id)
                      normalized-key raw_label_normalized
                      alias-fp (store-alias-fp ds id)]
                  (cond
                    (contains? ambiguous id)
                    (do
                      (when (< idx max-print)
                        (println "SKIP (ambiguous supplier):" id raw_label_normalized))
                      (recur (inc idx)
                        (rest remaining)
                        stores-by-supplier
                        would-create
                        would-reuse-exact
                        would-reuse-receipt
                        would-reuse-heuristic
                        would-map
                        (inc skipped-ambiguous)
                        skipped-no-supplier))

                    (nil? supplier_id)
                    (do
                      (when (< idx max-print)
                        (println "SKIP (no supplier inferred):" id raw_label_normalized))
                      (recur (inc idx)
                        (rest remaining)
                        stores-by-supplier
                        would-create
                        would-reuse-exact
                        would-reuse-receipt
                        would-reuse-heuristic
                        would-map
                        skipped-ambiguous
                        (inc skipped-no-supplier)))

                    :else
                    (let [candidates (get stores-by-supplier supplier_id [])
                          match (match-store normalized-key alias-fp candidates)]
                      (if match
                        (do
                          (when (< idx max-print)
                            (println
                              (str "WOULD MAP (reuse " (name (:match match)) "):"
                                " " id
                                " -> store " (:id match)
                                " supplier " supplier_id
                                " key " normalized-key
                                " receipts " receipts_cnt
                                (when (= :fuzzy (:match match))
                                  (str " score " (format "%.3f" (double (:score match)))))
                                (when (= :fuzzy (:match match))
                                  (str " margin " (format "%.3f" (double (:margin match))))))))
                          (recur (inc idx)
                            (rest remaining)
                            stores-by-supplier
                            would-create
                            (if (= :exact (:match match)) (inc would-reuse-exact) would-reuse-exact)
                            (if (= :receipt (:match match)) (inc would-reuse-receipt) would-reuse-receipt)
                            (if (#{:loose :fuzzy} (:match match)) (inc would-reuse-heuristic) would-reuse-heuristic)
                            (inc would-map)
                            skipped-ambiguous
                            skipped-no-supplier))
                        (let [planned-store-id (str "planned-" id)
                              stores-by-supplier' (update stores-by-supplier
                                                    supplier_id
                                                    (fnil conj [])
                                                    {:id planned-store-id
                                                     :normalized_key normalized-key
                                                     :fp alias-fp})]
                          (when (< idx max-print)
                            (println
                              (str "WOULD MAP (create store):"
                                " " id
                                " -> NEW store"
                                " supplier " supplier_id
                                " key " normalized-key
                                " receipts " receipts_cnt)))
                          (recur (inc idx)
                            (rest remaining)
                            stores-by-supplier'
                            (inc would-create)
                            would-reuse-exact
                            would-reuse-receipt
                            would-reuse-heuristic
                            (inc would-map)
                            skipped-ambiguous
                            skipped-no-supplier))))))))]
        (when (> (count aliases) max-print)
          (println (str "... (showing first " max-print " of " (count aliases) ")")))

        (when dedupe-existing?
          (let [{:keys [planned plan]} (dedupe-existing-stores ds {:apply? false})]
            (println "")
            (println "Dedupe plan (existing stores):")
            (println "  would merge groups:" planned)
            (doseq [{:keys [supplier_id pj canonical duplicates reason]} (take 20 plan)]
              (println (str "WOULD MERGE (" reason ") supplier " supplier_id " pj " pj " canonical " canonical " <- " (pr-str duplicates))))
            (when (> (count plan) 20)
              (println (str "... (showing first 20 of " (count plan) ")")))))

        (println "")
        (println "Dry-run plan:")
        (println "  would create stores:" would-create)
        (println "  would reuse stores (exact):" would-reuse-exact)
        (println "  would reuse stores (receipt PJ):" would-reuse-receipt)
        (println "  would reuse stores (loose/fuzzy):" would-reuse-heuristic)
        (println "  would map aliases:" would-map)
        (println "  skipped (ambiguous supplier):" skipped-ambiguous)
        (println "  skipped (no supplier inferred):" skipped-no-supplier)
        (println "")
        (println "Dry-run only. Re-run with --apply to create/link stores.")
        (System/exit 0)))

    (when-not (or yes? (confirm! {:profile profile
                                  :dbname dbname
                                  :action (cond
                                            reset-stores?
                                            "This will RESET stores (delete all stores) and clear store_id on store_aliases/expenses, then recreate and map stores."

                                            dedupe-existing?
                                            "This will CREATE/MAP stores and also DEDUPE existing stores (where safe), in the target database."

                                            :else
                                            "This will CREATE stores and MAP store_aliases in the target database.")
                                  :phrases (cond
                                             reset-stores? #{"RESET STORES"}
                                             dedupe-existing? #{"DEDUPE STORES"}
                                             :else #{"CREATE STORES"})}))
      (println "❌ Cancelled.")
      (System/exit 1))

    (let [result
          (jdbc/with-transaction [tx ds]
            (let [_reset (when reset-stores?
                           (reset-stores! tx))
                  _dedupe (when (and dedupe-existing? (not reset-stores?))
                            (dedupe-existing-stores tx {:apply? true}))
                  stores-by-supplier (volatile!
                                       (into {}
                                         (map (fn [supplier-id]
                                                [supplier-id (if reset-stores?
                                                               []
                                                               (build-store-candidates-for-supplier tx supplier-id))])
                                           supplier-ids)))]
              (reduce
                (fn [{:keys [created linked-exact linked-receipt linked-heuristic mapped skipped-ambiguous skipped-no-supplier] :as acc}
                     {:keys [id raw_label raw_label_normalized]}]
                  (cond
                    (contains? ambiguous id)
                    (assoc acc :skipped-ambiguous (inc skipped-ambiguous))

                    (nil? (get chosen id))
                    (assoc acc :skipped-no-supplier (inc skipped-no-supplier))

                    :else
                    (let [{:keys [supplier_id]} (get chosen id)
                          normalized-key raw_label_normalized
                          display-name (or (some-> raw_label str/trim not-empty) normalized-key)
                          alias-fp (store-alias-fp tx id)
                          candidates (or (get @stores-by-supplier supplier_id)
                                       (let [rows (build-store-candidates-for-supplier tx supplier_id)]
                                         (vswap! stores-by-supplier assoc supplier_id rows)
                                         rows))
                          match (match-store normalized-key alias-fp candidates)
                          existing-store-id (:id match)
                          {:keys [store_id created?] :as insert-result}
                          (when-not existing-store-id
                            (insert-store! tx {:supplier-id supplier_id
                                               :display-name display-name
                                               :normalized-key normalized-key}))
                          store-id (or existing-store-id store_id)
                          _ (when-not store-id
                              (throw (ex-info "Failed to create/find store id"
                                       {:store_alias_id id
                                        :supplier_id supplier_id
                                        :normalized_key normalized-key})))
                          _ (when (and (not existing-store-id) store-id)
                              (vswap! stores-by-supplier update supplier_id (fnil conj []) {:id store-id
                                                                                            :normalized_key normalized-key
                                                                                            :fp alias-fp}))
                          mapped-result (map-alias! tx id store-id)
                          mapped-count (update-count mapped-result)]
                      (cond-> acc
                        (and existing-store-id (= :exact (:match match))) (assoc :linked-exact (inc linked-exact))
                        (and existing-store-id (= :receipt (:match match))) (assoc :linked-receipt (inc linked-receipt))
                        (and existing-store-id (#{:loose :fuzzy} (:match match))) (assoc :linked-heuristic (inc linked-heuristic))
                        (and (not existing-store-id) created?) (assoc :created (inc created))
                        (and (not existing-store-id) (false? created?)) (assoc :linked-exact (inc linked-exact))
                        (= 1 mapped-count) (assoc :mapped (inc mapped))))))
                {:created 0
                 :linked-exact 0
                 :linked-receipt 0
                 :linked-heuristic 0
                 :mapped 0
                 :skipped-ambiguous 0
                 :skipped-no-supplier 0}
                aliases)))]
      (println "✅ Done")
      (println "  created stores:" (:created result))
      (println "  reused existing stores (exact):" (:linked-exact result))
      (println "  reused existing stores (receipt PJ):" (:linked-receipt result))
      (println "  reused existing stores (loose/fuzzy):" (:linked-heuristic result))
      (println "  mapped aliases:" (:mapped result))
      (println "  skipped (ambiguous supplier):" (:skipped-ambiguous result))
      (println "  skipped (no supplier inferred):" (:skipped-no-supplier result))
      (println "")
      (let [after (stats ds)]
        (println "After:")
        (println "  stores:" (:stores after))
        (println "  store_aliases:" (:store_aliases after))
        (println "  store_aliases (unmapped):" (:store_aliases_unmapped after)))
      (System/exit 0))))

(apply -main *command-line-args*)
