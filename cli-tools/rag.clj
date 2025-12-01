#!/usr/bin/env bb
;; Simple docs RAG: build a TF-IDF index and query it.
;; - Index: chunks Markdown under docs/, extracts optional <!-- ai: {...} --> metadata,
;;          computes TF-IDF vectors, stores top terms per chunk in docs/rag/index.edn
;; - Query: loads index, computes query vector, cosine ranks chunks, prints top-k

(ns rag
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import (java.time Instant)
           (java.nio.file Files Paths)
           (java.nio.file.attribute FileTime)))

(defn usage []
  (println "Usage:")
  (println "  bb cli-tools/rag.clj index [docs-dir] [out-dir]")
  (println "  bb cli-tools/rag.clj query -q 'text' [-k 5] [-n namespace] [-t tag] [-m 0.02]")
  (println "Defaults: docs-dir=docs, out-dir=docs/rag, k=8, m=0.02")
  (System/exit 1))

(defn list-md [^String root]
  (->> (file-seq (io/file root))
       (filter #(.isFile ^java.io.File %))
       (filter #(re-find #"\.md$" (.getName ^java.io.File %)))
       (map #(.getPath ^java.io.File %))))

(defn read-file [p]
  (slurp (io/file p)))

(def bm25-k1 1.4)
(def bm25-b 0.75)
(def cosine-weight 0.65)
(def bm25-weight (- 1.0 cosine-weight))

(defn parse-ai-meta [s]
  (try
    ;; Only honor ai-metadata if it appears near the top of the file
    (let [head (->> (str/split s #"\r?\n") (take 40) (str/join "\n"))]
      (when-let [[_ m] (re-find #"(?s)<!--\s*ai:\s*(\{.*?\})\s*-->" head)]
        (edn/read-string m)))
    (catch Exception _ nil)))

(defn slug [^String heading]
  (-> heading
      str/lower-case
      (str/replace #"[^a-z0-9\s-]" "")
      (str/replace #"\s+" "-")
      (str/replace #"-+" "-")))

(defn chunk-md [path s]
  (let [lines (vec (str/split s #"\r?\n"))
        meta (parse-ai-meta s)
        n (count lines)
        headings (->> (map-indexed vector lines)
                      (keep (fn [[i line]]
                              (when-let [[_ h] (re-matches #"^(?:###|##|#)\s+(.*)" line)]
                                {:idx i :heading (str/trim h)}))))
        points (concat [{:idx 0 :heading "intro"}] headings [{:idx n :heading nil}])
        chunks (->> (partition 2 1 points)
                    (map (fn [[a b]]
                           (let [start (:idx a)
                                 end (:idx b)
                                 heading (:heading a)
                                 heading* (or heading "intro")
                                 anchor (slug heading*)
                                 ;; exclude the heading line from content for non-intro sections
                                 content-start (if (= heading* "intro") start (inc start))
                                 content-lines (subvec lines content-start end)
                                 text (-> (str/join "\n" content-lines) str/trim)]
                             {:path path
                              :heading heading*
                              :anchor anchor
                              :text text
                              :line (long (inc content-start))
                              :namespaces (:namespaces meta)
                              :tags (:tags meta)
                              :kind (:kind meta)})))
                    (remove (comp str/blank? :text)))]
    chunks))

(def stopwords
  (set ["the" "and" "for" "are" "with" "that" "this" "from" "your" "you" "not" "but"
        "have" "has" "was" "were" "will" "can" "all" "any" "into" "use" "using" "how"
        "what" "when" "where" "why" "which" "also" "more" "most" "such" "their" "there"
        "about" "only" "other" "some" "than" "then" "them" "these" "those" "both"
        "within" "each" "per" "via" "between" "across" "after" "before" "under" "over"
        "on" "in" "at" "of" "to" "by" "a" "an" "is" "it" "be" "or" "as" "if" "we" "our"
        "do" "does" "did" "no" "yes" "one" "two" "three" "should" "may" "might" "could"
        "up" "out" "off" "into" "across" "into" "docs" "doc" "readme" "overview"]))

(defn tokenize [^String s]
  (let [s1 (-> s
               str/lower-case
               (str/replace #"[`*_>#+=\-]" " ")
               (str/replace #"[^a-z0-9/.]+" " "))]
    (->> (str/split s1 #"\s+")
         (map #(str/replace % #"\.+$" ""))
         (filter #(>= (count %) 2))
         (remove stopwords))))

(defn tf [tokens-or-freq]
  (let [freq (if (map? tokens-or-freq) tokens-or-freq (frequencies tokens-or-freq))
        denom (Math/sqrt (reduce + (map #(* % %) (vals freq))))]
    (into {} (map (fn [[t c]] [t (/ c (max 1.0 denom))]) freq))))

(defn doc-freqs [docs]
  (reduce (fn [m tokens]
            (reduce (fn [m2 t] (update m2 t (fnil inc 0))) m (distinct tokens)))
          {} docs))

(defn tfidf-idf [N df]
  (into {}
        (map (fn [[t d]] [t (Math/log (/ (+ N 1.0) (+ d 1.0)))]))
        df))

(defn bm25-idf-map [N df]
  (into {}
        (map (fn [[t d]]
               (let [numer (+ (- N d) 0.5)
                     denom (+ d 0.5)
                     ratio (if (pos? denom) (/ numer denom) 0.0)]
                 [t (Math/log (+ 1.0 ratio))])))
        df))

(defn dot [a b]
  (let [ks (if (< (count a) (count b)) (keys a) (keys b))]
    (reduce (fn [s k] (+ s (* (double (get a k 0.0)) (double (get b k 0.0))))) 0.0 ks)))

(defn l2 [v]
  (Math/sqrt (reduce (fn [s [_ w]] (+ s (* w w))) 0.0 v)))

(defn top-n [m n]
  (->> m (sort-by val >) (take n) (into {})))

(defn ensure-dir [p]
  (let [f (io/file p)]
    (.mkdirs f)
    p))

(defn now [] (str (Instant/now)))

(defn build-index [docs-dir out-dir]
  (let [paths (->> (list-md docs-dir)
                   (remove #(str/starts-with? % (str docs-dir "/rag/"))))
        chunks (vec (mapcat (fn [p] (chunk-md p (read-file p))) paths))
        token-seqs (mapv (comp tokenize :text) chunks)
        doc-lens (mapv count token-seqs)
        total-len (reduce + 0 doc-lens)
        avg-len (if (seq doc-lens)
                  (/ (double total-len) (double (count doc-lens)))
                  1.0)
        df (doc-freqs token-seqs)
        N (double (max 1 (count token-seqs)))
        idf-map (tfidf-idf N df)
        bm25-idf (bm25-idf-map N df)
        chunks* (mapv (fn [c tokens]
                        (let [freqs (frequencies tokens)
                              tfm (tf freqs)
                              wraw (reduce (fn [m [t tfw]]
                                             (if-let [iw (get idf-map t)]
                                               (assoc m t (* tfw iw)) m))
                                           {} tfm)
                              weights (top-n wraw 64)
                              norm (l2 weights)
                              preview (-> (:text c) (subs 0 (min 200 (count (:text c)))))
                              token-count (count tokens)]
                          (-> c
                              (assoc :weights weights)
                              (assoc :norm norm)
                              (assoc :preview preview)
                              (assoc :token-count token-count)
                              (assoc :freqs freqs)
                              (dissoc :text))))
                      chunks token-seqs)
        idx {:meta {:built-at (now)
                    :docs-dir docs-dir
                    :out-dir out-dir
                    :file-count (count paths)
                    :chunk-count (count chunks*)
                    :avg-chunk-len avg-len}
             :idf idf-map
             :bm25-idf bm25-idf
             :chunks (vec chunks*)}]
    (ensure-dir out-dir)
    (spit (str out-dir "/index.edn") (pr-str idx))
    (println "Indexed" (count paths) "files," (count chunks*) "chunks →" (str out-dir "/index.edn"))))

(defn parse-args [args]
  (loop [m {} [a & more] args]
    (if-not a m
            (case a
              "-q" (recur (assoc m :q (first more)) (rest more))
              "-k" (recur (assoc m :k (Integer/parseInt (first more))) (rest more))
              "-n" (recur (assoc m :ns (first more)) (rest more))
              "-t" (recur (assoc m :tag (keyword (first more))) (rest more))
              "-m" (recur (assoc m :min (Double/parseDouble (first more))) (rest more))
              (recur (update m :rest (fnil conj []) a) more)))))

(defn load-index [p]
  (-> (slurp p) edn/read-string))

(defn cosine [qa qn wa wn]
  (let [num (dot qa wa)
        den (* (or qn (l2 qa)) (or wn (l2 wa)))]
    (if (pos? den) (/ num den) 0.0)))

(defn match-filter? [chunk {:keys [ns tag]}]
  (and (if ns (some #(= (str %) ns) (:namespaces chunk)) true)
       (if tag (some #{tag} (:tags chunk)) true)))

(defn bm25-score [chunk query-terms bm25-idf avg-len]
  (let [freqs (:freqs chunk)
        bm25-idf (or bm25-idf {})
        avg-len (double (max 1.0 (or avg-len 1.0)))]
    (if (and freqs (seq query-terms))
      (let [doc-len (double (max 1.0 (or (:token-count chunk) 1.0)))
            norm (+ (- 1.0 bm25-b) (* bm25-b (/ doc-len avg-len)))]
        (reduce
          (fn [sum term]
            (let [freq (double (get freqs term 0))]
              (if (zero? freq)
                sum
                (let [idf (double (get bm25-idf term 0.0))
                      numer (* freq (+ bm25-k1 1.0))
                      denom (+ freq (* bm25-k1 norm))]
                  (+ sum (* idf (/ numer denom)))))))
          0.0
          query-terms))
      0.0)))

(defn query-index [{:keys [q k min ns tag] :as opts}]
  (when (str/blank? q) (println "Missing -q query string") (System/exit 1))
  (let [idx (load-index "docs/rag/index.edn")
        idf-map (:idf idx)
        bm25-idf (:bm25-idf idx)
        avg-len (get-in idx [:meta :avg-chunk-len])
        query-tokens (tokenize q)
        qfreqs (frequencies query-tokens)
        qvec (reduce (fn [m [t c]]
                       (if-let [iw (get idf-map t)]
                         (assoc m t (* c iw)) m))
                     {}
                     qfreqs)
        qn (l2 qvec)
        k (or k 8)
        min (or min 0.02)
        results (->> (:chunks idx)
                     (filter #(match-filter? % opts))
                     (map (fn [ch]
                            (let [cos (cosine qvec qn (:weights ch) (:norm ch))
                                  bm25 (bm25-score ch (keys qfreqs) bm25-idf avg-len)
                                  score (+ (* cosine-weight cos) (* bm25-weight bm25))]
                              (assoc ch :score score :cosine cos :bm25 bm25))))
                     (filter #(>= (:score %) min))
                     (sort-by :score >)
                     (take k))]
    (doseq [{:keys [score path line anchor heading preview cosine bm25]} results]
      (println (format "%.3f  %s:%d — %s (cos %.3f | bm25 %.3f)"
                       score path (long line) heading cosine bm25))
      (println (str "      see: " path "#" anchor))
      (println (str "      " (-> preview (str/replace #"\s+" " ")))))))

(defn -main [& args]
  (let [[cmd & more] args]
    (case cmd
      "index" (let [[docs-dir out-dir] more
                      docs-dir (or docs-dir "docs")
                      out-dir (or out-dir "docs/rag")] (build-index docs-dir out-dir))
      "query" (let [opts (parse-args more)] (query-index opts))
      (usage))))

(apply -main *command-line-args*)
