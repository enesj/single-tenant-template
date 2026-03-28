(ns app.domain.backend.expenses.workers.receipt-ocr.extraction.shape
  (:require
    [app.domain.backend.expenses.services.receipts.parsing :as receipt-parsing]
    [app.domain.backend.expenses.workers.receipt-ocr.common :as common]
    [clojure.string :as str])
  (:import
    [java.sql Timestamp]))

(def ReceiptExtraction
  [:map {:closed false}
   [:merchant {:optional true}
    [:maybe
     [:map {:closed false}
      [:name string?]
      [:address {:optional true} [:maybe string?]]
      [:tax_id {:optional true} [:maybe string?]]]]]
   [:purchased_at {:optional true} [:maybe string?]]
   [:currency {:optional true} [:maybe string?]]
   [:totals [:map {:closed false}
             [:subtotal {:optional true} [:maybe [:or number? string?]]]
             [:tax {:optional true} [:maybe [:or number? string?]]]
             [:total [:or number? string?]]]]
   [:items [:sequential [:map {:closed false}
                         [:raw_label string?]
                         [:qty {:optional true} [:maybe [:or number? string?]]]
                         [:unit_price {:optional true} [:maybe [:or number? string?]]]
                         [:line_total [:or number? string?]]]]]])

(defn extraction->guesses
  [{:keys [merchant totals currency purchased_at items]} {:keys [default-currency]}]
  (let [supplier (some-> merchant :name str/trim not-empty)
        total (common/parse-money (some-> totals :total))
        currency* (common/normalize-currency currency (or default-currency "BAM"))
        purchased-at (some-> purchased_at common/parse-instant)
        purchased-at-ts (some-> purchased-at Timestamp/from)
        items-count (if (sequential? items) (count items) 0)]
    {:supplier_guess supplier
     :total_amount_guess total
     :currency_guess currency*
     :purchased_at_guess purchased-at-ts
     :items-count items-count}))

(defn review-required?
  [{:keys [supplier_guess total_amount_guess currency_guess items-count]}]
  (or (nil? supplier_guess)
    (nil? total_amount_guess)
    (nil? currency_guess)
    (zero? (long (or items-count 0)))))

(defn looks-like-json-schema?
  [m]
  (boolean
    (and (map? m)
      (contains? m :properties)
      (contains? m :type)
      (contains? m :required))))

(defn abs-decimal-diff
  [a b]
  (when (and a b)
    (double (.abs (.subtract (bigdec a) (bigdec b))))))

(defn normalize-line-item
  [item]
  (cond-> item
    (and (map? item)
      (contains? item :line_total)
      (not (contains? item :line-total)))
    (assoc :line-total (:line_total item))))

(declare confidence-favors-items-total?)

(defn lines-total-mismatch?
  ([items total-amount]
   (lines-total-mismatch? items total-amount nil))
  ([items total-amount provider-confidence]
   (let [items* (mapv normalize-line-item (or items []))
         lines-total (receipt-parsing/lines-total items*)]
     (when-let [abs-diff (abs-decimal-diff lines-total total-amount)]
       (and (> abs-diff 0.01)
         (not (confidence-favors-items-total? provider-confidence items* total-amount)))))))

(defn items-total-amount
  [items]
  (when (sequential? items)
    (let [items* (mapv normalize-line-item items)]
      (when (seq items*)
        (receipt-parsing/lines-total items*)))))

(defn- amount->cents-string
  [amount]
  (when-let [m (common/parse-money amount)]
    (format "%.0f" (* 100.0 (double (bigdec m))))))

(defn- single-digit-total-difference?
  [a b]
  (let [a* (amount->cents-string a)
        b* (amount->cents-string b)]
    (boolean
      (and a*
        b*
        (= (count a*) (count b*))
        (= 1 (count (filter not (map = a* b*))))))))

(defn- confidence-favors-items-total?
  [provider-confidence items total-amount]
  (let [items-total (items-total-amount items)
        abs-diff (abs-decimal-diff items-total total-amount)
        line-total-reliability (some-> (:line_total_reliability provider-confidence) double)
        total-confidence (some-> (:selected_total_confidence provider-confidence) double)]
    (and provider-confidence
      items-total
      abs-diff
      line-total-reliability
      (single-digit-total-difference? items-total total-amount)
      (> abs-diff 0.01)
      (<= abs-diff 0.05)
      (>= line-total-reliability 0.85)
      (or (nil? total-confidence)
        (and (<= total-confidence 0.8)
          (< total-confidence line-total-reliability))))))

(defn prefer-markdown-items?
  "Prefer markdown-derived items when they explain the final total much better."
  [provider-items markdown-items final-total]
  (let [provider-items (vec (or provider-items []))
        markdown-items (vec (or markdown-items []))
        final-total (common/parse-money final-total)
        provider-total (items-total-amount provider-items)
        markdown-total (items-total-amount markdown-items)
        provider-diff (abs-decimal-diff provider-total final-total)
        markdown-diff (abs-decimal-diff markdown-total final-total)]
    (and final-total
      (seq provider-items)
      (seq markdown-items)
      (= (count provider-items) (count markdown-items))
      (some? provider-diff)
      (some? markdown-diff)
      (> provider-diff 0.05)
      (<= markdown-diff 0.05)
      (< (+ markdown-diff 0.01) provider-diff))))

(defn prefer-markdown-total?
  "Prefer markdown-derived total only when it is clearly better supported."
  [provider-total markdown-total items]
  (let [provider-total (common/parse-money provider-total)
        markdown-total (common/parse-money markdown-total)
        items-total (items-total-amount items)
        provider-diff (abs-decimal-diff provider-total items-total)
        markdown-diff (abs-decimal-diff markdown-total items-total)
        provider-vs-markdown (abs-decimal-diff markdown-total provider-total)]
    (cond
      (nil? markdown-total)
      false

      (nil? provider-total)
      true

      (nil? items-total)
      (and (some? provider-vs-markdown)
        (> provider-vs-markdown 0.05))

      :else
      (and (some? provider-diff)
        (some? markdown-diff)
        (> provider-diff 0.05)
        (<= markdown-diff 0.05)
        (< (+ markdown-diff 0.01) provider-diff)))))
