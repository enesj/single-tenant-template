(ns app.domain.frontend.expenses.admin.adapters.ui-state
  (:require
    [app.admin.frontend.adapters.core :as adapters.core]
    [app.template.frontend.db.paths :as paths]
    [re-frame.core :as rf]))

;; =============================================================================
;; UI state initialization
;; =============================================================================

(defn- initialize-entity-ui-state
  [db entity-key {:keys [sort-field sort-direction]
                  :or {sort-direction :desc}}]
  (let [metadata-path (paths/entity-metadata entity-key)
        ui-state-path (paths/list-ui-state entity-key)
        selected-path (paths/entity-selected-ids entity-key)
        sort-config (cond-> {}
                      sort-field (assoc :field sort-field)
                      sort-direction (assoc :direction sort-direction))]
    (adapters.core/assoc-paths db
      [[(conj metadata-path :sort) sort-config]
       [ui-state-path {:sort sort-config
                       :pagination (merge {:current-page 1}
                                     (:pagination (get-in db ui-state-path)))}]
       [selected-path #{}]])))

(rf/reg-event-db
  ::initialize-entity
  (fn [db [_ entity-key opts]]
    (initialize-entity-ui-state db entity-key opts)))

;; =============================================================================
;; Adapter init helpers
;; =============================================================================

(def ^:private default-init-opts
  {:expenses {:sort-field :purchased-at :sort-direction :desc}
   :receipts {:sort-field :created-at :sort-direction :desc}
   :suppliers {:sort-field :created-at :sort-direction :desc}
   :payers {:sort-field :label :sort-direction :asc}
   :articles {:sort-field :created-at :sort-direction :desc}
   :article-aliases {:sort-field :created-at :sort-direction :desc}
   :price-observations {:sort-field :observed-at :sort-direction :desc}
   :expense-items {:sort-field :created-at :sort-direction :desc}})

(defn init-entity-adapter!
  "Initialize list UI state for a single domain entity.

  Allows optional overrides for things like default sort.
  Example:
    (init-entity-adapter! :expenses {:sort-direction :asc})"
  ([entity-key]
   (init-entity-adapter! entity-key nil))
  ([entity-key opts]
   (rf/dispatch [::initialize-entity
                 entity-key
                 (merge (get default-init-opts entity-key {}) (or opts {}))])))

(def entity-init-fns
  "Map of entity-key -> no-arg init fn.

  This is useful for registries and preloading code that needs a stable lookup
  instead of a long list of per-entity vars."
  (into {}
    (map (fn [entity-key]
           [entity-key (partial init-entity-adapter! entity-key)])
      (keys default-init-opts))))

(defn init-all-adapters!
  "Initialize list UI state for all expenses domain entities." 
  []
  (doseq [[_ init-fn] entity-init-fns]
    (when (fn? init-fn)
      (init-fn))))

;; Backwards-compatible per-entity vars (consumed by domain registry today)
(def init-expenses-adapter! (get entity-init-fns :expenses))
(def init-receipts-adapter! (get entity-init-fns :receipts))
(def init-suppliers-adapter! (get entity-init-fns :suppliers))
(def init-payers-adapter! (get entity-init-fns :payers))
(def init-articles-adapter! (get entity-init-fns :articles))
(def init-article-aliases-adapter! (get entity-init-fns :article-aliases))
(def init-price-observations-adapter! (get entity-init-fns :price-observations))
(def init-expense-items-adapter! (get entity-init-fns :expense-items))
