(ns app.domain.backend.expenses.workers.receipt-ocr.extraction.merchant
  (:require
    [app.domain.backend.expenses.workers.receipt-ocr.markdown :as markdown]
    [clojure.string :as str]))

(def branch-store-prefix-pattern
  #"(?iu)^(ogranak|podružnica|poslovnica|pj\b|tc\b|pc\b|cc\b|centar|market|maloprodaja|prodavnica)\b")

(def legal-entity-pattern
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

(defn post-process-merchant
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

(defn merge-markdown-merchant-header
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
