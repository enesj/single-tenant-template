(ns app.domain.backend.expenses.services.stores.resolution
  (:require
    [app.domain.backend.expenses.services.places-api :as places-api]
    [app.domain.backend.expenses.services.service-configs :as configs]
    [app.domain.backend.expenses.services.stores.matching :as store-matching]
    [app.domain.backend.expenses.services.stores.receipt-fingerprint :as receipt-fp]
    [app.domain.backend.expenses.services.stores.repo :as repo]
    [app.domain.backend.expenses.services.stores.upsert :as upsert]
    [app.domain.backend.expenses.services.suppliers :as suppliers]
    [app.shared.type-conversion :as type-conv]
    [clojure.string :as str]))

(def ^:private try-uuid type-conv/try-parse-uuid)

(defn- same-store-text?
  [a b]
  (let [a* (some-> a str str/trim not-empty)
        b* (some-> b str str/trim not-empty)]
    (and (seq a*)
      (seq b*)
      (= (str/lower-case a*)
        (str/lower-case b*)))))

(defn- normalize-store-display-name
  [supplier-name store-name address]
  (let [supplier-name* (some-> supplier-name str str/trim not-empty)
        store-name* (some-> store-name str str/trim not-empty)
        address* (some-> address str str/trim not-empty)]
    (cond
      (same-store-text? store-name* supplier-name*)
      nil

      (and (or (same-store-text? store-name* address*)
             (nil? store-name*))
        (seq supplier-name*)
        (seq address*))
      (str/join " " [supplier-name* address*])

      :else
      store-name*)))

(defn- fallback-store-without-location-evidence
  [db supplier-id supplier-name opts]
  (when-let [supplier-id* (try-uuid supplier-id)]
    (let [supplier-name* (some-> supplier-name str str/trim not-empty)
          existing-stores (try
                            (repo/list-stores-for-supplier db supplier-id*)
                            (catch Exception e
                              (throw (ex-info "Failed to list stores for supplier fallback"
                                       {:supplier-id supplier-id*}
                                       e))))]
      (cond
        (= 1 (count existing-stores))
        (let [store (first existing-stores)]
          {:store store
           :store-id (:id store)
           :store-alias-label nil})

        (and (empty? existing-stores) (seq supplier-name*))
        (let [store (upsert/find-or-create-store!
                      db
                      {:supplier_id supplier-id*
                       :display_name supplier-name*
                       :normalized_key (configs/normalize-store-key supplier-name*)}
                      opts)]
          {:store store
           :store-id (:id store)
           :store-alias-label nil})

        :else
        nil))))

