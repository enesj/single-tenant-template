(ns app.domain.backend.expenses.services.stores.receipt-fingerprint
  "Receipt-derived store fingerprints.

  Goal: Use stable receipt header elements (e.g. `PJ` branch number + branch name)
  to match OCR store aliases to an existing store without relying on exact string
  equality (and without requiring Places).

  This is intentionally conservative: it will only return a store_id when the
  match is unambiguous."
  (:require
    [app.domain.backend.expenses.services.service-configs :as configs]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]))

(def ^:private pj-name-sim-threshold
  0.8)

(def ^:private pj-name-sim-margin
  0.03)

(defn- parse-pj
  [markdown]
  (let [s (or markdown "")
        extract (fn [re]
                  (some-> (re-find re s) second str str/trim not-empty))
        raw (or
              ;; Poslovna jedinica (PJ / P.J / P.O)
              (extract #"(?i)\bP\s*\.?\s*[JO]\b\s*[:\.]?\s*(?:br\.|broj)?\s*\.?\s*(\d{1,4})")

              ;; Podružnica (branch)
              (extract #"(?i)\bPodru(?:ž|z)nica\b\s*[:\.]?\s*(?:br\.|broj)?\s*\.?\s*(\d{1,4})")

              ;; Prodavnica (store)
              (extract #"(?i)\bProdavnica\b\s*[:\.]?\s*(?:br\.|broj)?\s*\.?\s*(\d{1,4})"))]
    (when (seq raw)
      ;; Drop leading zeros to avoid `066` vs `66` splits.
      (str (Long/parseLong raw)))))

(defn- first-line-matching
  [s re]
  (some (fn [line]
          (when (re-find re line) line))
    (str/split-lines (or s ""))))

(defn- parse-pj-name
  "Parse the store/branch name from the same line as the unit number, and normalize it.

  For many receipts this is the `PJ` line, but we also support `Podružnica` and
  `Prodavnica` prefixes.

  Example: "
  [markdown]
  (when-let [line (first-line-matching markdown #"(?i)\b(?:P\s*\.?\s*[JO]|Podru(?:ž|z)nica|Prodavnica)\b")]
    (let [after-num (-> line
                      (str/replace #"(?i)^.*\b(?:P\s*\.?\s*[JO]|Podru(?:ž|z)nica|Prodavnica)\b\s*[:\.]?\s*(?:br\.|broj)?\s*\.?\s*\d{1,4}\s*" "")
                      (str/replace #"\"" "")
                      (str/replace #",+" " ")
                      (str/replace #"\s+" " ")
                      str/trim)
          no-city (-> after-num
                    (str/replace #"(?i)\s*(sarajevo|mostar)\b.*$" "")
                    str/trim)
          normalized (some-> no-city not-empty configs/normalize-store-key)]
      normalized)))

(defn- levenshtein-distance
  [^String a ^String b]
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
      (aget prev blen))))

(defn- levenshtein-ratio
  [a b]
  (let [a (or a "")
        b (or b "")
        maxlen (max (.length ^String a) (.length ^String b))]
    (if (zero? maxlen)
      1.0
      (- 1.0 (/ (double (levenshtein-distance a b)) (double maxlen))))))

(defn- receipt->fp
  [parsed-markdown]
  (let [pj (parse-pj parsed-markdown)
        pj-name (parse-pj-name parsed-markdown)]
    {:pj pj
     :pj-name pj-name}))

(defn pj-number
  "Extract PJ branch number from receipt markdown.

  Returns a string (digits) or nil."
  [parsed-markdown]
  (parse-pj parsed-markdown))

(defn- select-receipt-markdown-by-store-id
  [db supplier-id]
  (jdbc/execute!
    db
    (sql/format {:select [[:s.id :store_id]
                          [:r.parsed_markdown :parsed_markdown]]
                 :from [[:stores :s]]
                 :join [[:store_aliases :sa] [:= :sa.store_id :s.id]
                        [:receipts :r] [:= :r.store_alias_id :sa.id]]
                 :where [:and
                         [:= :s.supplier_id supplier-id]
                         [:is-not :r.parsed_markdown nil]]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn- store-fingerprints-by-store-id
  "Return store_id -> {:pjs #{..} :pj-names #{..} :pj-counts {pj n}}"
  [db supplier-id]
  (reduce
    (fn [acc {:keys [store_id parsed_markdown]}]
      (let [{:keys [pj pj-name]} (receipt->fp parsed_markdown)]
        (if-not (seq pj)
          acc
          (-> acc
            (update-in [store_id :pjs] (fnil conj #{}) pj)
            (update-in [store_id :pj-names] (fnil conj #{}) pj-name)
            (update-in [store_id :pj-counts pj] (fnil inc 0))))))
    {}
    (select-receipt-markdown-by-store-id db supplier-id)))

(defn match-store-id
  "Match a supplier store_id from a receipt's markdown.

  Returns store_id (uuid) or nil.

  Conservative rules:
  - Requires a PJ number.
  - If exactly one store has that PJ, return it.
  - If multiple stores have that PJ (already-duplicated data), pick the best
    candidate by PJ-name similarity if available and unambiguous.
  - Otherwise, return nil."
  [db supplier-id parsed-markdown]
  (let [{:keys [pj pj-name]} (receipt->fp parsed-markdown)]
    (when (seq pj)
      (let [fps (store-fingerprints-by-store-id db supplier-id)
            candidates (->> fps
                         (keep (fn [[store-id {:keys [pjs]}]]
                                 (when (contains? (or pjs #{}) pj)
                                   store-id)))
                         vec)]
        (cond
          (= 1 (count candidates))
          (first candidates)

          (and (seq pj-name) (> (count candidates) 1))
          (let [scored (->> candidates
                         (map (fn [store-id]
                                (let [{:keys [pj-names pj-counts]} (get fps store-id)
                                      score (apply max 0.0 (for [n (remove nil? (or pj-names #{}))]
                                                             (double (levenshtein-ratio pj-name n))))
                                      cnt (long (get pj-counts pj 0))]
                                  {:store-id store-id
                                   :score score
                                   :cnt cnt})))
                         (sort-by (juxt (comp - :score) (comp - :cnt) (comp str :store-id))))
                best (first scored)
                second-best (second scored)
                margin (- (double (or (:score best) 0.0)) (double (or (:score second-best) 0.0)))]
            (when (and best
                    (>= (double (:score best)) pj-name-sim-threshold)
                    (or (nil? second-best)
                      (>= margin pj-name-sim-margin)))
              (:store-id best)))

          :else
          nil)))))
