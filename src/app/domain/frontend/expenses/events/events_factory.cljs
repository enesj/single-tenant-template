(ns app.domain.frontend.expenses.events.events-factory
  "Factory functions for generating standard CRUD events for expenses domain entities.
   
   This namespace provides generic event generators that eliminate code duplication
   across the expenses domain events while maintaining flexibility for entity-specific
   behavior through configuration."
  (:require
    [ajax.core :as ajax]
    [app.admin.frontend.utils.http :as admin-http]
    [app.template.frontend.db.paths :as paths]
    [day8.re-frame.http-fx]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; =============================================================================
;; Entity Configuration Protocol
;; =============================================================================

(defn validate-entity-config
  "Validates that an entity configuration has all required keys."
  [{:keys [entity-key base-path api-endpoint] :as config}]
  (when-not (and entity-key base-path api-endpoint)
    (throw (ex-info "Invalid entity configuration"
             {:missing-keys (cond-> []
                              (not entity-key) (conj :entity-key)
                              (not base-path) (conj :base-path)
                              (not api-endpoint) (conj :api-endpoint))
              :config config})))
  config)

;; =============================================================================
;; Generic State Management
;; =============================================================================

(defn begin-load
  "Sets loading state and clears errors for an entity."
  [db entity-key base-path]
  (-> db
    (assoc-in (paths/entity-loading? entity-key) true)
    (assoc-in (paths/entity-error entity-key) nil)
    (assoc-in (conj base-path :loading?) true)
    (assoc-in (conj base-path :error) nil)))

(defn finish-load
  "Clears loading state and sets error if present."
  [db entity-key base-path error]
  (let [error-val (when error (admin-http/extract-error-message error))]
    (-> db
      (assoc-in (paths/entity-loading? entity-key) false)
      (assoc-in (paths/entity-error entity-key) error-val)
      (assoc-in (conj base-path :loading?) false)
      (assoc-in (conj base-path :error) error-val))))

;; =============================================================================
;; Generic Pagination Resolver
;; =============================================================================

(defn resolve-pagination
  "Generic pagination resolver that works with configurable parameter keys.
   
   Options:
   - :default-per-page (default: 25)
   - :param-keys (map of parameter names to extract from params)
     Default: {:limit :limit, :offset :offset, :page :page, :per-page :per-page}"
  [entity-key db {:keys [_limit _offset _page _per-page] :as params} {:keys [default-per-page param-keys]}]
  (let [{:keys [limit-key offset-key page-key per-page-key]} (or param-keys
                                                               {:limit-key :limit
                                                                :offset-key :offset
                                                                :page-key :page
                                                                :per-page-key :per-page})
        limit (get params limit-key)
        offset (get params offset-key)
        page (get params page-key)
        per-page (get params per-page-key)

        existing-per-page (or (get-in db (paths/list-per-page entity-key))
                            (get-in db (conj (paths/list-ui-state entity-key) :per-page))
                            (get-in db (conj (paths/list-ui-state entity-key) :pagination :per-page))
                            default-per-page 25)
        existing-page (or (get-in db (paths/list-current-page entity-key))
                        (get-in db (conj (paths/list-ui-state entity-key) :current-page))
                        (get-in db (conj (paths/list-ui-state entity-key) :pagination :current-page))
                        1)

        per-page (or limit per-page existing-per-page)
        page (or page (when offset (inc (quot offset (max per-page 1)))) existing-page 1)
        offset (or offset (* (max 0 (dec page)) per-page))]

    {:limit per-page
     :offset offset
     :page page
     :per-page per-page}))

(defn update-pagination-state
  "Updates all pagination-related paths in the database."
  [db entity-key pagination]
  (let [{:keys [page per-page]} pagination]
    (-> db
      (assoc-in (paths/list-per-page entity-key) per-page)
      (assoc-in (paths/list-current-page entity-key) page)
      (assoc-in (conj (paths/list-ui-state entity-key) :pagination) {:current-page page :per-page per-page})
      (assoc-in (conj (paths/entity-metadata entity-key) :pagination) {:page page :per-page per-page}))))

(defn- extra-query-params
  "Extract non-pagination params for list requests.

   Removes pagination keys (limit/offset/page/per-page) and nil values,
   forwarding everything else to the backend for filtering."
  [params {:keys [param-keys]}]
  (let [{:keys [limit-key offset-key page-key per-page-key]}
        (or param-keys
          {:limit-key :limit
           :offset-key :offset
           :page-key :page
           :per-page-key :per-page})
        reserved #{limit-key offset-key page-key per-page-key}
        reserved-strings (set (map name reserved))]
    (into {}
      (remove (fn [[k v]]
                (or (nil? v)
                  (contains? reserved k)
                  (and (string? k) (contains? reserved-strings k)))))
      (or params {}))))

;; =============================================================================
;; Event Generator Functions
;; =============================================================================

(defn generate-list-events
  "Generates load-list, list-loaded, and load-failed events for an entity."
  [{:keys [entity-key base-path api-endpoint pagination-opts] :as config}]
  (validate-entity-config config)

  (let [pag-opts (or pagination-opts {:default-per-page 25})
        event-ns (str "app.domain.frontend.expenses.events." (name entity-key))]

    ;; load-list event
    (rf/reg-event-fx
      (keyword event-ns "load-list")
      (fn [{:keys [db]} [_ params]]
        (let [params (or params {})
              fetch-limit (:fetch-limit params)
              fetch-offset (or (:fetch-offset params) 0)
              fetch-mode? (some? fetch-limit)
              ;; In server mode, seed params with current DB filter state so that
              ;; page changes and CRUD reloads preserve the active search/filters.
              server-mode? (= :server (get-in db (paths/list-pagination-mode entity-key)))
              db-filters (when (and server-mode? (not fetch-mode?))
                           (get-in db (paths/list-filters entity-key) {}))
              params (cond-> params
                       (seq db-filters) (as-> p (merge db-filters p)))
              params* (cond-> params
                        fetch-mode? (dissoc :fetch-limit :fetch-offset))
              {:keys [limit offset] :as pagination} (when-not fetch-mode?
                                                      (resolve-pagination entity-key db params* pag-opts))
              extra-params (extra-query-params params* pag-opts)
              request-params (if fetch-mode?
                               (merge {:limit fetch-limit :offset fetch-offset} extra-params)
                               (merge {:limit limit :offset offset} extra-params))
              db* (-> db
                    (begin-load entity-key base-path))
              db* (if pagination
                    (update-pagination-state db* entity-key pagination)
                    db*)]
          {:db db*
           :http-xhrio (admin-http/admin-get
                         {:uri api-endpoint
                          :params request-params
                          :response-format (ajax/json-response-format {:keywords? true})
                          :on-success [(keyword event-ns "list-loaded") pagination]
                          :on-failure [(keyword event-ns "load-failed")]})})))

    ;; list-loaded event
    (rf/reg-event-fx
      (keyword event-ns "list-loaded")
      (fn [{:keys [db]} [_ pagination response]]
        (let [total (:total response)
              db* (-> db
                    (finish-load entity-key base-path nil)
                    (assoc-in (conj base-path :items)
                      (vec (or (get response (keyword (name entity-key))) []))))
              ;; Store total for server-mode pagination UI
              db* (if (some? total)
                    (assoc-in db* (paths/list-total-items entity-key) total)
                    db*)
              db* (if pagination
                    (update-pagination-state db* entity-key pagination)
                    db*)]
          {:db db*
           :dispatch-n [[:admin/refresh-entity-list entity-key response]]})))

    ;; load-failed event
    (rf/reg-event-fx
      (keyword event-ns "load-failed")
      (fn [{:keys [db]} [_ error]]
        {:db (finish-load db entity-key base-path error)}))))

(defn generate-detail-events
  "Generates load-detail and detail-loaded events for an entity.
   
   Config options:
   - :detail-response-key (optional) - key to extract entity from response.
     Defaults to (keyword (name entity-key)). Use this when the API returns
     a singular key (e.g. :expense) but entity-key is plural (e.g. :expenses)."
  [{:keys [entity-key base-path api-endpoint detail-response-key] :as config}]
  (validate-entity-config config)

  (let [event-ns (str "app.domain.frontend.expenses.events." (name entity-key))
        ;; Allow override of response key for cases like :expenses -> :expense
        response-key (or detail-response-key (keyword (name entity-key)))]

    ;; load-detail event
    (rf/reg-event-fx
      (keyword event-ns "load-detail")
      (fn [{:keys [db]} [_ entity-id]]
        (let [loading? (true? (get-in db (conj base-path :detail-loading?)))
              loading-id (get-in db (conj base-path :detail-loading-id))]
          ;; Prevent duplicate requests for the same entity while a request is in-flight.
          ;; This guards against StrictMode double-effects and modal remount flicker.
          (if (and loading? (= loading-id entity-id))
            {:db db}
            {:db (-> db
                   (assoc-in (conj base-path :detail-loading?) true)
                   (assoc-in (conj base-path :detail-loading-id) entity-id)
                   (assoc-in (conj base-path :error) nil))
             :http-xhrio (admin-http/admin-get
                           {:uri (str api-endpoint "/" entity-id)
                            :response-format (ajax/json-response-format {:keywords? true})
                            :on-success [(keyword event-ns "detail-loaded") entity-id]
                            :on-failure [(keyword event-ns "detail-load-failed") entity-id]})}))))

    ;; detail-loaded event
    (rf/reg-event-db
      (keyword event-ns "detail-loaded")
      (fn [db [_ entity-id response]]
        (let [entity (get response response-key)]
          (-> db
            (assoc-in (conj base-path :detail-loading?) false)
            (assoc-in (conj base-path :detail-loading-id) nil)
            (assoc-in (conj base-path :error) nil)
            (assoc-in (conj base-path :by-id entity-id) entity)))))

    ;; detail-load-failed event
    (rf/reg-event-db
      (keyword event-ns "detail-load-failed")
      (fn [db [_ _entity-id error]]
        (let [msg (admin-http/extract-error-message error)]
          (-> db
            (assoc-in (conj base-path :detail-loading?) false)
            (assoc-in (conj base-path :detail-loading-id) nil)
            (assoc-in (conj base-path :error) msg)))))))

(defn generate-detail-modal-events
  "Generates open/close detail modal events for an entity."
  [{:keys [entity-key base-path] :as config}]
  (validate-entity-config config)
  (let [event-ns (str "app.domain.frontend.expenses.events." (name entity-key))
        modal-path (conj base-path :detail-modal)]
    (rf/reg-event-fx
      (keyword event-ns "open-detail-modal")
      (fn [{:keys [db]} [_ entity-id]]
        (cond-> {:db (-> db
                       (assoc-in (conj modal-path :open?) true)
                       (assoc-in (conj modal-path :entity-id) entity-id))}
          entity-id
          (assoc :dispatch [(keyword event-ns "load-detail") entity-id]))))

    (rf/reg-event-db
      (keyword event-ns "close-detail-modal")
      (fn [db _]
        (-> db
          (assoc-in (conj modal-path :open?) false)
          (assoc-in (conj modal-path :entity-id) nil))))))

(defn generate-form-events
  "Generates create-entry, create-success, and create-failed events for entities with forms."
  [{:keys [entity-key _base-path api-endpoint form-path] :as config}]
  (validate-entity-config config)

  (let [event-ns (str "app.domain.frontend.expenses.events." (name entity-key))
        form-path' (or form-path [:admin (keyword (name entity-key)) :form])]

    ;; create-entry event
    (rf/reg-event-fx
      (keyword event-ns "create-entry")
      (fn [{:keys [db]} [_ form-data]]
        {:db (-> db
               (assoc-in (conj form-path' :loading?) true)
               (assoc-in (conj form-path' :error) nil))
         :http-xhrio (admin-http/admin-post
                       {:uri api-endpoint
                        :params form-data
                        :response-format (ajax/json-response-format {:keywords? true})
                        :on-success [(keyword event-ns "create-entry-success")]
                        :on-failure [(keyword event-ns "create-entry-failed")]})}))

    ;; create-entry-success event
    (rf/reg-event-fx
      (keyword event-ns "create-entry-success")
      (fn [{:keys [db]} [_ response]]
        (let [entity (get response (keyword (name entity-key)))
              entity-id (:id entity)]
          {:db (-> db
                 (assoc-in (conj form-path' :loading?) false)
                 (assoc-in (conj form-path' :error) nil)
                 (assoc-in (conj form-path' :last-created) entity-id))
           :dispatch-n [[:admin/refresh-entity entity-key entity]
                        [:admin/navigate-client (str "/admin/" (name entity-key) "/" entity-id)]]})))

    ;; create-entry-failed event
    (rf/reg-event-fx
      (keyword event-ns "create-entry-failed")
      (fn [{:keys [db]} [_ error]]
        {:db (-> db
               (assoc-in (conj form-path' :loading?) false)
               (assoc-in (conj form-path' :error) (admin-http/extract-error-message error)))}))))

;; =============================================================================
;; Main Registration Function
;; =============================================================================

(defn register-entity-events!
  "Registers all standard CRUD events for an entity based on its configuration.
   
   Example:
   (register-entity-events!
     {:entity-key :suppliers
      :base-path [:admin :expenses :suppliers]
      :api-endpoint \"/admin/api/expenses/suppliers\"
      :has-forms? false})"
  [{:keys [entity-key has-forms?] :as config}]
  (validate-entity-config config)

  ;; Always register list, detail, and modal events
  (generate-list-events config)
  (generate-detail-events config)
  (generate-detail-modal-events config)

  ;; Conditionally register form events
  (when has-forms?
    (generate-form-events config))

  (log/info (str "Registered events for entity: " entity-key)))