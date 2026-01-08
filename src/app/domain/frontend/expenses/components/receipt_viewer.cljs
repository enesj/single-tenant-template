(ns app.domain.frontend.expenses.components.receipt-viewer
  "Receipt detail display for admin UI."
  (:require
    [app.admin.frontend.components.shared-utils :as shared]
    [app.template.frontend.components.json-highlight :refer [json-display-card]]
    [clojure.string :as str]
    [uix.core :refer [$ defui use-effect use-state]]))

(defn- admin-protected-url?
  [url]
  (and (string? url)
    (or (str/starts-with? url "/admin/api/")
      (str/includes? url "/admin/api/"))))

(defn- with-download-param
  [url]
  (when (seq url)
    (let [separator (if (str/includes? url "?") "&" "?")]
      (str url separator "download=true"))))

(defn- fetch-receipt-blob
  "Fetch a receipt file as a Blob.

  - Adds `x-admin-token` for admin-protected URLs (e.g. /admin/api/*).
  - Uses same-origin credentials for user-session cookies."
  [url opts]
  (let [opts* (or opts #js {})
        token (.getItem js/localStorage "admin-token")]
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

(defn- download-blob!
  [blob filename]
  (let [url (.createObjectURL js/URL blob)
        link (.createElement js/document "a")]
    (set! (.-href link) url)
    (set! (.-download link) filename)
    (.appendChild (.-body js/document) link)
    (.click link)
    (.remove link)
    ;; Revoke on a short delay so the download can start.
    (js/setTimeout (fn [] (.revokeObjectURL js/URL url)) 1000)))

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
  "Preview-only receipt card (image/pdf + Open/Download actions).

  Intended for reuse in layouts that already show metadata elsewhere (e.g. Approve tab)."
  [{:keys [receipt title expanded? on-toggle] :or {title "Preview"}}]
  (let [{:keys [id content-type original-filename download-url]} receipt
        rid-str (or (some-> id str) "unknown")
        download-href (with-download-param download-url)
        admin-protected? (admin-protected-url? download-url)
        previewable? (or (str/starts-with? (or content-type "") "image/")
                       (= content-type "application/pdf"))
        ;; Support both controlled and uncontrolled expansion state
        [local-expanded? set-local-expanded!] (use-state true)
        current-expanded? (if (some? expanded?) expanded? local-expanded?)
        [preview set-preview!] (use-state nil)
        [loading? set-loading!] (use-state false)
        [load-error set-load-error!] (use-state nil)
        preview-url (when (and (some? preview)
                            (= (:source-url preview) download-url))
                      (:blob-url preview))]
    ;; Clean up object URLs when preview changes/unmounts.
    (use-effect
      (fn []
        (fn []
          (when (seq (:blob-url preview))
            (.revokeObjectURL js/URL (:blob-url preview)))))
      [preview])

    ;; Reset preview state when the receipt (download-url) changes.
    (use-effect
      (fn []
        (set-preview! nil)
        (set-loading! false)
        (set-load-error! nil)
        js/undefined)
      [download-url])

    ;; Lazily fetch blob previews for admin-protected URLs (img/iframe can't send headers).
    (use-effect
      (fn []
        (if (and admin-protected?
              previewable?
              current-expanded?
              (seq download-url)
              (nil? preview-url)
              (nil? load-error))
          (let [controller (js/AbortController.)
                opts #js {:signal (.-signal controller)}]
            (set-loading! true)
            (-> (fetch-receipt-blob download-url opts)
              (.then (fn [blob]
                       (let [url (.createObjectURL js/URL blob)]
                         (set-preview! {:source-url download-url
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
      [admin-protected? previewable? current-expanded? download-url preview-url load-error])
    ($ :div {:class "ds-card ds-card-bordered bg-base-100"}
      ($ :div {:class "ds-card-body space-y-3"}
        ($ :div {:class "flex items-center justify-between gap-2"}
          ($ :h3 {:class "text-sm font-semibold"} title)
          (when (seq download-url)
            ($ :div {:class "flex items-center gap-2"}
              ($ :button {:id (str "btn-toggle-receipt-preview-" rid-str)
                          :type "button"
                          :class "ds-btn ds-btn-ghost ds-btn-xs"
                          :on-click (fn [e]
                                      (.preventDefault e)
                                      (.stopPropagation e)
                                      (if on-toggle
                                        (on-toggle)
                                        (set-local-expanded! (not local-expanded?))))}
                (if current-expanded? "Hide" "Show"))
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
                                   (-> (fetch-receipt-blob download-url #js {})
                                     (.then (fn [blob]
                                              (let [url (.createObjectURL js/URL blob)]
                                                (set-preview! {:source-url download-url
                                                               :blob-url url})
                                                ;; NOTE: async open may be popup-blocked; once preview-url is set,
                                                ;; the anchor will point to the blob URL and a second click works.
                                                (open-url-in-new-tab! url))))
                                     (.catch (fn [err]
                                               (when-not (= "AbortError" (.-name err))
                                                 (set-load-error! (or (.-message err) "Failed to open receipt.")))))
                                     (.finally (fn []
                                                 (set-loading! false))))))}
                "Open")
              ($ :a {:id (str "btn-download-receipt-" rid-str)
                     :href download-href
                     :class "ds-btn ds-btn-primary ds-btn-xs"
                     :on-click (when admin-protected?
                                 (fn [e]
                                   (.preventDefault e)
                                   (.stopPropagation e)
                                   (let [filename (or (some-> original-filename str)
                                                    (str "receipt-" rid-str))]
                                     (if (seq preview-url)
                                       (let [link (.createElement js/document "a")]
                                         (set! (.-href link) preview-url)
                                         (set! (.-download link) filename)
                                         (.appendChild (.-body js/document) link)
                                         (.click link)
                                         (.remove link))
                                       (do
                                         (set-loading! true)
                                         (set-load-error! nil)
                                         (-> (fetch-receipt-blob (or download-href download-url) #js {})
                                           (.then (fn [blob]
                                                    (download-blob! blob filename)))
                                           (.catch (fn [err]
                                                     (when-not (= "AbortError" (.-name err))
                                                       (set-load-error! (or (.-message err) "Failed to download receipt.")))))
                                           (.finally (fn []
                                                       (set-loading! false)))))))))}
                "Download"))))

        (cond
          (not (seq download-url))
          ($ :p {:class "text-xs text-base-content/60"}
            "Receipt preview is not available without a download URL.")

          (not current-expanded?)
          ($ :div {:id (str "receipt-preview-collapsed-" rid-str)
                   :class "text-xs text-base-content/60"}
            "Preview hidden")

          (and admin-protected? previewable? (not (seq preview-url)))
          ($ :div {:class "w-full bg-base-200 rounded-lg p-6 flex items-center justify-center"}
            (cond
              loading?
              ($ :span {:class "ds-loading ds-loading-spinner ds-loading-md text-primary"})

              (seq load-error)
              ($ :div {:class "text-xs text-error"} load-error)

              :else
              ($ :span {:class "text-xs text-base-content/60"} "Loading preview…")))

          (str/starts-with? (or content-type "") "image/")
          ($ :div {:class "w-full bg-base-200 rounded-lg overflow-hidden"}
            ($ :img {:id (str "receipt-preview-img-" rid-str)
                     :src (or preview-url download-url)
                     :alt (or original-filename "Receipt image")
                     :class "w-full max-h-[70vh] object-contain"}))

          (= content-type "application/pdf")
          ($ :iframe {:id (str "receipt-preview-pdf-" rid-str)
                      :src (or preview-url download-url)
                      :title (or original-filename "Receipt PDF")
                      :class "w-full h-[70vh] rounded-lg bg-base-200"})

          :else
          ($ :p {:class "text-xs text-base-content/60"}
            "Preview is not available for this file type."))))))

(defui receipt-viewer
  [{:keys [receipt show-summary?] :or {show-summary? true}}]
  (let [{:keys [status original-filename content-type file-size storage-key
                supplier-guess total-amount-guess currency-guess purchased-at-guess
                error-message error-details raw-parse-json raw-extract-json
                parsed-markdown expense-id retry-count created-at updated-at]} receipt
        status-label (shared/format-value status "—" false)]
    ($ :div {:class "grid gap-6 lg:grid-cols-2"}
      ($ :div {:class "space-y-4"}
        (when show-summary?
          ($ :div {:class "ds-card ds-card-bordered bg-base-100"}
            ($ :div {:class "ds-card-body space-y-3"}
              ($ :div {:class "flex items-center gap-2"}
                ($ :span {:class (status-class status)} status-label)
                (when (seq error-message)
                  ($ :span {:class "text-xs text-error"} error-message)))
              ($ :div {:class "grid gap-3 md:grid-cols-2"}
                (label-value "Original File" original-filename)
                (label-value "Storage Key" storage-key)
                (label-value "Content Type" content-type)
                (label-value "File Size" (format-bytes file-size))
                (label-value "Supplier Guess" supplier-guess)
                (label-value "Total Guess" total-amount-guess)
                (label-value "Currency" currency-guess)
                (label-value "Purchased At" (shared/format-date purchased-at-guess))
                (label-value "Retry Count" retry-count)
                (label-value "Expense ID" expense-id)
                (label-value "Created At" (shared/format-date created-at))
                (label-value "Updated At" (shared/format-date updated-at))))))

        ($ receipt-preview {:receipt receipt})

        (when (seq parsed-markdown)
          ($ :div {:class "ds-card ds-card-bordered bg-base-100"}
            ($ :div {:class "ds-card-body"}
              ($ :h3 {:class "text-sm font-semibold"} "Parsed Markdown")
              ($ :pre {:class "text-xs whitespace-pre-wrap"} parsed-markdown)))))

      ($ :div {:class "space-y-4"}
        (when (seq raw-parse-json)
          ($ json-display-card
            {:title "Raw Parse JSON"
             :json-value raw-parse-json
             :max-height "max-h-96"}))
        (when (seq raw-extract-json)
          ($ json-display-card
            {:title "Raw Extract JSON"
             :json-value raw-extract-json
             :max-height "max-h-96"}))
        (when (seq error-details)
          ($ json-display-card
            {:title "Error Details"
             :json-value error-details
             :max-height "max-h-80"}))))))
