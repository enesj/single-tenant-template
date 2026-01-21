(ns app.domain.backend.expenses.integrations.cerebras.receipt-refine
  "Prompt + JSON schema for refining receipt OCR markdown into structured extraction.

  Designed for Cerebras (OpenAI-compatible) structured outputs.

  The model-facing schema intentionally uses integer cents to avoid locale issues
  with ',' / '.' decimal separators in OCR output. The app converts the model
  output into the internal receipt extraction shape used by
  `app.domain.backend.expenses.workers.receipt-ocr.extraction`.

  Optional fields are represented as nullable values, so the top-level shape is
  stable even when the model cannot confidently extract a value."
  (:require
    [clojure.string :as str]))

(def receipt-extraction-json-schema
  {"title" "ReceiptExtractionCentsV1"
   "type" "object"
   "additionalProperties" false
   "properties"
   {"merchant" {"anyOf" [{"type" "object"
                           "additionalProperties" false
                           "properties" {"name" {"anyOf" [{"type" "string"}
                                                            {"type" "null"}]}
                                         "address" {"anyOf" [{"type" "string"}
                                                               {"type" "null"}]}
                                         "tax_id" {"anyOf" [{"type" "string"}
                                                              {"type" "null"}]}}
                           "required" ["name" "address" "tax_id"]}
                          {"type" "null"}]}

    "purchased_at" {"anyOf" [{"type" "string"}
                              {"type" "null"}]
                    "description" "ISO-8601 timestamp if present, else null."}

    "currency" {"anyOf" [{"type" "string"}
                          {"type" "null"}]
                "description" "ISO 4217 currency code (e.g. BAM/EUR/USD), else null."}

    "totals" {"type" "object"
              "additionalProperties" false
              "properties" {"subtotal_cents" {"anyOf" [{"type" "integer"}
                                                        {"type" "null"}]}
                            "tax_cents" {"anyOf" [{"type" "integer"}
                                                   {"type" "null"}]}
                            "total_cents" {"anyOf" [{"type" "integer"}
                                                     {"type" "null"}]}}
              "required" ["subtotal_cents" "tax_cents" "total_cents"]}

    "items" {"type" "array"
             "items" {"type" "object"
                      "additionalProperties" false
                      "properties" {"name" {"type" "string"}
                                    "quantity" {"anyOf" [{"type" "number"}
                                                          {"type" "null"}]
                                                "description" "Quantity can be decimal (up to 3 decimals)."}
                                    "unit_price_cents" {"anyOf" [{"type" "integer"}
                                                                   {"type" "null"}]
                                                        "description" "Final per-unit price after discounts."}
                                    "line_total_cents" {"anyOf" [{"type" "integer"}
                                                                  {"type" "null"}]
                                                        "description" "Final line total after discounts."}}
                      "required" ["name" "quantity" "unit_price_cents" "line_total_cents"]}}}
   "required" ["merchant" "purchased_at" "currency" "totals" "items"]})

(def receipt-extraction-system-prompt
  (str
    "You extract structured receipt data from OCR markdown.\n"
    "Return exactly one JSON object that matches the provided JSON Schema.\n"
    "Do not add extra keys.\n"
    "\n"
    "Numeric rules:\n"
    "- All monetary fields are integer cents (e.g. 13.40 -> 1340, 0.89 -> 89).\n"
    "- Use the final price after discounts for unit_price_cents and line_total_cents.\n"
    "- Quantity may be decimal with up to 3 decimal places (e.g. 1.098).\n"
    "\n"
    "Merchant rules:\n"
    "- Merchant name is usually in the header. If the name appears as \"<merchant>\" in quotes, use the text inside quotes.\n"
    "- Ignore non-merchant header lines like fiscal markers, long numeric IDs, cashier lines, and tax/payer labels.\n"
    "\n"
    "Items rules (VERY IMPORTANT):\n"
    "- Only include actual purchased items.\n"
    "- If there is an items table (headers like Naziv/Name/Opis + Cijena/Price + Kol./Qty + Ukupno/Total), start items at the first data row under that header.\n"
    "- Stop items when you reach totals/payment/tax sections.\n"
    "- DO NOT include rows/sections about totals, payment methods, or tax summaries as items.\n"
    "  Examples to exclude: ukupno/total/ukupan iznos, uplaceno/primljeno, kartica/gotovina, povrat/razlika, PDV/VAT/porez.\n"
    "- Remove leading article/product codes from item names when present.\n"
    "\n"
    "Totals rules:\n"
    "- totals.total_cents is the final amount paid. Prefer the last grand total.\n"
    "- Ignore totals that explicitly say 'without tax' (bez poreza / без пореза) or tax-only totals.\n"
    "\n"
    "If a value is not present or not reliable, use null (do not invent values)."))

