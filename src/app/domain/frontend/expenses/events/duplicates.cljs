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
           (assoc-in (conj state-path :selections) {}))
     :dispatch [::detect-all {:entity-type entity-type}]}))

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
  (fn [{:keys [db]} [_ {:keys [entity-type primary-id secondary-ids cluster-idx]}]]
    {:db         (assoc-in db (conj state-path :merging?) true)
     :http-xhrio (admin-http/admin-post
                   {:uri       "/admin/api/expenses/duplicates/merge-preview"
                    :params    {:entity-type  (name entity-type)
                                :primary-id   (str primary-id)
                                :secondary-ids (mapv str secondary-ids)}
                    :on-success [::merge-preview-success cluster-idx]
                    :on-failure [::merge-preview-failure]})}))

(rf/reg-event-db
  ::merge-preview-success
  (fn [db [_ cluster-idx response]]
    (-> db
      (assoc-in (conj state-path :merging?) false)
      (assoc-in (conj state-path :merge-preview) (:preview response))
      (assoc-in (conj state-path :show-merge-modal?) true)
      (assoc-in (conj state-path :pending-merge :cluster-idx) cluster-idx))))

(rf/reg-event-db
  ::merge-preview-failure
  (fn [db [_ error]]
    (log/error "Failed to get merge preview" {:error error})
    (-> db
      (assoc-in (conj state-path :merging?) false)
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
    {:db       (-> db
                 (assoc-in (conj state-path :merging?) false)
                 (assoc-in (conj state-path :show-merge-modal?) false)
                 (assoc-in (conj state-path :merge-preview) nil)
                 (assoc-in (conj state-path :pending-merge) nil))
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
;; Subscriptions
;; ============================================================================

(rf/reg-sub ::loading-by-strategy
  (fn [db _] (get-in db (conj state-path :loading-by-strategy) {})))

(rf/reg-sub ::loading-for-strategy?
  (fn [db [_ strategy]]
    (get-in db (conj state-path :loading-by-strategy strategy) false)))

(rf/reg-sub ::loading?
  :<- [::loading-by-strategy]
  (fn [loading-map _]
    (boolean (some true? (vals loading-map)))))

(rf/reg-sub ::error
  (fn [db _] (get-in db (conj state-path :error))))

(rf/reg-sub ::clusters-by-strategy
  (fn [db _] (get-in db (conj state-path :clusters-by-strategy) {})))

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
