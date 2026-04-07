(ns app.e2e.helpers
  "Reusable Playwright Java interop wrappers for E2E tests.

   Provides:
   - Browser navigation and interaction helpers
   - Auth flow helpers (register, login, logout)
   - Tenant operation helpers (select, switch, invite)
   - API helpers (bypass UI for fast test setup)
   - Assertion helpers
   - DB query helpers for verification"
  (:require
    [app.e2e.fixtures :as fixtures]
    [app.template.backend.security.email :as email-privacy]
    [cheshire.core :as json]
    [clojure.string :as str])
  (:import
    [com.microsoft.playwright
     Locator
     Page Page$WaitForURLOptions Page$WaitForSelectorOptions Page$GetByRoleOptions
     BrowserContext]
    [com.microsoft.playwright.assertions PlaywrightAssertions]
    [com.microsoft.playwright.options
     AriaRole RequestOptions WaitForSelectorState]))

;; ============================================================================
;; Navigation
;; ============================================================================

(defn navigate
  "Navigate the current page to a path relative to the test base URL."
  ([path] (navigate fixtures/*page* path))
  ([^Page page path]
   (let [url (str fixtures/base-url path)]
     (.navigate page url)
     page)))

(defn current-url
  "Get the current page URL."
  ([] (current-url fixtures/*page*))
  ([^Page page] (.url page)))

(defn wait-for-url
  "Wait until the page URL matches a regex pattern string."
  ([pattern] (wait-for-url fixtures/*page* pattern))
  ([^Page page pattern]
   (let [opts (-> (Page$WaitForURLOptions.)
                (.setTimeout 10000))]
     (.waitForURL page pattern opts))))

(defn wait-for-load
  "Wait for the page to reach load state."
  ([] (wait-for-load fixtures/*page*))
  ([^Page page]
   (.waitForLoadState page)
   page))

(defn reload
  "Reload the current page."
  ([] (reload fixtures/*page*))
  ([^Page page]
   (.reload page)
   (.waitForLoadState page)
   page))

;; ============================================================================
;; Element Interaction
;; ============================================================================

(defn locator
  "Get a Playwright Locator for the given CSS selector."
  ([selector] (locator fixtures/*page* selector))
  ([^Page page selector]
   (.locator page selector)))

(defn locator-by-role
  "Get a Locator by ARIA role with optional name filter."
  ([role] (locator-by-role fixtures/*page* role nil))
  ([role name-text] (locator-by-role fixtures/*page* role name-text))
  ([^Page page role name-text]
   (let [role-enum (case role
                     :button   AriaRole/BUTTON
                     :link     AriaRole/LINK
                     :textbox  AriaRole/TEXTBOX
                     :heading  AriaRole/HEADING
                     :row      AriaRole/ROW
                     :cell     AriaRole/CELL
                     :combobox AriaRole/COMBOBOX
                     :checkbox AriaRole/CHECKBOX
                     :radio    AriaRole/RADIO
                     :tab      AriaRole/TAB
                     :dialog   AriaRole/DIALOG
                     :listbox  AriaRole/LISTBOX
                     :option   AriaRole/OPTION
                     :navigation AriaRole/NAVIGATION
                     (throw (ex-info (str "Unknown role: " role) {:role role})))]
     (if name-text
       (.getByRole page role-enum
         (-> (Page$GetByRoleOptions.)
           (.setName name-text)))
       (.getByRole page role-enum)))))

(defn locator-by-text
  "Get a Locator that matches text content."
  ([text] (locator-by-text fixtures/*page* text))
  ([^Page page text]
   (.getByText page text)))

(defn locator-by-test-id
  "Get a Locator by data-testid attribute."
  ([test-id] (locator-by-test-id fixtures/*page* test-id))
  ([^Page page test-id]
   (.getByTestId page test-id)))

(defn fill
  "Fill a form input identified by selector with a value."
  ([selector value] (fill fixtures/*page* selector value))
  ([^Page page selector value]
   (.fill (.locator page selector) value)))

(defn click
  "Click an element identified by selector."
  ([selector] (click fixtures/*page* selector))
  ([^Page page selector]
   (.click (.locator page selector))))

(defn click-locator
  "Click a Playwright Locator directly."
  [^Locator loc]
  (.click loc))

(defn select-option
  "Select an option in a <select> element by value."
  ([selector value] (select-option fixtures/*page* selector value))
  ([^Page page selector value]
   (.selectOption (.locator page selector) value)))

(defn inner-text
  "Get the inner text of an element."
  ([selector] (inner-text fixtures/*page* selector))
  ([^Page page selector]
   (.innerText (.locator page selector))))

(defn text-content
  "Get the text content of a Locator."
  [^Locator loc]
  (.textContent loc))

(defn input-value
  "Get the value of an input element."
  ([selector] (input-value fixtures/*page* selector))
  ([^Page page selector]
   (.inputValue (.locator page selector))))

(defn is-visible?
  "Check if an element is visible on the page."
  ([selector] (is-visible? fixtures/*page* selector))
  ([^Page page selector]
   (.isVisible (.locator page selector))))

(defn is-enabled?
  "Check if an element is enabled (not disabled)."
  ([selector] (is-enabled? fixtures/*page* selector))
  ([^Page page selector]
   (.isEnabled (.locator page selector))))

(defn element-count
  "Count the number of elements matching a selector."
  ([selector] (element-count fixtures/*page* selector))
  ([^Page page selector]
   (.count (.locator page selector))))

(defn wait-for-selector
  "Wait for an element matching the selector to appear."
  ([selector] (wait-for-selector fixtures/*page* selector))
  ([^Page page selector]
   (.waitForSelector page selector)
   page))

(defn wait-for-hidden
  "Wait for an element to become hidden."
  ([selector] (wait-for-hidden fixtures/*page* selector))
  ([^Page page selector]
   (.waitForSelector page selector
     (-> (Page$WaitForSelectorOptions.)
       (.setState WaitForSelectorState/HIDDEN)))
   page))

;; ============================================================================
;; Auth Flow Helpers
;; ============================================================================

(defn register!
  "Register a new user via the UI.
   Returns the page after registration completes."
  ([opts] (register! fixtures/*page* opts))
  ([^Page page {:keys [email name password]}]
   (navigate page "/register")
   (wait-for-load page)
   (fill page "#register-name" name)
   (fill page "#register-email" email)
   (fill page "#register-password" password)
   (click page "#register-submit")
   ;; Wait for navigation to complete (redirect away from /register)
   (.waitForLoadState page)
   (Thread/sleep 500) ;; Brief pause for SPA routing
   page))

(defn login!
  "Log in via the UI.
   Returns the page after login completes."
  ([opts] (login! fixtures/*page* opts))
  ([^Page page {:keys [email password]}]
   (navigate page "/login")
   (wait-for-load page)
   (fill page "#login-email" email)
   (fill page "#login-password" password)
   (click page "#login-submit")
   ;; Wait for navigation to complete (redirect away from /login)
   (.waitForLoadState page)
   (Thread/sleep 500) ;; Brief pause for SPA routing
   page))

(defn logout!
  "Log out via the API (faster than UI click)."
  ([] (logout! fixtures/*page*))
  ([^Page page]
   (navigate page "/logout")
   (wait-for-load page)
   page))

;; ============================================================================
;; API Helpers (bypass UI for faster setup)
;; ============================================================================

(defn api-request-context
  "Get an APIRequestContext from the current browser context.
   Shares cookies with the browser session."
  ([] (api-request-context fixtures/*context*))
  ([^BrowserContext context]
   (.request context)))

(defn api-get
  "Make a GET request using the browser context's cookie jar."
  ([path] (api-get fixtures/*context* path))
  ([^BrowserContext context path]
   (let [api-ctx (.request context)
         url     (str fixtures/base-url path)
         resp    (.get api-ctx url)]
     {:status (.status resp)
      :body   (try (json/parse-string (.text resp) true) (catch Exception _ (.text resp)))})))

(defn api-post
  "Make a POST request with JSON body using the browser context's cookie jar."
  ([path body] (api-post fixtures/*context* path body))
  ([^BrowserContext context path body]
   (let [api-ctx (.request context)
         url     (str fixtures/base-url path)
         opts    (-> (RequestOptions/create)
                   (.setHeader "Content-Type" "application/json")
                   (.setData (json/generate-string body)))
         resp    (.post api-ctx url opts)]
     {:status (.status resp)
      :body   (try (json/parse-string (.text resp) true) (catch Exception _ (.text resp)))})))

(defn api-put
  "Make a PUT request with JSON body."
  ([path body] (api-put fixtures/*context* path body))
  ([^BrowserContext context path body]
   (let [api-ctx (.request context)
         url     (str fixtures/base-url path)
         opts    (-> (RequestOptions/create)
                   (.setHeader "Content-Type" "application/json")
                   (.setData (json/generate-string body)))
         resp    (.put api-ctx url opts)]
     {:status (.status resp)
      :body   (try (json/parse-string (.text resp) true) (catch Exception _ (.text resp)))})))

(defn api-delete
  "Make a DELETE request."
  ([path] (api-delete fixtures/*context* path))
  ([^BrowserContext context path]
   (let [api-ctx (.request context)
         url     (str fixtures/base-url path)
         resp    (.delete api-ctx url)]
     {:status (.status resp)
      :body   (try (json/parse-string (.text resp) true) (catch Exception _ (.text resp)))})))

(defn api-auth-status
  "Get the current auth status from the API."
  ([] (api-auth-status fixtures/*context*))
  ([context]
   (api-get context "/auth/status")))

(defn api-register!
  "Register a user via API (faster than UI). Returns the response.

   NOTE: Backend expects `full-name` (kebab-case JSON key), but many tests pass `:name`.
   We accept either and send `full-name` to the server."
  ([opts] (api-register! fixtures/*context* opts))
  ([context {:keys [email name full-name password]}]
   (api-post context "/api/v1/auth/register"
     {:email email
      :full-name (or full-name name)
      :password password})))

(defn api-login!
  "Login via API (faster than UI). Returns the response."
  ([opts] (api-login! fixtures/*context* opts))
  ([context {:keys [email password]}]
   (api-post context "/api/v1/auth/login"
     {:email email :password password})))

(defn api-logout!
  "Logout via API."
  ([] (api-logout! fixtures/*context*))
  ([context]
   (api-post context "/api/v1/auth/logout" {})))

;; ============================================================================
;; Tenant Operation Helpers
;; ============================================================================

(defn api-switch-tenant!
  "Switch the active tenant via API."
  ([tenant-id] (api-switch-tenant! fixtures/*context* tenant-id))
  ([context tenant-id]
   (let [tid (str tenant-id)]
     (when (str/blank? tid)
       (throw (ex-info "tenant-id is required for tenant switch" {:tenant-id tenant-id})))
     (api-post context "/api/v1/tenant/switch" {:tenant-id tid}))))

(defn api-fetch-memberships
  "Get the current user's tenant memberships via API."
  ([] (api-fetch-memberships fixtures/*context*))
  ([context]
   (api-get context "/api/v1/tenant/memberships")))

(defn api-fetch-members
  "Get members of the current tenant via API."
  ([] (api-fetch-members fixtures/*context*))
  ([context]
   (api-get context "/api/v1/tenant/members")))

(defn api-invite-member!
  "Send an invitation via API. Returns response with invitation data."
  ([opts] (api-invite-member! fixtures/*context* opts))
  ([context {:keys [email role]}]
   (api-post context "/api/v1/tenant/invitations"
     {:email email :role (or role "member")})))

(defn api-accept-invitation!
  "Accept an invitation via API."
  ([token] (api-accept-invitation! fixtures/*context* token))
  ([context token]
   (api-post context "/api/v1/tenant/invitations/accept" {:token token})))

(defn api-revoke-invitation!
  "Revoke a pending invitation by ID via API."
  ([invitation-id] (api-revoke-invitation! fixtures/*context* invitation-id))
  ([context invitation-id]
   (api-delete context (str "/api/v1/tenant/invitations/" invitation-id))))

(defn api-change-role!
  "Change a member's role via API."
  ([membership-id new-role] (api-change-role! fixtures/*context* membership-id new-role))
  ([context membership-id new-role]
   (api-put context (str "/api/v1/tenant/members/" membership-id "/role")
     {:role new-role})))

(defn api-remove-member!
  "Remove a member by membership ID via API."
  ([membership-id] (api-remove-member! fixtures/*context* membership-id))
  ([context membership-id]
   (api-delete context (str "/api/v1/tenant/members/" membership-id))))

(defn api-transfer-ownership!
  "Transfer tenant ownership to another user via API."
  ([user-id] (api-transfer-ownership! fixtures/*context* user-id))
  ([context user-id]
   (api-post context "/api/v1/tenant/transfer-ownership" {:user-id (str user-id)})))

(defn api-resend-invitation!
  "Resend an invitation email via API."
  ([invitation-id] (api-resend-invitation! fixtures/*context* invitation-id))
  ([context invitation-id]
   (api-post context (str "/api/v1/tenant/invitations/" invitation-id "/resend") {})))

;; ============================================================================
;; Tenant UI Helpers
;; ============================================================================

(defn select-tenant!
  "Select a tenant on the /tenant-select page by clicking its card."
  ([tenant-name] (select-tenant! fixtures/*page* tenant-name))
  ([^Page page tenant-name]
   (wait-for-selector page "[data-testid='tenant-card']")
   (-> (.locator page (str "[data-testid='tenant-card']:has-text(\"" tenant-name "\")"))
     (.click))
   (wait-for-load page)
   page))

(defn switch-tenant!
  "Switch tenants using the sidebar tenant switcher."
  ([tenant-name] (switch-tenant! fixtures/*page* tenant-name))
  ([^Page page tenant-name]
   ;; Click the tenant switcher in sidebar footer
   (click page "[data-testid='tenant-switcher']")
   ;; Select the target tenant from the dropdown
   (-> (.locator page (str "[data-testid='tenant-option']:has-text(\"" tenant-name "\")"))
     (.click))
   (wait-for-load page)
   page))

(defn get-sidebar-tenant-name
  "Get the tenant name displayed in the sidebar."
  ([] (get-sidebar-tenant-name fixtures/*page*))
  ([^Page page]
   (inner-text page "[data-testid='sidebar-tenant-name']")))

;; ============================================================================
;; Member Management UI Helpers
;; ============================================================================

(defn invite-member!
  "Invite a member via the Members page UI."
  ([opts] (invite-member! fixtures/*page* opts))
  ([^Page page {:keys [email role]}]
   (navigate page "/tenant/members")
   (wait-for-load page)
   (fill page "#invite-email" email)
   (when role
     (select-option page "#invite-role" role))
   (click page "#invite-submit")
   (wait-for-load page)
   page))

(defn revoke-invitation!
  "Revoke a pending invitation via the Members page UI."
  ([email] (revoke-invitation! fixtures/*page* email))
  ([^Page page email]
   (let [row (.locator page (str "[data-testid='invitation-row']:has-text(\"" email "\")"))]
     (.click (.locator row "[data-testid='revoke-invitation']"))
     (wait-for-load page)
     page)))

;; ============================================================================
;; Assertion Helpers
;; ============================================================================

(defn assert-visible
  "Assert that an element matching the selector is visible."
  ([selector] (assert-visible fixtures/*page* selector))
  ([^Page page selector]
   (let [loc (.locator page selector)]
     (-> (PlaywrightAssertions/assertThat loc)
       (.toBeVisible)))))

(defn assert-hidden
  "Assert that an element matching the selector is hidden."
  ([selector] (assert-hidden fixtures/*page* selector))
  ([^Page page selector]
   (let [loc (.locator page selector)]
     (-> (PlaywrightAssertions/assertThat loc)
       (.not)
       (.toBeVisible)))))

(defn assert-text
  "Assert that an element's text content contains the expected string."
  ([selector expected] (assert-text fixtures/*page* selector expected))
  ([^Page page selector expected]
   (let [loc (.locator page selector)]
     (-> (PlaywrightAssertions/assertThat loc)
       (.toContainText expected)))))

(defn assert-url-contains
  "Assert that the current URL contains the given substring."
  ([substring] (assert-url-contains fixtures/*page* substring))
  ([^Page page substring]
   (let [url (.url page)]
     (when-not (.contains url substring)
       (throw (AssertionError.
                (str "Expected URL to contain \"" substring "\" but was \"" url "\"")))))))

(defn assert-url-matches
  "Assert that the current URL matches a regex pattern."
  ([pattern] (assert-url-matches fixtures/*page* pattern))
  ([^Page page pattern]
   (-> (PlaywrightAssertions/assertThat page)
     (.toHaveURL (java.util.regex.Pattern/compile pattern)))))

(defn assert-element-count
  "Assert that exactly n elements match the selector."
  ([selector n] (assert-element-count fixtures/*page* selector n))
  ([^Page page selector n]
   (let [loc (.locator page selector)]
     (-> (PlaywrightAssertions/assertThat loc)
       (.hasCount n)))))

;; ============================================================================
;; DB Query Helpers (for verification)
;; ============================================================================

(defn query-db
  "Execute a SQL query against the test DB. Returns unqualified maps."
  [sql & params]
  (apply fixtures/query-db sql params))

(defn get-tenant-by-slug
  "Look up a tenant by slug."
  [slug]
  (first (query-db "SELECT * FROM tenants WHERE slug = ?" slug)))

(defn get-tenant-by-id
  "Look up a tenant by ID."
  [id]
  (first (query-db "SELECT * FROM tenants WHERE id = ?::uuid" (str id))))

(defn get-user-by-email
  "Look up a user by email."
  [email]
  (first (query-db "SELECT * FROM users WHERE email_lookup_hash = ?"
           (email-privacy/email->lookup-hash email))))

(defn get-memberships-for-user
  "Get all active memberships for a user by email."
  [email]
  (query-db
    "SELECT tm.*, t.name as tenant_name, t.slug as tenant_slug
     FROM tenant_memberships tm
     JOIN users u ON u.id = tm.user_id
     JOIN tenants t ON t.id = tm.tenant_id
     WHERE u.email_lookup_hash = ? AND tm.status = 'active'
     ORDER BY tm.created_at"
    (email-privacy/email->lookup-hash email)))

(defn get-memberships-for-tenant
  "Get all memberships for a tenant by slug."
  [slug]
  (mapv (fn [row]
          (let [email (email-privacy/resolve-email row)]
            (cond-> row
              email (assoc :email email))))
    (query-db
      "SELECT tm.*, u.email_ciphertext AS user_email_ciphertext, u.full_name
       FROM tenant_memberships tm
       JOIN users u ON u.id = tm.user_id
       JOIN tenants t ON t.id = tm.tenant_id
       WHERE t.slug = ?
       ORDER BY tm.created_at" slug)))

(defn get-invitation-token
  "Get the invitation token for a pending invitation by email."
  [email]
  (:token
   (first (query-db
            "SELECT token FROM tenant_invitations
              WHERE email_lookup_hash = ? AND status = 'pending'
              ORDER BY created_at DESC LIMIT 1"
             (email-privacy/email->lookup-hash email)))))

(defn get-invitation-by-email
  "Get the most recent invitation for an email."
  [email]
  (some-> (first (query-db
                   "SELECT * FROM tenant_invitations
                    WHERE email_lookup_hash = ?
                    ORDER BY created_at DESC LIMIT 1"
                   (email-privacy/email->lookup-hash email)))
    (as-> row
      (cond-> row
        (email-privacy/resolve-email row)
        (assoc :email (email-privacy/resolve-email row))))))

(defn get-expense-categories-for-tenant
  "Get expense categories for a tenant by slug."
  [slug]
  (query-db
    "SELECT ec.* FROM expense_categories ec
     JOIN tenants t ON t.id = ec.tenant_id
     WHERE t.slug = ?
     ORDER BY ec.name" slug))

(defn get-payer-types-for-tenant
  "Get payer types for a tenant by slug."
  [slug]
  (query-db
    "SELECT pt.* FROM payer_types pt
     JOIN tenants t ON t.id = pt.tenant_id
     WHERE t.slug = ?
     ORDER BY pt.label" slug))

(defn get-audit-logs
  "Get recent audit log entries."
  ([] (get-audit-logs 20))
  ([limit]
   (query-db
     (str "SELECT * FROM audit_logs ORDER BY created_at DESC LIMIT " limit))))

(defn count-table
  "Count rows in a table, optionally filtered by tenant slug."
  ([table-name]
   (:count (first (query-db (str "SELECT count(*) FROM " table-name)))))
  ([table-name tenant-slug]
   (:count (first (query-db
                    (str "SELECT count(*) FROM " table-name " e "
                      "JOIN tenants t ON t.id = e.tenant_id "
                      "WHERE t.slug = ?")
                    tenant-slug)))))

;; ============================================================================
;; Test User Credentials
;; ============================================================================

(def user-a {:email "e2e-user-a@example.com" :name "E2E User A" :password "TestPass123!"})
(def user-b {:email "e2e-user-b@example.com" :name "E2E User B" :password "TestPass123!"})
(def user-c {:email "e2e-user-c@example.com" :name "E2E User C" :password "TestPass123!"})
(def user-d {:email "e2e-user-d@example.com" :name "E2E User D" :password "TestPass123!"})

(comment
  ;; REPL helpers
  (get-user-by-email "e2e-user-a@example.com")
  (get-memberships-for-user "e2e-user-a@example.com")
  (get-tenant-by-slug "e2e-user-a")
  :rcf)
