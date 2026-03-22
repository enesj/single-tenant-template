(ns app.template.frontend.pages.home
  (:require
    [app.template.frontend.components.button :refer [button]]
    [app.template.frontend.i18n :refer [use-t]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui]]))

;; ---------------------------------------------------------------------------
;; Inline SVG icons (no external deps)
;; ---------------------------------------------------------------------------

(defui icon-receipt []
  ($ :svg {:xmlns "http://www.w3.org/2000/svg" :fill "none" :viewBox "0 0 24 24"
           :stroke-width "1.5" :stroke "currentColor" :class "w-8 h-8"}
    ($ :path {:stroke-linecap "round" :stroke-linejoin "round"
              :d "M9 14.25l6-6m4.5-3.493V21.75l-3.75-1.5-3.75 1.5-3.75-1.5-3.75 1.5V4.757c0-1.108.806-2.057 1.907-2.185a48.507 48.507 0 0111.186 0c1.1.128 1.907 1.077 1.907 2.185zM9.75 9h.008v.008H9.75V9zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm4.125 4.5h.008v.008h-.008V13.5zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0z"})))

(defui icon-chart []
  ($ :svg {:xmlns "http://www.w3.org/2000/svg" :fill "none" :viewBox "0 0 24 24"
           :stroke-width "1.5" :stroke "currentColor" :class "w-8 h-8"}
    ($ :path {:stroke-linecap "round" :stroke-linejoin "round"
              :d "M3 13.125C3 12.504 3.504 12 4.125 12h2.25c.621 0 1.125.504 1.125 1.125v6.75C7.5 20.496 6.996 21 6.375 21h-2.25A1.125 1.125 0 013 19.875v-6.75zM9.75 8.625c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125v11.25c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V8.625zM16.5 4.125c0-.621.504-1.125 1.125-1.125h2.25C20.496 3 21 3.504 21 4.125v15.75c0 .621-.504 1.125-1.125 1.125h-2.25a1.125 1.125 0 01-1.125-1.125V4.125z"})))

(defui icon-users []
  ($ :svg {:xmlns "http://www.w3.org/2000/svg" :fill "none" :viewBox "0 0 24 24"
           :stroke-width "1.5" :stroke "currentColor" :class "w-8 h-8"}
    ($ :path {:stroke-linecap "round" :stroke-linejoin "round"
              :d "M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z"})))

(defui icon-tag []
  ($ :svg {:xmlns "http://www.w3.org/2000/svg" :fill "none" :viewBox "0 0 24 24"
           :stroke-width "1.5" :stroke "currentColor" :class "w-8 h-8"}
    ($ :path {:stroke-linecap "round" :stroke-linejoin "round"
              :d "M9.568 3H5.25A2.25 2.25 0 003 5.25v4.318c0 .597.237 1.17.659 1.591l9.581 9.581c.699.699 1.78.872 2.607.33a18.095 18.095 0 005.223-5.223c.542-.827.369-1.908-.33-2.607L11.16 3.66A2.25 2.25 0 009.568 3z"})
    ($ :path {:stroke-linecap "round" :stroke-linejoin "round"
              :d "M6 6h.008v.008H6V6z"})))

(defui icon-camera []
  ($ :svg {:xmlns "http://www.w3.org/2000/svg" :fill "none" :viewBox "0 0 24 24"
           :stroke-width "1.5" :stroke "currentColor" :class "w-6 h-6"}
    ($ :path {:stroke-linecap "round" :stroke-linejoin "round"
              :d "M6.827 6.175A2.31 2.31 0 015.186 7.23c-.38.054-.757.112-1.134.175C2.999 7.58 2.25 8.507 2.25 9.574V18a2.25 2.25 0 002.25 2.25h15A2.25 2.25 0 0021.75 18V9.574c0-1.067-.75-1.994-1.802-2.169a47.865 47.865 0 00-1.134-.175 2.31 2.31 0 01-1.64-1.055l-.822-1.316a2.192 2.192 0 00-1.736-1.039 48.774 48.774 0 00-5.232 0 2.192 2.192 0 00-1.736 1.039l-.821 1.316z"})
    ($ :path {:stroke-linecap "round" :stroke-linejoin "round"
              :d "M16.5 12.75a4.5 4.5 0 11-9 0 4.5 4.5 0 019 0z"})))

