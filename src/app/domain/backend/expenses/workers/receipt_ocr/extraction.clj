(ns app.domain.backend.expenses.workers.receipt-ocr.extraction
  "Extraction post-processing + persistence.

  The OCR provider returns a mix of:
  - structured extraction JSON (extraction)
  - markdown text

  We reconcile these into the shape expected by the receipts workflow and persist
  results + derived guesses."
  (:require
    [app.domain.backend.expenses.services.article-aliases :as aliases]
    [app.domain.backend.expenses.services.articles :as articles]
    [app.domain.backend.expenses.services.receipts.approval :as receipt-approval]
    [app.domain.backend.expenses.services.receipts.parsing :as receipt-parsing]
    [app.domain.backend.expenses.services.receipts.queries :as receipt-queries]
    [app.domain.backend.expenses.services.receipts.status :as receipt-status]
    [app.domain.backend.expenses.services.store-aliases :as store-aliases]
    [app.domain.backend.expenses.services.stores :as stores]
    [app.domain.backend.expenses.services.supplier-aliases :as supplier-aliases]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [app.domain.backend.expenses.services.user-expense-settings :as user-expense-settings]
    [app.domain.backend.expenses.workers.receipt-ocr.common :as common]
    [app.domain.backend.expenses.workers.receipt-ocr.markdown :as markdown]
    [clojure.string :as str]
    [malli.core :as m]
    [taoensso.timbre :as log])
  (:import
    [java.sql Timestamp]))

