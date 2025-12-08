(ns app.admin.frontend.adapters.expenses
  "Adapters and entity specs for the expenses domain (admin).
   Normalizes API responses and syncs them into the template entity store."
  (:require
    [app.admin.frontend.adapters.core :as adapters.core]
    [app.admin.frontend.utils.http :as admin-http]
    [app.template.frontend.db.paths :as paths]
    [re-frame.core :as rf]
    [taoensso.timbre :as log]))

;; =============================================================================
;; Entity Specs (fallbacks when UI config not yet loaded)
;; =============================================================================

(def expenses-entity-spec
  {:id :expenses
   :fields [{:id :supplier-display-name :label "Supplier"}
            {:id :supplier-normalized-key :label "Supplier key"}
            {:id :payer-label :label "Payer"}
            {:id :payer-type :label "Payer type"}
            {:id :total-amount :label "Total"}
            {:id :currency :label "Currency"}
            {:id :purchased-at :label "Purchased at"}
            {:id :status :label "Status"}]})

(def receipts-entity-spec
  {:id :receipts
   :fields [{:id :original-filename :label "File"}
            {:id :status :label "Status"}
            {:id :supplier-guess :label "Supplier guess"}
            {:id :created-at :label "Created at"}]})

(def suppliers-entity-spec
  {:id :suppliers
   :fields [{:id :display-name :label "Name"}
            {:id :normalized-key :label "Normalized key"}
            {:id :address :label "Address"}
            {:id :tax-id :label "Tax ID"}
            {:id :created-at :label "Created at"}]})

(def payers-entity-spec
  {:id :payers
   :fields [{:id :label :label "Label"}
            {:id :type :label "Type"}
            {:id :is-default :label "Default?"}]})

(def articles-entity-spec
  {:id :articles
   :fields [{:id :canonical-name :label "Name"}
            {:id :barcode :label "Barcode"}
            {:id :category :label "Category"}
            {:id :created-at :label "Created at"}]})

(def article-aliases-entity-spec
  {:id :article-aliases
   :fields [{:id :supplier-display-name :label "Supplier"}
            {:id :article-canonical-name :label "Article"}
            {:id :raw-label-normalized :label "Alias"}
            {:id :confidence :label "Confidence"}
            {:id :created-at :label "Created at"}]})

(def price-observations-entity-spec
  {:id :price-observations
   :fields [{:id :article-canonical-name :label "Article"}
            {:id :supplier-display-name :label "Supplier"}
            {:id :observed-at :label "Observed at"}
            {:id :unit-price :label "Unit price"}
            {:id :line-total :label "Line total"}
            {:id :currency :label "Currency"}]})

;; Register entity spec subscriptions with fallback values
(adapters.core/register-entity-spec-sub!
  {:entity-key :expenses
   :value-fn (fn [spec _] (or spec expenses-entity-spec))})

(adapters.core/register-entity-spec-sub!
  {:entity-key :receipts
   :value-fn (fn [spec _] (or spec receipts-entity-spec))})

(adapters.core/register-entity-spec-sub!
  {:entity-key :suppliers
   :value-fn (fn [spec _] (or spec suppliers-entity-spec))})

(adapters.core/register-entity-spec-sub!
  {:entity-key :payers
   :value-fn (fn [spec _] (or spec payers-entity-spec))})

(adapters.core/register-entity-spec-sub!
  {:entity-key :articles
   :value-fn (fn [spec _] (or spec articles-entity-spec))})

(adapters.core/register-entity-spec-sub!
  {:entity-key :article-aliases
   :value-fn (fn [spec _] (or spec article-aliases-entity-spec))})

(adapters.core/register-entity-spec-sub!
  {:entity-key :price-observations
   :value-fn (fn [spec _] (or spec price-observations-entity-spec))})

;; =============================================================================
;; Normalization helpers
;; =============================================================================

(defn expense->template-entity
  [expense]
  (adapters.core/normalize-entity
    expense
    {:entity-ns :expenses
     :id-keys [:id]
     :stringify-keys [:supplier_id :payer_id :receipt_id]
     :alias-keys {:supplier_display_name [:supplier-display-name]
                  :supplier_normalized_key [:supplier-normalized-key]
                  :payer_label [:payer-label]
                  :payer_type [:payer-type]
                  :total_amount [:total-amount]
                  :purchased_at [:purchased-at]
                  :is_posted [:is-posted]
                  :created_at [:created-at]
                  :updated_at [:updated-at]}
     :post-transform (fn [m]
                       (let [posted? (get m :is-posted)]
                         (assoc m :status (if (true? posted?) "posted" "draft"))))}))

