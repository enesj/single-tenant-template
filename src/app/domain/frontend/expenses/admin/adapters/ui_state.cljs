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

(defn init-expenses-adapter!
  []
  (rf/dispatch [::initialize-entity
                :expenses
                {:sort-field :purchased-at :sort-direction :desc}]))

(defn init-receipts-adapter!
  []
  (rf/dispatch [::initialize-entity
                :receipts
                {:sort-field :created-at :sort-direction :desc}]))

(defn init-suppliers-adapter!
  []
  (rf/dispatch [::initialize-entity
                :suppliers
                {:sort-field :created-at :sort-direction :desc}]))

(defn init-payers-adapter!
  []
  (rf/dispatch [::initialize-entity
                :payers
                {:sort-field :label :sort-direction :asc}]))

(defn init-articles-adapter!
  []
  (rf/dispatch [::initialize-entity
                :articles
                {:sort-field :created-at :sort-direction :desc}]))

(defn init-article-aliases-adapter!
  []
  (rf/dispatch [::initialize-entity
                :article-aliases
                {:sort-field :created-at :sort-direction :desc}]))

(defn init-price-observations-adapter!
  []
  (rf/dispatch [::initialize-entity
                :price-observations
                {:sort-field :observed-at :sort-direction :desc}]))
