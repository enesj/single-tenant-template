(ns app.domain.frontend.expenses.events.user-expenses.list-support
  "Local helpers for user-expenses list request params and loading state."
  (:require
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.events.list.filter-serialization :as filter-serialization]))

(defn build-list-request-params
  "Build request params for a user-expenses list from the current template list UI state."
  [db entity-key default-limit]
  (let [per-page (paths/resolved-list-per-page db entity-key default-limit)
        current-page (paths/resolved-list-current-page db entity-key)
        filters (filter-serialization/flatten-ui-filters
                  (or (get-in db (paths/list-filters entity-key)) {}))
        sort-params (paths/resolved-list-sort-query-params db entity-key)]
    (merge {:limit per-page
            :offset (* (max 0 (dec current-page)) per-page)}
      filters
      sort-params)))

(defn begin-loading
  "Set loading true and clear the local error at the given paths."
  [db loading-path error-path]
  (-> db
    (assoc-in loading-path true)
    (assoc-in error-path nil)))

(defn finish-loading
  "Set loading false and store the extracted error message at the given paths."
  [db loading-path error-path error]
  (-> db
    (assoc-in loading-path false)
    (assoc-in error-path (when error (http/extract-error-message error)))))

(defn begin-entity-load
  "Set shared entity loading metadata for an entity-backed list fetch."
  [db entity-type]
  (begin-loading db (paths/entity-loading? entity-type) (paths/entity-error entity-type)))

(defn finish-entity-load
  "Clear shared entity loading metadata for an entity-backed list fetch."
  [db entity-type error]
  (finish-loading db (paths/entity-loading? entity-type) (paths/entity-error entity-type) error))

(defn entity-list-fetch-fx
  "Build the standard GET list effect for a user-expenses entity-backed list."
  [db {:keys [entity-key default-request-params params uri on-success on-failure]}]
  (let [request-params (merge (or default-request-params {})
                         (when (map? params) params))]
    {:db (begin-entity-load db entity-key)
     :http-xhrio (x/xhrio db
                   {:method :get
                    :uri uri
                    :params request-params
                    :on-success on-success
                    :on-failure on-failure})}))

(defn entity-list-success-fx
  "Build the standard success effect for a user-expenses entity-backed list.

  Assumes rows come from `:data`, `:total` falls back to `(count rows)`, and
  optional `:date-highlights` is stored under the list UI state."
  [db entity-key response sync-event-id]
  (let [rows (vec (or (:data response) []))
        total (or (:total response) (count rows))
        date-highlights (:date-highlights response)]
    {:db (cond-> (-> (finish-entity-load db entity-key nil)
                   (assoc-in (paths/list-total-items entity-key) total))
           (map? date-highlights)
           (assoc-in (conj (paths/list-ui-state entity-key) :date-highlights) date-highlights))
     :dispatch [sync-event-id rows]}))