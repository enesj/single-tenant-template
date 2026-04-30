(ns app.domain.frontend.expenses.events.duplicates
  "Re-frame events and subscriptions for the Dedup & Merge admin tool."
  (:require
    [app.admin.frontend.utils.http :as admin-http]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; ============================================================================
;; State path: [:admin/duplicates]
;; ============================================================================

(def ^:private state-path [:admin/duplicates])

(def ^:private all-strategies ["exact" "prefix" "trigram" "levenshtein"])

(defonce ^:private manual-search-timer (atom nil))

(defn- empty-manual-selection []
  {:primary-id nil
   :secondary-ids []
   :selected-items {}})

(defn- reset-manual-state
  [db]
  (-> db
    (assoc-in (conj state-path :manual-query) "")
    (assoc-in (conj state-path :manual-results) [])
    (assoc-in (conj state-path :manual-loading?) false)
    (assoc-in (conj state-path :manual-selection) (empty-manual-selection))))

(defn- normalize-id [id]
  (some-> id str))

(defn- normalize-entity
  [entity]
  (if-let [id (normalize-id (:id entity))]
    (assoc entity :id id)
    entity))

(defn- upsert-manual-item
  [db entity]
  (let [entity* (normalize-entity entity)
        id (:id entity*)]
    (if id
      (assoc-in db (conj state-path :manual-selection :selected-items id) entity*)
      db)))

;; ============================================================================
;; UI State Events
;; ============================================================================

(rf/reg-event-fx
  ::set-entity-type
  (fn [{:keys [db]} [_ entity-type]]
    {:db (-> db
           (assoc-in (conj state-path :entity-type) entity-type)
           (assoc-in (conj state-path :clusters-by-strategy) {})
           (assoc-in (conj state-path :loading-by-strategy) {})
           (assoc-in (conj state-path :error) nil)
           (assoc-in (conj state-path :selections) {})
           (reset-manual-state))
     :dispatch [::detect-all {:entity-type entity-type}]}))

(rf/reg-event-db
  ::set-mode
  (fn [db [_ mode]]
    (-> db
      (assoc-in (conj state-path :mode) mode)
      (assoc-in (conj state-path :error) nil)
      (assoc-in (conj state-path :show-merge-modal?) false)
      (assoc-in (conj state-path :merge-preview) nil)
      (assoc-in (conj state-path :pending-merge) nil))))

(rf/reg-event-db
  ::set-strategy
  (fn [db [_ strategy]]
    (-> db
      (assoc-in (conj state-path :strategy) strategy)
      (assoc-in (conj state-path :selections) {}))))

(rf/reg-event-db
  ::select-primary
  (fn [db [_ cluster-idx member-id]]
    (assoc-in db (conj state-path :selections cluster-idx :primary-id) member-id)))

(rf/reg-event-db
  ::toggle-secondary
  (fn [db [_ cluster-idx member-id]]
    (let [path    (conj state-path :selections cluster-idx :secondary-ids)
          current (set (get-in db path []))
          updated (if (contains? current member-id)
                    (disj current member-id)
                    (conj current member-id))]
      (assoc-in db path updated))))

(rf/reg-event-db
  ::close-merge-modal
  (fn [db _]
    (-> db
      (assoc-in (conj state-path :show-merge-modal?) false)
      (assoc-in (conj state-path :merge-preview) nil)
      (assoc-in (conj state-path :pending-merge) nil))))

;; ============================================================================
;; Detect All (fires all three strategies in parallel)
;; ============================================================================

(rf/reg-event-fx
  ::detect-all
  (fn [{:keys [db]} [_ {:keys [entity-type]}]]
    (let [et (or entity-type (get-in db (conj state-path :entity-type)) "suppliers")]
      {:db         (-> db
                     (assoc-in (conj state-path :loading-by-strategy)
                       (zipmap all-strategies (repeat true)))
                     (assoc-in (conj state-path :clusters-by-strategy) {})
                     (assoc-in (conj state-path :error) nil)
                     (assoc-in (conj state-path :selections) {}))
       :dispatch-n (mapv (fn [strategy]
                           [::detect-for-strategy {:entity-type et :strategy strategy}])
                     all-strategies)})))

(rf/reg-event-fx
  ::detect-for-strategy
  (fn [_ [_ {:keys [entity-type strategy]}]]
    {:http-xhrio (admin-http/admin-get
                   {:uri       "/admin/api/expenses/duplicates/detect"
                    :params    {:entity-type entity-type :strategy strategy}
                    :on-success [::detect-for-strategy-success strategy]
                    :on-failure [::detect-for-strategy-failure strategy]})}))

(rf/reg-event-db
  ::detect-for-strategy-success
  (fn [db [_ strategy response]]
    (-> db
      (assoc-in (conj state-path :loading-by-strategy strategy) false)
      (assoc-in (conj state-path :clusters-by-strategy strategy) (:clusters response)))))

(rf/reg-event-db
  ::detect-for-strategy-failure
  (fn [db [_ strategy error]]
    (log/error "Failed to detect duplicates" {:strategy strategy :error error})
    (-> db
      (assoc-in (conj state-path :loading-by-strategy strategy) false)
      (assoc-in (conj state-path :error) (admin-http/extract-error-message error)))))

;; ============================================================================
;; Merge Preview
;; ============================================================================

(rf/reg-event-fx
  ::merge-preview
  (fn [{:keys [db]} [_ {:keys [entity-type primary-id secondary-ids] :as merge-params}]]
    (let [normalized-secondary-ids (mapv str secondary-ids)
          pending-merge (-> merge-params
                          (assoc :primary-id (str primary-id))
                          (assoc :secondary-ids normalized-secondary-ids))]
      {:db (-> db
             (assoc-in (conj state-path :merging?) true)
             (assoc-in (conj state-path :pending-merge) pending-merge)
             (assoc-in (conj state-path :error) nil))
       :http-xhrio (admin-http/admin-post
                     {:uri "/admin/api/expenses/duplicates/merge-preview"
                      :params {:entity-type (name entity-type)
                               :primary-id (str primary-id)
                               :secondary-ids normalized-secondary-ids}
                      :on-success [::merge-preview-success]
                      :on-failure [::merge-preview-failure]})})))

(rf/reg-event-db
  ::merge-preview-success
  (fn [db [_ response]]
    (-> db
      (assoc-in (conj state-path :merging?) false)
      (assoc-in (conj state-path :merge-preview) (:preview response))
      (assoc-in (conj state-path :show-merge-modal?) true))))

(rf/reg-event-db
  ::merge-preview-failure
  (fn [db [_ error]]
    (log/error "Failed to get merge preview" {:error error})
    (-> db
      (assoc-in (conj state-path :merging?) false)
      (assoc-in (conj state-path :pending-merge) nil)
      (assoc-in (conj state-path :error) (admin-http/extract-error-message error)))))

;; ============================================================================
;; Execute Merge
;; ============================================================================

(rf/reg-event-fx
  ::execute-merge
  (fn [{:keys [db]} [_ {:keys [entity-type primary-id secondary-ids]}]]
    {:db         (assoc-in db (conj state-path :merging?) true)
     :http-xhrio (admin-http/admin-post
                   {:uri       "/admin/api/expenses/duplicates/merge"
                    :params    {:entity-type  (name entity-type)
                                :primary-id   (str primary-id)
                                :secondary-ids (mapv str secondary-ids)}
                    :on-success [::execute-merge-success]
                    :on-failure [::execute-merge-failure]})}))

(rf/reg-event-fx
  ::execute-merge-success
  (fn [{:keys [db]} [_ _response]]
    {:db (-> db
           (assoc-in (conj state-path :merging?) false)
           (assoc-in (conj state-path :show-merge-modal?) false)
           (assoc-in (conj state-path :merge-preview) nil)
           (assoc-in (conj state-path :pending-merge) nil)
           (reset-manual-state))
     :dispatch [::detect-all {}]}))

(rf/reg-event-db
  ::execute-merge-failure
  (fn [db [_ error]]
    (log/error "Failed to merge entities" {:error error})
    (-> db
      (assoc-in (conj state-path :merging?) false)
      (assoc-in (conj state-path :error) (admin-http/extract-error-message error)))))

;; ============================================================================
;; Hide / Unhide False-Positive Clusters
;; ============================================================================

(rf/reg-event-fx
  ::ignore-cluster
  (fn [{:keys [db]} [_ {:keys [entity-type cluster-id member-ids note]}]]
    {:db         (-> db
                   (assoc-in (conj state-path :flagging?) true)
                   (assoc-in (conj state-path :error) nil))
     :http-xhrio (admin-http/admin-post
                   {:uri       "/admin/api/expenses/duplicates/ignore"
                    :params    {:entity-type (if (keyword? entity-type) (name entity-type) entity-type)
                                :cluster-id  cluster-id
                                :member-ids  (mapv str member-ids)
                                :note        note}
                    :on-success [::ignore-cluster-success]
                    :on-failure [::ignore-cluster-failure]})}))

(rf/reg-event-fx
  ::ignore-cluster-success
  (fn [{:keys [db]} _]
    {:db       (assoc-in db (conj state-path :flagging?) false)
     :dispatch [::detect-all {}]}))

(rf/reg-event-db
  ::ignore-cluster-failure
  (fn [db [_ error]]
    (log/error "Failed to ignore duplicate cluster" {:error error})
    (-> db
      (assoc-in (conj state-path :flagging?) false)
      (assoc-in (conj state-path :error) (admin-http/extract-error-message error)))))

;; ============================================================================
;; Manual Search & Selection
;; ============================================================================

(rf/reg-event-fx
  ::set-manual-query
  (fn [{:keys [db]} [_ query]]
    (when @manual-search-timer
      (js/clearTimeout @manual-search-timer)
      (reset! manual-search-timer nil))
    (let [query* (or query "")]
      (if (>= (count query*) 2)
        (do
          (reset! manual-search-timer
            (js/setTimeout
              #(rf/dispatch [::fetch-manual-results query*])
              300))
          {:db (-> db
                 (assoc-in (conj state-path :manual-query) query*)
                 (assoc-in (conj state-path :manual-loading?) true)
                 (assoc-in (conj state-path :error) nil))})
        {:db (-> db
               (assoc-in (conj state-path :manual-query) query*)
               (assoc-in (conj state-path :manual-loading?) false)
               (assoc-in (conj state-path :manual-results) [])
               (assoc-in (conj state-path :error) nil))}))))

(rf/reg-event-fx
  ::fetch-manual-results
  (fn [{:keys [db]} [_ query]]
    (let [entity-type (get-in db (conj state-path :entity-type) "suppliers")]
      {:http-xhrio (admin-http/admin-get
                     {:uri "/admin/api/expenses/duplicates/manual-search"
                      :params {:entity-type entity-type
                               :q query
                               :limit 20}
                      :on-success [::fetch-manual-results-success query entity-type]
                      :on-failure [::fetch-manual-results-failure]})})))

(rf/reg-event-db
  ::fetch-manual-results-success
  (fn [db [_ query entity-type response]]
    (if (and (= query (get-in db (conj state-path :manual-query)))
          (= entity-type (get-in db (conj state-path :entity-type))))
      (-> db
        (assoc-in (conj state-path :manual-loading?) false)
        (assoc-in (conj state-path :manual-results) (mapv normalize-entity (or (:results response) []))))
      db)))

(rf/reg-event-db
  ::fetch-manual-results-failure
  (fn [db [_ error]]
    (log/error "Failed to search manual merge candidates" {:error error})
    (-> db
      (assoc-in (conj state-path :manual-loading?) false)
      (assoc-in (conj state-path :error) (admin-http/extract-error-message error)))))

(rf/reg-event-db
  ::manual-add-result
  (fn [db [_ entity]]
    (upsert-manual-item db entity)))

(rf/reg-event-db
  ::manual-remove-result
  (fn [db [_ entity-id]]
    (let [entity-id* (normalize-id entity-id)]
      (if entity-id*
        (-> db
          (update-in (conj state-path :manual-selection :selected-items) dissoc entity-id*)
          (update-in (conj state-path :manual-selection :secondary-ids)
            (fn [ids]
              (->> ids
                (remove #(= entity-id* %))
                vec)))
          ((fn [db*]
             (if (= entity-id* (get-in db* (conj state-path :manual-selection :primary-id)))
               (assoc-in db* (conj state-path :manual-selection :primary-id) nil)
               db*))))
        db))))

(rf/reg-event-db
  ::manual-select-primary
  (fn [db [_ entity]]
    (let [entity* (normalize-entity entity)
          entity-id (:id entity*)]
      (if entity-id
        (-> db
          (upsert-manual-item entity*)
          (assoc-in (conj state-path :manual-selection :primary-id) entity-id)
          (update-in (conj state-path :manual-selection :secondary-ids)
            (fn [ids]
              (->> ids
                (remove #(= entity-id %))
                vec))))
        db))))

(rf/reg-event-db
  ::manual-toggle-secondary
  (fn [db [_ entity]]
    (let [entity* (normalize-entity entity)
          entity-id (:id entity*)
          primary-id (get-in db (conj state-path :manual-selection :primary-id))
          current (set (get-in db (conj state-path :manual-selection :secondary-ids) []))]
      (cond
        (nil? entity-id) db
        (= entity-id primary-id) db
        :else
        (assoc-in
         (upsert-manual-item db entity*)
          (conj state-path :manual-selection :secondary-ids)
          (vec (if (contains? current entity-id)
                 (disj current entity-id)
                 (conj current entity-id))))))))

;; ============================================================================
;; Subscriptions
;; ============================================================================

(rf/reg-sub ::loading-for-strategy?
  (fn [db [_ strategy]]
    (get-in db (conj state-path :loading-by-strategy strategy) false)))

(rf/reg-sub ::error
  (fn [db _] (get-in db (conj state-path :error))))

(rf/reg-sub ::clusters
  (fn [db _]
    (let [strategy (get-in db (conj state-path :strategy) "prefix")]
      (get-in db (conj state-path :clusters-by-strategy strategy)))))

(rf/reg-sub ::cluster-count-for-strategy
  (fn [db [_ strategy]]
    (count (get-in db (conj state-path :clusters-by-strategy strategy) []))))

(rf/reg-sub ::entity-type
  (fn [db _] (get-in db (conj state-path :entity-type) "suppliers")))

(rf/reg-sub ::strategy
  (fn [db _] (get-in db (conj state-path :strategy) "prefix")))

(rf/reg-sub ::selections
  (fn [db _] (get-in db (conj state-path :selections) {})))

(rf/reg-sub ::show-merge-modal?
  (fn [db _] (get-in db (conj state-path :show-merge-modal?) false)))

(rf/reg-sub ::merge-preview
  (fn [db _] (get-in db (conj state-path :merge-preview))))

(rf/reg-sub ::merging?
  (fn [db _] (get-in db (conj state-path :merging?) false)))

(rf/reg-sub ::flagging?
  (fn [db _] (get-in db (conj state-path :flagging?) false)))

(rf/reg-sub ::pending-merge
  (fn [db _] (get-in db (conj state-path :pending-merge))))

(rf/reg-sub ::mode
  (fn [db _] (get-in db (conj state-path :mode) "automatic")))

(rf/reg-sub ::manual-query
  (fn [db _] (get-in db (conj state-path :manual-query) "")))

(rf/reg-sub ::manual-results
  (fn [db _] (get-in db (conj state-path :manual-results) [])))

(rf/reg-sub ::manual-loading?
  (fn [db _] (get-in db (conj state-path :manual-loading?) false)))

(rf/reg-sub ::manual-selection
  (fn [db _] (get-in db (conj state-path :manual-selection) (empty-manual-selection))))

(rf/reg-sub ::manual-selected-members
  (fn [db _]
    (->> (get-in db (conj state-path :manual-selection :selected-items) {})
      vals
      (sort-by (juxt #(or (:display-name %)
                        (:canonical-name %)
                        (:name %)
                        "")
                 :id))
      vec)))