(defn receipt->template-entity
  [receipt]
  (adapters.core/normalize-entity
    receipt
    {:entity-ns :receipts
     :id-keys [:id]
     :alias-keys {:original_filename [:original-filename]
                  :supplier_guess [:supplier-guess]
                  :created_at [:created-at]}}))

(defn supplier->template-entity
  [supplier]
  (adapters.core/normalize-entity
    supplier
    {:entity-ns :suppliers
     :id-keys [:id]
     :alias-keys {:display_name [:display-name]
                  :normalized_key [:normalized-key]
                  :address [:address]
                  :tax_id [:tax-id]
                  :created_at [:created-at]}}))

(defn payer->template-entity
  [payer]
  (adapters.core/normalize-entity
    payer
    {:entity-ns :payers
     :id-keys [:id]
     :alias-keys {:is_default [:is-default]
                  :created_at [:created-at]}}))

(defn article->template-entity
  [article]
  (adapters.core/normalize-entity
    article
    {:entity-ns :articles
     :id-keys [:id]
     :alias-keys {:canonical_name [:canonical-name]
                  :normalized_key [:normalized-key]
                  :barcode [:barcode]
                  :category [:category]
                  :created_at [:created-at]
                  :updated_at [:updated-at]}}))

(defn article-alias->template-entity
  [alias]
  (adapters.core/normalize-entity
    alias
    {:entity-ns :article-aliases
     :id-keys [:id]
     :stringify-keys [:supplier_id :article_id]
     :alias-keys {:supplier_display_name [:supplier-display-name]
                  :article_canonical_name [:article-canonical-name]
                  :raw_label_normalized [:raw-label-normalized]
                  :confidence [:confidence]
                  :created_at [:created-at]}}))

(defn price-observation->template-entity
  [obs]
  (adapters.core/normalize-entity
    obs
    {:entity-ns :price-observations
     :id-keys [:id]
     :stringify-keys [:supplier_id :article_id :expense_item_id]
     :alias-keys {:article_canonical_name [:article-canonical-name]
                  :supplier_display_name [:supplier-display-name]
                  :observed_at [:observed-at]
                  :unit_price [:unit-price]
                  :line_total [:line-total]
                  :qty [:qty]
                  :currency [:currency]
                  :created_at [:created-at]}}))

;; =============================================================================
;; Template sync events
;; =============================================================================

(adapters.core/register-sync-event!
  {:event-id ::sync-expenses
   :entity-key :expenses
   :normalize-fn expense->template-entity
   :log-prefix "[expenses] Syncing expenses to template:"})

(adapters.core/register-sync-event!
  {:event-id ::sync-receipts
   :entity-key :receipts
   :normalize-fn receipt->template-entity
   :log-prefix "[expenses] Syncing receipts to template:"})

(adapters.core/register-sync-event!
  {:event-id ::sync-suppliers
   :entity-key :suppliers
   :normalize-fn supplier->template-entity
   :log-prefix "[expenses] Syncing suppliers to template:"})

(adapters.core/register-sync-event!
  {:event-id ::sync-payers
   :entity-key :payers
   :normalize-fn payer->template-entity
   :log-prefix "[expenses] Syncing payers to template:"})

(adapters.core/register-sync-event!
  {:event-id ::sync-articles
   :entity-key :articles
   :normalize-fn article->template-entity
   :log-prefix "[expenses] Syncing articles to template:"})

(adapters.core/register-sync-event!
  {:event-id ::sync-article-aliases
   :entity-key :article-aliases
   :normalize-fn article-alias->template-entity
   :log-prefix "[expenses] Syncing article aliases to template:"})

(adapters.core/register-sync-event!
  {:event-id ::sync-price-observations
   :entity-key :price-observations
   :normalize-fn price-observation->template-entity
   :log-prefix "[expenses] Syncing price observations to template:"})

;; =============================================================================
;; UI state initialization
;; =============================================================================

