(ns app.domain.backend.expenses.services.stores.matching
  "Heuristic store matching.

  Used to map a store alias normalized key to an existing store when receipt
  fingerprints are missing or insufficient.

  This is intentionally conservative: it only returns a match when it is
  unambiguous."
  (:require
    [clojure.string :as str]))

(def ^:private fuzzy-similarity-threshold
  0.92)

(def ^:private fuzzy-similarity-margin
  0.03)

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

(defn- similarity
  "Return similarity in [0..1], based on normalized Levenshtein distance."
  [a b]
  (let [a (normalize-for-match a)
        b (normalize-for-match b)
        maxlen (max (.length ^String a) (.length ^String b))]
    (if (zero? maxlen)
      1.0
      (- 1.0 (/ (double (levenshtein-distance a b)) (double maxlen))))))

(defn match-store
  "Match a store candidate by comparing the target normalized key to candidate keys.

  `candidates` must be a seq of maps containing:
  - :id  (uuid)
  - :key (normalized string)

  Returns {:store-id uuid :match :exact|:loose|:fuzzy :score n} or nil."
  [target-key candidates]
  (let [target-key (some-> target-key str/trim not-empty)
        candidates (->> candidates
                     (keep (fn [{:keys [id key]}]
                             (let [k (some-> key str/trim not-empty)]
                               (when (and id (seq k))
                                 {:id id :key k}))))
                     vec)]
    (when (and (seq target-key) (seq candidates))
      (let [exact (some (fn [{:keys [id key]}]
                          (when (= key target-key)
                            {:store-id id :match :exact :score 1.0}))
                    candidates)]
        (or exact
          (let [target-loose (loose-key target-key)
                loose-matches (->> candidates
                                (filter (fn [{:keys [key]}]
                                          (= (loose-key key) target-loose)))
                                vec)]
            (cond
              (= 1 (count loose-matches))
              {:store-id (:id (first loose-matches)) :match :loose :score 1.0}

              (> (count loose-matches) 1)
              nil

              :else
              (let [target-core (core-key target-key)
                    target-city (city-from-loose target-loose)
                    target-num (store-number target-loose)
                    scored (->> candidates
                             (map (fn [{:keys [id key]}]
                                    (let [cand-loose (loose-key key)
                                          cand-core (core-key key)
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
                                      {:id id :key key :score score :threshold threshold})))
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
                  {:store-id (:id best)
                   :match :fuzzy
                   :score best-score
                   :margin margin})))))))))