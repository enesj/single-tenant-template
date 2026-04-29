(ns app.domain.backend.expenses.services.duplicates.similarity
  (:require
    [clojure.set :as set]))

(defn make-union-find
  [ids]
  (atom (zipmap ids ids)))

(defn uf-find
  [uf x]
  (let [parent (get @uf x x)]
    (if (= parent x)
      x
      (let [root (uf-find uf parent)]
        (swap! uf assoc x root)
        root))))

(defn uf-union
  [uf x y]
  (let [rx (uf-find uf x)
        ry (uf-find uf y)]
    (when (not= rx ry)
      (swap! uf assoc ry rx))))

(defn uf-clusters
  [uf]
  (->> (keys @uf)
    (group-by #(uf-find uf %))
    vals
    (filter #(> (count %) 1))))

(defn clusters-from-pairs
  [rows pairs {:keys [limit max-cluster-size] :or {limit 50 max-cluster-size 10}}]
  (let [all-ids (distinct (concat (map :id_a pairs) (map :id_b pairs)))
        uf (make-union-find all-ids)]
    (doseq [{:keys [id_a id_b]} pairs]
      (uf-union uf id_a id_b))
    (let [clusters (uf-clusters uf)
          id->row (zipmap (map :id rows) rows)]
      (->> clusters
        (filter #(<= (count %) max-cluster-size))
        (mapv (fn [ids]
                {:members (->> ids
                            (mapv id->row)
                            (remove nil?)
                            vec)
                 :count   (count ids)}))
        (sort-by #(- (:count %)))
        (take limit)
        vec))))

(defn same-group?
  [group-col row-a row-b]
  (or (nil? group-col)
    (= (get row-a group-col) (get row-b group-col))))

(defn trigram-set
  [s]
  (let [value (str "  " (or s "") "  ")]
    (if (<= (count value) 3)
      #{value}
      (->> (range 0 (- (count value) 2))
        (map (fn [idx] (subs value idx (+ idx 3))))
        set))))

(defn trigram-similarity
  [a b]
  (let [ta (trigram-set a)
        tb (trigram-set b)
        denom (+ (count ta) (count tb))]
    (if (zero? denom)
      0.0
      (/ (* 2.0 (count (set/intersection ta tb))) denom))))

(defn levenshtein-distance
  [a b]
  (let [a (vec (or a ""))
        b (vec (or b ""))
        n (count a)
        m (count b)]
    (cond
      (zero? n) m
      (zero? m) n
      :else
      (loop [i 1
             prev (vec (range (inc m)))]
        (if (> i n)
          (peek prev)
          (let [curr (loop [j 1
                            row [i]]
                       (if (> j m)
                         (vec row)
                         (let [cost (if (= (nth a (dec i)) (nth b (dec j))) 0 1)
                               deletion (inc (nth prev j))
                               insertion (inc (peek row))
                               substitution (+ (nth prev (dec j)) cost)]
                           (recur (inc j) (conj row (min deletion insertion substitution))))))]
            (recur (inc i) curr)))))))

(defn detect-similar-pairs-in-memory
  [rows group-col score-fn matches?]
  (let [rows* (vec rows)
        total (count rows*)]
    (reduce
      (fn [acc idx]
        (let [row-a (nth rows* idx)
              key-a (:normalized_key row-a)]
          (if-not key-a
            acc
            (reduce
              (fn [acc2 jdx]
                (let [row-b (nth rows* jdx)
                      key-b (:normalized_key row-b)
                      score (when (and key-b (same-group? group-col row-a row-b))
                              (score-fn key-a key-b))]
                  (if (and score (matches? score))
                    (conj acc2 {:id_a (:id row-a)
                                :id_b (:id row-b)})
                    acc2)))
              acc
              (range (inc idx) total)))))
      []
      (range total))))