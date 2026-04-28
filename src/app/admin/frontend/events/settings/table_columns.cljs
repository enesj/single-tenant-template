(ns app.admin.frontend.events.settings.table-columns
  (:require
    [app.admin.frontend.config.loader :as config-loader]
    [app.admin.frontend.events.settings.utils :as utils]
    [app.admin.frontend.utils.http :as admin-http]
    [clojure.string :as str]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

(def ^:private legacy-audit-default-visible-columns
  ["action" "entity-name" "admin-email" "admin-name"])

(def ^:private audit-available-columns
  ["created-at"
   "action"
   "actor-display-name"
   "entity-name"
   "context-summary"
   "target-type"
   "operation"
   "severity"
   "http-status"
   "error-message"
   "admin-ref"
   "admin-name"
   "user-agent"
   "id"
   "actor-type"
   "actor-id"
   "target-id"
   "metadata"
   "ip"
   "updated-at"])

(def ^:private audit-default-visible-columns
  ["created-at" "action" "actor-display-name" "entity-name" "context-summary"])

(def ^:private audit-filterable-columns
  ["action" "entity-name" "admin-ref" "admin-name" "user-agent"])

(def ^:private audit-sortable-columns
  ["action" "entity-name" "admin-ref" "admin-name" "user-agent"])

(def ^:private audit-column-config
  {:actor-display-name {:width "160px" :type "text"}
   :context-summary {:width "360px" :type "text"}
   :operation {:width "160px" :type "text"}
   :severity {:width "110px" :formatter "status-badge"}
   :http-status {:width "110px"}
   :error-message {:width "320px" :type "text"}
   :user-agent {:width "200px"}
   :admin-ref {:width "180px"}
   :entity-name {:width "200px" :computed-field true :type "text"}
   :action {:width "140px"}
   :admin-name {:width "140px"}})

(def ^:private audit-column-metadata
  {:actor-display-name {:label "Actor"}
   :entity-name {:label "Subject"}
   :context-summary {:label "Details"}
   :target-type {:label "Target type"}
   :operation {:label "Operation"}
   :severity {:label "Severity"}
   :http-status {:label "HTTP status"}
   :error-message {:label "Error message"}})

(def ^:private audit-computed-fields
  {:entity-name {:type "join"
                 :compute-fn "join-entity-name"
                 :dependencies ["entity-type" "entity-id"]}})

(defn- normalize-column-name
  [value]
  (some-> value name str/trim))

(defn- legacy-audit-config?
  [audit-config]
  (let [default-visible (->> (:default-visible-columns audit-config)
                          (keep normalize-column-name)
                          vec)
        available-columns (->> (:available-columns audit-config)
                            (keep normalize-column-name)
                            set)]
    (and (= legacy-audit-default-visible-columns default-visible)
      (contains? available-columns "admin-email")
      (not (contains? available-columns "actor-display-name"))
      (not (contains? available-columns "context-summary")))))

(defn- normalize-audit-config
  [audit-config]
  (let [audit-config* (or audit-config {})]
    (cond-> (-> audit-config*
              (update :computed-fields merge audit-computed-fields)
              (update :column-config merge audit-column-config)
              (update :column-metadata merge audit-column-metadata))
      (legacy-audit-config? audit-config*)
      (assoc :available-columns audit-available-columns
        :default-visible-columns audit-default-visible-columns
        :filterable-columns audit-filterable-columns
        :sortable-columns audit-sortable-columns
        :always-visible ["action"]))))

(def ^:private legacy-admin-email-column "email-masked")
(def ^:private canonical-admin-email-column "email")

(defn- normalize-admin-email-column-id
  [value]
  (let [value-name (normalize-column-name value)]
    (cond
      (nil? value-name) nil
      (= legacy-admin-email-column value-name)
      (if (keyword? value)
        :email
        canonical-admin-email-column)
      :else value)))

(defn- normalize-admin-email-column-list
  [columns]
  (->> (or columns [])
    (map normalize-admin-email-column-id)
    (remove nil?)
    distinct
    vec))

(defn- normalize-admin-email-column-map
  [m]
  (reduce-kv
    (fn [acc k v]
      (assoc acc (normalize-admin-email-column-id k) v))
    {}
    (or m {})))

(defn- normalize-admin-config
  [admin-config]
  (cond-> (or admin-config {})
    true (update :available-columns normalize-admin-email-column-list)
    true (update :default-visible-columns normalize-admin-email-column-list)
    true (update :filterable-columns normalize-admin-email-column-list)
    true (update :sortable-columns normalize-admin-email-column-list)
    true (update :always-visible normalize-admin-email-column-list)
    true (update :computed-fields normalize-admin-email-column-map)
    true (update :column-config normalize-admin-email-column-map)
    true (update :column-metadata normalize-admin-email-column-map)))

(def ^:private users-identity-default-visible-columns
  ["email" "full-name" "status" "last-login-at" "created-at" "updated-at" "email-verified" "auth-provider"])

(defn- legacy-user-identity-config?
  [user-config]
  (let [default-visible (->> (:default-visible-columns user-config)
                          (keep normalize-column-name)
                          set)]
    (or (contains? default-visible "email-masked")
      (and (contains? default-visible "user-ref")
        (not (contains? default-visible "email"))))))

(defn- normalize-user-identity-config
  [user-config]
  (let [user-config* (or user-config {})]
    (cond-> (-> user-config*
              (update :available-columns #(->> (cons canonical-admin-email-column (or % []))
                                            normalize-admin-email-column-list))
              (update :default-visible-columns normalize-admin-email-column-list)
              (update :filterable-columns normalize-admin-email-column-list)
              (update :sortable-columns normalize-admin-email-column-list)
              (update :always-visible normalize-admin-email-column-list)
              (update :computed-fields normalize-admin-email-column-map)
              (update :column-config #(merge {:email {:width "220px"}}
                                        (normalize-admin-email-column-map %)))
              (update :column-metadata normalize-admin-email-column-map))
      (legacy-user-identity-config? user-config*)
      (assoc :default-visible-columns users-identity-default-visible-columns
        :sortable-columns ["full-name" "status" "created-at"]
        :always-visible ["email"]))))

