(ns app.domain.expenses.frontend.events.article-aliases
  (:require
    [ajax.core :as ajax]
    [app.admin.frontend.adapters.expenses :as expenses-adapter]
    [app.admin.frontend.utils.http :as admin-http]
    [app.template.frontend.db.paths :as paths]
    [day8.re-frame.http-fx]
    [re-frame.core :as rf]))

(def ^:private entity-key :article-aliases)
(def ^:private base-path [:admin :expenses :article-aliases])
(def ^:private default-per-page 50)

(defn- resolve-pagination
  [db {:keys [limit offset page per-page]}]
  (let [existing-per-page (or (get-in db (paths/list-per-page entity-key))
                            (get-in db (conj (paths/list-ui-state entity-key) :per-page))
                            (get-in db (conj (paths/list-ui-state entity-key) :pagination :per-page))
                            default-per-page)
        existing-page (or (get-in db (paths/list-current-page entity-key))
                        (get-in db (conj (paths/list-ui-state entity-key) :current-page))
                        (get-in db (conj (paths/list-ui-state entity-key) :pagination :current-page))
                        1)
        per-page (or limit per-page existing-per-page default-per-page)
        page (or page (when offset (inc (quot offset (max per-page 1)))) existing-page 1)
        offset (or offset (* (max 0 (dec page)) per-page))]
    {:limit per-page
     :offset offset
     :page page
     :per-page per-page}))

(defn- begin-load
  [db params]
  (let [{:keys [limit offset page per-page]} (resolve-pagination db params)]
    (-> db
      (assoc-in (paths/entity-loading? entity-key) true)
      (assoc-in (paths/entity-error entity-key) nil)
      (assoc-in [:admin :article-aliases :loading?] true)
      (assoc-in [:admin :article-aliases :error] nil)
      (assoc-in (paths/list-per-page entity-key) per-page)
      (assoc-in (paths/list-current-page entity-key) page)
      (assoc-in (conj (paths/list-ui-state entity-key) :pagination) {:current-page page :per-page per-page})
      (assoc-in (conj (paths/entity-metadata entity-key) :pagination) {:page page :per-page per-page})
      (assoc-in (conj base-path :loading?) true)
      (assoc-in (conj base-path :error) nil))))

(defn- finish-load
  [db error?]
  (let [error-val (when error? (admin-http/extract-error-message error?))]
    (-> db
      (assoc-in (paths/entity-loading? entity-key) false)
      (assoc-in (paths/entity-error entity-key) error-val)
      (assoc-in [:admin :article-aliases :loading?] false)
      (assoc-in [:admin :article-aliases :error] error-val)
      (assoc-in (conj base-path :loading?) false)
      (assoc-in (conj base-path :error) error-val))))

(rf/reg-event-fx
  ::load
  (fn [{:keys [db]} [_ {:keys [search] :as params}]]
    (let [{:keys [limit offset] :as pagination} (resolve-pagination db params)]
      {:db (begin-load db pagination)
       :http-xhrio (admin-http/admin-get
                     {:uri "/admin/api/expenses/article-aliases"
                      :params (cond-> {:limit limit :offset offset}
                                search (assoc :search search))
                      :response-format (ajax/json-response-format {:keywords? true})
                      :on-success [::loaded pagination]
                      :on-failure [::load-failed]})})))

(rf/reg-event-fx
  ::loaded
  (fn [{:keys [db]} [_ {:keys [limit offset]} {:keys [article-aliases]}]]
    (let [db* (-> db
                (finish-load nil)
                (assoc-in (conj base-path :items) (vec (or article-aliases []))))
          per-page (or limit default-per-page)
          page (inc (quot (or offset 0) (max per-page 1)))]
      {:db (-> db*
             (assoc-in (conj (paths/entity-metadata entity-key) :pagination) {:page page :per-page per-page})
             (assoc-in (conj (paths/list-ui-state entity-key) :pagination) {:current-page page :per-page per-page}))
       :dispatch-n [[::expenses-adapter/sync-article-aliases article-aliases]]})))

(rf/reg-event-fx
  ::load-failed
  (fn [{:keys [db]} [_ error]]
    {:db (finish-load db error)}))
