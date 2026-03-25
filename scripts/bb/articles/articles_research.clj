#!/usr/bin/env bb

;; Batch article research: local heuristics + optional Perplexity web search.
;;
;; Reads unmapped aliases from DB, applies local resolution heuristics,
;; optionally batches unresolved items to Perplexity sonar-pro for web research,
;; and outputs draft articles.edn + mappings.edn files for create_articles.clj
;; and map_aliases.clj.
;;
;; Usage:
;;   bb articles-research [dev|test|prod] [options]
;;   bb scripts/bb/articles/articles_research.clj dev --skip-research --pretty
;;
;; Output files (in tmp/):
;;   articles-suggested.edn  → feed to create_articles.clj --articles-file
;;   mappings-suggested.edn  → feed to map_aliases.clj --mappings-file
;;   noise-candidates.edn    → review, then delete_unmapped_aliases.clj
;;   research-summary.edn    → overview stats
;;   needs-research.edn      → (only with --skip-research) items requiring web search

(ns scripts.bb.articles.articles-research
  (:require
    [articles.db :as db]
    [babashka.http-client :as http]
    [clojure.data.json :as json]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [clojure.pprint :as pprint]
    [clojure.string :as str])
  (:import
    [java.text Normalizer Normalizer$Form]))

;; ============================================================
;; Perplexity API
;; ============================================================

(def ^:private perplexity-url "https://api.perplexity.ai/chat/completions")

