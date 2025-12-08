(ns app.admin.frontend.events.users.template.form-interceptors
  "Form submission interceptors for single-tenant admin users.
   
   This module intercepts form submissions for users in the admin context
   and routes them through the bridge CRUD system instead of the template's
   direct HTTP calls. This ensures consistent success handling and highlighting."
  (:require
    [app.shared.model-naming :as model-naming]
    [app.template.frontend.events.form]     ;; Ensure template form events are loaded first
    [clojure.string :as str]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

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
      ;; Debug logging for form submission
      (log/info "📝 Form submit interceptor:"
        {:entity-name entity-name
         :entity-k entity-k
         :editing editing
         :in-admin? in-admin?
         :values values
         :db-values db-values})
      (cond
        ;; Admin user edit - route through bridge system
        (and (= entity-k :users) editing in-admin?)
        {:db (assoc-in db [:forms :users :submitting?] true)
         :dispatch [:app.template.frontend.events.list.crud/update-entity :users (:id values) (dissoc db-values :id)]}

        ;; Admin user create - route through bridge system
        (and (= entity-k :users) (not editing) in-admin?)
        {:db (assoc-in db [:forms :users :submitting?] true)
         :dispatch [:app.template.frontend.events.list.crud/create-entity :users db-values]}

        ;; Admin suppliers edit - route through bridge system for expenses domain
        (and (= entity-k :suppliers) editing in-admin?)
        {:db (assoc-in db [:forms :suppliers :submitting?] true)
         :dispatch [:app.template.frontend.events.list.crud/update-entity :suppliers (:id values) (dissoc db-values :id)]}

        ;; Admin suppliers create - route through bridge system for expenses domain
        (and (= entity-k :suppliers) (not editing) in-admin?)
        (do
          (log/info "🛒 Creating supplier via bridge:" {:db-values db-values})
          {:db (assoc-in db [:forms :suppliers :submitting?] true)
           :dispatch [:app.template.frontend.events.list.crud/create-entity :suppliers db-values]})

        ;; Admin expenses edit - route through bridge system for expenses domain
        (and (= entity-k :expenses) editing in-admin?)
        {:db (assoc-in db [:forms :expenses :submitting?] true)
         :dispatch [:app.template.frontend.events.list.crud/update-entity :expenses (:id values) (dissoc db-values :id)]}

        ;; Admin expenses create - route through bridge system for expenses domain
        (and (= entity-k :expenses) (not editing) in-admin?)
        (do
          (log/info "💸 Creating expense via bridge:" {:db-values db-values})
          {:db (assoc-in db [:forms :expenses :submitting?] true)
           :dispatch [:app.template.frontend.events.list.crud/create-entity :expenses db-values]})

        ;; Admin receipts edit - route through bridge system for expenses domain
        (and (= entity-k :receipts) editing in-admin?)
        {:db (assoc-in db [:forms :receipts :submitting?] true)
         :dispatch [:app.template.frontend.events.list.crud/update-entity :receipts (:id values) (dissoc db-values :id)]}

        ;; Admin receipts create - route through bridge system for expenses domain
        (and (= entity-k :receipts) (not editing) in-admin?)
        (do
          (log/info "🧾 Creating receipt via bridge:" {:db-values db-values})
          {:db (assoc-in db [:forms :receipts :submitting?] true)
           :dispatch [:app.template.frontend.events.list.crud/create-entity :receipts db-values]})

        ;; Admin payers edit - route through bridge system for expenses domain
        (and (= entity-k :payers) editing in-admin?)
        {:db (assoc-in db [:forms :payers :submitting?] true)
         :dispatch [:app.template.frontend.events.list.crud/update-entity :payers (:id values) (dissoc db-values :id)]}

        ;; Admin payers create - route through bridge system for expenses domain
        (and (= entity-k :payers) (not editing) in-admin?)
        (do
          (log/info "💳 Creating payer via bridge:" {:db-values db-values})
          {:db (assoc-in db [:forms :payers :submitting?] true)
           :dispatch [:app.template.frontend.events.list.crud/create-entity :payers db-values]})

        ;; Admin articles edit - route through bridge system
        (and (= entity-k :articles) editing in-admin?)
        {:db (assoc-in db [:forms :articles :submitting?] true)
         :dispatch [:app.template.frontend.events.list.crud/update-entity :articles (:id values) (dissoc db-values :id)]}

        ;; Admin articles create - route through bridge system
        (and (= entity-k :articles) (not editing) in-admin?)
        {:db (assoc-in db [:forms :articles :submitting?] true)
         :dispatch [:app.template.frontend.events.list.crud/create-entity :articles db-values]}

        ;; Admin article aliases edit - route through bridge system
        (and (= entity-k :article-aliases) editing in-admin?)
        {:db (assoc-in db [:forms :article-aliases :submitting?] true)
         :dispatch [:app.template.frontend.events.list.crud/update-entity :article-aliases (:id values) (dissoc db-values :id)]}

        ;; Admin article aliases create - route through bridge system
        (and (= entity-k :article-aliases) (not editing) in-admin?)
        {:db (assoc-in db [:forms :article-aliases :submitting?] true)
         :dispatch [:app.template.frontend.events.list.crud/create-entity :article-aliases db-values]}

        ;; Admin price observations edit - route through bridge system
        (and (= entity-k :price-observations) editing in-admin?)
        {:db (assoc-in db [:forms :price-observations :submitting?] true)
         :dispatch [:app.template.frontend.events.list.crud/update-entity :price-observations (:id values) (dissoc db-values :id)]}

        ;; Admin price observations create - route through bridge system
        (and (= entity-k :price-observations) (not editing) in-admin?)
        {:db (assoc-in db [:forms :price-observations :submitting?] true)
         :dispatch [:app.template.frontend.events.list.crud/create-entity :price-observations db-values]}

        ;; Fallback to template default
        :else
        {:dispatch [:app.template.frontend.events.form/process-default-submission form-data]}))))

;; Note: The separate :admin.template.form/submit-user-edit and :admin.template.form/submit-user-create
;; events are no longer needed since we now route through the bridge system.
;; The bridge system handles admin-specific HTTP endpoints via the registered adapter.
