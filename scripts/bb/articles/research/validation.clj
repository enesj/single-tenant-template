(ns articles.research.validation
  "Category validation, manufacturer sanitization, English→Bosnian translation,
  and near-duplicate detection."
  (:require
    [articles.research.heuristics :as heuristics]
    [clojure.string :as str]))

;; ============================================================
;; Manufacturer sanitization
;; ============================================================

(defn sanitize-manufacturer
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
        ;; All-caps <= 4 chars with no vowels (OCR fragment — e.g. PODR, HOC, KOT)
        (and (<= (count trimmed) 4)
          (= trimmed (str/upper-case trimmed))
          (not (re-find #"[AEIOU]" trimmed))) nil
        ;; All-caps 3-4 chars with vowels that are likely OCR fragments (not known brand abbreviations)
        ;; Known valid short brands: BAT, dm, INA, OMO, ACE, AXE, UHU
        (and (<= (count trimmed) 4)
          (= trimmed (str/upper-case trimmed))
          (not (re-find #"(?i)^(BAT|INA|OMO|ACE|AXE|UHU|dm)$" trimmed))) nil
        ;; Packaging/bulk terms mistaken for brand names
        (re-find #"(?i)\brinfuz" trimmed) nil
        (re-find #"(?i)^brik$" trimmed) nil
        ;; Known OCR artifact "manufacturers"
        (re-find #"(?i)^(SAR|BAS|KOT|DJ ASS|G\.?A EX RA)$" trimmed) nil
        ;; Contains "Rinfuza" appended to a real name — strip it
        :else (let [cleaned (str/trim (str/replace trimmed #"(?i)\s+rinfuz\w*$" ""))]
                (when-not (str/blank? cleaned) cleaned))))))

;; ============================================================
;; Subcategory sanitization
;; ============================================================

(defn sanitize-subcategory
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

;; ============================================================
;; Category matching
;; ============================================================

(defn find-best-category
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
;; English → Bosnian category translation
;; ============================================================

(def ^:private english->bosnian-category
  (or (heuristics/load-taxonomy "english-bosnian-categories.edn")
    {"Dairy" "Mliječni proizvodi i jaja"
     "Other" "Ostalo"}))

(defn translate-category
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

(defn normalize-for-dedup
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

(defn detect-near-duplicates
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
