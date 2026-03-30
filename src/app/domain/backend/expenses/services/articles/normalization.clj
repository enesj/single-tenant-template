(ns app.domain.backend.expenses.services.articles.normalization
  "Article and alias normalization functions."
  (:require
    [clojure.string :as str]))

(def ^:private bosnian-char-map
  {"č" "c"
   "ć" "c"
   "š" "s"
   "ž" "z"
   "đ" "dj"})

(defn- normalize-label
  [label]
  (when label
    (-> label
      str/trim
      str/lower-case
      ((fn [s]
         (reduce-kv (fn [acc from to]
                      (str/replace acc from to))
           s
           bosnian-char-map)))
      (str/replace #"[^a-z0-9\s-]" "")
      (str/replace #"\s+" "-"))))

(defn normalize-article-key
  "Normalize a canonical article name for deduplication."
  [name]
  (normalize-label name))

(defn normalize-alias-label
  "Normalize raw line-item labels for alias lookup."
  [label]
  (normalize-label label))
