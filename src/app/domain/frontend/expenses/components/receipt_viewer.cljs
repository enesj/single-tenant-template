(ns app.domain.frontend.expenses.components.receipt-viewer
  "Receipt detail display for admin UI."
  (:require
    [app.admin.frontend.auth.persistence :as auth-persist]
    [app.template.frontend.components.shared-utils :as shared]
    [app.template.frontend.components.json-highlight :refer [json-display-card]]
    [clojure.string :as str]
    [uix.core :refer [$ defui use-effect use-state]]))

(defn- admin-protected-url?
  [url]
  (and (string? url)
    (or (str/starts-with? url "/admin/api/")
      (str/includes? url "/admin/api/"))))

(defn- fetch-receipt-blob
  "Fetch a receipt file as a Blob.

  - Adds `x-admin-token` for admin-protected URLs (e.g. /admin/api/*).
  - Uses same-origin credentials for user-session cookies."
  [url opts]
  (let [opts* (or opts #js {})
        token (auth-persist/get-persisted-token)]
    (when (and token (admin-protected-url? url))
      (set! (.-headers opts*) #js {"x-admin-token" token}))
    (set! (.-credentials opts*) "same-origin")
    (set! (.-cache opts*) "no-store")
    (-> (js/fetch url opts*)
      (.then (fn [resp]
               (if (.-ok resp)
                 (.blob resp)
                 (js/Promise.reject
                   (js/Error.
                     (str "Failed to load receipt (" (.-status resp) ")")))))))))

(defn- open-url-in-new-tab!
  [url]
  (when (seq url)
    (.open js/window url "_blank" "noopener,noreferrer")))

(defn- format-bytes
  [value]
  (let [bytes (cond
                (number? value) value
                (string? value) (js/parseFloat value)
                :else nil)
        kb 1024
        mb (* 1024 1024)]
    (cond
      (nil? bytes) "—"
      (< bytes kb) (str bytes " B")
      (< bytes mb) (str (.toFixed (/ bytes kb) 1) " KB")
      :else (str (.toFixed (/ bytes mb) 1) " MB"))))

(defn- status-class
  [status]
  (case status
    "uploaded" "ds-badge ds-badge-ghost"
    "parsing" "ds-badge ds-badge-info"
    "parsed" "ds-badge ds-badge-info"
    "extracting" "ds-badge ds-badge-warning"
    "extracted" "ds-badge ds-badge-success"
    "review_required" "ds-badge ds-badge-warning"
    "approved" "ds-badge ds-badge-success"
    "posted" "ds-badge ds-badge-success"
    "failed" "ds-badge ds-badge-error"
    "ds-badge"))

(defn- label-value
  [label value]
  ($ :div {:class "flex flex-col gap-1 p-3 bg-base-200 rounded-lg"}
    ($ :span {:class "text-xs uppercase tracking-wide text-base-content/70"} label)
    ($ :span {:class "text-sm font-medium"}
      (shared/format-value value "—" false))))

(defui receipt-preview
  "Preview-only receipt card (image/pdf + Open actions).

  Intended for reuse in layouts that already show metadata elsewhere (e.g. Approve tab)."
  [{:keys [receipt title expanded? preview-height-class]
    :or {title "Preview"
         preview-height-class "h-[70vh]"}}]
  (let [{:keys [id content-type original-filename download-url storage-key]} receipt
        rid-str (or (some-> id str) "unknown")
        admin-protected? (admin-protected-url? download-url)
        ;; Robust preview detection: by content-type OR by filename/storage-key extension
        filename (or original-filename storage-key "")
        ext (some-> filename (str/split #"\\.") last str/lower-case)
        image-exts #{"jpg" "jpeg" "png" "gif" "webp" "bmp" "tif" "tiff" "heic" "heif"}
        imageish? (or (str/starts-with? (or content-type "") "image/")
                    (contains? image-exts ext))
        pdfish? (or (= content-type "application/pdf") (= ext "pdf"))
        previewable? (or imageish? pdfish?)
        ct* (some-> content-type str str/trim str/lower-case)
        heicish? (or (contains? #{"heic" "heif"} ext)
                   (contains? #{"image/heic" "image/heif"} ct*))
        preview-download-url (when (seq download-url)
                               (cond
                                 (not imageish?) download-url
                                 :else
                                 (let [sep (if (str/includes? download-url "?") "&" "?")
                                       base (str download-url sep "preview=1")]
                                   (if heicish?
                                     (str base "&format=jpeg")
                                     base))))
        ;; Always expanded when expanded? is provided (no toggle)
        current-expanded? (if (some? expanded?) expanded? true)
        [zoom set-zoom!] (use-state 1.0)
        min-zoom 1.0
        max-zoom 4.0
        zoom-step 0.25
        [pan set-pan!] (use-state {:x 0 :y 0})
        [drag set-drag!] (use-state nil)
        dragging? (some? drag)
        [preview set-preview!] (use-state nil)
        [loading? set-loading!] (use-state false)
        [load-error set-load-error!] (use-state nil)
        preview-url (when (and (some? preview)
                            (= (:source-url preview) preview-download-url))
                      (:blob-url preview))]
    ;; Clean up object URLs when preview changes/unmounts.
    (use-effect
      (fn []
        (fn []
          (when (seq (:blob-url preview))
            (.revokeObjectURL js/URL (:blob-url preview)))))
      [preview])

    ;; Reset preview state when the receipt changes.
    (use-effect
      (fn []
        (set-preview! nil)
        (set-loading! false)
        (set-load-error! nil)
        (set-zoom! 1.0)
        (set-pan! {:x 0 :y 0})
        (set-drag! nil)
        js/undefined)
      [preview-download-url download-url])

    (use-effect
      (fn []
        (when (= zoom 1.0)
          (set-pan! {:x 0 :y 0})
          (set-drag! nil))
        js/undefined)
      [zoom])

    ;; Lazily fetch blob previews for admin-protected URLs (img/iframe can't send headers).
    (use-effect
      (fn []
        (if (and admin-protected?
              previewable?
              current-expanded?
              (seq preview-download-url)
              (nil? preview-url)
              (nil? load-error))
          (let [controller (js/AbortController.)
                opts #js {:signal (.-signal controller)}]
            (set-loading! true)
            (-> (fetch-receipt-blob preview-download-url opts)
              (.then (fn [blob]
                       (let [url (.createObjectURL js/URL blob)]
                         (set-preview! {:source-url preview-download-url
                                        :blob-url url}))))
              (.catch (fn [err]
                        ;; Ignore abort errors.
                        (when-not (= "AbortError" (.-name err))
                          (set-load-error! (or (.-message err) "Failed to load preview.")))))
              (.finally (fn []
                          (set-loading! false))))
            (fn []
              (.abort controller)))
          js/undefined))
      [admin-protected? previewable? current-expanded? preview-download-url preview-url load-error])
    ($ :div {:class "flex flex-col h-full min-h-0"}
      ($ :div {:class "flex items-center justify-between gap-2 mb-2 px-2"}
        ($ :h3 {:class "text-sm font-semibold"} title)
        (when (seq download-url)
          ($ :div {:class "flex items-center gap-2"}
            (when (and imageish? current-expanded?)
              ($ :div {:class "flex items-center gap-1"}
                ($ :button {:id (str "btn-zoom-out-receipt-preview-" rid-str)
                            :type "button"
                            :disabled (<= zoom min-zoom)
                            :class "ds-btn ds-btn-ghost ds-btn-xs"
                            :on-click (fn [e]
                                        (.preventDefault e)
                                        (.stopPropagation e)
                                        (set-zoom! (max min-zoom (- zoom zoom-step))))}
                  "−")
                ($ :span {:class "text-xs text-base-content/70 w-12 text-center"}
                  (str (js/Math.round (* 100 zoom)) "%"))
                ($ :button {:id (str "btn-zoom-in-receipt-preview-" rid-str)
                            :type "button"
                            :disabled (>= zoom max-zoom)
                            :class "ds-btn ds-btn-ghost ds-btn-xs"
                            :on-click (fn [e]
                                        (.preventDefault e)
                                        (.stopPropagation e)
                                        (set-zoom! (min max-zoom (+ zoom zoom-step))))}
                  "+")
                ($ :button {:id (str "btn-zoom-reset-receipt-preview-" rid-str)
                            :type "button"
                            :disabled (= zoom 1.0)
                            :class "ds-btn ds-btn-ghost ds-btn-xs"
                            :on-click (fn [e]
                                        (.preventDefault e)
                                        (.stopPropagation e)
                                        (set-zoom! 1.0)
                                        (set-pan! {:x 0 :y 0})
                                        (set-drag! nil))}
                  "Reset")))
            ($ :a {:id (str "link-open-receipt-" rid-str)
                   :href (or preview-url download-url)
                   :target "_blank"
                   :rel "noopener noreferrer"
                   :class "ds-btn ds-btn-ghost ds-btn-xs"
                   :on-click (when (and admin-protected? (not (seq preview-url)))
                               (fn [e]
                                 (.preventDefault e)
                                 (.stopPropagation e)
                                 (set-loading! true)
                                 (set-load-error! nil)
                                 (-> (fetch-receipt-blob preview-download-url #js {})
                                   (.then (fn [blob]
                                            (let [url (.createObjectURL js/URL blob)]
                                              (set-preview! {:source-url preview-download-url
                                                             :blob-url url})
                                              ;; NOTE: async open may be popup-blocked; once preview-url is set,
                                              ;; the anchor will point to the blob URL and a second click works.
                                              (open-url-in-new-tab! url))))
                                   (.catch (fn [err]
                                             (when-not (= "AbortError" (.-name err))
                                               (set-load-error! (or (.-message err) "Failed to open receipt.")))))
                                   (.finally (fn []
                                               (set-loading! false))))))}
              "Open"))))

      (cond
        (not (seq download-url))
        ($ :p {:class "text-xs text-base-content/60"}
          "Receipt preview is not available without a download URL.")

        (not current-expanded?)
        ($ :div {:id (str "receipt-preview-collapsed-" rid-str)
                 :class "text-xs text-base-content/60"}
          "Preview hidden")

          ;; Prefer rendering image/pdf directly; blob preview will swap in when ready.
        imageish?
        (let [zoomed? (> zoom 1.0)
              img-src (or preview-url preview-download-url download-url)
              {:keys [x y]} pan
              cursor-class (cond
                             (not zoomed?) ""
                             dragging? "cursor-grabbing"
                             :else "cursor-grab")
              container-classes (str "w-full bg-base-200 rounded-lg "
                                  preview-height-class
                                  " overflow-hidden select-none flex items-start justify-center "
                                  cursor-class)
              img-style (when zoomed?
                          #js {:transform (str "translate(" x "px, " y "px) scale(" zoom ")")
                               :transformOrigin "50% 50%"
                               :willChange "transform"})]
          ($ :div {:id (str "receipt-preview-img-container-" rid-str)
                   :class container-classes
                   :style #js {:touchAction "none"}
                   :on-pointer-down (when zoomed?
                                      (fn [e]
                                        (.preventDefault e)
                                        (let [target (.-currentTarget e)
                                              pid (.-pointerId e)]
                                          (when target
                                            (try
                                              (.setPointerCapture target pid)
                                              (catch :default _ nil)))
                                          (set-drag! {:pointer-id pid
                                                      :start-x (.-clientX e)
                                                      :start-y (.-clientY e)
                                                      :pan-x x
                                                      :pan-y y}))))
                   :on-pointer-move (when zoomed?
                                      (fn [e]
                                        (when (and drag (= (.-pointerId e) (:pointer-id drag)))
                                          (.preventDefault e)
                                          (let [dx (- (.-clientX e) (:start-x drag))
                                                dy (- (.-clientY e) (:start-y drag))]
                                            (set-pan! {:x (+ (:pan-x drag) dx)
                                                       :y (+ (:pan-y drag) dy)})))))
                   :on-pointer-up (when zoomed?
                                    (fn [e]
                                      (when (and drag (= (.-pointerId e) (:pointer-id drag)))
                                        (.preventDefault e)
                                        (set-drag! nil))))
                   :on-pointer-cancel (when zoomed?
                                        (fn [e]
                                          (.preventDefault e)
                                          (set-drag! nil)))}
            ($ :img {:id (str "receipt-preview-img-" rid-str)
                     :src img-src
                     :alt (or original-filename "Receipt image")
                     :draggable false
                     :class "w-full h-full object-contain object-top"
                     :style img-style})))

        pdfish?
        ($ :iframe {:id (str "receipt-preview-pdf-" rid-str)
                    :src (or preview-url download-url)
                    :title (or original-filename "Receipt PDF")
                    :class (str "w-full " preview-height-class " rounded-lg bg-base-200")})

          ;; Fallback loading/error state for admin-protected previews while blob URL is preparing
        (and admin-protected? previewable? (not (seq preview-url)))
        ($ :div {:class "w-full bg-base-200 rounded-lg p-6 flex items-center justify-center"}
          (cond
            loading?
            ($ :span {:class "ds-loading ds-loading-spinner ds-loading-md text-primary"})

            (seq load-error)
            ($ :div {:class "text-xs text-error"} load-error)

            :else
            ($ :span {:class "text-xs text-base-content/60"} "Loading preview…")))

        :else
        ($ :p {:class "text-xs text-base-content/60"}
          "Preview is not available for this file type.")))))


