(ns app.shared.fuzzy
  "Pure fuzzy matching helpers.")

(defn levenshtein-distance
  "Return the Levenshtein edit distance between two strings.

  Nil inputs are treated as empty strings."
  [a b]
  (let [a (or a "")
        b (or b "")
        alen (count a)
        blen (count b)]
    (cond
      (= a b) 0
      (zero? alen) blen
      (zero? blen) alen
      :else
      (loop [i 0
             prev (vec (range (inc blen)))]
        (if (= i alen)
          (nth prev blen)
          (let [curr (loop [j 0
                            curr [(inc i)]]
                       (if (= j blen)
                         curr
                         (let [cost (if (= (nth a i) (nth b j)) 0 1)
                               deletion (inc (nth prev (inc j)))
                               insertion (inc (nth curr j))
                               substitution (+ (nth prev j) cost)]
                           (recur (inc j) (conj curr (min deletion insertion substitution))))))]
            (recur (inc i) curr)))))))

(defn levenshtein-ratio
  "Return similarity ratio in [0,1], where 1 means identical strings.

  Ratio is computed as: 1 - (distance / max-length).
  Nil inputs are treated as empty strings, and empty/empty is 1.0."
  [a b]
  (let [a (or a "")
        b (or b "")
        maxlen (max (count a) (count b))]
    (if (zero? maxlen)
      1.0
      (- 1.0 (/ (double (levenshtein-distance a b))
               (double maxlen))))))