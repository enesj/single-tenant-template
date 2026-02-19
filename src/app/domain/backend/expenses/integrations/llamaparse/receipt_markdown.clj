(ns app.domain.backend.expenses.integrations.llamaparse.receipt-markdown
  "LlamaParse-specific normalization.

  LlamaParse can return rich structured `items` (including tables) and plain `text`.
  Our existing receipt OCR pipeline expects receipt-like markdown (often with pipe
  tables). This namespace converts LlamaParse results into that shape so the
  Mistral workflow stays unchanged."
  (:require
    [app.domain.backend.expenses.workers.receipt-ocr.common :as common]
    [clojure.string :as str]))

(def ^:private ba-datetime-line-re
  #"(?iu)^\d{1,2}\.\d{1,2}\.\d{2,4}\.?\s+\d{1,2}:\d{2}(?::\d{2})?$")

(def ^:private ba-date-line-re
  #"(?iu)^\d{1,2}\.\d{1,2}\.\d{2,4}\.?$")

(defn- date-line?
  [line]
  (let [line (some-> line str/trim not-empty)]
    (boolean
      (and line
        (or (re-matches ba-datetime-line-re line)
          (re-matches ba-date-line-re line))))))

(defn- normalize-text
  [s]
  (some-> s str str/lower-case (str/replace #"\s+" " ") str/trim not-empty))

(def ^:private ignore-merchant-prefixes
  ["jib" "pib" "ibfm" "ibem" "tbfm" "bf" "ve" "osn" "pdv" "vat" "total" "ukupno" "uplac" "upl" "gotovina" "kartica"])

(def ^:private ignore-merchant-exact
  #{"fiskalni racun" "fiskalni račun" "racun" "račun"})

(defn- separator-line?
  [line]
  (let [line (some-> line str/trim not-empty)
        compact (some-> line (str/replace #"\s+" ""))]
    (boolean
      (and line
        (or (and compact (re-matches #"^-{3,}$" compact))
          (and compact (re-matches #"^[=-]{3,}$" compact))
          (re-matches #"^(?:-\s*){6,}$" line)
          (re-matches #"^(?:=\s*){6,}$" line))))))

(defn- has-letter?
  [s]
  (boolean (and (string? s) (re-find #"\p{L}" s))))

(defn- merchant-like-line?
  [line]
  (let [line (some-> line str/trim not-empty)
        norm (normalize-text line)]
    (boolean
      (and line norm
        (has-letter? line)
        (not (date-line? line))
        (not (separator-line? line))
        (not (contains? ignore-merchant-exact norm))
        (not (some #(str/starts-with? norm %) ignore-merchant-prefixes))))))

(defn text->merchant-name
  "Pick a best-effort merchant/supplier name from LlamaParse `text`."
  [text]
  (when (string? text)
    (->> (str/split-lines text)
      (map #(some-> % str/trim not-empty))
      (remove nil?)
      (remove separator-line?)
      (remove date-line?)
      (filter merchant-like-line?)
      first)))

(defn text->date-line
  "Return the first BA-like date/datetime line from LlamaParse `text`."
  [text]
  (when (string? text)
    (->> (str/split-lines text)
      (map #(some-> % str/trim not-empty))
      (remove nil?)
      (filter date-line?)
      first)))

(def ^:private total-prefixes
  ["total" "ukupno" "ukupna" "ukupan iznos" "za uplatu" "za plac" "za pla" "uplaceno" "primljeno" "gotovina" "kartica"])

(defn text->total-line
  "Extract a best-effort TOTAL line from LlamaParse `text`.

  Returns a markdown line like: TOTAL: 21.88"
  [text]
  (when (string? text)
    (let [candidates
          (->> (str/split-lines text)
            (keep (fn [line0]
                    (let [line (some-> line0 str/trim not-empty)
                          norm (normalize-text line)
                          money (common/parse-money line)]
                      (when (and norm money (some #(str/starts-with? norm %) total-prefixes))
                        {:money money}))))
            vec)
          non-zero (->> candidates
                     (remove (fn [{:keys [money]}]
                               (zero? (.compareTo (bigdec money) 0M))))
                     vec)
          best (or (last non-zero) (last candidates))]
      (when-let [money (:money best)]
        (str "TOTAL: " (format "%.2f" (double money)))))))

(defn- csv-parse-line
  "Parse a single CSV line into a vector of cell strings.

  Minimal RFC4180 support (quotes + escaped quotes)."
  [line]
  (loop [chars (seq (or line ""))
         in-quote? false
         cell []
         cells []]
    (if-not (seq chars)
      (conj (vec cells) (str/trim (apply str cell)))
      (let [ch (first chars)
            next-ch (second chars)]
        (cond
          ;; Escaped quote inside a quoted cell: "" -> "
          (and (= ch \") in-quote? (= next-ch \"))
          (recur (nnext chars) in-quote? (conj cell ch) cells)

          (= ch \")
          (recur (next chars) (not in-quote?) cell cells)

          (and (= ch \,) (not in-quote?))
          (recur (next chars) in-quote? [] (conj cells (str/trim (apply str cell))))

          :else
          (recur (next chars) in-quote? (conj cell ch) cells))))))

(defn- csv->rows
  [csv]
  (when (string? csv)
    (->> (str/split-lines csv)
      (map #(some-> % str/trim not-empty))
      (remove nil?)
      (mapv csv-parse-line)
      not-empty)))

(defn- rows->pipe-table
  [rows]
  (when (seq rows)
    (let [width (apply max (map count rows))
          pad-row (fn [row]
                    (vec (concat row (repeat (- width (count row)) ""))))
          rows* (mapv pad-row rows)
          header (first rows*)
          sep (repeat width "---")
          body (subvec rows* 1)
          fmt-row (fn [row]
                    (str "| " (str/join " | " (map #(some-> % str str/trim) row)) " |"))]
      (str/join "\n" (concat [(fmt-row header)
                              (fmt-row sep)]
                       (map fmt-row body))))))

(defn- table-item->markdown
  [{:keys [md csv]}]
  (let [md (some-> md str/trim not-empty)]
    (cond
      (and md (str/includes? md "|")) md
      (seq (csv->rows csv)) (rows->pipe-table (csv->rows csv))
      :else nil)))

(defn response->table-markdowns
  "Extract markdown tables from LlamaParse parse result JSON.

  Prefers `TableItem.md`, falls back to `TableItem.csv` converted into a pipe table."
  [resp-json]
  (->> (get-in resp-json [:items :pages])
    (mapcat :items)
    (filter (fn [item]
              (= "table" (some-> (:type item) name str/lower-case))))
    (keep table-item->markdown)
    (map str/trim)
    (remove str/blank?)
    vec))

(defn normalize-receipt-markdown
  "Build receipt-like markdown from LlamaParse result JSON.

  The goal is a stable, Mistral-compatible markdown shape (header + pipe table + total)."
  [{:keys [text tables total]}]
  (let [merchant (text->merchant-name text)
        date-line (text->date-line text)
        total-line (or total (text->total-line text))
        blocks (cond-> []
                 merchant (conj merchant)
                 date-line (conj date-line)
                 (seq tables) (conj (str/join "\n\n" tables))
                 total-line (conj total-line))]
    (->> blocks
      (map str/trim)
      (remove str/blank?)
      (str/join "\n\n")
      not-empty)))