(defn- load-env-file [path]
  (when (.exists (io/file path))
    (->> (slurp path)
      str/split-lines
      (keep (fn [line]
              (when-let [[_ k v] (re-matches #"^([A-Za-z_][A-Za-z0-9_]*)=(.*)$" line)]
                [(str/trim k) (str/trim (str/replace v #"^['\"]|['\"]$" ""))])))
      (into {}))))

(defn- get-perplexity-key []
  (or (some-> (load-env-file ".env") (get "PERPLEXITY_API_KEY"))
    (System/getenv "PERPLEXITY_API_KEY")))

(defn- query-perplexity
  "Call Perplexity sonar-pro with system + user prompts. Returns parsed response map."
  [system-prompt user-prompt]
  (let [api-key (get-perplexity-key)]
    (when-not api-key
      (throw (ex-info "PERPLEXITY_API_KEY not found"
               {:hint "Add PERPLEXITY_API_KEY=<key> to .env or set env var"})))
    (let [payload {:model "sonar-pro"
                   :messages [{:role "system" :content system-prompt}
                              {:role "user" :content user-prompt}]
                   :max_tokens 4096
                   :temperature 0.1}
          resp (http/post perplexity-url
                 {:headers {"Authorization" (str "Bearer " api-key)
                            "Content-Type" "application/json"}
                  :body (json/write-str payload)
                  :throw false})]
      (when-not (= 200 (:status resp))
        (throw (ex-info "Perplexity API error"
                 {:status (:status resp)
                  :body (some-> (:body resp) (subs 0 (min 300 (count (:body resp)))))})))
      (json/read-str (:body resp) :key-fn keyword))))

(defn- extract-content [response]
  (get-in response [:choices 0 :message :content]))

(defn- parse-json-response
  "Parse a JSON array from Perplexity response, stripping markdown fences if present."
  [text]
  (when text
    (let [cleaned (-> text
                    str/trim
                    (str/replace #"^```(?:json)?\s*" "")
                    (str/replace #"\s*```\s*$" "")
                    str/trim)]
      (or (try
            (let [parsed (json/read-str cleaned :key-fn keyword)]
              (when (sequential? parsed) parsed))
            (catch Exception _ nil))
          ;; Fallback: extract first JSON array from text
        (when-let [match (re-find #"\[[\s\S]*\]" cleaned)]
          (try
            (let [parsed (json/read-str match :key-fn keyword)]
              (when (sequential? parsed) parsed))
            (catch Exception _ nil)))))))

;; ============================================================
;; OCR noise detection
;; ============================================================

(defn ocr-noise-reason
  "Heuristic OCR-noise detector. Returns a reason string or nil."
  [{:keys [raw_label raw_label_normalized]}]
  (let [raw (or raw_label "")
        norm (or raw_label_normalized "")
        trimmed (str/trim raw)
        trimmed-norm (str/trim norm)
        alnum (count (re-seq #"[A-Za-z0-9]" trimmed))]
    (cond
      (str/blank? trimmed)       "blank"
      (str/blank? trimmed-norm)  "blank-normalized"
      (re-matches #"(?i)^(x+|\*+|\-+|\.+|,+|_+)$" trimmed) "punctuation-only"
      (re-matches #"^\d+$" trimmed) "digits-only"
      (< alnum 3) "too-few-alnum"
      ;; "na" is a common Bosnian preposition ("on/for") — only flag "unknown" and "n/a"
      (re-find #"(?i)^(unknown|n/?a)$" trimmed) "placeholder"
      ;; Hex suffix pattern — OCR artifacts like "6f93", "4f92", "2b3c"
      ;; Only flag when the hex IS the label or label is very short with hex
      (and (<= alnum 12)
        (re-find #"\b[0-9a-f]{4}\b" (str/lower-case trimmed))
           ;; Don't flag real labels that happen to contain 4-char hex-like sequences
        (or (re-find #"\b\d+[a-f][0-9a-f]{2,3}\b" (str/lower-case trimmed))
          (< alnum 6)))
      "hex-ocr-artifact"
      :else nil)))

;; ============================================================
;; Local heuristic resolution
;; ============================================================

(def ^:private taxonomy-dir "scripts/bb/articles/taxonomy")

(defn- load-taxonomy
  "Load an EDN taxonomy file from the taxonomy directory. Returns nil if missing."
  [filename]
  (let [f (io/file taxonomy-dir filename)]
    (when (.exists f)
      (edn/read-string (slurp f)))))

(defn- save-taxonomy!
  "Write an EDN value back to a taxonomy file."
  [filename data]
  (spit (io/file taxonomy-dir filename)
    (with-out-str (pprint/pprint data))))

;; Supplier display_name patterns → default category
(def ^:private supplier-hints
  (or (load-taxonomy "supplier-hints.edn")
    [["(?i)apoteka|pharmacy|ljekarna"       "Zdravlje i apoteka"]
     ["(?i)mesnica|mesar"                    "Meso, morski plodovi i delikatesi"]
     ["(?i)pekara|bakery"                    "Pekara i deserti"]
     ["(?i)cevab|fast.?food|kafic|caffe|cafe|bistro|pizzeria|grill|slasticarna|restoran|konoba"
      "Pekara i deserti"]]))

;; Brand prefix → [manufacturer-name category-name subcategory-or-nil]
(def ^:private brand-rules
  (or (load-taxonomy "brand-rules.edn")
    [["(?i)^coca.?cola" "The Coca-Cola Company" "Pakovana hrana i pića" "Bezalkoholna pića"]]))

;; Product keyword → [category subcategory]
(def ^:private keyword-category-hints
  (or (load-taxonomy "keyword-category-hints.edn")
    {"mlijeko" ["Mliječni proizvodi i jaja" "Mlijeko"]
     "kafa"    ["Pakovana hrana i pića"     "Kafa"]}))

;; Bosnian meat-related keywords
(def ^:private meat-words
  (or (load-taxonomy "meat-words.edn")
    #{"juneci" "teleci" "pileci" "govedina" "sunka" "salama"}))

(defn- strip-diacritics [s]
  (-> (Normalizer/normalize (str s) Normalizer$Form/NFD)
    (str/replace #"\p{InCombiningDiacriticalMarks}+" "")))

(defn- label-words-ascii
  "Extract lowercase ASCII words from a raw label (diacritics stripped)."
  [raw-label]
  (when raw-label
    (->> (str/split (-> raw-label str/lower-case strip-diacritics) #"[^a-z]+")
      (remove str/blank?)
      set)))

(defn- match-supplier-hint [supplier-name]
  (when supplier-name
    (some (fn [[pattern category]]
            (when (re-find (re-pattern pattern) supplier-name)
              {:category category :source :supplier-hint}))
      supplier-hints)))

(defn- match-brand [raw-label]
  (when raw-label
    (some (fn [[pattern mfr cat subcat]]
            (when (re-find (re-pattern pattern) raw-label)
              (cond-> {:category cat :source :brand-rule}
                mfr    (assoc :manufacturer mfr)
                subcat (assoc :subcategory subcat))))
      brand-rules)))

(defn- match-keywords [raw-label]
  (let [words (label-words-ascii raw-label)]
    (when words
      (let [coffee-context? (some #{"barista" "intenso" "espresso" "dolce" "lungo" "kafa" "nescafe"} words)]
        ;; Check meat words first
        (if (some meat-words words)
          {:category "Meso, morski plodovi i delikatesi" :subcategory "Svježe meso" :source :keyword-meat}
          ;; Then check keyword hints, with kapsule context-awareness
          (some (fn [w]
                  (when-let [[cat subcat] (get keyword-category-hints w)]
                    ;; "kapsule" in coffee context → Kafa, not pharmacy
                    (if (and (= w "kapsule") coffee-context?)
                      {:category "Pakovana hrana i pića" :subcategory "Kafa" :source :keyword-hint}
                      {:category cat :subcategory subcat :source :keyword-hint})))
            words))))))

(defn- build-canonical-name
  "Build a reasonable canonical name from a raw OCR label.
  Title-cases words, preserves size/weight tokens as uppercase."
  [raw-label]
  (-> raw-label
    str/trim
      ;; Remove trailing OCR artifact hex codes (3-4 chars, e.g. "6f93", "a73")
    (str/replace #"\s+[0-9a-f]{3,4}$" "")
      ;; Normalize spaces
    (str/replace #"\s+" " ")
    str/trim
      ;; Title-case
    (#(str/join " "
        (map (fn [w]
               (cond
                   ;; Size/weight tokens stay uppercase
                 (re-matches #"(?i)\d+[\.,]?\d*(g|gr|kg|ml|l|cl|dl|mg|kom|tbl)?" w) (str/upper-case w)
                   ;; Otherwise title-case
                 (not (str/blank? w))
                 (str (str/upper-case (subs w 0 1))
                   (when (> (count w) 1) (str/lower-case (subs w 1))))
                 :else w))
          (str/split % #"\s+"))))))

(defn- resolve-locally
  "Try to resolve an alias group using local heuristics.
  Returns a resolution map or nil if unresolvable."
  [raw-label supplier-name]
  (let [brand-match   (match-brand raw-label)
        supplier-hint (match-supplier-hint supplier-name)
        keyword-match (match-keywords raw-label)
        ;; Priority: brand > keyword > supplier
        category    (or (:category brand-match)
                      (:category keyword-match)
                      (:category supplier-hint))
        subcategory (or (:subcategory brand-match)
                      (:subcategory keyword-match))
        manufacturer (:manufacturer brand-match)
        source       (or (:source brand-match)
                       (:source keyword-match)
                       (:source supplier-hint))]
    (when category
      {:canonical-name  (build-canonical-name raw-label)
       :manufacturer-name manufacturer
       :category-name   category
       :subcategory-name (or subcategory "Opste")
       :confidence       (if brand-match :high :medium)
       :resolution       :local
       :resolution-source source})))

;; ============================================================
;; Cross-supplier deduplication
;; ============================================================

(defn- group-aliases
  "Group unmapped aliases by raw_label_normalized to deduplicate across suppliers.
  Returns a vec of maps sorted by alias count descending."
  [aliases]
  (->> aliases
    (group-by :raw_label_normalized)
    (map (fn [[norm-label items]]
           {:raw-label-normalized norm-label
            :raw-label (:raw_label (first items))
            :suppliers (vec (distinct (keep :supplier items)))
            :alias-ids (mapv :id items)}))
    (sort-by (comp - count :alias-ids))
    vec))

;; ============================================================
;; Perplexity batch research
;; ============================================================

(defn- build-research-prompt
  "Build Perplexity system + user prompts for a batch of alias groups.
  When subcategory-map is provided, includes existing taxonomy as constraint."
  [groups db-category-names subcategory-map]
  (let [cat-list (str/join ", " db-category-names)
        taxonomy-text (when (seq subcategory-map)
                        (str "\n\nExisting category → subcategory taxonomy. You MUST pick a subcategory from this list when one fits. Only create a new Bosnian subcategory if absolutely none of the existing ones are appropriate:\n"
                          (->> subcategory-map
                            (map (fn [[cat subcats]]
                                   (str "  " cat ": " (str/join ", " subcats))))
                            (str/join "\n"))))
        items-text (->> groups
                     (map-indexed
                       (fn [idx {:keys [raw-label suppliers]}]
                         (format "%d. \"%s\" (store: %s)"
                           (inc idx)
                           raw-label
                           (str/join " / " (take 2 suppliers)))))
                     (str/join "\n"))]
    {:system (str "You are a product identification assistant for Bosnian retail/grocery receipts.\n"
               "Respond with ONLY a JSON array. No markdown code fences, no explanation text.\n\n"
               "Available product categories: " cat-list "\n"
               (or taxonomy-text "")
               "\n\nEach element must have exactly these keys:\n"
               "{\"i\": <1-based index>, \"name\": \"<canonical product name with size/weight if known>\", "
               "\"mfr\": \"<manufacturer/brand or null if generic/unbranded>\", "
               "\"cat\": \"<one category from list above>\", "
               "\"subcat\": \"<subcategory in Bosnian from the taxonomy above, or a new descriptive one>\"}\n\n"
               "Rules:\n"
               "- Include size/weight/volume in name when present (e.g. \"Coca-Cola 1.25L\")\n"
               "- For unbranded items (loose meat, produce, bags), set mfr to null\n"
               "- Set mfr to null when unsure of the brand — NEVER extract mfr from measurement suffixes (/KO, /KG, /pc), trailing OCR abbreviations, or short codes in the label\n"
               "- You MUST pick an existing subcategory from the taxonomy above when one fits\n"
               "- Only create a new subcategory if absolutely none of the existing ones match\n"
               "- All category and subcategory names MUST be in Bosnian (never English)\n"
               "- If unsure, use your best guess based on store context\n"
               "- Never use \"General\" or \"Opšte\" as subcategory — be descriptive\n"
               "- New subcategory names must be in Bosnian language")
     :user (str "Identify these products from Bosnian store receipts:\n" items-text)}))

(defn- research-batch!
  "Send a batch of alias groups to Perplexity for identification.
  Returns a vector of resolved group maps."
  [groups db-category-names subcategory-map]
  (let [{:keys [system user]} (build-research-prompt groups db-category-names subcategory-map)
        response (query-perplexity system user)
        content (extract-content response)
        parsed (parse-json-response content)]
    (if parsed
      (->> parsed
        (keep (fn [item]
                (let [idx (dec (or (:i item) 0))
                      group (get (vec groups) idx)]
                  (when (and group item)
                    (merge group
                      {:canonical-name   (or (:name item) (build-canonical-name (:raw-label group)))
                       :manufacturer-name (:mfr item)
                       :category-name    (or (:cat item) "Ostalo")
                       :subcategory-name (or (:subcat item) "Opste")
                       :confidence       :medium
                       :resolution       :perplexity})))))
        vec)
      (do
        (binding [*out* *err*]
          (println "WARNING: Failed to parse Perplexity response")
          (when content
            (println "  Content preview:" (subs (str content) 0 (min 200 (count (str content)))))))
        (mapv #(assoc % :resolution :failed :confidence :low) groups)))))

;; ============================================================
;; Category validation
;; ============================================================

(defn- sanitize-manufacturer
  "Reject OCR artifact manufacturer names. Returns cleaned name or nil."
  [mfr-name]
  (when mfr-name
    (let [trimmed (str/trim mfr-name)]
      (cond
        (str/blank? trimmed) nil
        (<= (count trimmed) 2) nil
        ;; Starts with / (label suffix fragment)
        (re-find #"^/" trimmed) nil
        ;; Ends with unit suffix like /KO, /KG, /KOM, /pc, /L
        (re-find #"(?i)/(ko|kg|kom|pc|l)$" trimmed) nil
        ;; All-caps <= 4 chars with no vowels (OCR fragment)
        (and (<= (count trimmed) 4)
          (= trimmed (str/upper-case trimmed))
          (not (re-find #"[aeiouAEIOU]" trimmed))) nil
        :else trimmed))))

(defn- sanitize-subcategory
  "Try to match a subcategory against existing subcategories for a category.
  Returns the best DB match, or the original if no match found."
  [subcat-name cat-name subcategory-map]
  (if-not (and subcat-name cat-name)
    (or subcat-name "Opste")
    (let [existing (get subcategory-map cat-name)
          lower (str/lower-case subcat-name)]
      (or
        ;; Exact match
        (some #(when (= subcat-name %) %) existing)
        ;; Case-insensitive match
        (some #(when (= lower (str/lower-case %)) %) existing)
        ;; Substring match (subcat contained in existing, or vice versa)
        (some #(when (or (str/includes? (str/lower-case %) lower)
                       (str/includes? lower (str/lower-case %)))
                 %) existing)
        ;; No match — keep original (will create new subcategory)
        subcat-name))))

(defn- find-best-category
  "Match a category name against DB categories. Returns the DB name or the input."
  [cat-name db-category-names]
  (let [lower (str/lower-case (or cat-name ""))]
    (or ;; Exact match
      (some #(when (= cat-name %) %) db-category-names)
        ;; Case-insensitive match
      (some #(when (= lower (str/lower-case %)) %) db-category-names)
        ;; Partial match
      (some #(when (str/includes? (str/lower-case %) lower) %) db-category-names)
        ;; Default
      cat-name)))

;; ============================================================
;; English → Bosnian category post-processing
;; ============================================================

(def ^:private english->bosnian-category
  (or (load-taxonomy "english-bosnian-categories.edn")
    {"Dairy" "Mliječni proizvodi i jaja"
     "Other" "Ostalo"}))

(defn- translate-category
  "If a category looks like English, try to map it to Bosnian. Returns best match."
  [cat-name db-category-names]
  (or
    ;; Direct English→Bosnian lookup (case-insensitive)
    (some (fn [[eng bos]]
            (when (= (str/lower-case eng) (str/lower-case (or cat-name "")))
              bos))
      english->bosnian-category)
    ;; Partial match against English keys
    (some (fn [[eng bos]]
            (when (str/includes? (str/lower-case (or cat-name ""))
                    (str/lower-case eng))
              bos))
      english->bosnian-category)
    ;; Already Bosnian or unknown — pass through
    cat-name))

;; ============================================================
;; Near-duplicate detection
;; ============================================================

(defn- normalize-for-dedup
  "Aggressively normalize a canonical name for duplicate detection.
  Strips size/weight, punctuation, and lowercases."
  [name]
  (when name
    (-> name
      str/lower-case
      (str/replace #"\d+[\.,]?\d*\s*(g|gr|kg|ml|l|cl|dl|mg|kom|tbl)\b" "")
      (str/replace #"[^a-z0-9 ]+" " ")
      (str/replace #"\s+" " ")
      str/trim)))

(defn- detect-near-duplicates
  "Find groups of articles with similar normalized names.
  Returns a vec of {:group-key :articles} for groups with 2+ members."
  [articles-edn]
  (->> articles-edn
    (group-by #(normalize-for-dedup (:canonical-name %)))
    (filter (fn [[_ items]] (>= (count items) 2)))
    (mapv (fn [[group-key items]]
            {:group-key group-key
             :count (count items)
             :articles (mapv :canonical-name items)}))
    (sort-by (comp - :count))))

;; ============================================================
;; Output generation
;; ============================================================

(defn- learn-brand-rules!
  "Scan Perplexity-resolved items for new brand patterns and append to brand-rules.edn.
  Returns count of new rules learned."
  [perplexity-results]
  (let [existing-patterns (set (map first brand-rules))
        new-rules (atom [])]
    (doseq [{:keys [canonical-name manufacturer-name resolution]} perplexity-results
            :when (and (= resolution :perplexity)
                    manufacturer-name
                    (sanitize-manufacturer manufacturer-name))]
      (let [first-word (first (str/split (str/trim (or canonical-name "")) #"\s+"))
            pattern (str "(?i)^" (java.util.regex.Pattern/quote (str/lower-case first-word)) "\\b")]
        (when (and (not (str/blank? first-word))
                (> (count first-word) 2)
                (not (contains? existing-patterns pattern))
                (not (some #(= (first %) pattern) @new-rules)))
          (swap! new-rules conj
            [pattern manufacturer-name
             (:category-name (first (filter #(= (:canonical-name %) canonical-name) perplexity-results)))
             nil]))))
    (when (seq @new-rules)
      (let [current (or (load-taxonomy "brand-rules.edn") [])
            updated (vec (concat current @new-rules))]
        (save-taxonomy! "brand-rules.edn" updated)))
    (count @new-rules)))

(defn- suggestion->article-edn
  [{:keys [canonical-name manufacturer-name category-name subcategory-name]}]
  (cond-> {:canonical-name canonical-name
           :category-name (or category-name "Ostalo")
           :subcategory-name (or subcategory-name "Opste")}
    manufacturer-name (assoc :manufacturer-name manufacturer-name)))

(defn- suggestion->mapping-entries
  [{:keys [alias-ids canonical-name]}]
  (let [article-key (db/normalize-key canonical-name)]
    (mapv (fn [aid] {:alias-id (str aid) :article-key article-key})
      alias-ids)))

;; ============================================================
;; CLI parsing
;; ============================================================

(def ^:private default-batch-size 15)

(defn- die! [msg]
  (binding [*out* *err*] (println msg))
  (System/exit 2))

(defn- usage []
  (println "Batch article research: local heuristics + Perplexity web search.")
  (println "")
  (println "Usage:")
  (println "  bb articles-research [dev|test|prod] [options]")
  (println "  bb scripts/bb/articles/articles_research.clj dev --skip-research --pretty")
  (println "")
  (println "Options:")
  (println "  --skip-research       Only run local heuristics (no Perplexity API)")
  (println "  --batch-size N        Aliases per Perplexity batch (default: 15)")
  (println "  --supplier PATTERN    Only process aliases from matching suppliers")
  (println "  --output-prefix PFX   Prefix output files: tmp/{pfx}-articles-suggested.edn etc.")
  (println "  --limit N             Max alias groups to process")
  (println "  --pretty              Pretty-print output")
  (println "  --help                Show this help")
  (println "")
  (println "Output files (in tmp/):")
  (println "  articles-suggested.edn    → create_articles.clj --articles-file")
  (println "  mappings-suggested.edn    → map_aliases.clj --mappings-file")
  (println "  noise-candidates.edn      → review + delete_unmapped_aliases.clj")
  (println "  research-summary.edn      → overview stats")
  (println "  needs-research.edn        → (--skip-research only) items needing web search"))

(defn- parse-args [args]
  (loop [args (seq args)
         opts {:skip-research? false
               :batch-size default-batch-size
               :supplier-filter nil
               :output-prefix nil
               :limit nil
               :pretty? false}]
    (if-not args
      opts
      (let [[a b & more] args]
        (cond
          (or (= a "--help") (= a "-h")) (do (usage) (System/exit 0))
          (= a "--skip-research") (recur (next args) (assoc opts :skip-research? true))
          (= a "--pretty")        (recur (next args) (assoc opts :pretty? true))
          (= a "--batch-size")    (recur more (assoc opts :batch-size (or (db/parse-long b) default-batch-size)))
          (= a "--supplier")      (recur more (assoc opts :supplier-filter b))
          (= a "--output-prefix") (recur more (assoc opts :output-prefix b))
          (= a "--limit")         (recur more (assoc opts :limit (db/parse-long b)))
          :else (die! (str "Unknown arg: " a)))))))

;; ============================================================
;; DB queries
;; ============================================================

(defn- fetch-unmapped-aliases [db]
  (db/query db
    (str "SELECT aa.id, aa.raw_label, aa.raw_label_normalized,\n"
      "       aa.supplier_id, s.display_name AS supplier\n"
      "FROM article_aliases aa\n"
      "LEFT JOIN suppliers s ON aa.supplier_id = s.id\n"
      "WHERE aa.article_id IS NULL\n"
      "ORDER BY s.display_name NULLS LAST, aa.raw_label")))

(defn- fetch-category-names [db]
  (->> (db/query db "SELECT name FROM categories ORDER BY name")
    (mapv :name)))

(defn- fetch-existing-article-keys [db]
  (->> (db/query db "SELECT normalized_key FROM articles")
    (map :normalized_key)
    set))

(defn- fetch-subcategory-map
  "Fetch existing subcategories grouped by category name.
  Returns {\"Category\" [\"Subcat1\" \"Subcat2\" ...]}."
  [db]
  (->> (db/query db
         (str "SELECT c.name AS category_name, sc.name AS subcategory_name\n"
           "FROM subcategories sc\n"
           "JOIN categories c ON sc.category_id = c.id\n"
           "ORDER BY c.name, sc.name"))
    (group-by :category_name)
    (reduce-kv (fn [m k v] (assoc m k (mapv :subcategory_name v))) {})))

;; ============================================================
;; Main
;; ============================================================

(defn- log [& args]
  (binding [*out* *err*]
    (apply println args)))

(defn -main [& args]
  (let [{:keys [profile args]} (db/parse-profile args)
        opts (parse-args args)
        {:keys [skip-research? batch-size supplier-filter output-prefix limit pretty?]} opts

        config (db/read-config profile)
        db (:database config)

        ;; File prefix for output files
        file-prefix (if output-prefix (str output-prefix "-") "")

        ;; 1. Fetch data from DB
        _ (log "Fetching unmapped aliases...")
        raw-aliases (fetch-unmapped-aliases db)
        _ (log "  " (count raw-aliases) "unmapped aliases")

        db-category-names (fetch-category-names db)
        subcategory-map (fetch-subcategory-map db)
        existing-keys (fetch-existing-article-keys db)
        all-known-subcats (set (mapcat val subcategory-map))

        ;; 2. Group by raw_label_normalized (dedup across suppliers)
        alias-groups (group-aliases raw-aliases)
        _ (log "  " (count alias-groups) "unique alias groups (after cross-supplier dedup)")

        ;; Apply supplier filter
        alias-groups (if supplier-filter
                       (filterv (fn [{:keys [suppliers]}]
                                  (some #(re-find (re-pattern (str "(?i)" supplier-filter)) (or % ""))
                                    suppliers))
                         alias-groups)
                       alias-groups)
        _ (when supplier-filter
            (log "  " (count alias-groups) "groups after supplier filter"))

        ;; Apply limit
        alias-groups (if limit (vec (take limit alias-groups)) alias-groups)

        ;; 3. Detect OCR noise
        noise-groups (filterv (fn [{:keys [raw-label raw-label-normalized]}]
                                (ocr-noise-reason {:raw_label raw-label
                                                   :raw_label_normalized raw-label-normalized}))
                       alias-groups)
        clean-groups (vec (remove (set noise-groups) alias-groups))
        _ (log "  " (count noise-groups) "noise candidates")

        ;; 4. Match against existing articles
        already-matched (filterv (fn [{:keys [raw-label]}]
                                   (contains? existing-keys (db/normalize-key raw-label)))
                          clean-groups)
        remaining (vec (remove (set already-matched) clean-groups))
        _ (when (seq already-matched)
            (log "  " (count already-matched) "match existing articles"))

        ;; 5. Local heuristic resolution
        locally-resolved (atom [])
        needs-research (atom [])
        _ (doseq [group remaining]
            (if-let [resolution (resolve-locally (:raw-label group) (first (:suppliers group)))]
              (swap! locally-resolved conj (merge group resolution))
              (swap! needs-research conj group)))
        _ (log "  " (count @locally-resolved) "resolved locally")
        _ (log "  " (count @needs-research) "need web research")

        ;; 6. Perplexity batch research
        researched (if (or skip-research? (empty? @needs-research))
                     (do
                       (when (and skip-research? (seq @needs-research))
                         (log "  Skipping research (--skip-research). See tmp/needs-research.edn"))
                       [])
                     (let [batches (partition-all batch-size @needs-research)
                           total (count batches)]
                       (log "Running Perplexity research (" total "batches of up to" batch-size ")...")
                       (->> batches
                         (map-indexed
                           (fn [idx batch]
                             (log "  Batch" (str (inc idx) "/" total) "...")
                             (try
                               (let [result (research-batch! (vec batch) db-category-names subcategory-map)]
                                 (when (< (inc idx) total)
                                   (Thread/sleep 1000))
                                 result)
                               (catch Exception e
                                 (log "  ERROR in batch" (str (inc idx) ":") (ex-message e))
                                 (mapv #(assoc % :resolution :failed :confidence :low) batch)))))
                         (apply concat)
                         vec)))

        ;; 7. Combine all resolved items
        all-resolved (vec (concat @locally-resolved researched))
        successful (filterv #(not= :failed (:resolution %)) all-resolved)
        failed (filterv #(= :failed (:resolution %)) all-resolved)

        ;; 7b. Learning: append new brand discoveries from Perplexity to brand-rules.edn
        learned-count (if (seq researched)
                        (learn-brand-rules! researched)
                        0)
        _ (when (pos? learned-count)
            (log "  " learned-count "new brand rules learned and saved to taxonomy"))

        ;; 8. Post-process: sanitize manufacturers, translate categories, validate subcategories
        successful (mapv (fn [s]
                           (-> s
                             (update :manufacturer-name sanitize-manufacturer)
                             (update :category-name translate-category db-category-names)
                             (update :category-name find-best-category db-category-names)
                             (#(update % :subcategory-name sanitize-subcategory (:category-name %) subcategory-map))))
                     successful)

        ;; 8b. Validate subcategories — warn about unknown ones
        unknown-subcats (->> successful
                          (keep (fn [{:keys [subcategory-name category-name]}]
                                  (when (and subcategory-name
                                          (not (contains? all-known-subcats subcategory-name))
                                          (not= subcategory-name "Opste"))
                                    {:subcategory subcategory-name :category category-name})))
                          distinct
                          vec)
        _ (when (seq unknown-subcats)
            (log "  " (count unknown-subcats) "new subcategories will be created"))

        ;; 9. Build output — deduplicate articles by normalized key
        seen-keys (atom #{})
        articles-edn (reduce (fn [acc s]
                               (let [k (db/normalize-key (:canonical-name s))]
                                 (if (contains? @seen-keys k)
                                   acc
                                   (do (swap! seen-keys conj k)
                                     (conj acc (suggestion->article-edn s))))))
                       []
                       successful)

        ;; 10. Build mappings (for all successful + existing matches)
        mappings-from-new (->> successful (mapcat suggestion->mapping-entries) vec)
        mappings-from-existing (->> already-matched
                                 (mapcat (fn [{:keys [alias-ids raw-label]}]
                                           (let [k (db/normalize-key raw-label)]
                                             (mapv (fn [aid] {:alias-id (str aid) :article-key k})
                                               alias-ids))))
                                 vec)
        all-mappings (vec (concat mappings-from-new mappings-from-existing))

        ;; 11. Build noise list
        noise-edn (->> noise-groups
                    (mapv (fn [{:keys [alias-ids raw-label raw-label-normalized]}]
                            {:raw-label raw-label
                             :alias-ids (mapv str alias-ids)
                             :alias-count (count alias-ids)
                             :reason (ocr-noise-reason {:raw_label raw-label
                                                        :raw_label_normalized raw-label-normalized})})))

        ;; 12. Near-duplicate detection
        duplicates (detect-near-duplicates articles-edn)
        _ (when (seq duplicates)
            (log "  WARNING:" (count duplicates) "potential near-duplicate groups detected"))

        summary (cond-> {:generated-at           (str (java.time.Instant/now))
                         :profile                (name profile)
                         :total-aliases           (count raw-aliases)
                         :unique-groups           (count alias-groups)
                         :noise-candidates        (count noise-groups)
                         :noise-alias-count       (reduce + (map (comp count :alias-ids) noise-groups))
                         :existing-matches        (count already-matched)
                         :locally-resolved        (count @locally-resolved)
                         :perplexity-researched   (count researched)
                         :research-failed         (count failed)
                         :still-needs-research    (if skip-research? (count @needs-research) 0)
                         :articles-suggested      (count articles-edn)
                         :mappings-suggested      (count all-mappings)
                         :near-duplicates         (count duplicates)
                         :new-subcategories       (count unknown-subcats)
                         :brand-rules-learned     learned-count
                         :skip-research?          skip-research?}
                  (seq unknown-subcats)
                  (assoc :new-subcategory-list (mapv :subcategory unknown-subcats)))]

    ;; Write output files
    (.mkdirs (io/file "tmp"))
    (let [f (fn [name] (str "tmp/" file-prefix name))]
      (spit (f "articles-suggested.edn")  (with-out-str (pprint/pprint articles-edn)))
      (spit (f "mappings-suggested.edn")  (with-out-str (pprint/pprint all-mappings)))
      (spit (f "noise-candidates.edn")    (with-out-str (pprint/pprint noise-edn)))
      (spit (f "research-summary.edn")    (with-out-str (pprint/pprint summary)))

      ;; Write near-duplicate warnings
      (when (seq duplicates)
        (spit (f "duplicate-warnings.edn") (with-out-str (pprint/pprint duplicates))))

      ;; Write needs-research list when skipping research
      (when (and skip-research? (seq @needs-research))
        (spit (f "needs-research.edn")
          (with-out-str
            (pprint/pprint
              (mapv (fn [{:keys [raw-label suppliers alias-ids]}]
                      {:raw-label raw-label
                       :suppliers suppliers
                       :alias-count (count alias-ids)})
                @needs-research)))))

      (log "")
      (log "Output written to tmp/")
      (log (str "  " file-prefix "articles-suggested.edn  (" (count articles-edn) " articles)"))
      (log (str "  " file-prefix "mappings-suggested.edn  (" (count all-mappings) " mappings)"))
      (log (str "  " file-prefix "noise-candidates.edn    (" (count noise-edn) " noise entries)"))
      (when (seq duplicates)
        (log (str "  " file-prefix "duplicate-warnings.edn  (" (count duplicates) " groups)")))
      (when (and skip-research? (seq @needs-research))
        (log (str "  " file-prefix "needs-research.edn      (" (count @needs-research) " items)"))))

    ;; Print summary to stdout
    (if pretty?
      (db/pprint-edn summary)
      (db/prn-edn summary))))

(apply -main *command-line-args*)
