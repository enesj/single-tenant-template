(ns app.domain.backend.expenses.services.suppliers.similarity
  (:require
    [clojure.string :as str])
  (:import
    [java.text Normalizer Normalizer$Form]))

(defn- strip-diacritics
  [value]
  (when value
    (-> value
      (Normalizer/normalize Normalizer$Form/NFD)
      (str/replace #"\p{M}+" "")
      (str/replace #"Đ" "D")
      (str/replace #"đ" "d"))))

(defn- join-single-letter-runs
  [tokens]
  (if (seq tokens)
    (let [{:keys [acc run]}
          (reduce
            (fn [{:keys [acc run]} token]
              (if (= 1 (count token))
                {:acc acc
                 :run (conj run token)}
                {:acc (cond-> acc
                        (seq run) (conj (apply str run))
                        true (conj token))
                 :run []}))
            {:acc [] :run []}
            tokens)
          acc (cond-> acc (seq run) (conj (apply str run)))]
      acc)
    []))

(def ^:private legal-suffix-tokens-sim
  #{"DOO" "DD" "AD" "LLC" "LTD" "INC" "GMBH" "AG"})

(defn- canonicalize-company-tokens
  [tokens]
  (if (seq tokens)
    (let [idx (->> tokens
                (map-indexed vector)
                (keep (fn [[i t]] (when (contains? legal-suffix-tokens-sim t) i)))
                first)]
      (if (and (some? idx) (pos? (long idx)))
        (subvec (vec tokens) 0 (long idx))
        (vec tokens)))
    []))

(defn normalize-for-similarity-tokens
  [value]
  (when-let [value* (some-> value str str/trim not-empty)]
    (let [normalized (-> value*
                       strip-diacritics
                       str/upper-case
                       (str/replace #"[^A-Z0-9]+" " ")
                       (str/replace #"\s+" " ")
                       str/trim)
          tokens (->> (str/split normalized #"\s+")
                   (remove str/blank?)
                   vec)]
      (-> tokens
        join-single-letter-runs
        canonicalize-company-tokens))))

(defn normalize-for-similarity
  [value]
  (when-let [tokens (normalize-for-similarity-tokens value)]
    (when (seq tokens)
      (str/join " " tokens))))

(defn normalize-for-prefix
  [value]
  (some-> value
    normalize-for-similarity
    (str/replace #"\s+" "")))

(defn normalized-length
  [value]
  (count (str/replace (or value "") #"\s+" "")))

(defn- digits-seq
  [value]
  (let [digits (some->> (re-seq #"\d+" (str value)) (str/join "") not-empty)]
    digits))

(defn- next-row
  [previous current other-seq]
  (reduce
    (fn [row [diagonal above other]]
      (let [update-val (if (= other current)
                         diagonal
                         (inc (min diagonal above (peek row))))]
        (conj row update-val)))
    [(inc (first previous))]
    (map vector previous (next previous) other-seq)))

(defn- levenshtein-distance
  [a b]
  (let [a* (seq (or a ""))
        b* (seq (or b ""))]
    (cond
      (empty? a*) (count b*)
      (empty? b*) (count a*)
      :else
      (peek
        (reduce
          (fn [previous current]
            (next-row previous current b*))
          (vec (range (inc (count b*))))
          a*)))))

(defn- levenshtein-ratio
  [a b]
  (let [max-len (max (count (or a "")) (count (or b "")))]
    (if (zero? max-len)
      0.0
      (- 1.0 (/ (double (levenshtein-distance a b)) (double max-len))))))

(defn- candidate-accepted?
  [input-len input-prefix candidate-prefix ratio]
  (cond
    (<= input-len 2)
    (and (seq input-prefix)
      (str/starts-with? candidate-prefix input-prefix))

    (<= input-len 4) (>= ratio 0.80)
    (<= input-len 7) (>= ratio 0.70)
    :else (>= ratio 0.60)))

(defn- score-candidate
  [input-norm input-prefix input-len input-digits {:keys [name] :as place}]
  (let [candidate-norm (normalize-for-similarity name)
        candidate-prefix (normalize-for-prefix name)
        candidate-digits (digits-seq name)
        ratio (levenshtein-ratio input-norm candidate-norm)]
    (when (and (seq candidate-norm)
            (or (nil? input-digits)
              (and candidate-digits (str/includes? candidate-digits input-digits)))
            (candidate-accepted? input-len input-prefix candidate-prefix ratio))
      {:place place
       :normalized candidate-norm
       :score (if (<= input-len 2) 1.0 ratio)})))

(defn best-place-candidate
  [input places]
  (let [input-norm (normalize-for-similarity input)
        input-prefix (normalize-for-prefix input)
        input-len (normalized-length input-norm)
        input-digits (digits-seq input)]
    (when (and (seq input-norm) (seq places))
      (->> places
        (keep (partial score-candidate input-norm input-prefix input-len input-digits))
        (sort-by :score >)
        first))))
