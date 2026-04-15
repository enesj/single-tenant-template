(ns app.mobile.frontend.auth-redirect
  "Pure helpers for deciding post-auth mobile navigation."
  (:require
    [clojure.string :as str]))

(defn auth-status-navigation-path
  "Return the path the mobile app should navigate to after auth status loads.
   This may be a hard redirect (for example to `/m/login`) or the current in-app
   path again when route state needs to be re-seeded after a hard refresh."
  [authenticated? tenant-required? current-view current-path]
  (let [auth-paths #{"/m" "/m/login" "/m/forgot-password" "/m/tenant-select"}
        auth-views #{:m/login :m/forgot-password :m/tenant-select}
        known-in-app-path? (and (string? current-path)
                             (or (= current-path "/m")
                               (str/starts-with? current-path "/m/")))]
    (cond
      tenant-required? "/m/tenant-select"
      (not authenticated?) "/m/login"
      (or (auth-paths current-path)
        (auth-views current-view)
        (and (nil? current-view) (not known-in-app-path?)))
      "/m/dashboard"
      (and (nil? current-view) known-in-app-path?)
      current-path
      :else nil)))
