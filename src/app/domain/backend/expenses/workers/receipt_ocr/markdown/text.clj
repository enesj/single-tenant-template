(ns app.domain.backend.expenses.workers.receipt-ocr.markdown.text
  (:require
    [clojure.string :as str]))

(defn normalize-text [s]
  (some-> s
    str
    str/lower-case
    (str/replace #"\s+" " ")
    str/trim
    not-empty))

(defn strip-line-markup [s]
  (some-> s
    str
    (str/replace #"(?is)<[^>]+>" " ")
    (str/replace #"^[\s#>*]+" "")
    (str/replace #"[|¦│]" " ")
    (str/replace #"(?i)&nbsp;" " ")
    (str/replace #"\s+" " ")
    str/trim
    not-empty))

(defn html-markdown->plain-text [markdown]
  (when (string? markdown)
    (-> markdown
      (str/replace #"(?is)<td\b[^>]*>" "")
      (str/replace #"(?is)</td>" "\n")
      (str/replace #"(?is)<tr\b[^>]*>" "")
      (str/replace #"(?is)</tr>" "\n")
      (str/replace #"(?is)<[^>]+>" " ")
      (str/replace #"(?i)&nbsp;" " ")
      (str/replace #"\r" "")
      (str/replace #"\n{2,}" "\n"))))

(defn label-present? [markdown raw-label]
  (let [m (normalize-text markdown)
        l (normalize-text raw-label)]
    (boolean (and m l (str/includes? m l)))))

(defn has-letter? [s]
  (boolean (and (string? s) (re-find #"\p{L}" s))))

(defn normalize-item-label [raw-label]
  (let [raw-label (some-> raw-label
                    str
                    (str/replace #"\|" " ")
                    (str/replace #"\s+" " ")
                    str/trim
                    not-empty)]
    (when raw-label
      (if-let [[_ _ rest]
               (re-matches #"(?i)^([0-9]{4,}|[A-Z][0-9]{4,})[ \t]+(.+)$" raw-label)]
        (let [rest (str/trim rest)]
          (if (has-letter? rest)
            rest
            raw-label))
        raw-label))))

(def unit-prefixes #{"t/pc"})

(defn unit-prefix? [s]
  (when (string? s)
    (contains? unit-prefixes (normalize-text s))))
