(ns app.domain.backend.expenses.workers.receipt-ocr.extraction.supplier-store
  (:require
    [app.domain.backend.expenses.services.article-aliases :as aliases]
    [app.domain.backend.expenses.services.store-aliases :as store-aliases]
    [app.domain.backend.expenses.services.stores :as stores]
    [app.domain.backend.expenses.services.supplier-aliases :as supplier-aliases]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [app.domain.backend.expenses.services.user-expense-settings :as user-expense-settings]
    [app.domain.backend.expenses.workers.receipt-ocr.markdown :as markdown]
    [clojure.string :as str]
    [taoensso.timbre :as log]))

(defn resolve-user-region
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

(defn close-ocr-supplier-keys?
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

(defn resolve-supplier-and-alias
  [db supplier-guess extraction opts]
  (let [supplier-guess* (some-> supplier-guess str str/trim not-empty)
        supplier-display-guess (or (markdown/strip-legal-suffix supplier-guess*) supplier-guess*)
        merchant (:merchant extraction)
        raw-address (some-> merchant :raw_address str str/trim not-empty)
        address (some-> merchant :address str str/trim not-empty)
        store-name (some-> merchant :store_name str str/trim not-empty)
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
                  (supplier-aliases/map-alias-to-supplier! db alias-id supplier-id 25))
                {:supplier-id supplier-id
                 :supplier-alias-id alias-id
                 :source :alias_repaired})
              (catch Exception e
                (log/warn e "Failed to repair supplier alias mapping to OCR brand"
                  {:supplier-alias-id (:id alias-row)})
                nil))))
        descriptor-supplier-for-alias
        (fn [alias-row]
          (let [alias-normalized (some-> alias-row :raw_label_normalized str str/trim not-empty)]
            (when (seq alias-normalized)
              (try
                (suppliers/find-unique-descriptor-suffix-supplier db alias-normalized)
                (catch Exception _
                  nil)))))
        alias-needs-descriptor-repair?
        (fn [alias-row descriptor-supplier]
          (let [alias-id (:id alias-row)
                mapped-supplier-id (:supplier_id alias-row)
                mapped-confidence (long (or (:confidence alias-row) 0))
                descriptor-id (:id descriptor-supplier)
                mapped-supplier (when mapped-supplier-id
                                  (try
                                    ((:get suppliers/service) db mapped-supplier-id)
                                    (catch Exception _
                                      nil)))
                mapped-normalized (some-> mapped-supplier :normalized_key str str/trim not-empty)
                alias-normalized (some-> alias-row :raw_label_normalized str str/trim not-empty)]
            (and alias-id
              mapped-supplier-id
              descriptor-id
              (< mapped-confidence 100)
              (seq alias-normalized)
              (seq mapped-normalized)
              (= alias-normalized mapped-normalized)
              (not= mapped-supplier-id descriptor-id))))
        maybe-repair-mapped-alias-to-descriptor
        (fn [alias-row]
          (when-let [descriptor-supplier (descriptor-supplier-for-alias alias-row)]
            (when (alias-needs-descriptor-repair? alias-row descriptor-supplier)
              (try
                (let [alias-id (:id alias-row)
                      supplier-id (:id descriptor-supplier)]
                  (supplier-aliases/map-alias-to-supplier! db alias-id supplier-id 25)
                  {:supplier-id supplier-id
                   :supplier-alias-id alias-id
                   :source :alias_descriptor_repaired})
                (catch Exception e
                  (log/warn e "Failed to repair supplier alias mapping to descriptor-tail supplier"
                    {:supplier-alias-id (:id alias-row)})
                  nil)))))]
    (if-not supplier-guess*
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
            (maybe-repair-mapped-alias-to-descriptor alias-row)
            {:supplier-id mapped-supplier-id
             :supplier-alias-id alias-id
             :source :alias})
          (if-let [descriptor-supplier (descriptor-supplier-for-alias alias-row)]
            (let [supplier-id (:id descriptor-supplier)]
              (when (and alias-id supplier-id)
                (supplier-aliases/map-alias-to-supplier-if-unmapped! db alias-id supplier-id 25))
              {:supplier-id supplier-id
               :supplier-alias-id alias-id
               :source :alias_descriptor})
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
                    (supplier-aliases/map-alias-to-supplier-if-unmapped! db alias-id supplier-id 25))
                  {:supplier-id supplier-id
                   :supplier-alias-id alias-id
                   :source (or source :resolved)})))))))))

(defn resolve-store-and-alias
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
