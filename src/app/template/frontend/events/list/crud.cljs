(ns app.template.frontend.events.list.crud
  "CRUD operations for list entities - fetching, deletion, and error handling"
  (:require
    [app.shared.frontend.bridges.crud :as crud-bridges]
    [app.template.frontend.api :as api]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.db.db :refer [common-interceptors]]
    [app.template.frontend.db.paths :as paths]
    [app.template.frontend.state.normalize :as normalize]
    [clojure.string :as str]
    [day8.re-frame.http-fx]
    [re-frame.core :as rf]))

;;; -------------------------
;;; Entity Fetching
;;; -------------------------

(crud-bridges/register-template-crud-events!)

(rf/reg-event-fx
  ::fetch-entities
  common-interceptors
  (fn [{:keys [db]} [entity-type]]

    (when entity-type
      ;; In admin UI, route sensitive entities through admin API
      (let [in-admin? (str/includes? (.-pathname js/window.location) "/admin")
            admin-managed? (contains? #{:users :tenants} entity-type)]
        (if (and in-admin? admin-managed?)
          {:dispatch [:admin/fetch-entities entity-type]}
          ;; Handle both string/keyword entity types and map values with :value/:label format
          (let [entity-name (cond
                              (map? entity-type) (:value entity-type)
                              (string? entity-type) entity-type
                              (keyword? entity-type) (name entity-type)
                              :else (str entity-type))
                uri (api/entity-endpoint entity-name)]
            ;; Note: We're intentionally NOT clearing recently-updated or recently-created IDs here
            ;; so they persist until explicit navigation away from the page or explicit clearing
            {:db (assoc-in db (paths/entity-loading? entity-type) true)
             :http-xhrio (http/api-request
                           {:method :get
                            :uri uri
                            :on-success [::fetch-success entity-type]
                            :on-failure [::fetch-failure entity-type]})}))))))

(rf/reg-event-fx
  ::fetch-success
  common-interceptors
  (fn [{:keys [db]} [entity-type response]]
    ;; Debug logging for transaction_types API response
    ;; Debug logging for transaction_types API response
    (let [normalized (normalize/normalize-entities response)]
      {:db (-> db
             (assoc-in (paths/entity-data entity-type) (:data normalized))
             (assoc-in (paths/entity-ids entity-type) (:ids normalized))
             (assoc-in (paths/entity-metadata entity-type)
               {:loading? false
                :error nil
                :last-updated (js/Date.now)})
             (assoc-in (paths/list-total-items entity-type) (count response)))})))

(rf/reg-event-db
  ::fetch-failure
  common-interceptors
  (fn [db [entity-type response]]
    (assoc-in db (paths/entity-metadata entity-type)
              {:loading? false
               :error (or (get-in response [:response :error])
                        (str "Failed to fetch " entity-type))
               :last-updated nil})))

;;; -------------------------
;;; Entity Deletion
;;; -------------------------

(rf/reg-event-fx
  ::delete-entity
  common-interceptors
  (fn [cofx [entity-type id]]
    ;; Guard against malformed dispatches like
    ;; [:app.template.frontend.events.list.crud/delete-entity nil nil]
    ;; which would otherwise throw when calling `name` on nil.
    ;; NOTE: trim-v interceptor already removes event name, so destructure directly
    (if (and entity-type id)
      (crud-bridges/run-bridge-operation
        :delete :request crud-bridges/default-delete-request cofx entity-type [id])
      (do
        (when js/console
          (js/console.warn
            "delete-entity called without entity-type or id"
            (clj->js {:entity-type entity-type :id id})))
        {:db (:db cofx)}))))

(rf/reg-event-fx
  ::delete-success
  common-interceptors
  (fn [cofx [entity-type id]]
    ;; NOTE: trim-v interceptor already removes event name, so destructure directly
    (crud-bridges/run-bridge-operation
      :delete :on-success crud-bridges/default-crud-success cofx entity-type [id])))

(rf/reg-event-fx
  ::delete-failure
  common-interceptors
  (fn [cofx [entity-type error]]
    ;; NOTE: trim-v interceptor already removes event name, so destructure directly
    (crud-bridges/run-bridge-operation
      :delete :on-failure crud-bridges/default-delete-failure cofx entity-type [error])))

;;; -------------------------
;;; Entity Update
;;; -------------------------

;; NOTE: ::update-success and ::update-failure are registered by the bridge system
;; in app.shared.frontend.bridges.crud/register-template-crud-events!
;; The bridge system handles recently-updated tracking and allows adapter overrides.

;;; -------------------------
;;; Entity Creation
;;; -------------------------

(rf/reg-event-fx
  ::create-entity
  common-interceptors
  (fn [cofx [entity-type form-data]]
    ;; NOTE: trim-v interceptor already removes event name, so destructure directly
    (crud-bridges/run-bridge-operation
      :create :request crud-bridges/default-create-request cofx entity-type [form-data])))

(rf/reg-event-fx
  ::create-success
  common-interceptors
  (fn [cofx [entity-type response]]
    ;; NOTE: trim-v interceptor already removes event name, so destructure directly
    (crud-bridges/run-bridge-operation
      :create :on-success crud-bridges/default-crud-success cofx entity-type [response])))

(rf/reg-event-fx
  ::create-failure
  common-interceptors
  (fn [cofx [entity-type error]]
    ;; NOTE: trim-v interceptor already removes event name, so destructure directly
    (crud-bridges/run-bridge-operation
      :create :on-failure crud-bridges/default-create-failure cofx entity-type [error])))

;;; -------------------------
;;; Error Handling
;;; -------------------------

(rf/reg-event-db
  ::clear-error
  common-interceptors
  (fn [db [entity-type]]
    (assoc-in db (paths/entity-error entity-type) nil)))
