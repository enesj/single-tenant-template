(ns app.domain.backend.expenses.services.articles.normalization
  "Article and alias normalization functions."
  (:require
    [clojure.string :as str]))

(defn normalize-article-key
  "Normalize a canonical article name for deduplication."
  [name]
  (when name
    (-> name
      str/trim
      str/lower-case
      (str/replace #"[^a-z0-9\s-]" "")
      (str/replace #"\s+" "-"))))

(defn normalize-alias-label
  "Normalize raw line-item labels for alias lookup."
  [label]
  (when label
    (-> label
      str/trim
      str/lower-case
      (str/replace #"[^a-z0-9\s-]" "")
      (str/replace #"\s+" "-"))))
