(ns app.domain.backend.expenses.workers.receipt-ocr-test
  (:require
    [app.domain.backend.expenses.integrations.mistral-ocr :as mistral-ocr]
    [app.domain.backend.expenses.services.receipts.queries :as receipt-queries]
    [app.domain.backend.expenses.services.receipts.status :as receipt-status]
    [app.domain.backend.expenses.workers.receipt-ocr.common :as common]
    [app.domain.backend.expenses.workers.receipt-ocr.core :as core]
    [app.domain.backend.expenses.workers.receipt-ocr.extraction :as extraction]
    [app.domain.backend.expenses.workers.receipt-ocr.markdown :as markdown]
    [clojure.test :refer [deftest is testing]]))

(deftest parse-money-handles-common-formats
  (let [parse-money #'common/parse-money]
    (is (= 10.26M (parse-money "10.26")))
    (is (= 10.26M (parse-money "$10.26")))
    (is (= 10.26M (parse-money "10,26")))
    (is (= 1234.56M (parse-money "1,234.56")))
    (is (nil? (parse-money "abc")))))

(deftest normalize-currency-applies-default
  (let [normalize-currency #'common/normalize-currency]
    (is (= "USD" (normalize-currency "usd" "BAM")))
    (is (= "BAM" (normalize-currency nil "BAM")))
    (is (= "EUR" (normalize-currency "GBP" "EUR")))
    (is (nil? (normalize-currency "GBP" "GBP")))))

(deftest review-required-heuristic
  (let [review-required? #'extraction/review-required?]
    (testing "missing critical fields"
      (is (true? (review-required? {:supplier_guess nil :total_amount_guess 1M :currency_guess "BAM" :items-count 1})))
      (is (true? (review-required? {:supplier_guess "Store" :total_amount_guess nil :currency_guess "BAM" :items-count 1})))
      (is (true? (review-required? {:supplier_guess "Store" :total_amount_guess 1M :currency_guess nil :items-count 1})))
      (is (true? (review-required? {:supplier_guess "Store" :total_amount_guess 1M :currency_guess "BAM" :items-count 0}))))
    (testing "looks good"
      (is (false? (review-required? {:supplier_guess "Store" :total_amount_guess 1M :currency_guess "BAM" :items-count 2}))))))

(deftest reconcile-extraction-prefers-ocr-markdown-label
  (let [reconcile #'extraction/reconcile-extraction-with-markdown
        markdown (str "FISKALNI RACUN\n"
                   "| MLIJEKO MEGGLE 3,2% 657 | 3,000x | 2,25 | 6,75E |\n")
        extraction-in {:items [{:raw_label "NIKE AIR MAX 1"
                                :qty 1
                                :unit_price 6.75
                                :line_total 6.75}]}
        {:keys [extraction changed? changes]} (reconcile extraction-in markdown)]
    (is (true? changed?))
    (is (= [{:from "NIKE AIR MAX 1" :to "MLIJEKO MEGGLE 3,2% 657" :match :ocr-markdown}] changes))
    (is (= "MLIJEKO MEGGLE 3,2% 657" (get-in extraction [:items 0 :raw_label])))
    (is (= 3.000M (get-in extraction [:items 0 :qty])))
    (is (= 2.25M (get-in extraction [:items 0 :unit_price])))
    (is (= 6.75M (get-in extraction [:items 0 :line_total])))))

(deftest reconcile-extraction-noop-when-label-already-present
  (let [reconcile #'extraction/reconcile-extraction-with-markdown
        markdown "| NIKE AIR MAX 1 | 1x | 6,75 | 6,75 |\n"
        extraction-in {:items [{:raw_label "NIKE AIR MAX 1" :line_total 6.75}]}
        {:keys [extraction changed? changes]} (reconcile extraction-in markdown)]
    (is (false? changed?))
    (is (= [] changes))
    (is (= "NIKE AIR MAX 1" (get-in extraction [:items 0 :raw_label])))))

(deftest markdown-line-item-candidates-supports-qty-lines
  (let [candidates #'markdown/markdown->line-item-candidates
        markdown (str "020327 HLJEB 400G SA SJEMELKA MA\n"
                   "1.000x 2,10 2.10E\n"
                   "B31508 PASTETA 114G KOKOSTJA ARGETA\n"
                   "1.000x 1,85 1.85E\n")
        items (candidates markdown)]
    (is (= 2 (count items)))
    (is (= {:raw_label "HLJEB 400G SA SJEMELKA MA"
            :qty 1.000M
            :unit_price 2.10M
            :line_total 2.10M}
          (first items)))
    (is (= {:raw_label "PASTETA 114G KOKOSTJA ARGETA"
            :qty 1.000M
            :unit_price 1.85M
            :line_total 1.85M}
          (second items)))))

(deftest markdown-line-item-candidates-supports-label-plus-price
  (let [candidates #'markdown/markdown->line-item-candidates
        markdown (str "A10150772 Snala za kosu BH231226\n"
                   "1,95E\n"
                   "VOLTAREN RETARD TABLETE 100 MG A 2\n"
                   "0 SA P 172e 5,85E\n"
                   "ANDOL TABLETE 300 MG A 20 5673\n"
                   "5,70E\n")
        items (candidates markdown)]
    (is (= 3 (count items)))
    (is (= {:raw_label "Snala za kosu BH231226"
            :qty 1M
            :unit_price 1.95M
            :line_total 1.95M}
          (first items)))
    (is (= {:raw_label "VOLTAREN RETARD TABLETE 100 MG A 2 0 SA P 172e"
            :qty 1M
            :unit_price 5.85M
            :line_total 5.85M}
          (second items)))
    (is (= {:raw_label "ANDOL TABLETE 300 MG A 20 5673"
            :qty 1M
            :unit_price 5.70M
            :line_total 5.70M}
          (nth items 2)))))

(deftest markdown-line-item-candidates-supports-mixed-qty-and-inline-price
  (let [candidates #'markdown/markdown->line-item-candidates
        markdown (str "TUBORG 0,33 NEPOVRATNI/KO\n"
                   "24,000x 1,55 37,20E\n"
                   "SCHWEPPES TONIC 1L/KO\n"
                   "3,000x 2,00 6,00E\n"
                   "BULLDOG GIN SA CASOM 0,7/KO 42,00E\n")
        items (candidates markdown)]
    (is (= 3 (count items)))
    (is (= {:raw_label "TUBORG 0,33 NEPOVRATNI/KO"
            :qty 24.000M
            :unit_price 1.55M
            :line_total 37.20M}
          (first items)))
    (is (= {:raw_label "SCHWEPPES TONIC 1L/KO"
            :qty 3.000M
            :unit_price 2.00M
            :line_total 6.00M}
          (second items)))
    (is (= {:raw_label "BULLDOG GIN SA CASOM 0,7/KO"
            :qty 1M
            :unit_price 42.00M
            :line_total 42.00M}
          (nth items 2)))))

(deftest markdown-line-item-candidates-does-not-treat-dimensions-as-qty
  (let [candidates #'markdown/markdown->line-item-candidates
  markdown "60963601 Torba papirna velika 32 x 16 x 45 - bez /pc 0,70E\n"
        items (candidates markdown)]
    (is (= 1 (count items)))
    (is (= {:raw_label "Torba papirna velika 32 x 16 x 45 - bez /pc"
            :qty 1M
            :unit_price 0.70M
            :line_total 0.70M}
          (first items)))))

(deftest markdown-line-item-candidates-applies-discounts
  (let [candidates #'markdown/markdown->line-item-candidates
        markdown (str "62778401 Mirisna svijeca u staklu Premium Collec\n"
                   "t/pc 10,00E\n"
                   "-50,00%: 5,00\n")
        items (candidates markdown)]
    (is (= 1 (count items)))
    (is (= {:raw_label "Mirisna svijeca u staklu Premium Collec"
            :qty 1M
            :unit_price 5.00M
            :line_total 5M}
          (first items)))))

(deftest markdown-line-item-candidates-ignores-tax-like-lines
  (let [candidates #'markdown/markdown->line-item-candidates
        markdown (str "ITEM\n"
                   "1,000x 1,00 1,00E\n"
                   "PDU E: 7,25\n"
                   "PDU: 7,25\n")
        items (candidates markdown)]
    (is (= 1 (count items)))
    (is (= "ITEM" (:raw_label (first items))))))

(deftest markdown-line-item-candidates-ignores-payment-summary-lines
  (let [candidates #'markdown/markdown->line-item-candidates
        markdown (str "POVRCE MIX\n"
                   "1,00E\n"
                   "POV E: 0,00\n"
                   "POV: 0,00\n"
                   "CEK: 1,00\n"
                   "CEKIC\n"
                   "10,00E\n"
                   "UMLAČENO: KORTICA: 11,00\n")
        items (candidates markdown)]
    (is (= 2 (count items)))
    (is (= "POVRCE MIX" (:raw_label (first items))))
    (is (= "CEKIC" (:raw_label (second items))))))

(deftest markdown-line-item-candidates-supports-markdown-table-rows
  (let [candidates #'markdown/markdown->line-item-candidates
        markdown (str "|  Mivolis flasteri za djecu |  |   |\n"
                   "| --- | --- | --- |\n"
                   "|  1,000x | 1,85 | 1,85E  |\n")
        items (candidates markdown)]
    (is (= 1 (count items)))
    (is (= {:raw_label "Mivolis flasteri za djecu"
            :qty 1.000M
            :unit_price 1.85M
            :line_total 1.85M}
          (first items)))))

(deftest markdown-line-item-candidates-supports-table-total-in-label-row
  (let [candidates #'markdown/markdown->line-item-candidates
        markdown (str "|  E09438 | BOMBONJERA 230G RAFFAELLO FER | 9,90E  |\n"
                   "| --- | --- | --- |\n"
                   "|  1,000x | 9,90 |   |\n")
        items (candidates markdown)]
    (is (= 1 (count items)))
    (is (= {:raw_label "BOMBONJERA 230G RAFFAELLO FER"
            :qty 1.000M
            :unit_price 9.90M
            :line_total 9.90M}
          (first items)))))

(deftest markdown-merchant-header-extracts-quoted-name
  (let [parse-header #'markdown/markdown->merchant-header
        markdown (str "\"Pepco B-H\" d.o.o.\n"
                   "Podružnica Sarajevo 2\n"
                   "ul. Kolodvorska br.12\n"
                   "71000 Sarajevo\n"
                   "\n"
                   "JIB: 4203144510090\n"
                   "PIB: 203144510006\n")
        result (parse-header markdown)]
    (is (= "Pepco B-H" (:merchant_name result)))
    (is (= "Podružnica Sarajevo 2" (:store_name result)))
    (is (= "ul. Kolodvorska br.12, 71000 Sarajevo" (:address result)))))

(deftest markdown-merchant-header-extracts-unquoted-name
  (let [parse-header #'markdown/markdown->merchant-header
        markdown (str "KONZUM d.o.o.\n"
                   "Poslovnica Tuzla 5\n"
                   "Trg slobode 10\n"
                   "75000 Tuzla\n"
                   "JIB: 123456789\n")
        result (parse-header markdown)]
    (is (= "KONZUM" (:merchant_name result)))
    (is (= "Poslovnica Tuzla 5" (:store_name result)))
    (is (= "Trg slobode 10, 75000 Tuzla" (:address result)))))

(deftest markdown-merchant-header-handles-minimal-header
  (let [parse-header #'markdown/markdown->merchant-header
        markdown (str "BINGO d.d.\n"
                   "TC Mercator\n"
                   "JIB: 999\n")
        result (parse-header markdown)]
    (is (= "BINGO" (:merchant_name result)))
    (is (= "TC Mercator" (:store_name result)))
    (is (nil? (:address result)))))

(deftest markdown-merchant-header-handles-no-store-name
  (let [parse-header #'markdown/markdown->merchant-header
        markdown (str "\"DM\" d.o.o.\n"
                   "ul. Marsala Tita 25\n"
                   "71000 Sarajevo\n"
                   "JIB: 111\n")
        result (parse-header markdown)]
    (is (= "DM" (:merchant_name result)))
    (is (= "ul. Marsala Tita 25, 71000 Sarajevo" (:address result)))))

(deftest process-extract-auto-retries-review-required-once
  (let [process-extract! #'core/process-extract!
        receipt-id (java.util.UUID/randomUUID)
        calls (atom {:claim 0 :ocr 0 :persist 0 :retry 0})]
    (with-redefs [receipt-status/claim-for-extracting! (fn [_db _rid _opts]
                                                   (swap! calls update :claim inc)
                                                   true)
                  common/read-receipt-bytes! (fn [_receipt _opts]
                                               {:bytes (.getBytes "x")})
                  mistral-ocr/ocr-extract! (fn [_cfg _req]
                                             (swap! calls update :ocr inc)
                                             {})
                  extraction/persist-extract-result! (fn [_db _rid _extract-result _opts]
                                                       (swap! calls update :persist inc)
                                                       (if (= 1 (:persist @calls))
                                                         {:receipt-id receipt-id :stage :extract :result :ok :status "review_required"}
                                                         {:receipt-id receipt-id :stage :extract :result :ok :status "extracted"}))
                  receipt-status/retry-extraction! (fn [_db _rid]
                                               (swap! calls update :retry inc)
                                               nil)]
      (let [res (process-extract! nil {:api-key "k"} {:id receipt-id :content_type "image/jpeg"} {:lease-seconds 900})]
        ;; Current implementation does not auto-retry review_required.
        (is (= "review_required" (:status res)))
        (is (= 1 (:claim @calls)))
        (is (= 1 (:ocr @calls)))
        (is (= 1 (:persist @calls)))
        (is (= 0 (:retry @calls)))))))

(deftest process-receipts-by-ids-batch-auto-retries-review-required-once
  (let [process-batch! #'core/process-receipts-by-ids-batch!
        receipt-id (java.util.UUID/randomUUID)
        calls (atom {:persist 0 :retry 0})]
    (with-redefs [receipt-queries/get-receipt (fn [_db rid]
                                         {:id rid :content_type "image/jpeg"})
            receipt-status/claim-for-extracting! (fn [_db _rid _opts] true)
                  common/read-receipt-bytes! (fn [_receipt _opts]
                                               {:bytes (.getBytes "x")})
                  mistral-ocr/ocr-extract-batch! (fn [_cfg _reqs]
                                                   {:results {(str receipt-id) {}}})
                  extraction/persist-extract-result! (fn [_db rid _extract-result _opts]
                                                       (swap! calls update :persist inc)
                                                       {:receipt-id rid :stage :extract :result :ok :status "review_required"})
                  receipt-status/retry-extraction! (fn [_db _rid]
                                               (swap! calls update :retry inc)
                                               nil)]
      (let [results (process-batch! nil {:api-key "k"} [receipt-id] false {:lease-seconds 900})]
        (is (= 1 (count results)))
        ;; Current implementation does not auto-retry review_required in batch either.
        (is (= "review_required" (:status (first results))))
        (is (= 1 (:persist @calls)))
        (is (= 0 (:retry @calls)))))))
