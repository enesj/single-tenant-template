(ns app.template.frontend.pages.home
  (:require
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]
    [app.template.frontend.components.button :refer [button]]))

(def quick-links
  [{:title "Open Admin"
    :desc "Manage users, settings, and entity specs"
    :href "/admin/login"
    :icon "🛠️"}
   {:title "Entities"
    :desc "Browse the CRUD scaffolding from the template"
    :href "/entities"
    :icon "📄"}
   {:title "Login"
    :desc "Start a session in the single-tenant shell"
    :href "/login"
    :icon "🔐"}])

(def features
  [{:title "Single-tenant ready"
    :copy "No tenant switching, no RLS—just a clean starting point for your app."
    :badge "Template"}
   {:title "Reloadable dev"
    :copy "BB + Shadow CLJS hot reload with watchers already wired in."
    :badge "DX"}
   {:title "Admin + UI kit"
    :copy "Prebuilt admin shell, list/form components, and validation pipeline."
    :badge "UI"}])

(defui feature-tile [{:keys [title copy badge]}]
  ($ :div {:class "bg-base-100 ds-card shadow-sm border border-base-200"}
    ($ :div {:class "ds-card-body space-y-2"}
      ($ :div {:class "text-xs ds-badge ds-badge-outline"} badge)
      ($ :h3 {:class "text-xl font-semibold"} title)
      ($ :p {:class "text-sm text-gray-600"} copy))))

(defui quick-link [{:keys [title desc href icon]}]
  ($ :div {:class "bg-white shadow-sm rounded-lg p-4 flex gap-3 items-start hover:shadow transition cursor-pointer"
           :on-click #(rf/dispatch [:navigate-to href])}
    ($ :div {:class "text-2xl"} icon)
    ($ :div
      ($ :h4 {:class "font-semibold text-gray-900"} title)
      ($ :p {:class "text-sm text-gray-600"} desc))))

(defui home-page []
  ($ :div {:class "min-h-screen bg-gradient-to-b from-slate-50 to-white"}
    ;; Hero
    ($ :div {:class "max-w-6xl mx-auto px-4 py-16"}
      ($ :div {:class "grid lg:grid-cols-2 gap-10 items-center"}
        ($ :div
          ($ :p {:class "text-sm uppercase tracking-wide text-blue-600 font-semibold mb-3"}
            "Single-tenant starter")
          ($ :h1 {:class "text-4xl sm:text-5xl font-bold text-slate-900 mb-4 leading-tight"}
            "Build fast with the template + admin shell")
          ($ :p {:class "text-lg text-slate-600 mb-6 max-w-xl"}
            "A focused starting point for a single-tenant Clojure/ClojureScript app with hot reload, admin UI, and CRUD scaffolding ready to extend.")
          ($ :div {:class "flex flex-wrap gap-3"}
            ($ button {:btn-type :primary
                       :class "ds-btn-lg"
                       :on-click #(rf/dispatch [:navigate-to "/admin/login"])}
              "Open Admin")
            ($ button {:btn-type :ghost
                       :class "ds-btn-lg"
                       :on-click #(rf/dispatch [:navigate-to "/entities"])}
              "View Entities")))
        ($ :div {:class "bg-white shadow-lg rounded-2xl p-6 border border-slate-100"}
          ($ :div {:class "text-sm text-slate-500 mb-3"} "What’s inside")
          ($ :ul {:class "space-y-2 text-sm text-slate-700 list-disc list-inside"}
            ($ :li "Admin shell with auth, list, and form components")
            ($ :li "CRUD routes + re-frame wiring ready to extend")
            ($ :li "Live-reload dev environment (bb run-app)")
            ($ :li "Template/shared libraries kept intact for reuse"))
          ($ :div {:class "mt-4 text-xs text-slate-500"}
            "Tip: start dev with `bb run-app` then open http://localhost:8080"))))

    ;; Features
    ($ :div {:class "max-w-6xl mx-auto px-4 pb-12"}
      ($ :div {:class "grid md:grid-cols-3 gap-4"}
        (for [{:keys [title copy badge]} features]
          ($ feature-tile {:key title :title title :copy copy :badge badge}))))

    ;; Quick links
    ($ :div {:class "max-w-6xl mx-auto px-4 pb-16"}
      ($ :div {:class "bg-base-100 rounded-2xl shadow-sm border border-base-200 p-6"}
        ($ :div {:class "flex items-center justify-between mb-4"}
          ($ :h2 {:class "text-xl font-semibold text-slate-900"} "Jump in")
          ($ :span {:class "text-xs text-slate-500"}
            "Everything runs at http://localhost:8080 by default"))
        ($ :div {:class "grid md:grid-cols-3 gap-4"}
          (for [{:keys [title] :as link} quick-links]
            ($ quick-link (assoc link :key title))))))))
