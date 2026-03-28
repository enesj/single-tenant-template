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

(defn auto-create-aliases!
  [db supplier-id extraction opts]
  (let [auto-create-articles? (true? (:auto-create-articles? opts))]
    (when (and (map? extraction) (sequential? (:items extraction)))
      (mapv
        (fn [{:keys [raw_label unit] :as _item}]
          (let [raw-label* (some-> raw_label str str/trim)]
            (if-not (valid-alias-label? raw-label*)
              {:raw_label raw-label*
               :unit unit
               :article_alias_id nil
               :article_id nil}
              (try
                (let [alias-row (aliases/find-or-create-alias! db supplier-id raw-label* unit)
                      alias-id (:id alias-row)
                      existing-article-id (:article_id alias-row)
                      article-id
                      (cond
                        existing-article-id existing-article-id

                        (and auto-create-articles? alias-id)
                        (let [article (articles/find-or-create-article-by-canonical-name! db raw-label*)
                              article-id (:id article)]
                          (when article-id
                            (aliases/map-alias-to-article! db alias-id article-id))
                          article-id)

                        :else nil)]
                  {:raw_label raw-label*
                   :unit unit
                   :article_alias_id alias-id
                   :article_id article-id})
                (catch Exception e
                  (log/warn e "Failed to auto-create article alias/article from receipt extraction item"
                    {:supplier-id supplier-id
                     :raw_label raw-label*
                     :unit unit
                     :auto-create-articles? auto-create-articles?})
                  {:raw_label raw-label*
                   :unit unit
                   :article_alias_id nil
                   :article_id nil})))))
        (:items extraction)))))
