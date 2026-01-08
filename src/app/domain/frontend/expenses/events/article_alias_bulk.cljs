(ns app.domain.frontend.expenses.events.article-alias-bulk
  "Bulk alias creation for a single article (admin UX)."
  (:require
    [ajax.core :as ajax]
    [app.admin.frontend.utils.http :as admin-http]
    [app.domain.frontend.expenses.events.article-aliases :as aliases-events]
    [app.domain.frontend.expenses.events.suppliers :as suppliers-events]
    [re-frame.core :as rf]))

(def ^:private base-path [:admin :expenses :articles :add-aliases-modal])

(defn- set-error [db msg]
  (assoc-in db (conj base-path :error) msg))

(defn- set-working [db working?]
  (assoc-in db (conj base-path :working?) (boolean working?)))

(rf/reg-event-db
  ::close
  (fn [db _]
    (-> db
      (assoc-in (conj base-path :open?) false)
      (assoc-in (conj base-path :error) nil)
      (assoc-in (conj base-path :working?) false))))

(rf/reg-event-fx
  ::open
  (fn [{:keys [db]} [_ article-id]]
    {:db (-> db
           (assoc-in (conj base-path :open?) true)
           (assoc-in (conj base-path :article-id) article-id)
           (assoc-in (conj base-path :error) nil)
           (assoc-in (conj base-path :result) nil)
           (assoc-in (conj base-path :working?) false))
     :dispatch [::suppliers-events/load-list {:limit 200 :offset 0}]}))

(rf/reg-event-fx
  ::submit
  (fn [{:keys [db]} [_ {:keys [article-id supplier-id raw-labels allow-reassign?] :as payload}]]
    (cond
      (not (seq article-id))
      {:db (set-error db "Missing article id.")}

      (not (seq supplier-id))
      {:db (set-error db "Select a supplier.")}

      :else
      {:db (-> db (set-working true) (set-error nil))
       :http-xhrio (admin-http/admin-post
                     {:uri (str "/admin/api/expenses/articles/" article-id "/aliases")
                      :params {:supplier_id supplier-id
                               :raw_labels raw-labels
                               :allow_reassign? (boolean allow-reassign?)}
                      :response-format (ajax/json-response-format {:keywords? true})
                      :on-success [::submit-success payload]
                      :on-failure [::submit-failed]})})))

(rf/reg-event-fx
  ::submit-success
  (fn [{:keys [db]} [_ {:keys [article-id]} response]]
    {:db (-> db
           (set-working false)
           (assoc-in (conj base-path :result) (get-in response [:result]))
           (set-error nil))
     ;; refresh related list on article detail
     :dispatch [::aliases-events/load-list {:article_id article-id :limit 10 :offset 0}]}))

(rf/reg-event-db
  ::submit-failed
  (fn [db [_ error]]
    (-> db
      (set-working false)
      (set-error (admin-http/extract-error-message error)))))
