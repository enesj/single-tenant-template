(ns app.template.frontend.components.layout
  (:require
    [app.template.frontend.components.auth :refer [auth-component]]
    [app.template.frontend.components.icons :refer [admins-icon
                                                    arrow-up
                                                    article-aliases-icon
                                                    articles-icon
                                                    chart-bar
                                                    dashboard-icon
                                                    expense-items-icon
                                                    expenses-icon
                                                    logout-icon

                                                    payers-icon
                                                    receipts-icon
                                                    suppliers-icon
                                                    unmapped-items-icon
                                                    users-icon]]
    [app.template.frontend.components.settings.global-settings :refer [settings-panel]]
    [app.template.frontend.components.sidebar :refer [sidebar]]
    [app.template.frontend.events.i18n :as i18n]
    [app.template.frontend.events.tenant :as tenant]
    [app.template.frontend.i18n :refer [use-t]]
    [re-frame.core :as rf]
    [reitit.frontend.easy :as rtfe]
    [uix.core :refer [$ defui use-state]]
    [uix.re-frame :refer [use-subscribe]]))

(defn- tenant-href
  "Prefix path with /t/{slug} when a tenant slug is active."
  [slug path]
  (if (and slug (seq slug))
    (str "/t/" slug path)
    path))

(defn- stop-and-push! [e route href]
  (.preventDefault e)
  (if (nil? route)
    (when href
      (set! (.-href js/window.location) href))
    (try
      (rtfe/push-state route)
      ;; After push-state, update the URL bar to include the slug prefix
      ;; without triggering a new navigation.
      (when href
        (let [current (.-pathname js/window.location)]
          (when (not= current href)
            (.replaceState js/history nil "" href))))
      (catch :default _e
        ;; If the route name isn't registered (or push-state fails), fall back to
        ;; a plain navigation so sidebar links still work.
        (when href
          (set! (.-href js/window.location) href))))))

(defn- nav-item
  [{:keys [id label href route icon active?]}]
  {:id id
   :label label
   :href href
   :icon icon
   :active? active?
   :on-click (fn [e] (stop-and-push! e route href))})

(defn- normalize-role
  [role]
  (cond
    (keyword? role) (name role)
    (string? role) role
    :else nil))

(defui tenant-switcher []
  (let [tenant (use-subscribe [:current-tenant])
        memberships (use-subscribe [:tenant/memberships])
        loading? (use-subscribe [:tenant/loading?])
        [open? set-open!] (use-state false)
        tenant-name (or (:name tenant) (:tenants/name tenant) "No workspace")
        tenant-id (or (:id tenant) (:tenants/id tenant))
        has-multiple? (and (seq memberships) (> (count memberships) 1))]
    (when tenant
      ($ :div {:class "mb-2 relative"
               :id "tenant-switcher"}
        ;; Current tenant button
        ($ :button {:class (str "w-full flex items-center justify-between px-2 py-1.5 "
                             "rounded-lg text-sm hover:bg-base-300 transition-colors")
                    :id "tenant-switcher-btn"
                    :on-click (fn []
                                (set-open! (not open?))
                                (when-not memberships
                                  (rf/dispatch [::tenant/fetch-memberships])))}
          ($ :span {:class "font-medium truncate"} tenant-name)
          (when has-multiple?
            ;; Swap icon
            ($ :svg {:class "w-4 h-4 flex-shrink-0 ml-1 text-base-content/50"
                     :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
              ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                        :d "M7 16V4m0 0L3 8m4-4l4 4m6 0v12m0 0l4-4m-4 4l-4-4"}))))

        ;; Dropdown
        (when (and open? (seq memberships) has-multiple?)
          ($ :div {:class (str "absolute bottom-full left-0 w-full mb-1 "
                            "bg-base-100 border border-base-300 rounded-lg shadow-lg z-50")}
            (for [m memberships]
              (let [m-tenant-id (or (:tenant-id m) (:tenant_id m) (:tenants/id m) (:id m))
                    m-name (or (:tenant-name m) (:tenant_name m) (:tenants/name m) (:name m) "Unnamed")
                    m-role (or (:role m) (:tenant_memberships/role m))
                    is-current? (= (str m-tenant-id) (str tenant-id))]
                ($ :button {:key m-tenant-id
                            :class (str "w-full flex items-center justify-between px-3 py-2 "
                                     "text-sm hover:bg-base-200 first:rounded-t-lg last:rounded-b-lg "
                                     (when is-current? "bg-base-200 font-medium"))
                            :disabled (or is-current? loading?)
                            :on-click (fn []
                                        (when-not is-current?
                                          (rf/dispatch [::tenant/switch-tenant {:tenant-id m-tenant-id}]))
                                        (set-open! false))}
                  ($ :span {:class "truncate"} m-name)
                  ($ :span {:class "ds-badge ds-badge-xs ds-badge-ghost ml-1"} m-role))))))))))

