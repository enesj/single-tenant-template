(ns app.template.backend.routes.mobile
  "Mobile SPA routes (/m/*) — serves mobile-index.html for all mobile frontend paths."
  (:require
    [clojure.java.io :as io]
    [clojure.string :as str]
    [ring.middleware.anti-forgery :refer [*anti-forgery-token*]]))

(defn render-mobile-page
  "Serve the mobile SPA HTML with CSRF token injected."
  [_]
  (let [html-content (slurp (io/resource "public/mobile-index.html"))
        csrf-token (when (bound? #'*anti-forgery-token*)
                     *anti-forgery-token*)
        html-with-csrf (str/replace html-content "{{csrf-token}}" (or csrf-token ""))]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body html-with-csrf}))

(def mobile-spa-routes
  "Backend SPA fallback routes for the mobile frontend.
   All /m/* paths serve the same mobile SPA HTML — client-side routing handles the rest."
  ["/m"
   ;; Auth
   ["/login" {:get {:handler render-mobile-page}}]
   ["/forgot-password" {:get {:handler render-mobile-page}}]
   ["/tenant-select" {:get {:handler render-mobile-page}}]

   ;; Main tabs
   ["/dashboard" {:get {:handler render-mobile-page}}]
   ["/expenses" {:get {:handler render-mobile-page}}]
   ["/expenses/:id" {:get {:handler render-mobile-page}}]
   ["/reports" {:get {:handler render-mobile-page}}]
   ["/receipts" {:get {:handler render-mobile-page}}]
   ["/search" {:get {:handler render-mobile-page}}]

   ;; Capture flow
   ["/upload" {:get {:handler render-mobile-page}}]
   ["/upload/review" {:get {:handler render-mobile-page}}]
   ["/upload/review/:id" {:get {:handler render-mobile-page}}]
   ["/upload/manual" {:get {:handler render-mobile-page}}]

   ;; More menu
   ["/more" {:get {:handler render-mobile-page}}]

   ;; Catch-all for any other mobile paths
   ["/*path" {:get {:handler render-mobile-page}}]])
