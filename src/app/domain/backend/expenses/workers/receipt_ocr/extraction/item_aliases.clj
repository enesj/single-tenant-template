(ns app.domain.backend.expenses.workers.receipt-ocr.extraction.item-aliases
  (:require
    [app.domain.backend.expenses.services.article-aliases :as aliases]
    [app.domain.backend.expenses.services.articles :as articles]
    [clojure.string :as str]
    [taoensso.timbre :as log]))

(defn valid-alias-label?
  [raw-label]
  (let [raw-label* (some-> raw-label str str/trim)
        normalized (articles/normalize-alias-label raw-label*)]
    (and (not (str/blank? raw-label*))
      (not (str/blank? normalized)))))

(defn- alias-action
  [alias-row]
  (when (:id alias-row)
    (if (:created? alias-row)
      :created
      :reused)))

(def ^:private min-suspicious-alias-normalized-length
  2)

(defn- alias-label-diagnostics
  [raw-label unit]
  (let [raw-label* (some-> raw-label str str/trim)
        normalized (articles/normalize-alias-label raw-label*)]
    {:raw_label raw-label*
     :raw_label_length (some-> raw-label* count)
     :raw_label_normalized normalized
     :raw_label_normalized_length (some-> normalized count)
     :unit unit}))

(defn- suspicious-short-alias-label?
  [raw-label]
  (let [normalized (articles/normalize-alias-label (some-> raw-label str str/trim))]
    (and (seq normalized)
      (< (count normalized) min-suspicious-alias-normalized-length))))

(defn auto-create-aliases!
  [db supplier-id extraction opts]
  (let [auto-create-articles? (true? (:auto-create-articles? opts))]
    (when (and (map? extraction) (sequential? (:items extraction)))
      (mapv
        (fn [idx {:keys [raw_label unit qty unit_price line_total] :as _item}]
          (let [raw-label* (some-> raw_label str str/trim)
                suspicious-short? (suspicious-short-alias-label? raw-label*)
                log-context (merge
                              {:receipt-id (:receipt-id opts)
                               :item-index idx
                               :supplier-id supplier-id
                               :supplier-guess (:supplier-guess opts)
                               :provider (:provider opts)
                               :model (:model opts)
                               :auto-create-articles? auto-create-articles?
                               :qty qty
                               :unit_price unit_price
                               :line_total line_total}
                              (alias-label-diagnostics raw-label* unit))]
            (when suspicious-short?
              (log/warn "Receipt extraction item has suspicious short alias label before persistence"
                log-context))
            (if-not (valid-alias-label? raw-label*)
              {:raw_label raw-label*
               :unit unit
               :article_alias_id nil
               :article_id nil
               :alias_action nil}
              (try
                (let [alias-row (aliases/find-or-create-alias! db supplier-id raw-label* unit)
                      alias-id (:id alias-row)
                      alias-action (alias-action alias-row)
                      existing-article-id (:article_id alias-row)
                      article-id
                      (cond
                        existing-article-id existing-article-id

                        (and auto-create-articles? alias-id)
                        (let [article (articles/find-or-create-article-by-canonical-name! db raw-label* unit)
                              article-id (:id article)]
                          (when article-id
                            (aliases/map-alias-to-article! db alias-id article-id))
                          article-id)

                        :else nil)]
                  (when suspicious-short?
                    (log/warn "Suspicious short alias label reached alias persistence"
                      (assoc log-context
                        :alias-id alias-id
                        :alias-action alias-action
                        :existing-article-id existing-article-id
                        :article-id article-id)))
                  {:raw_label raw-label*
                   :unit unit
                   :article_alias_id alias-id
                   :article_id article-id
                   :alias_action alias-action})
                (catch Exception e
                  (log/warn e "Failed to auto-create article alias/article from receipt extraction item"
                    {:receipt-id (:receipt-id opts)
                     :item-index idx
                     :supplier-id supplier-id
                     :supplier-guess (:supplier-guess opts)
                     :provider (:provider opts)
                     :model (:model opts)
                     :raw_label raw-label*
                     :unit unit
                     :qty qty
                     :unit_price unit_price
                     :line_total line_total
                     :auto-create-articles? auto-create-articles?})
                  {:raw_label raw-label*
                   :unit unit
                   :article_alias_id nil
                   :article_id nil
                   :alias_action nil})))))
        (range)
        (:items extraction)))))
