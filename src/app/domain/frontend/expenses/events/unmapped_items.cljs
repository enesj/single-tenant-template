(ns app.domain.frontend.expenses.events.unmapped-items
  "Admin UX for mapping many raw labels to a single article.

  NOTE: This workflow is used in both admin routes (/admin/...) and the
  user-facing power-user page (/unmapped-items). Requests switch between
  admin and user endpoints based on runtime context."
  (:require
    [ajax.core :as ajax]
    [app.admin.frontend.utils.http :as admin-http]
    [app.domain.frontend.expenses.events.user-expenses.endpoints :as endpoints]
    [app.domain.frontend.expenses.events.user-expenses.xhrio :as x]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(def ^:private base-path [:admin :expenses :unmapped-items])

(defn- begin-load [db]
  (-> db
    (assoc-in (conj base-path :loading?) true)
    (assoc-in (conj base-path :error) nil)))

(defn- finish-load [db error]
  (-> db
    (assoc-in (conj base-path :loading?) false)
    (assoc-in (conj base-path :error) (when error (admin-http/extract-error-message error)))))

(defn- selected-item-ids [db]
  (or (get-in db (conj base-path :selection :item-ids)) #{}))

(defn- selected-items
  "Return the full item maps for the currently selected item IDs."
  [db]
  (let [ids (selected-item-ids db)
        items (or (get-in db (conj base-path :items)) [])]
    (filterv (fn [it] (contains? ids (:id it))) items)))

(defn- selected-supplier-id
  "Return the single supplier-id for selected items, or nil when none/mixed."
  [db]
  (let [sids (->> (selected-items db)
               (map :supplier-id)
               (remove nil?)
               set)]
    (when (= 1 (count sids))
      (first sids))))

(defn- selected-raw-labels
  [db]
  (->> (selected-items db)
    (map :raw-label)
    (remove str/blank?)
    vec))

;; -----------------------------------------------------------------------------
;; Lookups (suppliers + articles) for the map modal
;; -----------------------------------------------------------------------------

(defn- begin-lookups-load
  [db]
  (-> db
    (assoc-in (conj base-path :lookups :loading?) true)
    (assoc-in (conj base-path :lookups :error) nil)
    (assoc-in (conj base-path :lookups :pending) #{:suppliers :articles})))

(defn- finish-lookups-step
  [db k error]
  (let [pending-path (conj base-path :lookups :pending)
        pending (disj (or (get-in db pending-path) #{}) k)
        err-msg (when error (admin-http/extract-error-message error))]
    (cond-> (assoc-in db pending-path pending)
      err-msg (assoc-in (conj base-path :lookups :error) err-msg)
      (empty? pending) (assoc-in (conj base-path :lookups :loading?) false))))

(rf/reg-event-fx
  ::load-lookups
  (fn [{:keys [db]} _]
    {:db (begin-lookups-load db)
     :fx [[:http-xhrio
           (x/xhrio db
             {:method :get
              :uri endpoints/suppliers-endpoint
              :admin-uri endpoints/admin-suppliers-endpoint
              :params {:limit 200 :offset 0}
              :response-format (ajax/json-response-format {:keywords? true})
              :on-success [::lookups-suppliers-loaded]
              :on-failure [::lookups-suppliers-failed]})]
          [:http-xhrio
           (x/xhrio db
             {:method :get
              :uri endpoints/articles-endpoint
              :admin-uri endpoints/admin-articles-endpoint
              :params {:limit 200 :offset 0}
              :response-format (ajax/json-response-format {:keywords? true})
              :on-success [::lookups-articles-loaded]
              :on-failure [::lookups-articles-failed]})]]}))

(rf/reg-event-db
  ::lookups-suppliers-loaded
  (fn [db [_ response]]
    (let [items (vec (or (:suppliers response) (:data response) []))]
      (-> db
        (assoc-in (conj base-path :lookups :suppliers) items)
        (finish-lookups-step :suppliers nil)))))

(rf/reg-event-db
  ::lookups-suppliers-failed
  (fn [db [_ error]]
    (finish-lookups-step db :suppliers error)))

(rf/reg-event-db
  ::lookups-articles-loaded
  (fn [db [_ response]]
    (let [items (vec (or (:articles response) (:data response) []))]
      (-> db
        (assoc-in (conj base-path :lookups :articles) items)
        (finish-lookups-step :articles nil)))))

(rf/reg-event-db
  ::lookups-articles-failed
  (fn [db [_ error]]
    (finish-lookups-step db :articles error)))

;; -----------------------------------------------------------------------------
;; Unmapped items
;; -----------------------------------------------------------------------------

(rf/reg-event-fx
  ::load-unmapped-items
  (fn [{:keys [db]} [_ {:keys [limit offset supplier-id]}]]
    (let [qp (cond-> {:limit (or limit 50)
                      :offset (or offset 0)}
               supplier-id (assoc :supplier_id supplier-id))]
      {:db (begin-load db)
       :http-xhrio (x/xhrio db
                    {:method :get
                     :uri endpoints/articles-unmapped-items-endpoint
                     :admin-uri endpoints/admin-articles-unmapped-items-endpoint
                     :params qp
                     :response-format (ajax/json-response-format {:keywords? true})
                     :on-success [::unmapped-items-loaded]
                     :on-failure [::unmapped-items-load-failed]})})))

(rf/reg-event-db
  ::unmapped-items-loaded
  (fn [db [_ response]]
    (-> db
      (finish-load nil)
      (assoc-in (conj base-path :items) (vec (or (:unmapped-items response) [])))
      (assoc-in (conj base-path :selection :item-ids) #{}))))

(rf/reg-event-db
  ::unmapped-items-load-failed
  (fn [db [_ error]]
    (finish-load db error)))

;; -----------------------------------------------------------------------------
;; Selection
;; -----------------------------------------------------------------------------

(rf/reg-event-db
  ::set-supplier-filter
  (fn [db [_ supplier-id]]
    (-> db
      (assoc-in (conj base-path :filters :supplier-id) supplier-id)
      (assoc-in (conj base-path :selection :item-ids) #{}))))

(rf/reg-event-db
  ::toggle-select-item
  (fn [db [_ item-id]]
    (let [path (conj base-path :selection :item-ids)
          ids (or (get-in db path) #{})
          ids' (if (contains? ids item-id)
                 (disj ids item-id)
                 (conj ids item-id))]
      (assoc-in db path ids'))))

(rf/reg-event-db
  ::clear-selection
  (fn [db _]
    (assoc-in db (conj base-path :selection :item-ids) #{})))

;; -----------------------------------------------------------------------------
;; Map modal
;; -----------------------------------------------------------------------------

(rf/reg-event-db
  ::open-map-modal
  (fn [db _]
    (-> db
      (assoc-in (conj base-path :map-modal :open?) true)
      (assoc-in (conj base-path :map-modal :error) nil)
      (assoc-in (conj base-path :map-modal :result) nil)
      (assoc-in (conj base-path :map-modal :working?) false))))

(rf/reg-event-db
  ::close-map-modal
  (fn [db _]
    (-> db
      (assoc-in (conj base-path :map-modal :open?) false)
      (assoc-in (conj base-path :map-modal :error) nil)
      (assoc-in (conj base-path :map-modal :working?) false))))

(defn- set-map-error [db msg]
  (assoc-in db (conj base-path :map-modal :error) msg))

(defn- set-map-working [db working?]
  (assoc-in db (conj base-path :map-modal :working?) (boolean working?)))

(rf/reg-event-fx
  ::submit-map
  (fn [{:keys [db]} [_ {:keys [mode existing-article-id new-article-name
                               create-aliases? allow-reassign?]}]]
    (let [supplier-id (selected-supplier-id db)
          item-ids (vec (selected-item-ids db))
          raw-labels (selected-raw-labels db)]
      (cond
        (empty? item-ids)
        {:db (set-map-error db "Select at least one unmapped item.")}

        (nil? supplier-id)
        {:db (set-map-error db "Please select items from a single supplier.")}

        (and (= mode :existing) (not (seq existing-article-id)))
        {:db (set-map-error db "Select an article (or switch to Create new).")}

        (and (= mode :new) (str/blank? new-article-name))
        {:db (set-map-error db "Enter a canonical name for the new article.")}

        :else
        (let [db* (-> db
                    (set-map-working true)
                    (assoc-in (conj base-path :map-modal :error) nil)
                    (assoc-in (conj base-path :map-modal :result) nil)
                    (assoc-in (conj base-path :map-modal :progress) {:total (count item-ids)
                                                                     :done 0
                                                                     :failed 0}))]
          (if (= mode :new)
            {:db db*
             :http-xhrio (x/xhrio db*
                          {:method :post
                           :uri endpoints/articles-endpoint
                           :admin-uri endpoints/admin-articles-endpoint
                           :params {:canonical_name (str/trim new-article-name)}
                           :response-format (ajax/json-response-format {:keywords? true})
                           :on-success [::create-article-success {:supplier-id supplier-id
                                                                  :item-ids item-ids
                                                                  :raw-labels raw-labels
                                                                  :create-aliases? create-aliases?
                                                                  :allow-reassign? allow-reassign?}]
                           :on-failure [::create-article-failed]})}
            {:db db*
             :dispatch [::map-with-article {:supplier-id supplier-id
                                            :article-id existing-article-id
                                            :item-ids item-ids
                                            :raw-labels raw-labels
                                            :create-aliases? create-aliases?
                                            :allow-reassign? allow-reassign?}]}))))))

(rf/reg-event-fx
  ::create-article-success
  (fn [{:keys [db]} [_ ctx response]]
    (let [article-id (get-in response [:article :id])]
      (if (seq article-id)
        {:db db
         :dispatch [::map-with-article (assoc ctx :article-id article-id)]}
        {:db (-> db
               (set-map-working false)
               (set-map-error "Failed to create article (missing id in response)."))}))))

(rf/reg-event-db
  ::create-article-failed
  (fn [db [_ error]]
    (-> db
      (set-map-working false)
      (set-map-error (admin-http/extract-error-message error)))))

(rf/reg-event-fx
  ::map-with-article
  (fn [{:keys [db]} [_ {:keys [supplier-id article-id item-ids raw-labels create-aliases? allow-reassign?]}]]
    (let [article-id* article-id]
      (when-not (seq article-id*)
        (log/error "map-with-article called without article-id" {:supplier-id supplier-id}))

      (cond
        (not (seq article-id*))
        {:db (-> db (set-map-working false) (set-map-error "Missing article id."))}

        ;; If aliases are requested, do the alias batch request first.
        (true? create-aliases?)
        {:db db
         :http-xhrio (x/xhrio db
                      {:method :post
                       :uri (str endpoints/articles-endpoint "/" article-id* "/aliases")
                       :admin-uri (str endpoints/admin-articles-endpoint "/" article-id* "/aliases")
                       :params {:supplier_id supplier-id
                                :raw_labels raw-labels
                                :allow_reassign? (boolean allow-reassign?)}
                       :response-format (ajax/json-response-format {:keywords? true})
                       :on-success [::aliases-batch-success {:article-id article-id*
                                                             :item-ids item-ids}]
                       :on-failure [::aliases-batch-failed {:article-id article-id*
                                                            :item-ids item-ids}]})}

        :else
        {:db (assoc-in db (conj base-path :map-modal :alias-result) nil)
         :dispatch [::map-selected-items {:article-id article-id*
                                          :item-ids item-ids}]}))))

(rf/reg-event-fx
  ::aliases-batch-success
  (fn [{:keys [db]} [_ {:keys [article-id item-ids]} response]]
    {:db (assoc-in db (conj base-path :map-modal :alias-result) (get-in response [:result]))
     :dispatch [::map-selected-items {:article-id article-id
                                      :item-ids item-ids}]}))

(rf/reg-event-fx
  ::aliases-batch-failed
  (fn [{:keys [db]} [_ {:keys [article-id item-ids]} error]]
    {:db (-> db
           (assoc-in (conj base-path :map-modal :alias-result) nil)
           (set-map-error (admin-http/extract-error-message error)))
     :dispatch [::map-selected-items {:article-id article-id
                                      :item-ids item-ids}]}))

(rf/reg-event-fx
  ::map-selected-items
  (fn [{:keys [db]} [_ {:keys [article-id item-ids]}]]
    (let [requests (mapv
                     (fn [item-id]
                       [:http-xhrio
                        (x/xhrio db
                          {:method :post
                           :uri (str endpoints/articles-endpoint "/items/" item-id "/map")
                           :admin-uri (str endpoints/admin-articles-endpoint "/items/" item-id "/map")
                           :params {:article_id article-id
                                    :create_alias? false}
                           :response-format (ajax/json-response-format {:keywords? true})
                           :on-success [::map-item-success]
                           :on-failure [::map-item-failed]})])
                     item-ids)]
      {:db db
       :fx (into [] requests)})))

(defn- bump-progress
  [db k]
  (update-in db (conj base-path :map-modal :progress k) (fnil inc 0)))

(defn- maybe-finish
  [db]
  (let [{:keys [total done]} (get-in db (conj base-path :map-modal :progress))]
    (if (and (number? total) (number? done) (>= done total))
      (-> db
        (set-map-working false)
        (assoc-in (conj base-path :map-modal :result) {:done done
                                                       :total total})
        (assoc-in (conj base-path :map-modal :open?) false))
      db)))

(rf/reg-event-fx
  ::map-item-success
  (fn [{:keys [db]} [_ _response]]
    (let [db* (-> db
                (bump-progress :done)
                (assoc-in (conj base-path :selection :item-ids) #{}))
          db** (maybe-finish db*)
          reload? (false? (get-in db** (conj base-path :map-modal :working?)))]
      (cond-> {:db db**}
        reload? (assoc :dispatch [::load-unmapped-items {:limit 50 :offset 0
                                                         :supplier-id (get-in db** (conj base-path :filters :supplier-id))}])))))

(rf/reg-event-fx
  ::map-item-failed
  (fn [{:keys [db]} [_ error]]
    (log/warn "Failed to map item" {:error (admin-http/extract-error-message error)})
    (let [db* (-> db
                (bump-progress :done)
                (bump-progress :failed)
                (set-map-error (admin-http/extract-error-message error)))
          db** (maybe-finish db*)
          reload? (false? (get-in db** (conj base-path :map-modal :working?)))]
      (cond-> {:db db**}
        reload? (assoc :dispatch [::load-unmapped-items {:limit 50 :offset 0
                                                         :supplier-id (get-in db** (conj base-path :filters :supplier-id))}])))))