(def ^:private ReceiptExtraction
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

(defn- looks-like-json-schema? [m]
  (boolean
    (and (map? m)
      (contains? m :properties)
      (contains? m :type)
      (contains? m :required))))

(defn- abs-decimal-diff [a b]
  (when (and a b)
    (double (.abs (.subtract (bigdec a) (bigdec b))))))

(defn- normalize-line-item
  [item]
  (cond-> item
    (and (map? item)
      (contains? item :line_total)
      (not (contains? item :line-total)))
    (assoc :line-total (:line_total item))))

(defn- lines-total-mismatch?
  [items total-amount]
  (let [items* (mapv normalize-line-item (or items []))
        lines-total (receipt-parsing/lines-total items*)]
    ;; Keep mismatch semantics aligned with list status derivation:
    ;; `abs(lines_total - total_amount_guess) > 0.01`.
    (when-let [abs-diff (abs-decimal-diff lines-total total-amount)]
      (> abs-diff 0.01))))

(defn- items-total-amount
  [items]
  (when (sequential? items)
    (let [items* (mapv normalize-line-item items)]
      (when (seq items*)
        (receipt-parsing/lines-total items*)))))

(defn- prefer-markdown-items?
  "Prefer markdown-derived items when they explain the final total much better.

  This is primarily used for receipts where provider items capture pre-discount
  prices, while markdown rows include final discounted line totals."
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

(defn- prefer-markdown-total?
  "Prefer markdown-derived total only when it is clearly better supported.

  If provider/refined total already matches line-item sum, do not replace it with
  markdown total (which may represent tendered/payment amount)."
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

(def ^:private discount-label-pattern
  #"(?iu)\b(popust|popost|rabat|discount|akcija)\b")

(def ^:private totals-label-pattern
  #"(?iuU)\b(ukupno|ukupan|ukupna|total|subtotal|iznos|bez\s+poreza|za\s+uplatu|za\s+pla[ćc]anje|upla[ćc]eno|primljeno|gotovina|kartica|kusur|povrat|razlika|saldo|укупно|укупан|укупна|износ|без\s+пореза|за\s+уплату|за\s+пла[ћч]ање|упла[ћч]ено|примљено|готовина|картица|кусур|поврат|разлика|салдо)\b")

(def ^:private tax-label-pattern
  ;; Includes a few common abbreviations and OCR misreads seen in BA receipts:
  ;; - OSN/CSN (osnovica)
  ;; - POU (often a misread of PDV)
  #"(?iuU)\b(pdv|vat|porez|osnovica|stopa|(?:osn|csn)\.?|pou|пдв|порез|основица|стопа)\b")

(def ^:private payment-label-pattern
  #"(?iuU)\b(gotovina|kartica|visa|master(card)?|diners|amex|american|paypal|готовина|картица|виза|мастер|динерс|амекс)\b")

(def ^:private meta-label-pattern
  #"(?iu)\b(ra[čc]un|fiskalni|kasa|kasir|operator|datum|vrijeme|broj|id)\b")

(def ^:private meta-reference-number-pattern
  ;; Keep this strict: metadata reference markers should look like "br." or "br:" + value.
  ;; This avoids false positives for legitimate product labels ending with "BR".
  #"(?iu)\bbr(?:\.|:)\s*[:#-]?\s*[\p{L}\p{N}/-]+\b")

(def ^:private header-label-pattern
  #"(?iu)^(?:naziv|opis|artik(?:al|l|la|li)?|šifra|sifra|kol(?:\.|i[čc]ina)?|qty|quantity|cijena|price|jed(?:\.|inica)?)(?:[\s\|/:;,-]+(?:naziv|opis|artik(?:al|l|la|li)?|šifra|sifra|kol(?:\.|i[čc]ina)?|qty|quantity|cijena|price|jed(?:\.|inica)?))*$")

(defn- normalize-item-label
  "Normalize a raw item label for robust matching.

  This is used only for heuristics (filtering/deduping). It should not change
  the raw label displayed to users."
  [raw-label]
  (when-let [s (some-> raw-label str str/trim not-empty)]
    (-> s
      str/lower-case
      (str/replace #"[,:;]+" " ")
      (str/replace #"\s+" " ")
      str/trim)))

(defn- label-matches?
  [pattern label]
  (boolean
    (and (seq label)
      (re-find pattern label))))

(defn- discount-row?
  [label]
  (label-matches? discount-label-pattern label))

(defn- vat-percent-row?
  [label]
  (and (seq label)
    (str/includes? label "%")
    (boolean (re-find #"(?iu)\b(v\.?|pdv|vat|porez)\b" label))))

(defn- likely-grand-total-row?
  "Heuristic: drop rows that restate the grand total inside the items list.

  We only apply this when there are multiple items; otherwise a single-item
  receipt could legitimately have line_total == grand total."
  [{:keys [items-count grand-total]} raw-label line-total]
  (and (some? grand-total)
    (some? line-total)
    (> (long (or items-count 0)) 3)
    (<= (abs-decimal-diff line-total grand-total) 0.01)
    (let [raw (some-> raw-label str str/trim)
          label (normalize-item-label raw)
          short? (<= (count (or label "")) 8)
          colon? (boolean (and raw (str/ends-with? raw ":")))]
      (or (label-matches? totals-label-pattern label)
        colon?
        short?))))

(defn- abbrev-summary-row?
  "Heuristic: drop short colon-terminated abbreviation rows inside the items list.

  Some receipts include summary rows like \"OSN. E:\" (tax base) or \"PDV:\" (tax),
  but OCR can misread them (e.g. \"CSN. E:\", \"POU:\"). These are not purchased
  items and frequently inflate the computed line-item total.

  We keep this conservative by requiring:
  - item list is non-trivial (>3)
  - raw label ends with ':'
  - label is very short and mostly letters/dots/spaces
  - line_total exists

  Returns true when the row should be dropped as non-item metadata."
  [{:keys [items-count]} raw-label label line-total]
  (let [raw (some-> raw-label str str/trim)]
    (and (some? line-total)
      (> (long (or items-count 0)) 3)
      (seq raw)
      (str/ends-with? raw ":")
      (seq label)
      (let [compact (-> label
                      (str/replace #"[\s\.]" "")
                      (str/trim))]
        (and (<= (count compact) 6)
          (boolean (re-matches #"(?iu)\p{L}+" compact)))))))

(defn- non-item-reason
  "Return a keyword reason when the item looks like a non-purchased row.

  Returns nil when the row looks like a normal purchased item."
  [ctx item]
  (let [raw-label (:raw_label item)
        label (normalize-item-label raw-label)
        line-total (common/parse-money (:line_total item))]
    (cond
      (str/blank? (or label "")) :blank-label
      (nil? line-total) :missing-line-total
      (label-matches? header-label-pattern label) :header
      (or (label-matches? meta-label-pattern label)
        (label-matches? meta-reference-number-pattern raw-label)) :metadata
      (or (= "maestro" label)
        (label-matches? payment-label-pattern label)) :payment
      (label-matches? tax-label-pattern label) :tax
      (vat-percent-row? label) :tax
      (abbrev-summary-row? ctx raw-label label line-total) :metadata
      (likely-grand-total-row? ctx raw-label line-total) :grand-total
      (label-matches? totals-label-pattern label) :totals
      :else nil)))

(defn- discount-override?
  "Return true when a discount row likely overrides the previous item's final price."
  [prev-item discount-item]
  (let [prev-total (common/parse-money (:line_total prev-item))
        disc-total (common/parse-money (:line_total discount-item))
        prev-label (normalize-item-label (:raw_label prev-item))
        diff (when (and prev-total disc-total)
               (abs-decimal-diff prev-total disc-total))]
    (and prev-total
      disc-total
      (not (discount-row? prev-label))
      (< (double disc-total) (double prev-total))
      (some? diff)
      ;; Be conservative: treat as override only when close-ish (e.g. -10%).
      (<= (double diff) (* 0.6 (double prev-total))))))

(defn- apply-discount-override
  [prev-item discount-item]
  (let [disc-total (common/parse-money (:line_total discount-item))
        qty (common/parse-money (:qty prev-item))
        qty-one? (and (some? qty) (= 1M (bigdec qty)))]
    (cond-> (assoc prev-item :line_total disc-total)
      (or qty-one? (nil? (:unit_price prev-item)))
      (assoc :unit_price disc-total))))

(defn- clean-extraction-items
  "Drop obvious non-item rows (totals/payment/tax/headers).

  Also attempts a conservative discount-override rewrite:
  if a discount row follows an item and looks like a discounted final price,
  we keep the item but replace its line_total with the discount row's line_total.

  Returns {:items .. :post-processing ..}."
  [items {:keys [items-count grand-total] :as ctx}]
  (let [label-sample-limit 5
        add-sample (fn [m reason raw-label]
                     (let [s (some-> raw-label str str/trim not-empty)]
                       (if-not s
                         m
                         (update m reason (fn [v]
                                            (let [v (vec (or v []))]
                                              (if (>= (count v) label-sample-limit)
                                                v
                                                (conj v s))))))))
        {:keys [items dropped-by-reason dropped-labels-sample discount-overrides]}
        (reduce
          (fn [{:keys [items] :as acc} item]
            (let [raw-label (:raw_label item)
                  label (normalize-item-label raw-label)]
              (cond
                (discount-row? label)
                (let [prev (peek items)]
                  (if (and prev (discount-override? prev item))
                    (-> acc
                      (update :items (fn [v] (conj (pop v) (apply-discount-override prev item))))
                      (update :discount-overrides (fnil inc 0)))
                    (-> acc
                      (update-in [:dropped-by-reason :discount] (fnil inc 0))
                      (update :dropped-labels-sample add-sample :discount raw-label))))

                :else
                (if-let [reason (non-item-reason ctx item)]
                  (-> acc
                    (update-in [:dropped-by-reason reason] (fnil inc 0))
                    (update :dropped-labels-sample add-sample reason raw-label))
                  (update acc :items conj item)))))
          {:items []
           :dropped-by-reason {}
           :dropped-labels-sample {}
           :discount-overrides 0}
          (or items []))
        ;; NOTE: We no longer dedupe items because legitimate duplicate purchases
        ;; (e.g., buying "Milk 2.8% masti" twice) should be preserved.
        ;; OCR providers rarely produce spurious duplicate lines.
        original-count (long (or items-count (count (or items []))))
        kept-count (count items)
        dropped-count (->> dropped-by-reason vals (reduce + 0))
        post-processing
        (when (or (pos? dropped-count) (pos? (long discount-overrides)))
          {:original-count original-count
           :kept-count kept-count
           :dropped-count dropped-count
           :discount-overrides (long (or discount-overrides 0))
           :dropped-by-reason dropped-by-reason
           :dropped-labels-sample (not-empty dropped-labels-sample)
           :grand-total (when grand-total (str grand-total))})]
    {:items items
     :post-processing post-processing}))

(def ^:private branch-store-prefix-pattern
  #"(?iu)^(ogranak|podružnica|poslovnica|pj\b|tc\b|pc\b|cc\b|centar|market|maloprodaja|prodavnica)\b")

(def ^:private legal-entity-pattern
  #"(?iu)\b(d\.?\s*o\.?\s*o\.?|d\.?\s*d\.?|a\.?\s*d\.?|j\.?\s*p\.?|s\.?\s*p\.?|ustanova|inc\.?|llc\.?|ltd\.?)\b")

(defn- branch-store-name?
  [s]
  (boolean
    (and (string? s)
      (re-find branch-store-prefix-pattern (str/trim s)))))

(defn- legal-entity-name?
  [s]
  (boolean
    (and (string? s)
      (re-find legal-entity-pattern s))))

(defn- post-process-merchant
  "Normalize provider merchant fields.

  Handles two common OCR patterns:
  - merged `merchant.address` containing `store_name + address`
  - legal entity in `merchant.name` with brand in `merchant.store_name`

  When we detect the second pattern and can split the merged address into a
  branch store name, we promote:
  - brand -> `merchant.name` (used as supplier guess)
  - legal entity -> `merchant.legal_name`
  - branch -> `merchant.store_name`
  - remainder -> `merchant.address`.

  Best-effort; returns the original extraction when no safe normalization exists."
  [extraction]
  (if-not (map? extraction)
    extraction
    (let [merchant (:merchant extraction)]
      (if-not (map? merchant)
        extraction
        (let [merchant-name (some-> merchant :name str str/trim not-empty)
              store-name0-raw (some-> merchant :store_name str str/trim not-empty)
              store-name0 (when (and (seq store-name0-raw)
                                  (not (and (seq merchant-name)
                                         (= (str/lower-case store-name0-raw)
                                           (str/lower-case merchant-name)))))
                            store-name0-raw)
              address0 (some-> merchant :address str str/trim not-empty)
              raw-address0 (some-> merchant :raw_address str str/trim not-empty)
              split-addr (when (seq address0)
                           (markdown/split-store-name-and-address address0))
              split-store-name (some-> split-addr :store_name str str/trim not-empty)
              split-address (some-> split-addr :address str str/trim not-empty)
              merchant*
              (cond-> merchant
                (and (seq address0) (not (seq raw-address0)))
                (assoc :raw_address address0)

                (and (seq store-name0-raw) (not (seq store-name0)))
                (dissoc :store_name))
              promote-brand-and-branch?
              (and (seq merchant-name)
                (seq store-name0)
                (seq split-store-name)
                (seq split-address)
                (legal-entity-name? merchant-name)
                (not (branch-store-name? store-name0))
                (branch-store-name? split-store-name))]
          (cond
            promote-brand-and-branch?
            (assoc extraction :merchant (assoc merchant*
                                          :legal_name merchant-name
                                          :name store-name0
                                          :store_name split-store-name
                                          :address split-address))

            (and (not (seq store-name0))
              (seq split-store-name)
              (seq split-address))
            (assoc extraction :merchant (assoc merchant*
                                          :store_name split-store-name
                                          :address split-address))

            :else
            extraction))))))

(defn- post-process-extraction
  "Apply heuristic extraction cleanup.

  Returns {:extraction <updated> :post-processing <stats|nil>}.

  This is intentionally best-effort. It should never throw and should avoid
  destructive changes when signals are ambiguous."
  [extraction]
  (let [extraction (post-process-merchant extraction)]
    (if-not (and (map? extraction) (sequential? (:items extraction)))
      {:extraction extraction
       :post-processing nil}
      (let [items (:items extraction)
            ctx {:items-count (count items)
                 :grand-total (common/parse-money (get-in extraction [:totals :total]))}
            {:keys [items post-processing]} (clean-extraction-items items ctx)]
        {:extraction (assoc extraction :items items)
         :post-processing post-processing}))))

(defn- best-markdown-item-match [markdown-items item]
  (let [item-total (common/parse-money (:line_total item))
        item-qty (common/parse-money (:qty item))
        item-unit (common/parse-money (:unit_price item))]
    (when (and item-total (seq markdown-items))
      (->> markdown-items
        (map (fn [cand]
               (let [d-total (abs-decimal-diff (:line_total cand) item-total)
                     d-unit (abs-decimal-diff (:unit_price cand) item-unit)
                     d-qty (abs-decimal-diff (:qty cand) item-qty)
                     score (+ (* 10 (or d-total 999.0))
                             (* 2 (or d-unit 1.0))
                             (* 1 (or d-qty 1.0)))]
                 {:cand cand
                  :d-total d-total
                  :score score})))
        ;; Ensure we match the correct row primarily by line total
        (filter #(<= (double (or (:d-total %) 999.0)) 0.05))
        (sort-by :score)
        first
        :cand))))

(defn- valid-alias-label?
  [raw-label]
  (let [raw-label* (some-> raw-label str str/trim)
        normalized (articles/normalize-alias-label raw-label*)]
    (and (not (str/blank? raw-label*))
      (not (str/blank? normalized)))))

(defn- resolve-user-region
  [db {:keys [user_id] :as _receipt} {:keys [user-region places-cfg]}]
  (or user-region
    (:region-code places-cfg)
    (when (and user_id (map? (:currency-region-map places-cfg)))
      (try
        (let [persisted (user-expense-settings/get-user-expense-settings db user_id)
              effective (user-expense-settings/effective-settings persisted)
              currency (:default-currency effective)]
          (get (:currency-region-map places-cfg) currency))
        (catch Exception _
          nil)))))

(defn- token-char-diff
  [a b]
  (when (and (string? a)
          (string? b)
          (= (count a) (count b)))
    (reduce
      (fn [acc [ca cb]]
        (if (= ca cb)
          acc
          (inc acc)))
      0
      (map vector a b))))

(defn- close-ocr-supplier-keys?
  "Treat very small per-token OCR substitutions as the same supplier key.

  Example: hese-kemerc ~ hose-komerc.

  This is intentionally conservative and only applies when token counts/lengths
  align and each token differs by at most one character."
  [a b]
  (let [a* (some-> a str str/trim not-empty)
        b* (some-> b str str/trim not-empty)]
    (when (and (seq a*) (seq b*))
      (let [tokens-a (->> (str/split a* #"-") (remove str/blank?) vec)
            tokens-b (->> (str/split b* #"-") (remove str/blank?) vec)
            token-diffs (when (= (count tokens-a) (count tokens-b))
                          (mapv token-char-diff tokens-a tokens-b))
            flat-a (str/replace a* #"-" "")
            flat-b (str/replace b* #"-" "")]
        (boolean
          (and (seq token-diffs)
            (every? some? token-diffs)
            (>= (max (count flat-a) (count flat-b)) 8)
            (every? #(>= (count %) 3) tokens-a)
            (every? #(<= % 1) token-diffs)
            (<= (reduce + 0 token-diffs) 2)))))))

;; Returns {:supplier-id uuid :supplier-alias-id uuid|nil :source keyword}
;;
;; IMPORTANT: We must consult supplier_aliases first.
;; If an alias is already mapped, we can skip Places entirely.
(defn- resolve-supplier-and-alias
  "Resolve supplier + supplier_alias_id.

  Priority:
  1) If `supplier_guess` has an already-mapped supplier alias -> use it.
  2) Otherwise, if store context is present, attempt to infer an existing supplier via:
     - mapped store alias -> store -> supplier_id (high confidence)
     - store_name-derived supplier candidates that match an existing supplier row
       (best-effort; avoids creating a new supplier from a noisy guess)
  3) Fallback: resolve/create supplier from supplier_guess (may call Places).

  Returns {:supplier-id uuid :supplier-alias-id uuid|nil :source keyword}."
  [db supplier-guess extraction opts]
  (let [supplier-guess* (some-> supplier-guess str str/trim not-empty)
        supplier-display-guess (or (markdown/strip-legal-suffix supplier-guess*) supplier-guess*)
        merchant (:merchant extraction)
        raw-address (some-> merchant :raw_address str str/trim not-empty)
        address (some-> merchant :address str str/trim not-empty)
        store-name (some-> merchant :store_name str str/trim not-empty)
        ;; Keep store alias keying consistent with `resolve-store-and-alias`.
        store-alias-guess (or raw-address address store-name)
        store-name-tokens (when store-name
                            (->> (str/split (str/trim store-name) #"\s+")
                              (remove str/blank?)
                              vec))
        store-name-first-two (when (and (seq store-name-tokens)
                                     (>= (count store-name-tokens) 2))
                               (str (nth store-name-tokens 0) " " (nth store-name-tokens 1)))
        store-name-first-one (when (seq store-name-tokens)
                               (nth store-name-tokens 0))
        store-name-candidates (->> [store-name-first-two
                                    store-name-first-one]
                                (remove str/blank?)
                                distinct
                                vec)
        brand-promoted? (some-> merchant :legal_name str str/trim not-empty)
        infer-supplier-from-store-alias
        (fn []
          (when (seq store-alias-guess)
            (try
              (let [alias-row (store-aliases/find-or-create-alias! db store-alias-guess)
                    store-id (:store_id alias-row)
                    store (when store-id (stores/get-store db store-id))
                    supplier-id (:supplier_id store)]
                (when (and store-id supplier-id)
                  {:supplier-id supplier-id
                   :source :store_alias}))
              (catch Exception _
                nil))))
        infer-existing-supplier-from-store-name
        (fn []
          (when (seq store-name-candidates)
            (some
              (fn [cand]
                (try
                  (let [normalized (suppliers/normalize-supplier-key cand)]
                    (when (seq normalized)
                      (when-let [supplier (suppliers/find-by-normalized-key db normalized)]
                        {:supplier-id (:id supplier)
                         :source :store_name_db})))
                  (catch Exception _
                    nil)))
              store-name-candidates)))
        inferred-conflicts-with-promoted-brand?
        (fn [inferred]
          (let [inferred-supplier-id (:supplier-id inferred)
                inferred-supplier (when inferred-supplier-id
                                    (try
                                      ((:get suppliers/service) db inferred-supplier-id)
                                      (catch Exception _
                                        nil)))
                inferred-normalized (some-> inferred-supplier :normalized_key str str/trim not-empty)
                guess-normalized (some-> supplier-guess* suppliers/normalize-supplier-key)]
            (and (seq brand-promoted?)
              (seq guess-normalized)
              (seq inferred-normalized)
              (not= guess-normalized inferred-normalized)
              (not (close-ocr-supplier-keys? guess-normalized inferred-normalized)))))
        alias-needs-brand-repair?
        (fn [alias-row]
          (let [mapped-supplier-id (:supplier_id alias-row)
                mapped-confidence (long (or (:confidence alias-row) 0))
                mapped-supplier (when mapped-supplier-id
                                  (try
                                    ((:get suppliers/service) db mapped-supplier-id)
                                    (catch Exception _
                                      nil)))
                mapped-normalized (some-> mapped-supplier :normalized_key str str/trim not-empty)
                alias-normalized (some-> alias-row :raw_label_normalized str str/trim not-empty)
                guess-normalized (some-> supplier-guess* suppliers/normalize-supplier-key)]
            (and (seq supplier-guess*)
              mapped-supplier-id
              (< mapped-confidence 100)
              (or (and (seq alias-normalized)
                    (seq mapped-normalized)
                    (not= alias-normalized mapped-normalized))
                (and (seq guess-normalized)
                  (seq mapped-normalized)
                  (not= guess-normalized mapped-normalized))))))
        maybe-repair-mapped-alias
        (fn [alias-row]
          (when (alias-needs-brand-repair? alias-row)
            (try
              (let [{:keys [supplier]} (suppliers/find-or-create-supplier! db supplier-display-guess {})
                    supplier-id (:id supplier)
                    alias-id (:id alias-row)]
                (when (and alias-id supplier-id)
                  ;; This repairs previous OCR auto-mappings that promoted legal
                  ;; entity names into canonical supplier names.
                  (supplier-aliases/map-alias-to-supplier! db alias-id supplier-id 25))
                {:supplier-id supplier-id
                 :supplier-alias-id alias-id
                 :source :alias_repaired})
              (catch Exception e
                (log/warn e "Failed to repair supplier alias mapping to OCR brand"
                  {:supplier-alias-id (:id alias-row)})
                nil))))]
    (if-not supplier-guess*
      ;; No supplier guess -> try store-based inference, otherwise unknown.
      (or (infer-supplier-from-store-alias)
        (infer-existing-supplier-from-store-name)
        {:supplier-id (aliases/get-unknown-supplier-id db)
         :supplier-alias-id nil
         :source :unknown})
      (let [alias-row (supplier-aliases/find-or-create-alias! db supplier-guess*)
            alias-id (:id alias-row)
            mapped-supplier-id (:supplier_id alias-row)]
        (if mapped-supplier-id
          (or (maybe-repair-mapped-alias alias-row)
            {:supplier-id mapped-supplier-id
             :supplier-alias-id alias-id
             :source :alias})
          (let [inferred0 (or (infer-supplier-from-store-alias)
                            (infer-existing-supplier-from-store-name))
                inferred (when-not (inferred-conflicts-with-promoted-brand? inferred0)
                           inferred0)]
            (if (and (map? inferred) (:supplier-id inferred))
              (let [supplier-id (:supplier-id inferred)
                    source (:source inferred)
                    confidence (case source
                                 :store_alias 25
                                 :store_name_db 10
                                 10)]
                (when (and alias-id supplier-id)
                  (supplier-aliases/map-alias-to-supplier-if-unmapped! db alias-id supplier-id confidence))
                {:supplier-id supplier-id
                 :supplier-alias-id alias-id
                 :source source})
              (let [{:keys [supplier source]} (suppliers/resolve-or-create-supplier-with-places!
                                                db
                                                supplier-display-guess
                                                opts)
                    supplier-id (:id supplier)]
                (when (and alias-id supplier-id)
                  ;; Safe during ingestion; won't overwrite manual mappings.
                  (supplier-aliases/map-alias-to-supplier-if-unmapped! db alias-id supplier-id 25))
                {:supplier-id supplier-id
                 :supplier-alias-id alias-id
                 :source (or source :resolved)}))))))))

(defn- resolve-store-and-alias
  "Resolve a store (branch/location) for an already-resolved supplier.

  Store aliases are keyed by the provider's raw address guess when available
  (`merchant.raw_address`), otherwise by the post-processed address (preferred)
  or store_name (fallback).

  This keeps alias keying stable (so re-uploads map to the same alias) even when
  we split merged provider strings into `merchant.store_name` + `merchant.address`.

  When Places is configured, store resolution will attempt to canonicalize by
  store.place_id (without changing alias keying).

  Returns {:store-id uuid|nil :store-alias-id uuid|nil :store-guess string|nil :source keyword}."
  [db supplier-id extraction opts]
  (let [merchant (:merchant extraction)
        raw-address (some-> merchant :raw_address str str/trim not-empty)
        address (some-> merchant :address str str/trim not-empty)
        store-name (some-> merchant :store_name str str/trim not-empty)
        supplier-name (some-> merchant :name str str/trim not-empty)
        store-alias-guess (or raw-address address store-name)
        store-guess (or store-name address)]
    (if (or (nil? supplier-id) (not (seq store-alias-guess)))
      {:store-id nil
       :store-alias-id nil
       :store-guess store-guess
       :source :unknown}
      (let [alias-row (store-aliases/find-or-create-alias! db store-alias-guess)
            alias-id (:id alias-row)
            mapped-store-id (:store_id alias-row)]
        (if mapped-store-id
          (do
            (try
              (when (seq store-name)
                (let [mapped-store (stores/get-store db mapped-store-id)
                      existing-display (some-> mapped-store :display_name str str/trim not-empty)
                      looks-like-supplier-name? (and (seq supplier-name)
                                                  (seq existing-display)
                                                  (= (str/lower-case existing-display)
                                                    (str/lower-case supplier-name)))
                      should-promote-store-name? (or (not (seq existing-display))
                                                   looks-like-supplier-name?)]
                  (when should-promote-store-name?
                    (stores/update-store! db mapped-store-id
                      (cond-> {:display_name store-name}
                        (seq address) (assoc :address address))
                      opts))))
              (catch Exception e
                (log/warn e "Failed to promote mapped store display_name from merchant.store_name"
                  {:store-id mapped-store-id
                   :store-alias-id alias-id})))
            {:store-id mapped-store-id
             :store-alias-id alias-id
             :store-guess store-guess
             :source :alias})
          (let [{:keys [store-id store-alias-label]}
                (stores/resolve-store-from-merchant db supplier-id merchant
                  (assoc opts
                    :store-alias-raw-label (:raw_label alias-row)
                    :store-alias-normalized (:raw_label_normalized alias-row)))]
            (when (and alias-id store-id)
              (store-aliases/map-alias-to-store-if-unmapped! db alias-id store-id 25))
            {:store-id store-id
             :store-alias-id alias-id
             :store-guess (or store-name store-alias-label store-guess)
             :source :resolved}))))))

(defn- auto-create-aliases!
  "Best-effort alias creation for extracted line items.

  Returns a per-item snapshot vector aligned with `extraction.items`:
  [{:raw_label <string|nil>
    :article_alias_id <uuid|nil>
    :article_id <uuid|nil>}
   ...]

  When `opts.auto-create-articles?` is true, this will also:
  - create (or reuse) a canonical article for each alias that is still unmapped
  - map the alias to that article

  Never throws; per-item failures are logged and produce nil ids."
  [db supplier-id extraction opts]
  (let [auto-create-articles? (true? (:auto-create-articles? opts))]
    (when (and (map? extraction) (sequential? (:items extraction)))
      (mapv
        (fn [{:keys [raw_label] :as _item}]
          (let [raw-label* (some-> raw_label str str/trim)]
            (if-not (valid-alias-label? raw-label*)
              {:raw_label raw-label*
               :article_alias_id nil
               :article_id nil}
              (try
                (let [alias-row (aliases/find-or-create-alias! db supplier-id raw-label*)
                      alias-id (:id alias-row)
                      existing-article-id (:article_id alias-row)
                      article-id
                      (cond
                        existing-article-id existing-article-id

                        (and auto-create-articles? alias-id)
                        (let [article (articles/find-or-create-article-by-canonical-name! db raw-label*)
                              article-id (:id article)]
                          (when article-id
                            ;; Only map when the alias is still unmapped.
                            (aliases/map-alias-to-article! db alias-id article-id))
                          article-id)

                        :else nil)]
                  {:raw_label raw-label*
                   :article_alias_id alias-id
                   :article_id article-id})
                (catch Exception e
                  (log/warn e "Failed to auto-create article alias/article from receipt extraction item"
                    {:supplier-id supplier-id
                     :raw_label raw-label*
                     :auto-create-articles? auto-create-articles?})
                  {:raw_label raw-label*
                   :article_alias_id nil
                   :article_id nil})))))
        (:items extraction)))))

(defn- resolve-payer-id
  [db {:keys [payer_id user_id]}]
  (or payer_id
    (when user_id
      (-> (user-expense-settings/get-user-expense-settings db user_id)
        user-expense-settings/effective-settings
        :default-payer-id))))

(defn- resolve-purchased-at
  [{:keys [purchased_at_guess created_at]}]
  (or purchased_at_guess created_at))

(defn- build-review-data
  [supplier-id store-id payer-id receipt extraction {:keys [default-currency]}]
  (let [purchased-at (resolve-purchased-at receipt)
        total-amount (:total_amount_guess receipt)
        currency (or (:currency_guess receipt) default-currency "BAM")
        items (vec (or (:items extraction) []))
        notes (str "Extracted from receipt: "
                (or (:original_filename receipt)
                  (:storage_key receipt)
                  "receipt"))]
    (cond-> {:supplier_id supplier-id
             :payer_id payer-id
             :purchased_at purchased-at
             :total_amount total-amount
             :currency currency
             :notes notes
             :items items}
      store-id (assoc :store_id store-id))))

(defn- mark-review-required!
  [db receipt-id message]
  (receipt-status/update-status!
    db
    receipt-id
    "review_required"
    {:error_message message
     :error_details nil}))

(defn- auto-approve-extracted-receipt!
  [db receipt-id extraction supplier-id store-id opts]
  (when-let [receipt (receipt-queries/get-receipt db receipt-id)]
    (when (and (= "extracted" (:status receipt))
            (nil? (:expense_id receipt)))
      (let [payer-id (resolve-payer-id db receipt)
            review-data (build-review-data supplier-id store-id payer-id receipt extraction opts)
            {:keys [purchased_at total_amount items]} review-data]
        (cond
          (nil? payer-id)
          {:status "review_required"
           :error (mark-review-required! db receipt-id "Payer is required to auto-post receipt.")}

          (nil? purchased_at)
          {:status "review_required"
           :error (mark-review-required! db receipt-id "Purchase date is required to auto-post receipt.")}

          (nil? total_amount)
          {:status "review_required"
           :error (mark-review-required! db receipt-id "Total amount is required to auto-post receipt.")}

          (empty? items)
          {:status "review_required"
           :error (mark-review-required! db receipt-id "Line items are required to auto-post receipt.")}

          :else
          (try
            (if-let [user-id (:user_id receipt)]
              (receipt-approval/approve-and-post-for-user! db user-id receipt-id review-data)
              (receipt-approval/approve-and-post! db receipt-id review-data))
            (log/info "Auto-posted extracted receipt" {:receipt-id receipt-id})
            {:status "posted"}
            (catch Exception e
              (log/warn e "Failed to auto-post extracted receipt" {:receipt-id receipt-id})
              {:status "review_required"
               :error (mark-review-required! db receipt-id (or (.getMessage e) "Auto-post failed"))})))))))

(defn reconcile-extraction-with-markdown
  "If provider extraction items don't match the OCR markdown labels, reconcile
  labels and numeric fields by finding the best markdown match.

  Returns {:extraction .. :changed? .. :changes ..}."
  [extraction markdown]
  (if-not (and (map? extraction) (string? markdown) (sequential? (:items extraction)))
    {:extraction extraction
     :changed? false
     :changes []}
    (let [markdown-items (markdown/markdown->line-item-candidates markdown)
          {:keys [items changes]}
          (reduce
            (fn [acc item]
              (let [raw-label (:raw_label item)]
                (cond
                  (markdown/label-present-in-markdown? markdown raw-label)
                  (update acc :items conj item)

                  :else
                  (if-let [match (best-markdown-item-match markdown-items item)]
                    (-> acc
                      (update :items conj (merge item (select-keys match [:raw_label :qty :unit_price :line_total])))
                      (update :changes conj {:from raw-label
                                             :to (:raw_label match)
                                             :match :ocr-markdown}))
                    (update acc :items conj item)))))
            {:items [] :changes []}
            (:items extraction))
          changed? (boolean (seq changes))]
      {:extraction (assoc extraction :items items)
       :changed? changed?
       :changes changes})))

(defn- merge-markdown-merchant-header
  "Merge markdown-derived merchant header fields into a provider merchant map.

  This prefers preserving provider values, but will:
  - fill in missing `:store_name` / `:address`
  - replace a provider `:address` that appears to be a merged
    `store_name + address` string (when markdown provides a cleaner split)

  Some providers also duplicate `merchant.name` into `merchant.store_name`,
  which prevents downstream splitting/merging. We treat that as \"missing\" for
  the purposes of merging.

  We also preserve the provider's raw address under `:raw_address` so that store
  alias keying can remain stable even when we split `:store_name` / `:address`.

  Returns the updated merchant map (or the original value when it cannot merge)."
  [merchant markdown-merchant-header]
  (if-not (and (map? merchant) (map? markdown-merchant-header))
    merchant
    (let [merchant-name0 (some-> merchant :name str str/trim not-empty)
          store-name0-raw (some-> merchant :store_name str str/trim not-empty)
          store-name0 (when (and (seq store-name0-raw)
                              (not (and (seq merchant-name0)
                                     (= (str/lower-case store-name0-raw)
                                       (str/lower-case merchant-name0)))))
                        store-name0-raw)
          redundant-store-name? (and (seq store-name0-raw) (not (seq store-name0)))
          address0 (some-> merchant :address str str/trim not-empty)
          raw-address0 (some-> merchant :raw_address str str/trim not-empty)
          merchant*
          (cond-> merchant
            (and (seq address0) (not (seq raw-address0)))
            (assoc :raw_address address0)

            redundant-store-name?
            (dissoc :store_name))
          store-name-h (some-> markdown-merchant-header :store_name str str/trim not-empty)
          address-h (some-> markdown-merchant-header :address str str/trim not-empty)
          merged-address?
          (and (not (seq store-name0))
            (seq store-name-h)
            (seq address-h)
            (seq address0)
            (not= address0 address-h)
            (str/includes?
              (str/lower-case address0)
              (str/lower-case store-name-h)))
          address-new (cond
                        merged-address? address-h
                        (and (not (seq address0)) (seq address-h)) address-h
                        :else address0)
          store-name-new (or store-name0 store-name-h)]
      (cond-> merchant*
        (and (not (seq store-name0)) (seq store-name-new))
        (assoc :store_name store-name-new)

        (and (or merged-address? (not (seq address0))) (seq address-new))
        (assoc :address address-new)))))

(defn persist-extract-result!
  "Persist a provider extract result, enriched with markdown-derived guesses.

  Returns {:receipt-id .. :stage :extract :result :ok :status extracted|review_required}."
  [db receipt-id extract-result opts]
  (let [receipt (receipt-queries/get-receipt db receipt-id)
        user-region (resolve-user-region db receipt opts)
        opts (cond-> opts
               user-region (assoc :user-region user-region))
        markdown (:parsed-markdown extract-result)
        markdown-items (markdown/markdown->line-item-candidates markdown)
        markdown-merchant-header (markdown/markdown->merchant-header markdown)
        markdown-merchant-name (some-> (:merchant_name markdown-merchant-header) str/trim not-empty)
        markdown-supplier (if markdown-merchant-name
                            markdown-merchant-name
                            (markdown/markdown->supplier-guess markdown))
        markdown-total (markdown/markdown->total-amount markdown)
        markdown-purchased-at (markdown/markdown->purchased-at markdown)
        extraction0 (or (:extraction extract-result) {})
        extraction0 (if (looks-like-json-schema? extraction0) {} extraction0)
        extraction0 (cond
                      (seq (:items extraction0)) extraction0
                      (seq markdown-items) (assoc extraction0 :items markdown-items)
                      (sequential? (:items extraction0)) extraction0
                      :else (assoc extraction0 :items []))
        provider-total0 (common/parse-money (get-in extraction0 [:totals :total]))
        extraction0 (cond-> extraction0
                      (prefer-markdown-total? provider-total0 markdown-total (:items extraction0))
                      (assoc :totals {:total markdown-total})

                      (and (nil? (:purchased_at extraction0)) markdown-purchased-at)
                      (assoc :purchased_at markdown-purchased-at)

                      (and (nil? (get-in extraction0 [:merchant :name])) markdown-supplier)
                      (assoc :merchant {:name markdown-supplier})

                      (map? markdown-merchant-header)
                      (update :merchant merge-markdown-merchant-header markdown-merchant-header))
        final-total (common/parse-money (get-in extraction0 [:totals :total]))
        extraction0 (cond-> extraction0
                      (prefer-markdown-items? (:items extraction0) markdown-items final-total)
                      (assoc :items markdown-items))
        {:keys [extraction changed? changes]}
        (reconcile-extraction-with-markdown extraction0 markdown)
        {:keys [extraction post-processing]}
        (post-process-extraction extraction)
        valid-shape? (and (map? extraction) (m/validate ReceiptExtraction extraction))
        guesses (when (map? extraction)
                  (let [g (extraction->guesses extraction opts)]
                    (cond-> g
                      (and (nil? (:supplier_guess g)) markdown-supplier)
                      (assoc :supplier_guess markdown-supplier)

                      (and (nil? (:total_amount_guess g)) markdown-total)
                      (assoc :total_amount_guess markdown-total))))
        supplier-guess (or (:supplier_guess guesses) markdown-supplier)
        {:keys [supplier-id supplier-alias-id source]}
        (try
          (resolve-supplier-and-alias db supplier-guess extraction opts)
          (catch Exception e
            ;; Never fail extraction just because canonicalization failed.
            (log/warn e "Failed to resolve supplier from supplier_guess" {:receipt-id receipt-id})
            {:supplier-id (aliases/get-unknown-supplier-id db)
             :supplier-alias-id nil
             :source :unknown}))
        unknown-supplier-id (try
                              (aliases/get-unknown-supplier-id db)
                              (catch Exception _
                                nil))
        undefined-supplier? (or (nil? supplier-id)
                             (= :unknown source)
                             (and unknown-supplier-id
                               (= unknown-supplier-id supplier-id)))
        store-res
        (if (= :unknown source)
          {:store-id nil
           :store-alias-id nil
           :store-guess nil
           :source :unknown}
          (try
            (resolve-store-and-alias db supplier-id extraction
              (cond-> opts
                (seq markdown) (assoc :receipt-markdown markdown)))
            (catch Exception e
              ;; Never fail extraction just because store resolution failed.
              (log/warn e "Failed to resolve store from merchant" {:receipt-id receipt-id})
              {:store-id nil
               :store-alias-id nil
               :store-guess nil
               :source :unknown})))
        {:keys [store-id store-alias-id store-guess] :as store-res*}
        store-res
        store-source (:source store-res*)
        status (if (and valid-shape?
                     guesses
                     (not (review-required? guesses))
                     (not undefined-supplier?))
                 "extracted"
                 "review_required")
        lines-total-mismatch (and (= status "extracted")
                               (lines-total-mismatch? (:items extraction) (:total_amount_guess guesses)))
        llm-refine (:llm_refine extract-result)
        item-aliases-snapshot (if (= :unknown source)
                                (when (and (map? extraction) (sequential? (:items extraction)))
                                  (mapv (fn [{:keys [raw_label] :as _item}]
                                          {:raw_label (some-> raw_label str str/trim)
                                           :article_alias_id nil})
                                    (:items extraction)))
                                (try
                                  (auto-create-aliases! db supplier-id extraction opts)
                                  (catch Exception e
                                    (log/warn e "Failed to auto-create aliases from receipt extraction" {:receipt-id receipt-id})
                                    nil)))
        resolution-snapshot {:supplier {:supplier_id supplier-id
                                        :supplier_alias_id supplier-alias-id
                                        :supplier_guess supplier-guess
                                        :source source}
                             :store {:store_id store-id
                                     :store_alias_id store-alias-id
                                     :store_guess store-guess
                                     :source store-source}
                             :items item-aliases-snapshot}
        provider-name (or (some-> (:provider extract-result) str str/trim not-empty)
                        "mistral")
        raw-extract-json (cond-> {:provider provider-name
                                  :received_at (:received-at extract-result)
                                  :model (:model extract-result)
                                  :response (:raw extract-result)
                                  :extraction extraction
                                  :valid_shape? valid-shape?
                                  :resolution_snapshot resolution-snapshot}
                           (map? llm-refine)
                           (assoc :llm_refine (dissoc llm-refine :extraction))

                           changed?
                           (assoc :reconciliation {:changes changes
                                                   :source :parsed_markdown})

                           (map? post-processing)
                           (assoc :post_processing post-processing))]
    (receipt-status/store-extraction-results!
      db
      receipt-id
      (merge {:raw_extract_json raw-extract-json
              :parsed_markdown markdown}
        (select-keys guesses [:supplier_guess
                              :total_amount_guess
                              :currency_guess
                              :purchased_at_guess])
        (when supplier-alias-id {:supplier_alias_id supplier-alias-id})
        (when store-guess {:store_guess store-guess})
        (when store-alias-id {:store_alias_id store-alias-id})))
    (receipt-status/update-status! db receipt-id status {:error_message nil :error_details nil})

    (let [auto-post? (and (not (:defer-refine? opts))
                       (get opts :auto-post-after-upload? true))
          auto-res (when (and (= status "extracted") auto-post?)
                     (try
                       (auto-approve-extracted-receipt! db receipt-id extraction supplier-id store-id opts)
                       (catch Exception e
                         (log/warn e "Failed during auto-approve flow" {:receipt-id receipt-id})
                         nil)))
          _ (when (and (= status "extracted") (not auto-post?))
              (log/info "Auto-post after upload skipped"
                {:receipt-id receipt-id
                 :reason (if (:defer-refine? opts) :defer-refine :disabled)}))
          final-status (or (:status auto-res) status)
          review-required? (and (not= "posted" final-status)
                             (or (= status "review_required")
                               lines-total-mismatch))
          effective-status (if (and (= final-status "extracted") lines-total-mismatch)
                             "review_required"
                             final-status)
          refine-pending? (and (:defer-refine? opts) review-required?)
          _ (when refine-pending?
              (receipt-status/store-extraction-results!
                db
                receipt-id
                {:raw_extract_json (assoc raw-extract-json :refine_pending true)}))]
      {:receipt-id receipt-id
       :stage :extract
       :result :ok
       :status final-status
       :effective-status effective-status
       :review-required? review-required?})))
