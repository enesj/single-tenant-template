(ns app.domain.frontend.expenses.events.unmapped-items
  "Admin UX for mapping many aliases to a single article."
  (:require
    [ajax.core :as ajax]
    [app.admin.frontend.utils.http :as admin-http]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.paths :as paths]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(def ^:private base-path [:admin :expenses :unmapped-items])
(def ^:private suppliers-endpoint "/admin/api/expenses/suppliers")
(def ^:private articles-endpoint "/admin/api/expenses/articles")
(def ^:private unmapped-aliases-endpoint "/admin/api/expenses/articles/unmapped-aliases")

(defn- begin-load [db]
  (-> db
    (assoc-in (conj base-path :loading?) true)
    (assoc-in (conj base-path :error) nil)))

(defn- finish-load [db error]
  (-> db
    (assoc-in (conj base-path :loading?) false)
    (assoc-in (conj base-path :error) (when error (http/extract-error-message error)))))

(defn- selected-item-ids [db]
  (or (get-in db (conj base-path :selection :item-ids)) #{}))

(defn- current-unmapped-page-params
  [db]
  (let [entity-key :unmapped-items
        per-page (paths/resolved-list-per-page db entity-key 50)
        current-page (paths/resolved-list-current-page db entity-key)
        supplier-id (get-in db (conj base-path :filters :supplier-id))]
    (cond-> {:limit per-page
             :offset (* (max 0 (dec current-page)) per-page)}
      supplier-id (assoc :supplier-id supplier-id))))

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
        err-msg (when error (http/extract-error-message error))]
    (cond-> (assoc-in db pending-path pending)
      err-msg (assoc-in (conj base-path :lookups :error) err-msg)
      (empty? pending) (assoc-in (conj base-path :lookups :loading?) false))))

(rf/reg-event-fx
  ::load-lookups
  (fn [{:keys [db]} _]
    {:db (begin-lookups-load db)
     :fx [[:http-xhrio
           (admin-http/admin-get
             {:uri suppliers-endpoint
              :params {:limit 200 :offset 0}
              :response-format (ajax/json-response-format {:keywords? true})
              :on-success [::lookups-suppliers-loaded]
              :on-failure [::lookups-suppliers-failed]})]
          [:http-xhrio
           (admin-http/admin-get
             {:uri articles-endpoint
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
;; Unmapped aliases
;; -----------------------------------------------------------------------------

(rf/reg-event-fx
  ::refresh-unmapped-items-list
  (fn [{:keys [db]} _]
    {:dispatch [::load-unmapped-items (current-unmapped-page-params db)]}))

(rf/reg-event-fx
  ::load-unmapped-items
  (fn [{:keys [db]} [_ {:keys [limit offset supplier-id]}]]
    (let [qp (cond-> {:limit (or limit 50)
                      :offset (or offset 0)}
               supplier-id (assoc :supplier_id supplier-id))]
      {:db (begin-load db)
       :http-xhrio (admin-http/admin-get
                     {:uri unmapped-aliases-endpoint
                      :params qp
                      :response-format (ajax/json-response-format {:keywords? true})
                      :on-success [::unmapped-items-loaded]
                      :on-failure [::unmapped-items-load-failed]})})))

(rf/reg-event-db
  ::unmapped-items-loaded
  (fn [db [_ response]]
    (let [items (vec (or (:unmapped-aliases response) (:data response) []))
          total (or (:total response) (count items))]
      (-> db
        (finish-load nil)
        (assoc-in (conj base-path :items) items)
        (assoc-in (paths/list-total-items :unmapped-items) total)
        (assoc-in (conj base-path :selection :item-ids) #{})))))

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
  (fn [{:keys [db]} [_ {:keys [mode existing-article-id new-article-name]}]]
    (let [alias-ids (vec (selected-item-ids db))]
      (cond
        (empty? alias-ids)
        {:db (set-map-error db "Select at least one unmapped alias.")}

        (and (= mode :existing) (not (seq existing-article-id)))
        {:db (set-map-error db "Select an article (or switch to Create new).")}

        (and (= mode :new) (str/blank? new-article-name))
        {:db (set-map-error db "Enter a canonical name for the new article.")}

        :else
        (let [db* (-> db
                    (set-map-working true)
                    (assoc-in (conj base-path :map-modal :error) nil)
                    (assoc-in (conj base-path :map-modal :result) nil)
                    (assoc-in (conj base-path :map-modal :progress) {:total (count alias-ids)
                                                                     :done 0
                                                                     :failed 0}))]
          (if (= mode :new)
            {:db db*
             :http-xhrio (admin-http/admin-post
                           {:uri articles-endpoint
                            :params {:canonical_name (str/trim new-article-name)}
                            :response-format (ajax/json-response-format {:keywords? true})
                            :on-success [::create-article-success {:alias-ids alias-ids}]
                            :on-failure [::create-article-failed]})}
            {:db db*
             :dispatch [::map-with-article {:article-id existing-article-id
                                            :alias-ids alias-ids}]}))))))

(rf/reg-event-fx
  ::create-article-success
  (fn [{:keys [db]} [_ ctx response]]
    (let [article-id (or (get-in response [:article :id])
                       (get-in response [:data :id]))]
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
      (set-map-error (http/extract-error-message error)))))

(rf/reg-event-fx
  ::map-with-article
  (fn [{:keys [db]} [_ {:keys [article-id alias-ids]}]]
    (let [article-id* article-id]
      (when-not (seq article-id*)
        (log/error "map-with-article called without article-id"))

      (cond
        (not (seq article-id*))
        {:db (-> db (set-map-working false) (set-map-error "Missing article id."))}
        :else
        {:db (assoc-in db (conj base-path :map-modal :alias-result) nil)
         :dispatch [::map-selected-aliases {:article-id article-id*
                                            :alias-ids alias-ids}]}))))

(rf/reg-event-fx
  ::map-selected-aliases
  (fn [{:keys [db]} [_ {:keys [article-id alias-ids]}]]
    (let [requests (mapv
                     (fn [alias-id]
                       [:http-xhrio
                        (admin-http/admin-post
                          {:uri (str articles-endpoint "/aliases/" alias-id "/map")
                           :params {:article_id article-id}
                           :response-format (ajax/json-response-format {:keywords? true})
                           :on-success [::map-item-success]
                           :on-failure [::map-item-failed]})])
                     alias-ids)]
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
        reload? (assoc :dispatch [::refresh-unmapped-items-list])))))

(rf/reg-event-fx
  ::map-item-failed
  (fn [{:keys [db]} [_ error]]
    (log/warn "Failed to map item" {:error (http/extract-error-message error)})
    (let [db* (-> db
                (bump-progress :done)
                (bump-progress :failed)
                (set-map-error (http/extract-error-message error)))
          db** (maybe-finish db*)
          reload? (false? (get-in db** (conj base-path :map-modal :working?)))]
      (cond-> {:db db**}
        reload? (assoc :dispatch [::refresh-unmapped-items-list])))))