(defn resolve-store-from-merchant
  "Best-effort store resolution from merchant data."
  ([db supplier-id merchant]
   (resolve-store-from-merchant db supplier-id merchant nil))
  ([db supplier-id {:keys [name store_name address]}
    {:keys [places-cfg user-region receipt-markdown
            store-alias-normalized store-alias-raw-label supplier-display-name]
     :as opts}]
   (let [supplier-id* (try-uuid supplier-id)
         merchant-name* (some-> name str str/trim not-empty)
         supplier-display-name* (or (some-> supplier-display-name str str/trim not-empty)
                                  (when (and (not (seq merchant-name*)) supplier-id*)
                                    (some-> ((:get suppliers/service) db supplier-id*)
                                      :display_name
                                      str
                                      str/trim
                                      not-empty)))
         supplier-name* (or supplier-display-name*
                          merchant-name*)
         address* (some-> address str str/trim not-empty)
         store-name* (normalize-store-display-name supplier-name* store_name address)
         store-label (or address* store-name*)
         store-display-basic (or store-name* address* supplier-name* "Store")
         alias-key (or (some-> store-alias-normalized str str/trim not-empty)
                     (some-> store-label configs/normalize-store-key))
         pj-source (->> [receipt-markdown store-alias-raw-label store-name* address*]
                     (remove str/blank?)
                     (str/join "\n"))
         pj (some-> (receipt-fp/pj-number pj-source) str str/trim not-empty)
         pj-key (when pj
                  (configs/normalize-store-key (str "pj " pj)))
         store-key (or pj-key alias-key)
         places-enabled? (and (map? places-cfg)
                           (seq (:api-key places-cfg))
                           (not (seq pj-key))
                           (not (seq store-alias-normalized)))
         places-query (when (and places-enabled? (seq store-label))
                        (->> [supplier-name* store-name* address*]
                          (remove str/blank?)
                          (str/join " ")))
         places-opts {:region-code (or user-region (:region-code places-cfg))
                      :language-code (:language-code places-cfg)
                      :max-results (or (:max-results places-cfg) 3)
                      :location-bias (:location-bias places-cfg)
                      :field-mask "places.displayName,places.id,places.formattedAddress"}
         place (when (seq places-query)
                 (first (:places (places-api/search-text! places-cfg places-query places-opts))))
         place-id (some-> place :raw :id str str/trim not-empty)
         place-key (repo/place-id->normalized-key place-id)
         place-name (some-> place :name str str/trim not-empty)
         place-address (some-> place :raw :formattedAddress str str/trim not-empty)
         effective-alias-label (or address* place-address store-name*)
         effective-address (or place-address address*)
         effective-display (or store-name* effective-address place-name store-display-basic)]
     (when supplier-id*
       (if-not (seq effective-alias-label)
         (fallback-store-without-location-evidence db supplier-id* supplier-name* opts)
         (let [store (cond
                       (seq place-id)
                       (or
                         (when-let [existing (and (seq place-key)
                                               (repo/find-by-supplier-and-normalized-key
                                                 db
                                                 supplier-id*
                                                 place-key))]
                           (upsert/update-store! db (:id existing)
                             {:display_name effective-display
                              :address effective-address
                              :normalized_key place-key}
                             opts))
                         (when-let [existing (and (seq store-key)
                                               (repo/find-by-supplier-and-normalized-key db supplier-id* store-key))]
                           (upsert/update-store! db (:id existing)
                             {:display_name effective-display
                              :address effective-address
                              :normalized_key (or place-key store-key)}
                             opts))
                         (when-let [legacy (repo/find-by-supplier-and-normalized-key
                                             db
                                             supplier-id*
                                             (configs/normalize-store-key (str store-display-basic " " (or address* ""))))]
                           (upsert/update-store! db (:id legacy)
                             {:display_name effective-display
                              :address effective-address
                              :normalized_key (or place-key store-key)}
                             opts))
                         (upsert/find-or-create-store!
                           db
                           {:supplier_id supplier-id*
                            :display_name effective-display
                            :address effective-address
                            :normalized_key (or place-key store-key)}
                           opts))

                       :else
                       (or
                         (when (seq store-key)
                           (repo/find-by-supplier-and-normalized-key db supplier-id* store-key))
                         (when (and (seq alias-key) (not= alias-key store-key))
                           (repo/find-by-supplier-and-normalized-key db supplier-id* alias-key))
                         (when-let [store-id (receipt-fp/match-store-id db supplier-id* pj-source)]
                           (repo/get-store db store-id))
                         (when-let [{:keys [store-id]} (store-matching/match-store
                                                         alias-key
                                                         (repo/store-candidates-for-supplier db supplier-id*))]
                           (repo/get-store db store-id))
                         (upsert/find-or-create-store!
                           db
                           {:supplier_id supplier-id*
                            :display_name (or store-display-basic
                                            (some-> store-alias-raw-label str str/trim not-empty)
                                            store-key
                                            alias-key
                                            "Store")
                            :address address*
                            :normalized_key store-key}
                           opts)))
               _ (when (and (seq place-key) (:id store))
                   (upsert/merge-duplicate-stores-by-place-id! db supplier-id* place-id (:id store)))]
           {:store store
            :store-id (:id store)
            :store-alias-label effective-alias-label}))))))
