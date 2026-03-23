(ns app.mobile.frontend.pages.upload
  "Mobile upload page — camera/gallery capture, manual entry, pending reviews."
  (:require
    [ajax.core :as ajax]
    [app.mobile.frontend.components.header :refer [mobile-header]]
    [app.mobile.frontend.pages.receipt-review :refer [pending-receipts-section]]
    [app.template.frontend.api.http :as http]
    [app.template.frontend.i18n :refer [use-t]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-ref use-state] :as uix]
    [uix.re-frame :refer [use-subscribe]]))

;; ========================================================================
;; Upload events
;; ========================================================================

(rf/reg-event-fx
  :mobile/upload-receipt
  (fn [{:keys [db]} [_ file]]
    (let [form-data (js/FormData.)]
      (.append form-data "file" file)
      {:db (assoc-in db [:mobile :upload :loading?] true)
       :http-xhrio {:method :post
                    :uri "/api/v1/expenses/upload"
                    :body form-data
                    :format {:write identity :content-type false}
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success [:mobile/upload-receipt-success]
                    :on-failure [:mobile/upload-receipt-failure]}})))

(rf/reg-event-fx
  :mobile/upload-receipt-success
  (fn [{:keys [db]} [_ response]]
    {:db (-> db
           (assoc-in [:mobile :upload :loading?] false)
           (assoc-in [:mobile :upload :last-upload] response))
     :fx [[:dispatch [:mobile/show-toast "Receipt uploaded successfully"]]]}))

(rf/reg-event-fx
  :mobile/upload-receipt-failure
  (fn [{:keys [db]} [_ error]]
    {:db (-> db
           (assoc-in [:mobile :upload :loading?] false)
           (assoc-in [:mobile :upload :error]
             (or (http/extract-error-message error) "Upload failed")))}))

(rf/reg-event-db
  :mobile/show-toast
  (fn [db [_ message]]
    (assoc-in db [:mobile :toast] message)))

(rf/reg-event-db
  :mobile/clear-toast
  (fn [db _]
    (assoc-in db [:mobile :toast] nil)))

(rf/reg-sub
  :mobile/upload-loading?
  (fn [db _]
    (get-in db [:mobile :upload :loading?] false)))

(rf/reg-sub
  :mobile/upload-error
  (fn [db _]
    (get-in db [:mobile :upload :error])))

(rf/reg-sub
  :mobile/toast
  (fn [db _]
    (get-in db [:mobile :toast])))

;; ========================================================================
;; Components
;; ========================================================================

(defui capture-button [{:keys [id icon-path label sublabel on-click disabled?]}]
  ($ :button
    {:id id
     :type "button"
     :class (str "flex items-center w-full p-4 bg-base-100 rounded-xl shadow-sm "
              "active:bg-base-200 transition-colors "
              (when disabled? "opacity-50 pointer-events-none"))
     :on-click on-click
     :disabled disabled?}
    ($ :div {:class "flex items-center justify-center w-12 h-12 bg-primary/10 rounded-xl mr-4"}
      ($ :svg {:class "w-6 h-6 text-primary" :fill "none" :viewBox "0 0 24 24" :stroke-width "1.5" :stroke "currentColor"}
        ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :d icon-path})))
    ($ :div {:class "flex-1 text-left"}
      ($ :p {:class "font-medium"} label)
      (when sublabel
        ($ :p {:class "text-sm text-base-content/60"} sublabel)))))