(defui icon-workspace []
  ($ :svg {:xmlns "http://www.w3.org/2000/svg" :fill "none" :viewBox "0 0 24 24"
           :stroke-width "1.5" :stroke "currentColor" :class "w-6 h-6"}
    ($ :path {:stroke-linecap "round" :stroke-linejoin "round"
              :d "M2.25 12l8.954-8.955c.44-.439 1.152-.439 1.591 0L21.75 12M4.5 9.75v10.125c0 .621.504 1.125 1.125 1.125H9.75v-4.875c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21h4.125c.621 0 1.125-.504 1.125-1.125V9.75M8.25 21h8.25"})))

(defui icon-trending []
  ($ :svg {:xmlns "http://www.w3.org/2000/svg" :fill "none" :viewBox "0 0 24 24"
           :stroke-width "1.5" :stroke "currentColor" :class "w-6 h-6"}
    ($ :path {:stroke-linecap "round" :stroke-linejoin "round"
              :d "M2.25 18L9 11.25l4.306 4.307a11.95 11.95 0 015.814-5.519l2.74-1.22m0 0l-5.94-2.28m5.94 2.28l-2.28 5.941"})))

;; ---------------------------------------------------------------------------
;; Navigation bar
;; ---------------------------------------------------------------------------

(defui landing-navbar [{:keys [t]}]
  ($ :nav {:class "sticky top-0 z-50 bg-base-100/80 backdrop-blur-lg border-b border-base-200"}
    ($ :div {:class "max-w-6xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between"}
      ;; Logo
      ($ :a {:href "/" :class "flex items-center gap-2 font-bold text-xl text-base-content"}
        ($ :div {:class "w-8 h-8 rounded-lg bg-primary flex items-center justify-center"}
          ($ :svg {:xmlns "http://www.w3.org/2000/svg" :fill "none" :viewBox "0 0 24 24"
                   :stroke-width "2" :stroke "white" :class "w-5 h-5"}
            ($ :path {:stroke-linecap "round" :stroke-linejoin "round"
                      :d "M2.25 18.75a60.07 60.07 0 0115.797 2.101c.727.198 1.453-.342 1.453-1.096V18.75M3.75 4.5v.75A.75.75 0 013 6h-.75m0 0v-.375c0-.621.504-1.125 1.125-1.125H20.25M2.25 6v9m18-10.5v.75c0 .414.336.75.75.75h.75m-1.5-1.5h.375c.621 0 1.125.504 1.125 1.125v9.75c0 .621-.504 1.125-1.125 1.125h-.375m1.5-1.5H21a.75.75 0 00-.75.75v.75m0 0H3.75m0 0h-.375a1.125 1.125 0 01-1.125-1.125V15m1.5 1.5v-.75A.75.75 0 003 15h-.75M15 10.5a3 3 0 11-6 0 3 3 0 016 0zm3 0h.008v.008H18V10.5zm-12 0h.008v.008H6V10.5z"})))
        "Troskovi")

      ;; Right side: login + register
      ($ :div {:class "flex items-center gap-3"}
        ($ :a {:href "/login"
               :class "ds-btn ds-btn-ghost ds-btn-sm text-base-content"}
          (t :landing/nav-login))
        ($ button {:btn-type :primary
                   :class "ds-btn-sm"
                   :on-click #(rf/dispatch [:navigate-to "/register"])}
          (t :landing/nav-register))))))

;; ---------------------------------------------------------------------------
;; Hero section
;; ---------------------------------------------------------------------------

(defui hero-section [{:keys [t]}]
  ($ :section {:class "relative overflow-hidden"}
    ;; Background gradient
    ($ :div {:class "absolute inset-0 bg-gradient-to-br from-primary/5 via-base-100 to-secondary/5"})
    ;; Decorative circles
    ($ :div {:class "absolute -top-24 -right-24 w-96 h-96 rounded-full bg-primary/5 blur-3xl"})
    ($ :div {:class "absolute -bottom-24 -left-24 w-96 h-96 rounded-full bg-secondary/5 blur-3xl"})

    ($ :div {:class "relative max-w-6xl mx-auto px-4 sm:px-6 py-20 sm:py-28"}
      ($ :div {:class "grid lg:grid-cols-2 gap-12 items-center"}
        ;; Left: copy
        ($ :div {:class "space-y-6"}
          ($ :div {:class "inline-flex items-center gap-2 px-3 py-1 rounded-full bg-primary/10 text-primary text-sm font-medium"}
            ($ icon-trending)
            (t :landing/eyebrow))
          ($ :h1 {:class "text-4xl sm:text-5xl lg:text-6xl font-extrabold text-base-content leading-tight tracking-tight"}
            (t :landing/headline))
          ($ :p {:class "text-lg sm:text-xl text-base-content/70 max-w-lg leading-relaxed"}
            (t :landing/subheadline))
          ($ :div {:class "flex flex-col sm:flex-row items-start sm:items-center gap-4 pt-2"}
            ($ button {:btn-type :primary
                       :class "ds-btn-lg shadow-lg shadow-primary/25"
                       :on-click #(rf/dispatch [:navigate-to "/register"])}
              (t :landing/cta-primary))
            ($ :a {:href "/login"
                   :class "text-base-content/60 hover:text-primary transition-colors text-sm"}
              (t :landing/cta-login) " " ($ :span {:class "font-semibold underline underline-offset-2"} (t :landing/nav-login)))))

        ;; Right: mockup dashboard card
        ($ :div {:class "hidden lg:block"}
          ($ :div {:class "relative"}
            ;; Shadow/glow behind card
            ($ :div {:class "absolute inset-4 bg-primary/10 rounded-3xl blur-2xl"})
            ($ :div {:class "relative bg-base-100 rounded-2xl shadow-2xl border border-base-200 p-6 space-y-4"}
              ;; Mini header
              ($ :div {:class "flex items-center justify-between"}
                ($ :div {:class "flex items-center gap-2"}
                  ($ :div {:class "w-3 h-3 rounded-full bg-error"})
                  ($ :div {:class "w-3 h-3 rounded-full bg-warning"})
                  ($ :div {:class "w-3 h-3 rounded-full bg-success"}))
                ($ :div {:class "text-xs text-base-content/40"} "Nadzorna ploca"))
              ;; Summary cards row
              ($ :div {:class "grid grid-cols-2 gap-3"}
                ($ :div {:class "bg-base-200/50 rounded-xl p-3"}
                  ($ :div {:class "text-xs text-base-content/50 mb-1"} "30 dana")
                  ($ :div {:class "text-xl font-bold text-base-content"} "3,150 KM")
                  ($ :div {:class "text-xs text-success font-medium"} "+8.2%"))
                ($ :div {:class "bg-base-200/50 rounded-xl p-3"}
                  ($ :div {:class "text-xs text-base-content/50 mb-1"} "6 mjeseci")
                  ($ :div {:class "text-xl font-bold text-base-content"} "18,200 KM")
                  ($ :div {:class "text-xs text-base-content/40"} "810 troskova")))
              ;; Fake chart bars — monthly spend ~2800-3200 KM range
              ($ :div {:class "space-y-1"}
                ($ :div {:class "text-xs text-base-content/50 mb-2"} "Mjesecni trend")
                ($ :div {:class "flex items-end gap-2 h-16"}
                  ($ :div {:class "flex-1 bg-primary/30 rounded-t h-[87%]"})
                  ($ :div {:class "flex-1 bg-primary/50 rounded-t h-[97%]"})
                  ($ :div {:class "flex-1 bg-primary/40 rounded-t h-[92%]"})
                  ($ :div {:class "flex-1 bg-primary/70 rounded-t h-[100%]"})
                  ($ :div {:class "flex-1 bg-primary/30 rounded-t h-[86%]"})
                  ($ :div {:class "flex-1 bg-primary rounded-t h-[98%]"})))
              ;; Fake categories
              ($ :div {:class "space-y-2"}
                ($ :div {:class "flex items-center justify-between"}
                  ($ :span {:class "text-xs text-base-content/60"} "Hrana")
                  ($ :span {:class "text-xs font-medium"} "1,890 KM"))
                ($ :div {:class "w-full bg-base-200 rounded-full h-2"}
                  ($ :div {:class "bg-primary rounded-full h-2" :style {:width "60%"}}))
                ($ :div {:class "flex items-center justify-between"}
                  ($ :span {:class "text-xs text-base-content/60"} "Kucanstvo")
                  ($ :span {:class "text-xs font-medium"} "630 KM"))
                ($ :div {:class "w-full bg-base-200 rounded-full h-2"}
                  ($ :div {:class "bg-secondary rounded-full h-2" :style {:width "30%"}}))))))))))

;; ---------------------------------------------------------------------------
;; How it works
;; ---------------------------------------------------------------------------

(defui step-card [{:keys [number icon title desc]}]
  ($ :div {:class "relative flex flex-col items-center text-center space-y-4 p-6"}
    ;; Step number badge
    ($ :div {:class "w-12 h-12 rounded-full bg-primary/10 text-primary flex items-center justify-center text-lg font-bold"}
      number)
    ;; Icon
    ($ :div {:class "text-primary"}
      icon)
    ($ :h3 {:class "text-lg font-semibold text-base-content"} title)
    ($ :p {:class "text-sm text-base-content/60 max-w-xs"} desc)))

(defui how-it-works-section [{:keys [t]}]
  ($ :section {:class "py-20 bg-base-200/30"}
    ($ :div {:class "max-w-6xl mx-auto px-4 sm:px-6"}
      ($ :h2 {:class "text-3xl font-bold text-center text-base-content mb-16"}
        (t :landing/how-it-works))
      ($ :div {:class "grid md:grid-cols-3 gap-8 relative"}
        ;; Connecting line (desktop only)
        ($ :div {:class "hidden md:block absolute top-24 left-1/6 right-1/6 h-0.5 bg-gradient-to-r from-transparent via-primary/20 to-transparent"})
        ($ step-card {:number "1"
                      :icon ($ icon-workspace)
                      :title (t :landing/step-1-title)
                      :desc (t :landing/step-1-desc)})
        ($ step-card {:number "2"
                      :icon ($ icon-camera)
                      :title (t :landing/step-2-title)
                      :desc (t :landing/step-2-desc)})
        ($ step-card {:number "3"
                      :icon ($ icon-trending)
                      :title (t :landing/step-3-title)
                      :desc (t :landing/step-3-desc)})))))

;; ---------------------------------------------------------------------------
;; Features
;; ---------------------------------------------------------------------------

(defui feature-card [{:keys [icon title desc]}]
  ($ :div {:class "bg-base-100 rounded-2xl border border-base-200 p-6 space-y-4 hover:shadow-lg hover:border-primary/20 transition-all duration-300 group"}
    ($ :div {:class "w-14 h-14 rounded-xl bg-primary/10 text-primary flex items-center justify-center group-hover:bg-primary/20 transition-colors"}
      icon)
    ($ :h3 {:class "text-lg font-semibold text-base-content"} title)
    ($ :p {:class "text-sm text-base-content/60 leading-relaxed"} desc)))

(defui features-section [{:keys [t]}]
  ($ :section {:class "py-20"}
    ($ :div {:class "max-w-6xl mx-auto px-4 sm:px-6"}
      ($ :h2 {:class "text-3xl font-bold text-center text-base-content mb-4"}
        (t :landing/features-title))
      ($ :p {:class "text-center text-base-content/60 mb-12 max-w-2xl mx-auto"}
        (t :landing/subheadline))
      ($ :div {:class "grid sm:grid-cols-2 lg:grid-cols-4 gap-6"}
        ($ feature-card {:icon ($ icon-receipt)
                         :title (t :landing/feat-ocr-title)
                         :desc (t :landing/feat-ocr-desc)})
        ($ feature-card {:icon ($ icon-tag)
                         :title (t :landing/feat-categories-title)
                         :desc (t :landing/feat-categories-desc)})
        ($ feature-card {:icon ($ icon-users)
                         :title (t :landing/feat-team-title)
                         :desc (t :landing/feat-team-desc)})
        ($ feature-card {:icon ($ icon-chart)
                         :title (t :landing/feat-reports-title)
                         :desc (t :landing/feat-reports-desc)})))))

;; ---------------------------------------------------------------------------
;; Dashboard preview
;; ---------------------------------------------------------------------------

(defui preview-section [{:keys [t]}]
  ($ :section {:class "py-20 bg-base-200/30 overflow-hidden"}
    ($ :div {:class "max-w-6xl mx-auto px-4 sm:px-6 text-center"}
      ($ :h2 {:class "text-3xl font-bold text-base-content mb-4"}
        (t :landing/preview-caption))
      ($ :p {:class "text-base-content/60 mb-12 max-w-2xl mx-auto"}
        (t :landing/feat-reports-desc))
      ;; Dashboard mockup — larger version
      ($ :div {:class "relative mx-auto max-w-4xl"}
        ($ :div {:class "absolute inset-8 bg-primary/5 rounded-3xl blur-3xl"})
        ($ :div {:class "relative bg-base-100 rounded-2xl shadow-2xl border border-base-200 overflow-hidden"}
          ;; Browser chrome
          ($ :div {:class "flex items-center gap-2 px-4 py-3 bg-base-200/50 border-b border-base-200"}
            ($ :div {:class "flex gap-1.5"}
              ($ :div {:class "w-3 h-3 rounded-full bg-error/60"})
              ($ :div {:class "w-3 h-3 rounded-full bg-warning/60"})
              ($ :div {:class "w-3 h-3 rounded-full bg-success/60"}))
            ($ :div {:class "flex-1 mx-4"}
              ($ :div {:class "bg-base-300/50 rounded-lg px-3 py-1 text-xs text-base-content/40 text-center max-w-xs mx-auto"}
                "troskovi.app/dashboard")))
          ;; Dashboard content
          ($ :div {:class "p-6 space-y-4"}
            ;; Top row: summary cards
            ($ :div {:class "grid grid-cols-3 gap-4"}
              ($ :div {:class "bg-base-200/30 rounded-xl p-4 text-left"}
                ($ :div {:class "text-xs text-base-content/50 mb-1"} "Zadnjih 30 dana")
                ($ :div {:class "text-2xl font-bold"} "3,150 KM")
                ($ :div {:class "text-xs text-success"} "+8.2% vs prosli period"))
              ($ :div {:class "bg-base-200/30 rounded-xl p-4 text-left"}
                ($ :div {:class "text-xs text-base-content/50 mb-1"} "Zadnjih 6 mjeseci")
                ($ :div {:class "text-2xl font-bold"} "18,200 KM")
                ($ :div {:class "text-xs text-base-content/50"} "810 troskova"))
              ($ :div {:class "bg-base-200/30 rounded-xl p-4 text-left"}
                ($ :div {:class "text-xs text-base-content/50 mb-1"} "Prosjek / sedmica")
                ($ :div {:class "text-2xl font-bold"} "700 KM")
                ($ :div {:class "text-xs text-base-content/50"} "~100 KM / dan")))
            ;; Chart area — monthly ~2800-3200 KM
            ($ :div {:class "bg-base-200/20 rounded-xl p-4"}
              ($ :div {:class "text-sm font-medium text-base-content/70 mb-3"} "Mjesecni trend")
              ($ :div {:class "flex items-end gap-3 h-24"}
                ($ :div {:class "flex-1 flex flex-col items-center gap-1"}
                  ($ :div {:class "text-[10px] text-base-content/40"} "2,800 KM")
                  ($ :div {:class "w-full bg-primary/30 rounded-t" :style {:height "87%"}}))
                ($ :div {:class "flex-1 flex flex-col items-center gap-1"}
                  ($ :div {:class "text-[10px] text-base-content/40"} "3,100 KM")
                  ($ :div {:class "w-full bg-primary/50 rounded-t" :style {:height "97%"}}))
                ($ :div {:class "flex-1 flex flex-col items-center gap-1"}
                  ($ :div {:class "text-[10px] text-base-content/40"} "2,950 KM")
                  ($ :div {:class "w-full bg-primary/40 rounded-t" :style {:height "92%"}}))
                ($ :div {:class "flex-1 flex flex-col items-center gap-1"}
                  ($ :div {:class "text-[10px] text-base-content/40"} "3,200 KM")
                  ($ :div {:class "w-full bg-primary/70 rounded-t" :style {:height "100%"}}))
                ($ :div {:class "flex-1 flex flex-col items-center gap-1"}
                  ($ :div {:class "text-[10px] text-base-content/40"} "2,750 KM")
                  ($ :div {:class "w-full bg-primary/30 rounded-t" :style {:height "86%"}}))
                ($ :div {:class "flex-1 flex flex-col items-center gap-1"}
                  ($ :div {:class "text-[10px] text-base-content/40"} "3,150 KM")
                  ($ :div {:class "w-full bg-primary rounded-t" :style {:height "98%"}}))))
            ;; Bottom row: top suppliers + categories
            ($ :div {:class "grid grid-cols-2 gap-4"}
              ($ :div {:class "bg-base-200/30 rounded-xl p-4 text-left"}
                ($ :div {:class "text-sm font-medium text-base-content/70 mb-3"} "Top dobavljaci")
                ($ :div {:class "space-y-2"}
                  (for [[i [supplier-name amount]] (map-indexed vector [["BINGO" "1,250 KM"] ["KONZUM" "680 KM"] ["DM" "420 KM"]])]
                    ($ :div {:key i :class "flex items-center gap-2"}
                      ($ :div {:class "w-5 h-5 rounded-full bg-primary/20 text-primary text-[10px] font-bold flex items-center justify-center"}
                        (str (inc i)))
                      ($ :span {:class "text-xs flex-1 text-base-content/70"} supplier-name)
                      ($ :span {:class "text-xs font-medium"} amount)))))
              ($ :div {:class "bg-base-200/30 rounded-xl p-4 text-left"}
                ($ :div {:class "text-sm font-medium text-base-content/70 mb-3"} "Kategorije")
                ($ :div {:class "space-y-2"}
                  ($ :div
                    ($ :div {:class "flex justify-between text-xs mb-1"}
                      ($ :span {:class "text-base-content/60"} "Hrana")
                      ($ :span {:class "font-medium"} "60%"))
                    ($ :div {:class "w-full bg-base-300 rounded-full h-1.5"}
                      ($ :div {:class "bg-primary rounded-full h-1.5" :style {:width "60%"}})))
                  ($ :div
                    ($ :div {:class "flex justify-between text-xs mb-1"}
                      ($ :span {:class "text-base-content/60"} "Kucanstvo")
                      ($ :span {:class "font-medium"} "20%"))
                    ($ :div {:class "w-full bg-base-300 rounded-full h-1.5"}
                      ($ :div {:class "bg-secondary rounded-full h-1.5" :style {:width "20%"}})))
                  ($ :div
                    ($ :div {:class "flex justify-between text-xs mb-1"}
                      ($ :span {:class "text-base-content/60"} "Lijekovi")
                      ($ :span {:class "font-medium"} "12%"))
                    ($ :div {:class "w-full bg-base-300 rounded-full h-1.5"}
                      ($ :div {:class "bg-accent rounded-full h-1.5" :style {:width "12%"}})))
                  ($ :div
                    ($ :div {:class "flex justify-between text-xs mb-1"}
                      ($ :span {:class "text-base-content/60"} "Odjeca")
                      ($ :span {:class "font-medium"} "8%"))
                    ($ :div {:class "w-full bg-base-300 rounded-full h-1.5"}
                      ($ :div {:class "bg-info rounded-full h-1.5" :style {:width "8%"}}))))))))))))

;; ---------------------------------------------------------------------------
;; Mobile section
;; ---------------------------------------------------------------------------

(defui icon-smartphone []
  ($ :svg {:xmlns "http://www.w3.org/2000/svg" :fill "none" :viewBox "0 0 24 24"
           :stroke-width "1.5" :stroke "currentColor" :class "w-5 h-5"}
    ($ :path {:stroke-linecap "round" :stroke-linejoin "round"
              :d "M10.5 1.5H8.25A2.25 2.25 0 006 3.75v16.5a2.25 2.25 0 002.25 2.25h7.5A2.25 2.25 0 0018 20.25V3.75a2.25 2.25 0 00-2.25-2.25H13.5m-3 0V3h3V1.5m-3 0h3m-3 18.75h3"})))

(defui icon-check []
  ($ :svg {:xmlns "http://www.w3.org/2000/svg" :fill "none" :viewBox "0 0 24 24"
           :stroke-width "2" :stroke "currentColor" :class "w-4 h-4"}
    ($ :path {:stroke-linecap "round" :stroke-linejoin "round"
              :d "M4.5 12.75l6 6 9-13.5"})))

(defui mobile-section [{:keys [t]}]
  ($ :section {:class "py-20"}
    ($ :div {:class "max-w-6xl mx-auto px-4 sm:px-6"}
      ($ :div {:class "grid lg:grid-cols-2 gap-12 items-center"}
        ;; Left: phone mockup
        ($ :div {:class "flex justify-center lg:justify-end order-2 lg:order-1"}
          ($ :div {:class "relative"}
            ($ :div {:class "absolute inset-4 bg-secondary/10 rounded-3xl blur-2xl"})
            ($ :div {:class "relative w-64 bg-base-100 rounded-[2rem] shadow-2xl border-4 border-base-300 p-3 mx-auto"}
              ;; Phone status bar
              ($ :div {:class "flex justify-center mb-2"}
                ($ :div {:class "w-20 h-1 bg-base-300 rounded-full"}))
              ;; Phone screen content
              ($ :div {:class "bg-base-200/30 rounded-2xl p-4 space-y-3"}
                ;; Mini header
                ($ :div {:class "flex items-center justify-between mb-2"}
                  ($ :span {:class "text-xs font-bold text-base-content"} "Troskovi")
                  ($ :div {:class "w-6 h-6 rounded-full bg-primary/20"}))
                ;; Quick action buttons
                ($ :div {:class "grid grid-cols-2 gap-2"}
                  ($ :div {:class "bg-primary/10 rounded-xl p-3 text-center"}
                    ($ :div {:class "text-primary mx-auto mb-1 flex justify-center"} ($ icon-camera))
                    ($ :div {:class "text-[10px] text-base-content/70"} "Slikaj racun"))
                  ($ :div {:class "bg-secondary/10 rounded-xl p-3 text-center"}
                    ($ :div {:class "text-secondary mx-auto mb-1 flex justify-center"} ($ icon-receipt))
                    ($ :div {:class "text-[10px] text-base-content/70"} "Rucni unos")))
                ;; Mini expense list
                ($ :div {:class "space-y-2 mt-2"}
                  ($ :div {:class "text-[10px] text-base-content/50 font-medium"} "Danas")
                  (for [[i [item-name amount]] (map-indexed vector [["BINGO" "45.30 KM"] ["DM" "22.10 KM"] ["Apoteka" "18.50 KM"]])]
                    ($ :div {:key i :class "flex items-center justify-between bg-base-100 rounded-lg p-2"}
                      ($ :span {:class "text-[10px] text-base-content/70"} item-name)
                      ($ :span {:class "text-[10px] font-medium"} amount))))
                ;; Daily total
                ($ :div {:class "flex justify-between pt-2 border-t border-base-200"}
                  ($ :span {:class "text-[10px] text-base-content/50"} "Danas ukupno")
                  ($ :span {:class "text-xs font-bold"} "98.40 KM")))
              ;; Phone home indicator
              ($ :div {:class "flex justify-center mt-2"}
                ($ :div {:class "w-24 h-1 bg-base-300 rounded-full"})))))

        ;; Right: copy
        ($ :div {:class "space-y-6 order-1 lg:order-2"}
          ($ :div {:class "inline-flex items-center gap-2 px-3 py-1 rounded-full bg-secondary/10 text-secondary text-sm font-medium"}
            ($ icon-smartphone)
            (t :landing/mobile-coming-soon))
          ($ :h2 {:class "text-3xl sm:text-4xl font-bold text-base-content"}
            (t :landing/mobile-title))
          ($ :p {:class "text-lg text-base-content/70 max-w-lg"}
            (t :landing/mobile-desc))
          ;; Feature checklist
          ($ :div {:class "space-y-3"}
            (for [feat-key [:landing/mobile-feat-camera
                            :landing/mobile-feat-manual
                            :landing/mobile-feat-reports
                            :landing/mobile-feat-settings]]
              ($ :div {:key (name feat-key) :class "flex items-center gap-3"}
                ($ :div {:class "w-6 h-6 rounded-full bg-success/10 text-success flex items-center justify-center flex-shrink-0"}
                  ($ icon-check))
                ($ :span {:class "text-base-content/70"} (t feat-key))))))))))

;; ---------------------------------------------------------------------------
;; Bottom CTA
;; ---------------------------------------------------------------------------

(defui bottom-cta-section [{:keys [t]}]
  ($ :section {:class "py-20 relative overflow-hidden"}
    ($ :div {:class "absolute inset-0 bg-gradient-to-br from-primary/5 via-base-100 to-secondary/5"})
    ($ :div {:class "absolute top-12 right-12 w-64 h-64 rounded-full bg-primary/5 blur-3xl"})
    ($ :div {:class "relative max-w-2xl mx-auto px-4 sm:px-6 text-center space-y-6"}
      ($ :h2 {:class "text-3xl sm:text-4xl font-bold text-base-content"}
        (t :landing/bottom-cta-title))
      ($ :p {:class "text-base-content/60 text-lg"}
        (t :landing/bottom-cta-subtitle))
      ($ :div {:class "flex flex-col sm:flex-row items-center justify-center gap-4"}
        ($ button {:btn-type :primary
                   :class "ds-btn-lg shadow-lg shadow-primary/25"
                   :on-click #(rf/dispatch [:navigate-to "/register"])}
          (t :landing/cta-primary))
        ($ :a {:href "/login"
               :class "text-base-content/60 hover:text-primary transition-colors text-sm"}
          (t :landing/cta-login) " " ($ :span {:class "font-semibold underline underline-offset-2"} (t :landing/nav-login)))))))

;; ---------------------------------------------------------------------------
;; Footer
;; ---------------------------------------------------------------------------

(defui landing-footer [{:keys [t]}]
  ($ :footer {:class "border-t border-base-200 py-8"}
    ($ :div {:class "max-w-6xl mx-auto px-4 sm:px-6 flex flex-col sm:flex-row items-center justify-between gap-4"}
      ($ :p {:class "text-sm text-base-content/50"}
        (t :landing/footer-copy))
      ($ :div {:class "flex items-center gap-4 text-sm text-base-content/50"}
        ($ :a {:href "/about" :class "hover:text-base-content transition-colors"} "O nama")
        ($ :span {:class "text-base-content/20"} "|")
        ($ :a {:href "/login" :class "hover:text-base-content transition-colors"} (t :landing/nav-login))))))

;; ---------------------------------------------------------------------------
;; Main page
;; ---------------------------------------------------------------------------

(defui home-page []
  (let [t (use-t)]
    ($ :div {:class "min-h-screen bg-base-100"}
      ($ landing-navbar {:t t})
      ($ hero-section {:t t})
      ($ how-it-works-section {:t t})
      ($ features-section {:t t})
      ($ preview-section {:t t})
      ($ mobile-section {:t t})
      ($ bottom-cta-section {:t t})
      ($ landing-footer {:t t}))))
