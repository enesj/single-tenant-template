(ns app.domain.backend.expenses.services.duplicates.config
  (:require
    [app.domain.backend.expenses.services.service-configs.normalization :as normalize]))

(def entity-configs
  {:suppliers {:table "suppliers"
               :name-col :display_name
               :key-col :normalized_key
               :fk-tables {:expenses {:col :supplier_id}
                           :stores {:col :supplier_id}
                           :supplier_aliases {:col :supplier_id}
                           :article_aliases {:col :supplier_id}}}
   :articles {:table "articles"
              :name-col :canonical_name
              :key-col :normalized_key
              :group-col :unit
              :display-cols [:unit]
              :fk-tables {:expense_items {:col :article_id}
                          :article_aliases {:col :article_id}}}
   :stores {:table "stores"
            :name-col :display_name
            :key-col :normalized_key
            :group-col :supplier_id
            :fk-tables {:expenses {:col :store_id}
                        :store_aliases {:col :store_id}}}
   :manufacturers {:table "manufacturers"
                   :name-col :display_name
                   :key-col :normalized_key
                   :fk-tables {:articles {:col :manufacturer_id}}}
   :subcategories {:table "subcategories"
                   :name-col :name
                   :normalize-fn normalize/normalize-store-key
                   :fk-tables {:articles {:col :subcategory_id}}}})

(def default-prefix-fetch-limit
  5000)

(def max-prefix-fetch-limit
  20000)

(defn normalize-fetch-limit
  [fetch-limit]
  (-> (or fetch-limit default-prefix-fetch-limit)
    (max 1)
    (min max-prefix-fetch-limit)))

(defn get-entity-config!
  [entity-type]
  (or (get entity-configs entity-type)
    (throw (ex-info (str "Unknown entity type: " entity-type)
             {:entity-type entity-type
              :valid-types (keys entity-configs)}))))