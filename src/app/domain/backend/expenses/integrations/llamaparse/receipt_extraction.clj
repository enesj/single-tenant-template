(ns app.domain.backend.expenses.integrations.llamaparse.receipt-extraction
  "LlamaParse-specific receipt extraction."
  (:require
    [app.domain.backend.expenses.integrations.llamaparse.http :as http]
    [app.domain.backend.expenses.integrations.llamaparse.receipt-extraction.merchant :as merchant]
    [app.domain.backend.expenses.integrations.llamaparse.receipt-extraction.table-items :as table-items]
    [app.domain.backend.expenses.integrations.llamaparse.receipt-extraction.text :as text]
    [app.domain.backend.expenses.integrations.llamaparse.receipt-extraction.text-items :as text-items]
    [app.domain.backend.expenses.integrations.llamaparse.receipt-extraction.totals :as totals]
    [app.domain.backend.expenses.workers.receipt-ocr.common :as common]
    [clojure.string :as str]))

(defn response->header-text
  [resp-json]
  (text/response->header-text resp-json))

(defn response->combined-text
  [resp-json]
  (text/response->combined-text resp-json))

(defn response->extraction
  "Build a ReceiptExtraction map from a LlamaParse result response JSON."
  [resp-json]
  (let [header (response->header-text resp-json)
        text-content (http/response->text resp-json)
        combined (response->combined-text resp-json)
        date-line (text/text->date-line combined)
        purchased-at (text/date-line->iso date-line)
        merchant (merchant/text->merchant-context header text-content)
        table-items (table-items/response->table-items resp-json)
        {:keys [items total-lines]} (table-items/parse-table-items table-items)
        items (if (empty? items)
                (or (text-items/parse-text-items text-content)
                  (text-items/parse-text-items combined)
                  [])
                items)
        total-lines* (concat (when combined (str/split-lines combined)) total-lines)
        total (or (totals/extract-total total-lines* items)
                (totals/items-total items))]
    {:merchant (not-empty merchant)
     :purchased_at purchased-at
     :currency nil
     :totals (when total {:total total})
     :items (vec (or items []))}))

(defn extraction->markdown
  "Build a stable, receipt-like markdown representation from an extraction.

  `date-line` is the original receipt date line (e.g. '2.12.2025. 19:46')."
  [{:keys [merchant totals items]} {:keys [date-line]}]
  (let [merchant-name (some-> merchant :name text/safe-trim)
        date-line (text/safe-trim date-line)
        total (some-> totals :total common/parse-money)
        format-money (fn [m]
                       (when-let [m (common/parse-money m)]
                         (format "%.2f" (double (bigdec m)))))
        fmt-row (fn [row]
                  (str "| " (str/join " | " row) " |"))
        rows (->> (or items [])
               (mapv (fn [{:keys [raw_label qty unit_price line_total]}]
                       [(or (text/safe-trim raw_label) "")
                        (or (some-> qty common/parse-money str) "")
                        (or (format-money unit_price) "")
                        (or (format-money line_total) "")])))
        table (when (seq rows)
                (let [header ["Label" "Qty" "Unit" "Total"]
                      sep ["---" "---" "---" "---"]]
                  (str/join "\n" (concat [(fmt-row header)
                                          (fmt-row sep)]
                                   (map fmt-row rows)))))
        blocks (cond-> []
                 merchant-name (conj merchant-name)
                 date-line (conj date-line)
                 table (conj table)
                 total (conj (str "TOTAL: " (format "%.2f" (double (bigdec total))))))]
    (->> blocks
      (map text/safe-trim)
      (remove nil?)
      (str/join "\n\n")
      not-empty)))

(defn response->receipt
  "Return {:extraction .. :parsed-markdown .. :date-line ..} from LlamaParse response."
  [resp-json]
  (let [combined (response->combined-text resp-json)
        date-line (text/text->date-line combined)
        extraction (response->extraction resp-json)
        md (extraction->markdown extraction {:date-line date-line})]
    {:extraction extraction
     :parsed-markdown md
     :date-line date-line}))
