(ns app.admin.frontend.events.users.template.form-interceptors
  "Form submission interceptors for single-tenant admin users."
  (:require
    [app.shared.crud.factory :as crud]
    [app.shared.model-naming :as model-naming]
    [clojure.string :as str]
    [re-frame.core :as rf]))

(defn- convert-keys-to-db
  "Convert all keys in a map from kebab-case to snake_case for database/API compatibility"
  [m]
  (into {} (map (fn [[k v]] [(model-naming/app-keyword->db k) v]) m)))

(rf/reg-event-fx
  :app.template.frontend.events.form/submit-form
  (fn [_ [_ {:keys [entity-name editing values] :as form-data}]]
    (let [entity-k (keyword entity-name)
          in-admin? (str/includes? (.-pathname js/window.location) "/admin")
          ;; Convert keys to snake_case for API
          db-values (convert-keys-to-db values)]
      (cond
        ;; Admin user edit
        (and (= entity-k :users) editing in-admin?)
        {:dispatch [:admin.template.form/submit-user-edit (:id values) (dissoc db-values :id)]}

        ;; Admin user create
        (and (= entity-k :users) (not editing) in-admin?)
        {:dispatch [:admin.template.form/submit-user-create db-values]}

        ;; Fallback to template default
        :else
        {:dispatch [:app.template.frontend.events.form/submit-form-default form-data]}))))

(rf/reg-event-fx
  :admin.template.form/submit-user-create
  (crud/create-crud-handler
    :users :admin
    [:app.admin.frontend.forms/admin-create-success :users]
    [:app.template.frontend.events.form/create-failure :users]))

(rf/reg-event-fx
  :admin.template.form/submit-user-edit
  (crud/update-crud-handler
    :users :admin
    [:app.admin.frontend.forms/admin-update-success :users]
    [:app.template.frontend.events.form/update-failure :users]))

(rf/reg-event-fx
  :app.template.frontend.events.form/submit-form-default
  (fn [_ [_ form-data]]
    {:dispatch [:app.template.frontend.events.form/process-default-submission form-data]}))
