(ns app.admin.frontend.events.users.template.form-interceptors
  "Form submission interceptors for single-tenant admin users.
   
   This module intercepts form submissions for users in the admin context
   and routes them through the bridge CRUD system instead of the template's
   direct HTTP calls. This ensures consistent success handling and highlighting."
  (:require
    [app.shared.model-naming :as model-naming]
    [clojure.string :as str]
    [re-frame.core :as rf]))

(defn- convert-keys-to-db
  "Convert all keys in a map from kebab-case to snake_case for database/API compatibility"
  [m]
  (into {} (map (fn [[k v]] [(model-naming/app-keyword->db k) v]) m)))

(rf/reg-event-fx
  :app.template.frontend.events.form/submit-form
  (fn [{:keys [db]} [_ {:keys [entity-name editing values] :as form-data}]]
    (let [entity-k (keyword entity-name)
          in-admin? (str/includes? (.-pathname js/window.location) "/admin")
          ;; Convert keys to snake_case for API
          db-values (convert-keys-to-db values)]
      (cond
        ;; Admin user edit - route through bridge system
        (and (= entity-k :users) editing in-admin?)
        {:db (assoc-in db [:forms :users :submitting?] true)
         :dispatch [:app.template.frontend.events.list.crud/update-entity :users (:id values) (dissoc db-values :id)]}

        ;; Admin user create - route through bridge system
        (and (= entity-k :users) (not editing) in-admin?)
        {:db (assoc-in db [:forms :users :submitting?] true)
         :dispatch [:app.template.frontend.events.list.crud/create-entity :users db-values]}

        ;; Fallback to template default
        :else
        {:dispatch [:app.template.frontend.events.form/process-default-submission form-data]}))))

;; Note: The separate :admin.template.form/submit-user-edit and :admin.template.form/submit-user-create
;; events are no longer needed since we now route through the bridge system.
;; The bridge system handles admin-specific HTTP endpoints via the registered adapter.
