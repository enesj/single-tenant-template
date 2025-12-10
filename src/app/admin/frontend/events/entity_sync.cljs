(ns app.admin.frontend.events.entity-sync
  "Bridge events that sync domain-specific admin responses into the shared template entity store.

  This namespace connects the Home Expenses admin API responses (suppliers, payers, receipts,
  articles, article aliases, price observations, expenses) to the template entity store via
  the existing normalization + sync events registered in
  `app.admin.frontend.adapters.expenses`.

  It also provides the `:admin/refresh-entity-list` and `:admin/refresh-entity` event ids
  consumed by the expenses domain event factory."
  (:require
    [app.admin.frontend.adapters.expenses :as expenses-adapter]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(defn- extract-entities
  "Extract the collection of entities for `entity-key` from a standard admin API response.
  Expected shape: {<entity-key> [...]} plus optional pagination metadata."
  [entity-key response]
  (vec (or (get response (keyword (name entity-key))) [])))

(rf/reg-event-fx
  :admin/refresh-entity-list
  (fn [{:keys [_db]} [_ entity-key response]]
    (let [entities (extract-entities entity-key response)
          count*   (count entities)]
      (when (pos? count*)
        (log/debug "Syncing admin list into template entity store"
          {:entity entity-key :count count*}))
      (case entity-key
        ;; Expenses domain entities
        :expenses {:dispatch [::expenses-adapter/sync-expenses entities]}
        :receipts {:dispatch [::expenses-adapter/sync-receipts entities]}
        :suppliers {:dispatch [::expenses-adapter/sync-suppliers entities]}
        :payers {:dispatch [::expenses-adapter/sync-payers entities]}
        :articles {:dispatch [::expenses-adapter/sync-articles entities]}
        :article-aliases {:dispatch [::expenses-adapter/sync-article-aliases entities]}
        :price-observations {:dispatch [::expenses-adapter/sync-price-observations entities]}
        ;; Unknown / unsupported entity-key – no-op, rely on domain-specific state only.
        {}))))

(rf/reg-event-fx
  :admin/refresh-entity
  (fn [{:keys [_db]} [_ entity-key entity]]
    ;; For now we rely on list reloads (or CRUD bridge defaults) to refresh the template
    ;; entity store after single-entity mutations. Keeping this event as a no-op avoids
    ;; accidentally replacing the entire list with a single entity.
    (log/debug "admin/refresh-entity (no-op)"
      {:entity-key entity-key :id (:id entity)})
    {}))