(defn normalize-table-columns
  [table-columns]
  (-> (or table-columns {})
    (update :admins normalize-admin-config)
    (update :users normalize-user-identity-config)
    (update :audit-logs normalize-audit-config)))

;; =============================================================================
;; Load Table Columns Config
;; =============================================================================

(rf/reg-event-fx
  :app.admin.frontend.events.settings/load-table-columns
  (fn [{:keys [db]} _]
    {:db (assoc-in db [:admin :settings :table-columns-loading?] true)
     :http-xhrio (admin-http/admin-get
                   {:uri "/admin/api/settings/table-columns"
                    :on-success [:app.admin.frontend.events.settings/load-table-columns-success]
                    :on-failure [:app.admin.frontend.events.settings/load-table-columns-failure]})}))

(rf/reg-event-fx
  :app.admin.frontend.events.settings/load-table-columns-success
  (fn [{:keys [db]} [_ response]]
    (let [table-columns (normalize-table-columns (:table-columns response))]
      (log/info "Loaded table columns from backend" {:count (count table-columns)})
      {:db (-> db
             (assoc-in [:admin :settings :table-columns-loading?] false)
             (assoc-in [:admin :settings :table-columns] table-columns)
             (assoc-in [:admin :config :table-columns] table-columns)
             (assoc-in [:admin :settings :error] nil))})))

(rf/reg-event-fx
  :app.admin.frontend.events.settings/load-table-columns-failure
  (fn [{:keys [db]} [_ error]]
    (log/error "Failed to load table columns" error)
    (utils/load-failure-effect db :table-columns-loading? "Failed to load table columns" error)))

;; =============================================================================
;; Update Table Columns Entity Config
;; =============================================================================

(rf/reg-event-fx
  :app.admin.frontend.events.settings/update-table-columns-entity
  (fn [{:keys [db]} [_ entity-name entity-config]]
    (let [entity-kw (if (keyword? entity-name) entity-name (keyword entity-name))
          entity-config* (if (= :audit-logs entity-kw)
                           (normalize-audit-config entity-config)
                           entity-config)]
      {:db (-> db
             (assoc-in [:admin :settings :saving?] true)
             ;; Optimistically update
             (assoc-in [:admin :settings :table-columns entity-kw] entity-config*)
             (assoc-in [:admin :config :table-columns entity-kw] entity-config*))
       :http-xhrio (admin-http/admin-patch
                     {:uri "/admin/api/settings/table-columns/entity"
                      :params {:entity-name (name entity-kw)
                               :entity-config entity-config*}
                      :on-success [:app.admin.frontend.events.settings/update-table-columns-success entity-kw entity-config*]
                      :on-failure [:app.admin.frontend.events.settings/update-table-columns-failure entity-kw]})})))

(rf/reg-event-fx
  :app.admin.frontend.events.settings/update-table-columns-success
  (fn [{:keys [db]} [_ entity-kw entity-config _response]]
    (let [entity-config* (if (= :audit-logs entity-kw)
                           (normalize-audit-config entity-config)
                           entity-config)]
      (log/info "Table columns updated successfully" {:entity entity-kw})
      ;; Update config-loader cache
      (config-loader/register-preloaded-config! :table-columns entity-kw entity-config*)
      {:db (-> db
             (assoc-in [:admin :settings :saving?] false)
             (assoc-in [:admin :settings :last-saved] (js/Date.now))
             (assoc-in [:admin :config :table-columns entity-kw] entity-config*)
             (assoc-in [:admin :settings :error] nil))})))

(rf/reg-event-fx
  :app.admin.frontend.events.settings/update-table-columns-failure
  (fn [{:keys [db]} [_ entity-kw error]]
    (log/error "Failed to update table columns" {:entity entity-kw :error error})
    (utils/update-failure-effect db error "Failed to save table columns"
      [:app.admin.frontend.events.settings/load-table-columns])))

