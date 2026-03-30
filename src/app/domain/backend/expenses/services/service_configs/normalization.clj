(ns app.domain.backend.expenses.services.service-configs.normalization
  "Shared normalization helpers for expenses services."
  (:require
    [clojure.string :as str])
  (:import
    [java.text Normalizer Normalizer$Form]))

(defn- join-single-letter-tokens
  [value]
  (let [tokens (->> (str/split (or value "") #"\s+")
                 (remove str/blank?))]
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
        (str/join " " acc))
      value)))

(def ^:private html-entity->text
  {"amp" "&"
   "lt" "<"
   "gt" ">"
   "quot" "\""
   "apos" "'"
   "nbsp" " "})

(defn- codepoint->str
  [n]
  (try
    (when (and (number? n)
            (<= 0 (long n) 1114111))
      (String. (Character/toChars (int n))))
    (catch Exception _
      nil)))

(defn- unescape-html-entities-once
  [s]
  (let [s (some-> s str)]
    (when s
      (-> s
        (str/replace
          #"&#x([0-9A-Fa-f]{1,6});?"
          (fn [[m hex]]
            (try
              (let [n (Long/parseLong hex 16)]
                (or (codepoint->str n) m))
              (catch Exception _
                m))))
        (str/replace
          #"&#([0-9]{1,7});?"
          (fn [[m dec]]
            (try
              (let [n (Long/parseLong dec 10)]
                (or (codepoint->str n) m))
              (catch Exception _
                m))))
        (str/replace
          #"&([A-Za-z]+);?"
          (fn [[m ent]]
            (or (get html-entity->text (str/lower-case ent)) m)))))))

(defn unescape-html-entities
  [s]
  (let [s (some-> s str)]
    (when s
      (loop [i 0
             cur s]
        (let [nxt (unescape-html-entities-once cur)]
          (if (or (>= i 2) (= nxt cur))
            nxt
            (recur (inc i) nxt)))))))

(defn normalize-supplier-key
  [name]
  (when name
    (let [legal-suffix-tokens #{"doo" "dd" "ad" "llc" "ltd" "inc" "gmbh" "ag"}
          canonicalize-tokens
          (fn [tokens]
            (if (seq tokens)
              (let [idx (->> tokens
                          (map-indexed vector)
                          (keep (fn [[i t]] (when (contains? legal-suffix-tokens t) i)))
                          first)]
                (if (and (some? idx) (pos? (long idx)))
                  (subvec (vec tokens) 0 (long idx))
                  (vec tokens)))
              []))]
      (-> name
        unescape-html-entities
        str/trim
        (Normalizer/normalize Normalizer$Form/NFD)
        (str/replace #"\p{M}+" "")
        (str/replace #"Đ" "D")
        (str/replace #"đ" "d")
        str/lower-case
        (str/replace #"[^a-z0-9\s-]" "")
        (str/replace #"-" " ")
        join-single-letter-tokens
        (str/replace #"\s+" " ")
        str/trim
        (str/split #"\s+")
        (->> (remove str/blank?) vec canonicalize-tokens)
        (->> (str/join "-"))))))

(defn- canonicalize-store-number-abbreviations
  [value]
  (when value
    (-> value
      (str/replace #"(?iu)\bbroj\.?(?=\s*\d)" "br ")
      (str/replace #"(?iu)\bbr\.?(?=\s*\d)" "br "))))

(defn normalize-store-key
  [value]
  (when value
    (-> value
      unescape-html-entities
      canonicalize-store-number-abbreviations
      str/trim
      (Normalizer/normalize Normalizer$Form/NFD)
      (str/replace #"\p{M}+" "")
      (str/replace #"Đ" "D")
      (str/replace #"đ" "d")
      str/lower-case
      (str/replace #"[^a-z0-9\s-]" "")
      (str/replace #"-" " ")
      join-single-letter-tokens
      (str/replace #"\s+" " ")
      str/trim
      (str/split #"\s+")
      (->> (remove str/blank?)
        (str/join "-")))))

(defn normalize-manufacturer-key
  [name]
  (when name
    (-> name
      unescape-html-entities
      str/trim
      (Normalizer/normalize Normalizer$Form/NFD)
      (str/replace #"\p{M}+" "")
      (str/replace #"Đ" "D")
      (str/replace #"đ" "d")
      str/lower-case
      (str/replace #"[^a-z0-9\s-]" "")
      (str/replace #"-" " ")
      join-single-letter-tokens
      (str/replace #"\s+" " ")
      str/trim
      (str/split #"\s+")
      (->> (remove str/blank?)
        (str/join "-")))))

(defn normalize-city-key
  [city-name]
  (when city-name
    (some-> city-name
      unescape-html-entities
      str/trim
      not-empty
      (Normalizer/normalize Normalizer$Form/NFD)
      (str/replace #"\p{M}+" "")
      (str/replace #"Đ" "D")
      (str/replace #"đ" "d")
      str/lower-case
      (str/replace #"\s+" " ")
      str/trim
      not-empty)))