(def camera-constraints
  #js {:audio false
       :video #js {:facingMode #js {:ideal "environment"}
                   :width #js {:ideal 1280 :max 1600}
                   :height #js {:ideal 720 :max 1200}}})

(defn media-devices-supported? []
  (boolean (some-> js/navigator .-mediaDevices .-getUserMedia)))

(defn first-video-track [stream]
  (some-> stream .getVideoTracks array-seq first))

(defn stop-stream! [stream]
  (doseq [track (some-> stream .getTracks array-seq)]
    (.stop track)))

(defn torch-supported? [track]
  (try
    (boolean (some-> track .getCapabilities .-torch))
    (catch :default _
      false)))

(defn camera-error-message [err]
  (case (some-> err .-name)
    "NotAllowedError" "Camera access was blocked. You can still use the device camera instead."
    "NotFoundError" "No rear camera was found on this device."
    "NotReadableError" "The camera is already in use by another app."
    "OverconstrainedError" "This device could not open the preferred rear camera."
    "SecurityError" "The in-app camera needs a secure browser context on this device."
    "AbortError" "The camera was interrupted before it finished opening."
    "Couldn't start the in-app camera. You can still use the device camera instead."))

(defn attach-stream! [video-el stream]
  (when video-el
    (set! (.-autoplay video-el) true)
    (set! (.-muted video-el) true)
    (set! (.-playsInline video-el) true)
    (set! (.-srcObject video-el) stream)
    (when-let [play-promise (.play video-el)]
      (.catch play-promise (fn [_] nil)))))

(defn upload-file-from-blob [blob]
  (let [filename (str "receipt-" (.now js/Date) ".jpg")]
    (if (exists? js/File)
      (js/File. #js [blob] filename #js {:type "image/jpeg"})
      blob)))

(def max-capture-dimension 1600)

(defn bounded-capture-dimensions [width height]
  (let [width (double (max 1 width))
        height (double (max 1 height))
        longest-edge (max width height)
        scale (min 1.0 (/ max-capture-dimension longest-edge))]
    {:width (max 1 (js/Math.round (* width scale)))
     :height (max 1 (js/Math.round (* height scale)))}))

(defn capture-current-frame! [video-el on-success on-error]
  (let [width (or (.-videoWidth video-el) 0)
        height (or (.-videoHeight video-el) 0)]
    (if (or (<= width 0) (<= height 0))
      (on-error "The camera preview is not ready yet. Try again in a second.")
      (let [{:keys [width height]} (bounded-capture-dimensions width height)
            canvas (.createElement js/document "canvas")
            ctx (.getContext canvas "2d")]
        (set! (.-width canvas) width)
        (set! (.-height canvas) height)
        (.drawImage ctx video-el 0 0 width height)
        (.toBlob canvas
          (fn [blob]
            (if blob
              (on-success (upload-file-from-blob blob))
              (on-error "Couldn't capture a photo from the live camera.")))
          "image/jpeg"
          0.85)))))

(defn apply-torch! [track enabled? on-success on-failure]
  (if (and track (torch-supported? track) (fn? (.-applyConstraints track)))
    (-> (.applyConstraints track #js {:advanced #js [#js {:torch enabled?}]})
      (.then (fn [_]
               (when (fn? on-success)
                 (on-success enabled?))))
      (.catch (fn [err]
                (when (fn? on-failure)
                  (on-failure err)))))
    (when (fn? on-failure)
      (on-failure nil))))

(defn supported-fill-light-modes [capabilities]
  (let [modes (some-> capabilities .-fillLightMode)]
    (cond
      (nil? modes) #{}
      (string? modes) #{modes}
      (instance? js/Array modes) (set (array-seq modes))
      :else #{})))

(defn flash-supported? [capabilities]
  (contains? (supported-fill-light-modes capabilities) "flash"))

(defn try-capture-with-flash! [track video-el on-success on-error]
  (let [fallback! #(capture-current-frame! video-el on-success on-error)]
    (if (and track video-el (exists? js/ImageCapture))
      (try
        (let [image-capture (js/ImageCapture. track)]
          (if (fn? (.-getPhotoCapabilities image-capture))
            (-> (.getPhotoCapabilities image-capture)
              (.then (fn [capabilities]
                       (if (and (flash-supported? capabilities)
                             (fn? (.-takePhoto image-capture)))
                         (-> (.takePhoto image-capture #js {:fillLightMode "flash"})
                           (.then (fn [blob]
                                    (if blob
                                      (on-success (upload-file-from-blob blob))
                                      (fallback!))))
                           (.catch (fn [_]
                                     (fallback!))))
                         (fallback!))))
              (.catch (fn [_]
                        (fallback!))))
            (fallback!)))
        (catch :default _
          (fallback!)))
      (fallback!))))

(defn detect-flash-support! [track on-result]
  (if (and track (exists? js/ImageCapture))
    (try
      (let [image-capture (js/ImageCapture. track)]
        (if (fn? (.-getPhotoCapabilities image-capture))
          (-> (.getPhotoCapabilities image-capture)
            (.then (fn [capabilities]
                     (on-result (flash-supported? capabilities))))
            (.catch (fn [_]
                      (on-result false))))
          (on-result false)))
      (catch :default _
        (on-result false)))
    (on-result false)))

(defui toast-banner []
  (let [toast (use-subscribe [:mobile/toast])]
    ;; Auto-dismiss after 3s — hook called unconditionally per React rules
    (uix/use-effect
      (fn []
        (when toast
          (let [timer (js/setTimeout #(rf/dispatch [:mobile/clear-toast]) 3000)]
            #(js/clearTimeout timer))))
      [toast])
    (when toast
      ($ :div {:class "fixed top-4 left-4 right-4 z-50 ds-alert ds-alert-success shadow-lg"}
        ($ :span toast)))))

(defui camera-icon-button [{:keys [id label icon-path active? disabled? on-click]}]
  ($ :button
    {:id id
     :type "button"
     :title label
     :aria-label label
     :aria-pressed (boolean active?)
     :disabled disabled?
     :on-click on-click
     :class (str "flex h-14 w-14 items-center justify-center rounded-full border text-white shadow-xl backdrop-blur "
              (cond
                disabled? "border-white/10 bg-black/30 text-white/40"
                active? "border-amber-300 bg-amber-500/90"
                :else "border-white/25 bg-black/45 hover:bg-black/60"))}
    ($ :svg {:class "h-6 w-6" :fill "none" :viewBox "0 0 24 24" :stroke-width "1.75" :stroke "currentColor"}
      ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :d icon-path}))))

(defui camera-capture-page []
  (let [t (use-t)
        loading? (use-subscribe [:mobile/upload-loading?])
        live-video-ref (use-ref nil)
        live-stream-ref (use-ref nil)
        native-camera-ref (use-ref nil)
        [camera-starting? set-camera-starting!] (use-state true)
        [capturing-photo? set-capturing-photo!] (use-state false)
        [camera-error set-camera-error!] (use-state nil)
        [torch-available? set-torch-available!] (use-state false)
        [torch-enabled? set-torch-enabled!] (use-state false)
        [flash-available? set-flash-available!] (use-state false)
        [flash-enabled? set-flash-enabled!] (use-state false)
        [native-fallback? set-native-fallback!] (use-state false)
        busy? (boolean (or loading? camera-starting? capturing-photo?))
        stop-live-stream! (fn []
                            (when-let [stream @live-stream-ref]
                              (stop-stream! stream)
                              (reset! live-stream-ref nil))
                            (when-let [video-el @live-video-ref]
                              (set! (.-srcObject video-el) nil)))
        return-to-upload! (fn []
                            (stop-live-stream!)
                            (rf/dispatch [:mobile/navigate "/m/upload"]))
        finish-with-file! (fn [file]
                            (set-capturing-photo! false)
                            (stop-live-stream!)
                            (rf/dispatch [:mobile/navigate "/m/upload"])
                            (rf/dispatch [:mobile/upload-receipt file]))
        trigger-native-camera! (fn []
                                 (when-let [el @native-camera-ref]
                                   (.click el)))
        toggle-torch! (fn []
                        (when torch-available?
                          (when-let [track (first-video-track @live-stream-ref)]
                            (apply-torch! track
                              (not torch-enabled?)
                              (fn [enabled?]
                                (set-torch-enabled! enabled?)
                                (set-camera-error! nil))
                              (fn [_]
                                (set-torch-available! false)
                                (set-torch-enabled! false)
                                (set-camera-error! "Torch control is not available on this device/browser."))))))
        toggle-flash! (fn []
                        (if flash-available?
                          (set-flash-enabled! (not flash-enabled?))
                          (set-camera-error! "Flash capture is not available on this device/browser.")))
        capture-live-photo! (fn []
                              (if native-fallback?
                                (trigger-native-camera!)
                                (when-let [video-el @live-video-ref]
                                  (set-capturing-photo! true)
                                  (let [track (first-video-track @live-stream-ref)
                                        on-success (fn [file]
                                                     (finish-with-file! file))
                                        on-error (fn [message]
                                                   (set-capturing-photo! false)
                                                   (set-camera-error! message))]
                                    (if flash-enabled?
                                      (try-capture-with-flash! track video-el on-success on-error)
                                      (capture-current-frame! video-el on-success on-error))))))]

    (use-effect
      (fn []
        (if (media-devices-supported?)
          (let [cancelled? (atom false)
                media-devices (.-mediaDevices js/navigator)]
            (set-camera-starting! true)
            (set-camera-error! nil)
            (set-native-fallback! false)
            (-> (.getUserMedia media-devices camera-constraints)
              (.then (fn [stream]
                       (if @cancelled?
                         (stop-stream! stream)
                         (do
                           (reset! live-stream-ref stream)
                           (when-let [video-el @live-video-ref]
                             (attach-stream! video-el stream))
                           (let [track (first-video-track stream)
                                 torch? (torch-supported? track)]
                             (set-torch-available! torch?)
                             (set-torch-enabled! false)
                             (detect-flash-support! track
                               (fn [flash?]
                                 (set-flash-available! flash?)
                                 (set-flash-enabled! flash?)))
                             (set-camera-starting! false)
                             (set-capturing-photo! false))))))
              (.catch (fn [err]
                        (when-not @cancelled?
                          (when-let [stream @live-stream-ref]
                            (stop-stream! stream)
                            (reset! live-stream-ref nil))
                          (when-let [video-el @live-video-ref]
                            (set! (.-srcObject video-el) nil))
                          (set-camera-starting! false)
                          (set-native-fallback! true)
                          (set-torch-available! false)
                          (set-torch-enabled! false)
                          (set-flash-available! false)
                          (set-flash-enabled! false)
                          (set-camera-error! (camera-error-message err))))))
            (fn []
              (reset! cancelled? true)
              (when-let [stream @live-stream-ref]
                (stop-stream! stream)
                (reset! live-stream-ref nil))
              (when-let [video-el @live-video-ref]
                (set! (.-srcObject video-el) nil))))
          (do
            (set-camera-starting! false)
            (set-native-fallback! true)
            (set-flash-available! false)
            (set-flash-enabled! false)
            (set-torch-available! false)
            (set-torch-enabled! false)
            (set-camera-error! "Live camera preview is unavailable here. Capture will use the device camera instead.")
            js/undefined)))
      [])

    ($ :<>
      ($ toast-banner)
      ($ :input {:id "input-native-camera-upload-mobile"
                 :ref native-camera-ref
                 :type "file"
                 :accept "image/*"
                 :capture "environment"
                 :class "hidden"
                 :on-change (fn [e]
                              (when-let [file (-> e .-target .-files (aget 0))]
                                (finish-with-file! file)
                                (set! (.-value (.-target e)) "")))})
      ($ :div {:id "page-camera-capture-mobile"
               :class "relative min-h-[calc(100vh-5rem)] overflow-hidden bg-black text-white"}
        ($ :div {:class "absolute inset-0"}
          (if native-fallback?
            ($ :div {:class "flex h-full items-center justify-center bg-gradient-to-b from-neutral-900 to-black px-6 text-center"}
              ($ :div {:class "space-y-3"}
                ($ :div {:class "mx-auto flex h-20 w-20 items-center justify-center rounded-full border border-white/20 bg-white/10"}
                  ($ :svg {:class "h-10 w-10" :fill "none" :viewBox "0 0 24 24" :stroke-width "1.5" :stroke "currentColor"}
                    ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :d "M6.827 6.175A2.31 2.31 0 015.186 7.23c-.38.054-.757.112-1.134.175C2.999 7.58 2.25 8.507 2.25 9.574V18a2.25 2.25 0 002.25 2.25h15A2.25 2.25 0 0021.75 18V9.574c0-1.067-.75-1.994-1.802-2.169a47.865 47.865 0 00-1.134-.175 2.31 2.31 0 01-1.64-1.055l-.822-1.316a2.192 2.192 0 00-1.736-1.039 48.774 48.774 0 00-5.232 0 2.192 2.192 0 00-1.736 1.039l-.821 1.316z M16.5 12.75a4.5 4.5 0 11-9 0 4.5 4.5 0 019 0z"})))
                ($ :p {:class "text-lg font-semibold"}
                  (t :mobile/take-photo "Take Photo"))
                ($ :p {:class "text-sm text-white/70"}
                  "This browser cannot keep the live preview open here. Use the capture button below to open the device camera.")))
            ($ :video {:id "video-receipt-camera-mobile"
                       :ref live-video-ref
                       :class "h-full w-full object-cover"
                       :autoPlay true
                       :muted true
                       :playsInline true}))

          (when camera-starting?
            ($ :div {:class "absolute inset-0 z-20 flex flex-col items-center justify-center gap-3 bg-black/45 backdrop-blur-sm"}
              ($ :span {:class "ds-loading ds-loading-spinner ds-loading-lg text-white"})
              ($ :p {:class "text-sm text-white/80"} "Opening camera...")))

          ($ :div {:class "pointer-events-none absolute inset-x-0 top-0 z-30 flex items-start justify-between p-4"}
            ($ :div {:class "pointer-events-auto flex flex-col items-center gap-2"}
              ($ camera-icon-button
                {:id "btn-back-camera-mobile"
                 :label "Back"
                 :icon-path "M15.75 19.5L8.25 12l7.5-7.5"
                 :on-click return-to-upload!})
              ($ :span {:class "rounded-full bg-black/45 px-3 py-1 text-[11px] font-medium uppercase tracking-wide text-white/80"}
                "Back"))
            ($ :div {:class "pointer-events-auto flex items-start gap-3"}
              ($ :div {:class "flex flex-col items-center gap-2"}
                ($ camera-icon-button
                  {:id "btn-flash-upload-mobile"
                   :label (str "Flash " (if flash-enabled? "On" "Off"))
                   :icon-path "M12 3v10.5m0 0l3.75-3.75M12 13.5L8.25 9.75M5.25 15a6.75 6.75 0 1013.5 0c0-1.563-.53-3.002-1.42-4.148"
                   :active? flash-enabled?
                   :disabled? (or busy? (not flash-available?))
                   :on-click toggle-flash!})
                ($ :span {:class (str "rounded-full px-3 py-1 text-[11px] font-medium uppercase tracking-wide "
                                   (if flash-enabled?
                                     "bg-amber-500/90 text-black"
                                     "bg-black/45 text-white/80"))}
                  (if flash-enabled? "Flash On" "Flash Off")))
              ($ :div {:class "flex flex-col items-center gap-2"}
                ($ camera-icon-button
                  {:id "btn-torch-upload-mobile"
                   :label (str "Torch " (if torch-enabled? "On" "Off"))
                   :icon-path "M12 2.25c-1.311 0-2.53.568-3.375 1.49A4.48 4.48 0 007.5 6.75c0 .73.174 1.42.483 2.032.22.436.35.916.35 1.404v.564c0 .415.336.75.75.75h5.834a.75.75 0 00.75-.75v-.564c0-.488.13-.968.35-1.404.309-.611.483-1.302.483-2.032 0-1.17-.446-2.236-1.175-3.01A4.48 4.48 0 0012 2.25z M9.75 14.25h4.5m-4.125 2.25h3.75m-3 2.25h2.25"
                   :active? torch-enabled?
                   :disabled? (or busy? (not torch-available?))
                   :on-click toggle-torch!})
                ($ :span {:class (str "rounded-full px-3 py-1 text-[11px] font-medium uppercase tracking-wide "
                                   (if torch-enabled?
                                     "bg-amber-500/90 text-black"
                                     "bg-black/45 text-white/80"))}
                  (if torch-enabled? "Torch On" "Torch Off")))))

          (when camera-error
            ($ :div {:id "alert-camera-upload-mobile"
                     :class "absolute left-4 right-4 top-28 z-30 rounded-2xl border border-white/15 bg-black/60 px-4 py-3 text-sm text-white/90 backdrop-blur"}
              ($ :span camera-error)))

          ($ :div {:class "pointer-events-none absolute inset-x-0 bottom-8 z-30 flex items-end justify-center"}
            ($ :div {:class "pointer-events-auto flex flex-col items-center gap-3"}
              ($ :span {:class "rounded-full bg-black/45 px-4 py-1.5 text-[11px] font-medium uppercase tracking-[0.18em] text-white/80"}
                (cond
                  capturing-photo? "Capturing"
                  native-fallback? "Device Camera"
                  flash-enabled? "Flash Ready"
                  :else "Camera Ready"))
              ($ :button {:id "btn-capture-upload-mobile"
                          :type "button"
                          :aria-label (t :mobile/take-photo "Take Photo")
                          :disabled busy?
                          :on-click capture-live-photo!
                          :class (str "relative flex h-20 w-20 items-center justify-center rounded-full border-4 border-white shadow-2xl transition "
                                   (if busy?
                                     "bg-white/30"
                                     "bg-white/95 active:scale-95"))}
                ($ :span {:class (str "h-12 w-12 rounded-full border-2 transition "
                                   (if capturing-photo?
                                     "border-amber-400 bg-amber-300"
                                     "border-neutral-300 bg-neutral-900"))})))))))))

(defui upload-page []
  (let [t (use-t)
        loading? (use-subscribe [:mobile/upload-loading?])
        error (use-subscribe [:mobile/upload-error])
        offline-count (use-subscribe [:mobile/offline-queue-count])
        gallery-ref (use-ref nil)]
    ($ :<>
      ($ mobile-header {:title (t :mobile/tab-upload "Upload")})
      ($ toast-banner)

      ($ :div {:class "p-4 space-y-4"}
        (when loading?
          ($ :div {:class "fixed inset-0 z-40 bg-base-100/80 flex items-center justify-center"}
            ($ :div {:class "text-center"}
              ($ :span {:class "ds-loading ds-loading-spinner ds-loading-lg text-primary"})
              ($ :p {:class "mt-2 text-sm text-base-content/60"} "Uploading..."))))

        (when error
          ($ :div {:class "ds-alert ds-alert-error text-sm"}
            ($ :span error)))

        ($ :div {:class "space-y-3"}
          ($ :h2 {:class "text-sm font-semibold text-base-content/70 uppercase tracking-wide"}
            (t :mobile/capture-receipt "Capture Receipt"))

          ($ :input {:id "input-gallery-upload-mobile"
                     :ref gallery-ref
                     :type "file"
                     :accept "image/*"
                     :class "hidden"
                     :on-change (fn [e]
                                  (when-let [file (-> e .-target .-files (aget 0))]
                                    (rf/dispatch [:mobile/upload-receipt file])
                                    (set! (.-value (.-target e)) "")))})

          ($ capture-button
            {:id "btn-take-photo-upload-mobile"
             :icon-path "M6.827 6.175A2.31 2.31 0 015.186 7.23c-.38.054-.757.112-1.134.175C2.999 7.58 2.25 8.507 2.25 9.574V18a2.25 2.25 0 002.25 2.25h15A2.25 2.25 0 0021.75 18V9.574c0-1.067-.75-1.994-1.802-2.169a47.865 47.865 0 00-1.134-.175 2.31 2.31 0 01-1.64-1.055l-.822-1.316a2.192 2.192 0 00-1.736-1.039 48.774 48.774 0 00-5.232 0 2.192 2.192 0 00-1.736 1.039l-.821 1.316z M16.5 12.75a4.5 4.5 0 11-9 0 4.5 4.5 0 019 0z"
             :label (t :mobile/take-photo "Take Photo")
             :sublabel (t :mobile/take-photo-sub "Open the dedicated camera screen")
             :disabled? loading?
             :on-click #(rf/dispatch [:mobile/navigate "/m/upload/camera"])})

          ($ capture-button
            {:id "btn-gallery-upload-mobile"
             :icon-path "M2.25 15.75l5.159-5.159a2.25 2.25 0 013.182 0l5.159 5.159m-1.5-1.5l1.409-1.409a2.25 2.25 0 013.182 0l2.909 2.909M3.75 21h16.5A2.25 2.25 0 0022.5 18.75V5.25A2.25 2.25 0 0020.25 3H3.75A2.25 2.25 0 001.5 5.25v13.5A2.25 2.25 0 003.75 21z"
             :label (t :mobile/from-gallery "Choose from Gallery")
             :sublabel (t :mobile/from-gallery-sub "Select existing photo")
             :disabled? loading?
             :on-click #(when-let [el @gallery-ref] (.click el))}))

        ($ :div {:class "ds-divider text-base-content/40 text-xs"} (t :common/or "OR"))

        ($ :div {:class "space-y-3"}
          ($ :h2 {:class "text-sm font-semibold text-base-content/70 uppercase tracking-wide"}
            (t :mobile/manual-entry "Manual Entry"))
          ($ capture-button
            {:id "btn-manual-upload-mobile"
             :icon-path "M16.862 4.487l1.687-1.688a1.875 1.875 0 112.652 2.652L10.582 16.07a4.5 4.5 0 01-1.897 1.13L6 18l.8-2.685a4.5 4.5 0 011.13-1.897l8.932-8.931zm0 0L19.5 7.125M18 14v4.75A2.25 2.25 0 0115.75 21H5.25A2.25 2.25 0 013 18.75V8.25A2.25 2.25 0 015.25 6H10"
             :label (t :mobile/add-manually "Add Expense Manually")
             :sublabel (t :mobile/add-manually-sub "Enter details without receipt")
             :on-click #(rf/dispatch [:mobile/navigate "/m/upload/manual"])}))

        ($ pending-receipts-section)

        (when (and offline-count (pos? offline-count))
          ($ :div {:class "bg-warning/10 border border-warning/30 rounded-xl p-4 mt-4"}
            ($ :p {:class "text-sm font-medium text-warning"}
              (str offline-count " " (t :mobile/receipts-queued "receipts queued offline")))
            ($ :button {:id "btn-sync-offline-upload-mobile"
                        :type "button"
                        :class "ds-btn ds-btn-warning ds-btn-sm mt-2"
                        :on-click #(rf/dispatch [:mobile/trigger-sync])}
              (t :mobile/sync-now "Sync Now"))))))))
