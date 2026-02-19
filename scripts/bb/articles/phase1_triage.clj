#!/usr/bin/env bb

(ns scripts.bb.articles.phase1-triage
  (:require
    [clojure.data.json :as json]
    [clojure.edn :as edn]
    [clojure.string :as str]))

(defn ocr-noise-reason
  "Heuristic OCR-noise detector.

  Conservative: only flags entries that look truly non-product-like.
  (We do NOT delete anything in Phase 1; we only surface candidates.)"
  [{:keys [raw_label raw_label_normalized]}]
  (let [raw (or raw_label "")
        norm (or raw_label_normalized "")
        trimmed (str/trim raw)
        trimmed-norm (str/trim norm)
        alnum (count (re-seq #"[A-Za-z0-9]" trimmed))]
    (cond
      (str/blank? trimmed) "blank raw_label"
      (str/blank? trimmed-norm) "blank raw_label_normalized"
      (re-matches #"(?i)^(x+|\*+|\-+|\.+|,+|_+)$" trimmed)
      (str "punctuation-only: " (pr-str trimmed))

      (re-matches #"^\d+$" trimmed) (str "digits-only: " trimmed)
      (< alnum 3) (str "too-few-alnum-chars(" alnum "): " (pr-str trimmed))
      (re-find #"(?i)\b(unknown|n/?a|na)\b" trimmed)
      (str "looks-like-placeholder: " (pr-str trimmed))

      :else nil)))

(defn supplier-display
  [supplier]
  (if (str/blank? (str supplier))
    "(no supplier)"
    supplier))

(defn read-backlog!
  []
  (edn/read-string (slurp "tmp/phase1-backlog.edn")))

(defn read-variant-groups!
  []
  (json/read-str (slurp "tmp/phase1-variant-groups.json") :key-fn keyword))

(defn build-alias-id-index
  "Index aliases by [supplier raw_label raw_label_normalized] => [alias_id ...].

  This allows us to join `alias_id` into the brand-grouping output which does not
  currently include it."
  [aliases]
  (reduce (fn [m {:keys [alias_id supplier raw_label raw_label_normalized]}]
            (update m [supplier raw_label raw_label_normalized] (fnil conj []) alias_id))
    {}
    aliases))

(defn enrich-variant-risk-groups
  [variant-groups alias-id-index]
  (->> variant-groups
    (filter :variant-risk?)
    (mapv (fn [{:keys [aliases] :as g}]
            (assoc g :aliases
              (mapv (fn [{:keys [raw_label normalized supplier] :as a}]
                      (assoc a :alias_ids (get alias-id-index [supplier raw_label normalized])))
                aliases))))))

(defn supplier-clusters
  [aliases]
  (->> aliases
    (group-by (comp supplier-display :supplier))
    (map (fn [[supplier items]]
           {:supplier supplier
            :alias-count (count items)
            :aliases (mapv (fn [{:keys [alias_id raw_label raw_label_normalized]}]
                             {:alias_id alias_id
                              :raw_label raw_label
                              :raw_label_normalized raw_label_normalized})
                       items)
            :ocr-noise (->> items
                         (keep (fn [a]
                                 (when-let [r (ocr-noise-reason a)]
                                   {:alias_id (:alias_id a)
                                    :raw_label (:raw_label a)
                                    :reason r})))
                         vec)}))
    (sort-by (juxt (comp - :alias-count) :supplier))
    vec))

(defn ocr-noise-list
  [supplier-clusters]
  (->> supplier-clusters
    (mapcat :ocr-noise)
    (sort-by (juxt :reason :alias_id))
    vec))

(defn supplier-risk-stats
  "Count, per supplier, how many aliases appear inside any VARIANT RISK group.

  This is *not* a quality score; it just helps prioritize suppliers where extra care
  is needed."
  [supplier-clusters variant-risk-groups]
  (let [supplier->risky-keys
        (reduce
          (fn [m {:keys [aliases]}]
            (reduce
              (fn [m2 {:keys [supplier raw_label normalized]}]
                (update m2 (supplier-display supplier) (fnil conj #{}) [raw_label normalized]))
              m
              aliases))
          {}
          variant-risk-groups)]
    (->> supplier-clusters
      (mapv (fn [{:keys [supplier alias-count]}]
              {:supplier supplier
               :alias-count alias-count
               :risky-alias-count (count (get supplier->risky-keys supplier #{}))})))))

(defn -main
  []
  (let [aliases (read-backlog!)
        variant-groups (read-variant-groups!)
        alias-id-index (build-alias-id-index aliases)
        variant-risk-groups (enrich-variant-risk-groups variant-groups alias-id-index)
        supplier-clusters* (supplier-clusters aliases)
        ocr-noise (ocr-noise-list supplier-clusters*)
        supplier-risk (supplier-risk-stats supplier-clusters* variant-risk-groups)
        summary {:generated-at (str (java.time.LocalDate/now))
                 :alias-count (count aliases)
                 :supplier-count (count supplier-clusters*)
                 :brand-group-count (count variant-groups)
                 :variant-risk-group-count (count variant-risk-groups)
                 :ocr-noise-count (count ocr-noise)
                 :supplier-risk-stats supplier-risk
                 :supplier-clusters supplier-clusters*
                 :variant-risk-groups variant-risk-groups}]

    (spit "tmp/phase1-triage-summary.edn" (with-out-str (pr summary)))

    (println "alias-count" (count aliases))
    (println "supplier-count" (count supplier-clusters*))
    (println "brand-group-count" (count variant-groups))
    (println "variant-risk-group-count" (count variant-risk-groups))
    (println "ocr-noise-count" (count ocr-noise))
    (println "wrote tmp/phase1-triage-summary.edn")))

(apply -main *command-line-args*)