(defn- initialize-entity-ui-state
  [db entity-key {:keys [per-page sort-field sort-direction]
                  :or {per-page 25 sort-direction :desc}}]
  (let [metadata-path (paths/entity-metadata entity-key)
        ui-state-path (paths/list-ui-state entity-key)
        selected-path (paths/entity-selected-ids entity-key)
        existing-per-page (or (get-in db (paths/list-per-page entity-key)) per-page)
        existing-page (or (get-in db (paths/list-current-page entity-key)) 1)
        sort-config (cond-> {}
                      sort-field (assoc :field sort-field)
                      sort-direction (assoc :direction sort-direction))]
    (adapters.core/assoc-paths db
      [[metadata-path {:loading? false
                       :error nil
                       :sort sort-config
                       :pagination {:page existing-page :per-page existing-per-page}}]
       [ui-state-path {:sort sort-config
                       :pagination {:current-page existing-page :per-page existing-per-page}
                       :per-page existing-per-page}]
       [(paths/list-per-page entity-key) existing-per-page]
       [(paths/list-current-page entity-key) existing-page]
       [selected-path #{}]])))

(rf/reg-event-db
  ::initialize-entity
  (fn [db [_ entity-key opts]]
    (initialize-entity-ui-state db entity-key opts)))

(defn init-expenses-adapter!
  []
  (rf/dispatch [::initialize-entity :expenses {:per-page 25 :sort-field :purchased-at :sort-direction :desc}]))

(defn init-receipts-adapter!
  []
  (rf/dispatch [::initialize-entity :receipts {:per-page 25 :sort-field :created-at :sort-direction :desc}]))

(defn init-suppliers-adapter!
  []
  (rf/dispatch [::initialize-entity :suppliers {:per-page 50 :sort-field :created-at :sort-direction :desc}]))

(defn init-payers-adapter!
  []
  (rf/dispatch [::initialize-entity :payers {:per-page 50 :sort-field :label :sort-direction :asc}]))

(defn init-articles-adapter!
  []
  (rf/dispatch [::initialize-entity :articles {:per-page 50 :sort-field :created-at :sort-direction :desc}]))

(defn init-article-aliases-adapter!
  []
  (rf/dispatch [::initialize-entity :article-aliases {:per-page 50 :sort-field :created-at :sort-direction :desc}]))

(defn init-price-observations-adapter!
  []
  (rf/dispatch [::initialize-entity :price-observations {:per-page 50 :sort-field :observed-at :sort-direction :desc}]))

(defn- suppliers-request
  "Create HTTP request config for suppliers admin API."
  [{:keys [method id params on-success on-failure]}]
  (let [base-uri "/admin/api/expenses/suppliers"
        uri (if id (str base-uri "/" id) base-uri)]
    (log/info "🔧 suppliers-request:" {:method method :uri uri :params params})
    (admin-http/admin-request {:method method
                               :uri uri
                               :params params
                               :on-success on-success
                               :on-failure on-failure})))

(adapters.core/register-admin-crud-bridge!
  {:entity-key :suppliers
   :context-pred (fn [_] true)
   :operations
   {:delete {:request (fn [{:keys [db]} entity-type id default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (suppliers-request
                                          {:method :delete
                                           :id id
                                           :on-success [:app.template.frontend.events.list.crud/delete-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/delete-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _id default-effect]
                           ;; Reuse default DB updates but refresh via expenses domain loader
                           (assoc default-effect
                             :dispatch [:app.domain.expenses.frontend.events.suppliers/load {}]))}
    :create {:request (fn [{:keys [db]} entity-type form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (suppliers-request
                                          {:method :post
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/create-success entity-type]
                                           :on-failure [:app.template.frontend.events.list.crud/create-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _response default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.expenses.frontend.events.suppliers/load {}]))}
    :update {:request (fn [{:keys [db]} entity-type id form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (suppliers-request
                                          {:method :put
                                           :id id
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/update-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/update-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _id _response default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.expenses.frontend.events.suppliers/load {}]))}}})

(defn- expenses-request
  "Create HTTP request config for expenses admin API."
  [{:keys [method id params on-success on-failure]}]
  (let [base-uri "/admin/api/expenses/entries"
        uri (if id (str base-uri "/" id) base-uri)]
    (log/info "💸 expenses-request:" {:method method :uri uri :params params})
    (admin-http/admin-request {:method method
                               :uri uri
                               :params params
                               :on-success on-success
                               :on-failure on-failure})))

(adapters.core/register-admin-crud-bridge!
  {:entity-key :expenses
   :context-pred (fn [_] true)
   :operations
   {:delete {:request (fn [{:keys [db]} entity-type id default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (expenses-request
                                          {:method :delete
                                           :id id
                                           :on-success [:app.template.frontend.events.list.crud/delete-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/delete-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _id default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.expenses.frontend.events.expenses/load-list {}]))}
    :create {:request (fn [{:keys [db]} entity-type form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (expenses-request
                                          {:method :post
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/create-success entity-type]
                                           :on-failure [:app.template.frontend.events.list.crud/create-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _response default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.expenses.frontend.events.expenses/load-list {}]))}
    :update {:request (fn [{:keys [db]} entity-type id form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (expenses-request
                                          {:method :put
                                           :id id
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/update-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/update-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _id _response default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.expenses.frontend.events.expenses/load-list {}]))}}})

(defn- receipts-request
  "Create HTTP request config for receipts admin API."
  [{:keys [method id params on-success on-failure]}]
  (let [base-uri "/admin/api/expenses/receipts"
        uri (if id (str base-uri "/" id) base-uri)]
    (log/info "🧾 receipts-request:" {:method method :uri uri :params params})
    (admin-http/admin-request {:method method
                               :uri uri
                               :params params
                               :on-success on-success
                               :on-failure on-failure})))

(adapters.core/register-admin-crud-bridge!
  {:entity-key :receipts
   :context-pred (fn [_] true)
   :operations
   {:delete {:request (fn [{:keys [db]} entity-type id default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (receipts-request
                                          {:method :delete
                                           :id id
                                           :on-success [:app.template.frontend.events.list.crud/delete-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/delete-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _id default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.expenses.frontend.events.receipts/load-list {}]))}
    :create {:request (fn [{:keys [db]} entity-type form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (receipts-request
                                          {:method :post
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/create-success entity-type]
                                           :on-failure [:app.template.frontend.events.list.crud/create-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _response default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.expenses.frontend.events.receipts/load-list {}]))}
    :update {:request (fn [{:keys [db]} entity-type id form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (receipts-request
                                          {:method :put
                                           :id id
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/update-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/update-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _id _response default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.expenses.frontend.events.receipts/load-list {}]))}}})

(defn- payers-request
  "Create HTTP request config for payers admin API."
  [{:keys [method id params on-success on-failure]}]
  (let [base-uri "/admin/api/expenses/payers"
        uri (if id (str base-uri "/" id) base-uri)]
    (log/info "🔧 payers-request:" {:method method :uri uri :params params})
    (admin-http/admin-request {:method method
                               :uri uri
                               :params params
                               :on-success on-success
                               :on-failure on-failure})))

(defn- articles-request
  "Create HTTP request config for articles admin API."
  [{:keys [method id params on-success on-failure]}]
  (let [base-uri "/admin/api/expenses/articles"
        uri (if id (str base-uri "/" id) base-uri)]
    (log/info "📦 articles-request:" {:method method :uri uri :params params})
    (admin-http/admin-request {:method method
                               :uri uri
                               :params params
                               :on-success on-success
                               :on-failure on-failure})))

(adapters.core/register-admin-crud-bridge!
  {:entity-key :articles
   :context-pred (fn [_] true)
   :operations
   {:delete {:request (fn [{:keys [db]} entity-type id default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (articles-request
                                          {:method :delete
                                           :id id
                                           :on-success [:app.template.frontend.events.list.crud/delete-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/delete-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_ _ _ default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.expenses.frontend.events.articles/load {}]))}
    :create {:request (fn [{:keys [db]} entity-type form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (articles-request
                                          {:method :post
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/create-success entity-type]
                                           :on-failure [:app.template.frontend.events.list.crud/create-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_ _ _ default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.expenses.frontend.events.articles/load {}]))}
    :update {:request (fn [{:keys [db]} entity-type id form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (articles-request
                                          {:method :put
                                           :id id
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/update-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/update-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_ _ _ _ default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.expenses.frontend.events.articles/load {}]))}}})

(defn- article-aliases-request
  "Create HTTP request config for article aliases admin API."
  [{:keys [method id params on-success on-failure]}]
  (let [base-uri "/admin/api/expenses/article-aliases"
        uri (if id (str base-uri "/" id) base-uri)]
    (log/info "🔗 article-aliases-request:" {:method method :uri uri :params params})
    (admin-http/admin-request {:method method
                               :uri uri
                               :params params
                               :on-success on-success
                               :on-failure on-failure})))

(adapters.core/register-admin-crud-bridge!
  {:entity-key :article-aliases
   :context-pred (fn [_] true)
   :operations
   {:delete {:request (fn [{:keys [db]} entity-type id default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (article-aliases-request
                                          {:method :delete
                                           :id id
                                           :on-success [:app.template.frontend.events.list.crud/delete-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/delete-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_ _ _ default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.expenses.frontend.events.article-aliases/load {}]))}
    :create {:request (fn [{:keys [db]} entity-type form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (article-aliases-request
                                          {:method :post
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/create-success entity-type]
                                           :on-failure [:app.template.frontend.events.list.crud/create-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_ _ _ default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.expenses.frontend.events.article-aliases/load {}]))}
    :update {:request (fn [{:keys [db]} entity-type id form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (article-aliases-request
                                          {:method :put
                                           :id id
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/update-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/update-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_ _ _ _ default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.expenses.frontend.events.article-aliases/load {}]))}}})

(defn- price-observations-request
  "Create HTTP request config for price observations admin API."
  [{:keys [method id params on-success on-failure]}]
  (let [base-uri "/admin/api/expenses/price-observations"
        uri (if id (str base-uri "/" id) base-uri)]
    (log/info "📈 price-observations-request:" {:method method :uri uri :params params})
    (admin-http/admin-request {:method method
                               :uri uri
                               :params params
                               :on-success on-success
                               :on-failure on-failure})))

(adapters.core/register-admin-crud-bridge!
  {:entity-key :price-observations
   :context-pred (fn [_] true)
   :operations
   {:delete {:request (fn [{:keys [db]} entity-type id default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (price-observations-request
                                          {:method :delete
                                           :id id
                                           :on-success [:app.template.frontend.events.list.crud/delete-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/delete-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_ _ _ default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.expenses.frontend.events.price-observations/load {}]))}
    :create {:request (fn [{:keys [db]} entity-type form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (price-observations-request
                                          {:method :post
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/create-success entity-type]
                                           :on-failure [:app.template.frontend.events.list.crud/create-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_ _ _ default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.expenses.frontend.events.price-observations/load {}]))}
    :update {:request (fn [{:keys [db]} entity-type id form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (price-observations-request
                                          {:method :put
                                           :id id
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/update-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/update-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_ _ _ _ default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.expenses.frontend.events.price-observations/load {}]))}}})

(adapters.core/register-admin-crud-bridge!
  {:entity-key :payers
   :context-pred (fn [_] true)
   :operations
   {:delete {:request (fn [{:keys [db]} entity-type id default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (payers-request
                                          {:method :delete
                                           :id id
                                           :on-success [:app.template.frontend.events.list.crud/delete-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/delete-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _id default-effect]
                           ;; Reuse default DB updates but refresh via expenses domain loader
                           (assoc default-effect
                             :dispatch [:app.domain.expenses.frontend.events.payers/load {}]))}
    :create {:request (fn [{:keys [db]} entity-type form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (payers-request
                                          {:method :post
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/create-success entity-type]
                                           :on-failure [:app.template.frontend.events.list.crud/create-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _response default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.expenses.frontend.events.payers/load {}]))}
    :update {:request (fn [{:keys [db]} entity-type id form-data default-effect]
                        (if (adapters.core/admin-token db)
                          (assoc default-effect
                            :http-xhrio (payers-request
                                          {:method :put
                                           :id id
                                           :params form-data
                                           :on-success [:app.template.frontend.events.list.crud/update-success entity-type id]
                                           :on-failure [:app.template.frontend.events.list.crud/update-failure entity-type]}))
                          {:dispatch [:admin/redirect-to-login]}))
             :on-success (fn [_cofx _entity-type _id _response default-effect]
                           (assoc default-effect
                             :dispatch [:app.domain.expenses.frontend.events.payers/load {}]))}}})