(defui sidebar-footer [{:keys [user-display-name role is-owner? route-name]}]
  (let [t (use-t)
        locale (use-subscribe [:locale])]
    ($ :div {:class "p-3 border-t border-base-300"}
      ;; Tenant switcher
      ($ tenant-switcher)
      ;; User info
      ($ :div {:class "flex items-center justify-between mb-2"}
        ($ :div {:class "flex flex-col min-w-0"}
          ($ :span {:class "text-sm font-medium truncate"
                    :id "sidebar-user-name"}
            user-display-name)
          (when role
            ($ :span {:class (str "ds-badge ds-badge-xs mt-1 "
                               (if is-owner? "ds-badge-primary" "ds-badge-secondary"))
                      :id "sidebar-user-role"}
              role))))
      ;; Logout link
      ($ :a {:id "user-sidebar-logout"
             :href "/logout"
             :on-click (fn [e] (stop-and-push! e :logout "/logout"))
             :class (str "flex items-center gap-2 text-sm py-2 px-2 rounded-lg "
                      "hover:bg-base-200 transition-colors "
                      (when (= route-name :logout) "bg-base-200 font-medium"))}
        ($ logout-icon {:class "w-4 h-4"})
        (t :nav/log-out))
      ;; Language switcher
      ($ :div {:class "flex items-center gap-1 mt-2"
               :id "language-switcher"}
        ($ :button {:class (str "text-xs px-2 py-0.5 rounded transition-colors "
                             (if (= locale :bs)
                               "font-bold bg-base-200 text-base-content"
                               "text-base-content/50 hover:bg-base-200"))
                    :id "lang-bs-btn"
                    :on-click #(rf/dispatch [::i18n/set-locale :bs])}
          "BS")
        ($ :span {:class "text-base-content/30 text-xs"} "|")
        ($ :button {:class (str "text-xs px-2 py-0.5 rounded transition-colors "
                             (if (= locale :en)
                               "font-bold bg-base-200 text-base-content"
                               "text-base-content/50 hover:bg-base-200"))
                    :id "lang-en-btn"
                    :on-click #(rf/dispatch [::i18n/set-locale :en])}
          "EN")))))

(defui user-sidebar [{:keys [open?]}]
  (let [t (use-t)
        current-route (use-subscribe [:current-route])
        current-user (use-subscribe [:current-user])
        role (normalize-role (use-subscribe [:user-role]))
        slug (use-subscribe [:tenant/url-slug])
        is-owner? (= role "owner")
        power-user? (contains? #{"admin" "owner"} role)
        can-upload? (contains? #{"member" "admin" "owner"} role)
        route-name (or (get-in current-route [:data :name]) (:name current-route))
        active? (fn [names] (contains? names route-name))
        user-display-name (or (:full-name current-user) (:email current-user) "User")
        th (fn [path] (tenant-href slug path))

        expense-items (vec
                        (concat
                          [(nav-item {:id "user-sidebar-dashboard"
                                      :label (t :nav/dashboard)
                                      :href (th "/dashboard")
                                      :route :user-dashboard
                                      :icon ($ dashboard-icon {:class "w-6 h-6"})
                                      :active? (active? #{:user-dashboard
                                                          :expenses-dashboard
                                                          :expenses-dashboard-alias})})
                           (nav-item {:id "user-sidebar-receipts"
                                      :label (t :nav/receipts)
                                      :href (th "/receipts")
                                      :route :receipts
                                      :icon ($ receipts-icon {:class "w-6 h-6"})
                                      :active? (active? #{:receipts :receipt-detail})})
                           (nav-item {:id "user-sidebar-expenses-list"
                                      :label (t :nav/expenses)
                                      :href (th "/expenses/list")
                                      :route :expenses-list
                                      :icon ($ expenses-icon {:class "w-6 h-6"})
                                      :active? (active? #{:expenses-list
                                                          :expense-detail
                                                          :expense-new})})]
                          (when power-user?
                            [(nav-item {:id "user-sidebar-expense-items"
                                        :label (t :nav/expense-items)
                                        :href (th "/expense-items")
                                        :route :expense-items
                                        :icon ($ expense-items-icon {:class "w-6 h-6"})
                                        :active? (active? #{:expense-items})})])))

        operations-items (vec
                           (concat
                             (when can-upload?
                               [(nav-item {:id "user-sidebar-expenses-upload"
                                           :label (t :nav/upload)
                                           :href (th "/expenses/upload")
                                           :route :expense-upload
                                           :icon ($ arrow-up {:class "w-6 h-6"})
                                           :active? (active? #{:expense-upload})})])
                             [(nav-item {:id "user-sidebar-expenses-reports"
                                         :label (t :nav/reports)
                                         :href (th "/expenses/reports")
                                         :route :expense-reports
                                         :icon ($ chart-bar {:class "w-6 h-6"})
                                         :active? (active? #{:expense-reports})})]
                             (when power-user?
                               [(nav-item {:id "user-sidebar-unmapped-items"
                                           :label (t :nav/unmapped-aliases)
                                           :href (th "/unmapped-items")
                                           :route :unmapped-items
                                           :icon ($ unmapped-items-icon {:class "w-6 h-6"})
                                           :active? (active? #{:unmapped-items})})])))

        reference-items (vec
                          (concat
                            [(nav-item {:id "user-sidebar-suppliers"
                                        :label (t :nav/suppliers)
                                        :href (th "/suppliers")
                                        :route :expense-suppliers
                                        :icon ($ suppliers-icon {:class "w-6 h-6"})
                                        :active? (active? #{:expense-suppliers})})
                             (nav-item {:id "user-sidebar-payers"
                                        :label (t :nav/payers)
                                        :href (th "/payers")
                                        :route :expense-payers
                                        :icon ($ payers-icon {:class "w-6 h-6"})
                                        :active? (active? #{:expense-payers})})]
                            (when power-user?
                              [(nav-item {:id "user-sidebar-payer-types"
                                          :label (t :nav/payer-types)
                                          :href (th "/payer-types")
                                          :route :expense-payer-types
                                          :icon ($ payers-icon {:class "w-6 h-6"})
                                          :active? (active? #{:expense-payer-types})})
                               (nav-item {:id "user-sidebar-articles"
                                          :label (t :nav/articles)
                                          :href (th "/articles")
                                          :route :expense-articles
                                          :icon ($ articles-icon {:class "w-6 h-6"})
                                          :active? (active? #{:expense-articles})})
                               (nav-item {:id "user-sidebar-manufacturers"
                                          :label (t :nav/manufacturers)
                                          :href (th "/manufacturers")
                                          :route :expense-manufacturers
                                          :icon ($ suppliers-icon {:class "w-6 h-6"})
                                          :active? (active? #{:expense-manufacturers})})
                               (nav-item {:id "user-sidebar-categories"
                                          :label (t :nav/categories)
                                          :href (th "/categories")
                                          :route :expense-categories
                                          :icon ($ suppliers-icon {:class "w-6 h-6"})
                                          :active? (active? #{:expense-categories})})
                               (nav-item {:id "user-sidebar-expense-categories"
                                          :label (t :nav/expense-categories)
                                          :href (th "/expense-categories")
                                          :route :expense-categories-catalog
                                          :icon ($ suppliers-icon {:class "w-6 h-6"})
                                          :active? (active? #{:expense-categories-catalog})})
                               (nav-item {:id "user-sidebar-subcategories"
                                          :label (t :nav/subcategories)
                                          :href (th "/subcategories")
                                          :route :expense-subcategories
                                          :icon ($ suppliers-icon {:class "w-6 h-6"})
                                          :active? (active? #{:expense-subcategories})})
                               (nav-item {:id "user-sidebar-stores"
                                          :label (t :nav/stores)
                                          :href (th "/stores")
                                          :route :expense-stores
                                          :icon ($ suppliers-icon {:class "w-6 h-6"})
                                          :active? (active? #{:expense-stores})})
                               (nav-item {:id "user-sidebar-cities"
                                          :label (t :nav/cities)
                                          :href (th "/cities")
                                          :route :expense-cities
                                          :icon ($ suppliers-icon {:class "w-6 h-6"})
                                          :active? (active? #{:expense-cities})})

                               (nav-item {:id "user-sidebar-article-aliases"
                                          :label (t :nav/article-aliases)
                                          :href (th "/article-aliases")
                                          :route :expense-article-aliases
                                          :icon ($ article-aliases-icon {:class "w-6 h-6"})
                                          :active? (active? #{:expense-article-aliases})})
                               (nav-item {:id "user-sidebar-supplier-aliases"
                                          :label (t :nav/supplier-aliases)
                                          :href (th "/supplier-aliases")
                                          :route :expense-supplier-aliases
                                          :icon ($ article-aliases-icon {:class "w-6 h-6"})
                                          :active? (active? #{:expense-supplier-aliases})})
                               (nav-item {:id "user-sidebar-store-aliases"
                                          :label (t :nav/store-aliases)
                                          :href (th "/store-aliases")
                                          :route :expense-store-aliases
                                          :icon ($ article-aliases-icon {:class "w-6 h-6"})
                                          :active? (active? #{:expense-store-aliases})})])))

        workspace-items (vec
                          (concat
                            (when power-user?
                              [(nav-item {:id "user-sidebar-members"
                                          :label (t :nav/members)
                                          :href (th "/tenant/members")
                                          :route :tenant-members
                                          :icon ($ users-icon {:class "w-6 h-6"})
                                          :active? (active? #{:tenant-members})})])
                            (when is-owner?
                              [(nav-item {:id "user-sidebar-impersonation"
                                          :label (t :nav/impersonation)
                                          :href (th "/tenant/impersonation")
                                          :route :tenant-impersonation
                                          :icon ($ admins-icon {:class "w-6 h-6"})
                                          :active? (active? #{:tenant-impersonation})})])))

        sections (cond-> [{:title (t :nav/section-expenses) :items expense-items}
                          {:title (t :nav/section-operations) :items operations-items}
                          {:title (t :nav/section-reference) :items reference-items}]
                   (seq workspace-items)
                   (conj {:title (t :nav/section-workspace) :items workspace-items}))]
    ($ sidebar
      {:title (t :nav/app-title)
       :sections sections
       :open? open?
       :footer ($ sidebar-footer {:user-display-name user-display-name
                                  :role role
                                  :is-owner? is-owner?
                                  :route-name route-name})})))

(defui user-header [{:keys [on-toggle-sidebar]}]
  ($ :div {:class "flex-shrink-0 flex h-16 bg-base-300 shadow"}
    ($ :div {:class "flex-1 px-4 flex justify-between items-center"}
      ($ :div {:class "flex items-center"}
        ($ :button {:class "p-2 rounded-lg hover:bg-base-200 transition-colors"
                    :id "user-sidebar-toggle"
                    :on-click on-toggle-sidebar
                    :aria-label "Toggle sidebar"}
          ($ :svg {:class "w-6 h-6" :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
            ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                      :d "M4 6h16M4 12h16M4 18h16"}))))
      ($ :div {:class "flex items-center space-x-2"}
        ($ auth-component)
        ($ settings-panel {:global-settings? true})))))

(defui user-layout
  [{:keys [children]}]
  (let [[sidebar-open? set-sidebar-open!] (use-state false)]
    ($ :div {:class "h-screen flex overflow-hidden bg-base-100"}
      ($ user-sidebar {:open? sidebar-open?})
      ($ :div {:class "flex flex-col w-0 flex-1 overflow-hidden"}
        ($ user-header {:on-toggle-sidebar #(set-sidebar-open! (not sidebar-open?))})
        ($ :main {:class "flex-1 relative overflow-y-auto focus:outline-none bg-base-100"}
          children)))))
