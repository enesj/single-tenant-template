(ns app.template.backend.middleware.device-detection
  "User-agent based phone detection and redirect to mobile routes (/m/*)."
  (:require
    [clojure.string :as str]))

(def ^:private phone-patterns
  "Regex patterns that indicate a phone (not tablet) user-agent."
  [#"(?i)iPhone"
   #"(?i)Android.*Mobile"
   #"(?i)Windows Phone"
   #"(?i)BlackBerry"
   #"(?i)Opera Mini"
   #"(?i)IEMobile"])

(defn phone-ua?
  "Returns true if user-agent string indicates a phone device.
   Tablets (iPad, Android without Mobile) are NOT classified as phones."
  [ua]
  (when (and ua (not (str/blank? ua)))
    (boolean (some #(re-find % ua) phone-patterns))))

(def ^:private skip-prefixes
  "Path prefixes that should never be redirected."
  ["/m/"      ; already mobile
   "/api/"    ; API endpoints serve both surfaces
   "/admin/"  ; admin panel is desktop-only
   "/admin"   ; admin root
   "/js/"     ; static JS assets
   "/assets/" ; static CSS/images
   "/favicon" ; favicon
   "/health"  ; health check
   "/auth/"   ; auth callbacks
   "/login/"  ; OAuth launch URIs
   "/oauth"   ; OAuth callbacks
   "/logout"])

(defn- mobile-fallback-query-string?
  [query-string]
  (boolean (and query-string
             (re-find #"(^|&)mobile-fallback=1(&|$)" query-string))))

(defn- skip-redirect?
  "Returns true if this path should not be redirected to mobile."
  [path query-string]
  (or (some #(str/starts-with? path %) skip-prefixes)
    (mobile-fallback-query-string? query-string)
      ;; Don't redirect exact root paths that are OAuth or auth-related
    (= path "/verify-email")
    (= path "/email-verified")
    (= path "/reset-password")))

(def ^:private desktop-to-mobile-paths
  "Explicit desktop→mobile path mappings for known routes."
  {"/"                "/m/dashboard"
   "/home"            "/m/dashboard"
   "/login"           "/m/login"
   "/forgot-password" "/m/forgot-password"
   "/tenant-select"   "/m/tenant-select"
   "/dashboard"       "/m/dashboard"
   "/onboarding"      "/m/dashboard"})

(defn desktop-path->mobile
  "Map a desktop path to its mobile equivalent.
   Known paths get explicit mappings; others get /m prefix."
  [path]
  (or (get desktop-to-mobile-paths path)
      ;; For domain paths like /expenses/list → /m/expenses/list
    (str "/m" path)))

(defn wrap-mobile-redirect
  "Ring middleware that redirects phone user-agents to /m/* mobile routes.
   Tablets and desktops pass through unchanged.

   Skips redirect for: API, admin, static assets, already-mobile paths."
  [handler]
  (fn [request]
    (let [ua (get-in request [:headers "user-agent"])
          path (:uri request)
          query-string (:query-string request)
          method (:request-method request)]
      (if (and (= method :get)
            (phone-ua? ua)
            (not (skip-redirect? path query-string)))
        {:status 302
         :headers {"Location" (desktop-path->mobile path)
                   "Cache-Control" "no-cache, no-store"}
         :body ""}
        (handler request)))))