(defn receipt-extraction-user-prompt
  [markdown]
  (let [md (some-> markdown str)
        md (if (and md (<= (count md) 50000)) md (subs md 0 (min 50000 (count md))))]
    (str
      "Extract receipt data from this OCR markdown:\n\n"
      "```markdown\n"
      md
      "\n```")))

(defn build-chat-messages
  "Build the OpenAI-style messages payload for receipt extraction."
  [markdown]
  [{:role "system" :content receipt-extraction-system-prompt}
   {:role "user" :content (receipt-extraction-user-prompt markdown)}])

(defn normalize-llm-extraction
  "Normalize a parsed JSON extraction map by trimming strings and dropping empty values.

  This is intentionally conservative and should not invent values."
  [m]
  (when (map? m)
    (letfn [(trim-str [s] (some-> s str str/trim not-empty))
            (normalize-merchant [merchant]
              (when (map? merchant)
                (-> merchant
                  (update "name" trim-str)
                  (update "address" trim-str)
                  (update "tax_id" trim-str))))
            (normalize-item [item]
              (when (map? item)
                (-> item
                  (update "name" trim-str))))]
      (-> m
        (update "merchant" normalize-merchant)
        (update "purchased_at" trim-str)
        (update "currency" trim-str)
        (update "items" (fn [items] (when (sequential? items) (mapv normalize-item items))))))))

(defn cents->money
  "Convert integer cents to a BigDecimal money amount.

  Returns nil for non-numeric inputs."
  [cents]
  (when (number? cents)
    (.divide (bigdec cents) 100M)))

(defn- ->bd
  [n]
  (cond
    (nil? n) nil
    (instance? java.math.BigDecimal n) n
    (number? n) (bigdec n)
    :else nil))

(defn- safe-divide
  "Divide two BigDecimals with rounding.

  BigDecimal division can throw on non-terminating expansions; we round.
  Scale is chosen to preserve receipt precision without exploding digits."
  [a b]
  (when (and a b (not (zero? (.compareTo ^java.math.BigDecimal b 0M))))
    (.divide ^java.math.BigDecimal a ^java.math.BigDecimal b 6 java.math.RoundingMode/HALF_UP)))

(defn llm-extraction->receipt-extraction
  "Convert the model-facing cents schema into the app's internal extraction shape.

  Output shape matches `receipt-ocr.extraction/ReceiptExtraction`:
  {:merchant {:name .. :address .. :tax_id ..}
   :purchased_at <string-or-nil>
   :currency <string-or-nil>
   :totals {:subtotal <decimal> :tax <decimal> :total <decimal>}
   :items [{:raw_label <string> :qty <decimal> :unit_price <decimal> :line_total <decimal>} ...]}"
  [llm-extraction]
  (let [m (normalize-llm-extraction llm-extraction)
        merchant (get m "merchant")
        totals (get m "totals")
        items (get m "items")]
    (cond-> {}
      (and (map? merchant) (seq (get merchant "name")))
      (assoc :merchant (cond-> {:name (get merchant "name")}
                         (get merchant "address") (assoc :address (get merchant "address"))
                         (get merchant "tax_id") (assoc :tax_id (get merchant "tax_id"))))

      (contains? m "purchased_at")
      (assoc :purchased_at (get m "purchased_at"))

      (contains? m "currency")
      (assoc :currency (get m "currency"))

      (map? totals)
      (assoc :totals (cond-> {}
                       (some? (get totals "subtotal_cents"))
                       (assoc :subtotal (cents->money (get totals "subtotal_cents")))

                       (some? (get totals "tax_cents"))
                       (assoc :tax (cents->money (get totals "tax_cents")))

                       (contains? totals "total_cents")
                       (assoc :total (cents->money (get totals "total_cents")))))

      (sequential? items)
      (assoc :items
        (->> items
          (keep
            (fn [item]
              (let [name (some-> (get item "name") str str/trim not-empty)
                    qty (->bd (get item "quantity"))
                    unit-price (cents->money (get item "unit_price_cents"))
                    line-total (cents->money (get item "line_total_cents"))
                    unit-price (or unit-price (safe-divide line-total qty))
                    line-total (or line-total (when (and unit-price qty)
                                                (.multiply ^java.math.BigDecimal unit-price ^java.math.BigDecimal qty)))]
                (when (and name line-total)
                  (cond-> {:raw_label name
                           :line_total line-total}
                    qty (assoc :qty qty)
                    unit-price (assoc :unit_price unit-price))))))
          vec)))))
