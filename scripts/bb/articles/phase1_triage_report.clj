#!/usr/bin/env bb

(ns scripts.bb.articles.phase1-triage-report
  (:require
    [clojure.edn :as edn]
    [clojure.string :as str]))

(def summary
  (edn/read-string (slurp "tmp/phase1-triage-summary.edn")))

(def supplier-clusters (:supplier-clusters summary))
(def variant-risk-groups (:variant-risk-groups summary))

(def risk-alias-ids
  (->> variant-risk-groups
    (mapcat :aliases)
    (mapcat :alias_ids)
    (remove nil?)
    set))

(defn md-escape [s]
  (-> (str s)
    (str/replace "\\" "\\\\")
    (str/replace "`" "\\`")))

(defn fmt-alias-line
  [{:keys [alias_id raw_label raw_label_normalized]}]
  (let [risk? (contains? risk-alias-ids alias_id)
        label (md-escape (or raw_label ""))
        nk (md-escape (or raw_label_normalized ""))]
    (str "- "
      (when risk? "[VARIANT-RISK] ")
      "`" alias_id "` "
      label
      (when (seq nk) (str "  _(norm: `" nk "`)_")))))

(defn supplier-score
  "Higher score = earlier in recommended order.

  We prioritize high-volume suppliers, but penalize suppliers that contain aliases in
  VARIANT RISK groups (more likely to require careful size/variant separation)."
  [{:keys [alias-count risky-alias-count]}]
  (- alias-count (* 3 risky-alias-count)))

(defn recommended-order
  []
  (let [stats (:supplier-risk-stats summary)]
    (->> stats
      (map (fn [{:keys [supplier alias-count risky-alias-count] :as s}]
             (assoc s
               :score (supplier-score {:alias-count alias-count
                                       :risky-alias-count risky-alias-count}))))
      (sort-by (juxt (comp - :score) (comp - :alias-count) :supplier))
      vec)))

(defn fmt-variant-risk-group
  [{:keys [brand-key alias-count sizes aliases]}]
  (str "### " (md-escape brand-key) "\n\n"
    "- alias-count: " alias-count "\n"
    "- sizes: " (if (seq sizes) (str/join ", " sizes) "(none detected)") "\n"
    "- constraint: **Do not map different sizes/pack tokens (or restaurant vs retail) to the same article.**\n\n"
    (str/join "\n"
      (for [{:keys [raw_label normalized supplier sizes restaurant? alias_ids]} aliases]
        (str "- "
          "`" (or (some-> alias_ids first) "(alias_id?)") "` "
          (md-escape (or raw_label ""))
          " — " (md-escape (or supplier ""))
          (when restaurant? " _(restaurant)_")
          (when (seq sizes) (str " _(sizes: " (str/join ", " sizes) ")_"))
          (when (or (nil? alias_ids) (empty? alias_ids)) " _(could not join alias_id)_"))))
    "\n"))

(defn -main
  [& _]
  (let [order (recommended-order)
        out (str
              "# Phase 1 triage report\n\n"
              "Generated: " (:generated-at summary) "\n\n"
              "- aliases: " (:alias-count summary) "\n"
              "- suppliers: " (:supplier-count summary) "\n"
              "- brand groups: " (:brand-group-count summary) "\n"
              "- VARIANT RISK groups: " (:variant-risk-group-count summary) "\n"
              "- OCR noise flagged (conservative heuristic): " (:ocr-noise-count summary) "\n\n"

              "## Recommended processing order (supplier clusters)\n\n"
              (str/join
                "\n"
                (map-indexed
                  (fn [i {:keys [supplier alias-count risky-alias-count score]}]
                    (str (inc i) ". **" (md-escape supplier) "** — " alias-count " aliases"
                      (when (pos? risky-alias-count)
                        (str ", " risky-alias-count " in VARIANT RISK"))
                      " _(score: " score ")_"))
                  order))
              "\n\n"

              "## VARIANT RISK clusters\n\n"
              (if (seq variant-risk-groups)
                (str/join "\n" (map fmt-variant-risk-group variant-risk-groups))
                "(none)\n")

              "\n## Supplier clusters with aliases\n\n"
              (str/join
                "\n"
                (for [{:keys [supplier alias-count aliases]} supplier-clusters]
                  (str "### " (md-escape supplier) "\n\n"
                    "Aliases (" alias-count "):\n"
                    (str/join "\n" (map fmt-alias-line aliases))
                    "\n")))

              "\n## OCR noise candidates\n\n"
              (if (pos? (:ocr-noise-count summary))
                (str/join
                  "\n"
                  (for [{:keys [alias_id raw_label reason]} (mapcat :ocr-noise supplier-clusters)]
                    (str "- `" alias_id "` " (md-escape raw_label) " — " (md-escape reason))))
                "None flagged by conservative heuristic.\n"))]

    (spit "tmp/phase1-triage-report.md" out)
    (println "wrote tmp/phase1-triage-report.md")))

(apply -main *command-line-args*)
