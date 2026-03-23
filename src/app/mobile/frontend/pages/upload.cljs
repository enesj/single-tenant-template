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

(defui upload-page []
  (let [t (use-t)
        loading? (use-subscribe [:mobile/upload-loading?])
        error (use-subscribe [:mobile/upload-error])
        offline-count (use-subscribe [:mobile/offline-queue-count])
        camera-ref (use-ref nil)
        gallery-ref (use-ref nil)
        live-video-ref (use-ref nil)
        live-stream-ref (use-ref nil)
        [camera-open? set-camera-open!] (use-state false)
        [camera-starting? set-camera-starting!] (use-state false)
        [capturing-photo? set-capturing-photo!] (use-state false)
        [camera-error set-camera-error!] (use-state nil)
        [torch-available? set-torch-available!] (use-state false)
        [torch-enabled? set-torch-enabled!] (use-state false)
        busy? (boolean (or loading? camera-starting? capturing-photo?))
        close-live-camera! (fn []
                             (set-camera-open! false)
                             (set-camera-starting! false)
                             (set-capturing-photo! false)
                             (set-torch-available! false)
                             (set-torch-enabled! false))
        open-device-camera! (fn []
                              (when-let [el @camera-ref]
                                (.click el)))
        open-live-camera! (fn []
                            (set-camera-error! nil)
                            (if (media-devices-supported?)
                              (set-camera-open! true)
                              (when-let [el @camera-ref]
                                (.click el))))
        toggle-torch! (fn []
                        (when-let [track (first-video-track @live-stream-ref)]
                          (apply-torch! track
                            (not torch-enabled?)
                            (fn [enabled?]
                              (set-torch-enabled! enabled?)
                              (set-camera-error! nil))
                            (fn [_]
                              (set-torch-available! false)
                              (set-torch-enabled! false)
                              (set-camera-error! "Torch control is not available on this device/browser.")))))
        capture-live-photo! (fn []
                              (when-let [video-el @live-video-ref]
                                (set-capturing-photo! true)
                                (try-capture-with-flash! (first-video-track @live-stream-ref)
                                  video-el
                                  (fn [file]
                                    (set-capturing-photo! false)
                                    (close-live-camera!)
                                    (rf/dispatch [:mobile/upload-receipt file]))
                                  (fn [message]
                                    (set-capturing-photo! false)
                                    (set-camera-error! message)))))]

    (use-effect
      (fn []
        (when camera-open?
          (let [cancelled? (atom false)
                media-devices (.-mediaDevices js/navigator)]
            (set-camera-starting! true)
            (set-camera-error! nil)
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
                             (set-camera-starting! false)
                             (set-capturing-photo! false)
                             (set-torch-enabled! false))))))
              (.catch (fn [err]
                        (when-not @cancelled?
                          (set-camera-starting! false)
                          (set-camera-open! false)
                          (set-torch-available! false)
                          (set-torch-enabled! false)
                          (set-camera-error! (camera-error-message err))
                          (when-let [el @camera-ref]
                            (.click el))))))
            (fn []
              (reset! cancelled? true)
              (when-let [stream @live-stream-ref]
                (stop-stream! stream)
                (reset! live-stream-ref nil))
              (when-let [video-el @live-video-ref]
                (set! (.-srcObject video-el) nil))))))
      [camera-open?])

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

          (when camera-error
            ($ :div {:id "alert-camera-upload-mobile"
                     :class "ds-alert ds-alert-warning text-sm"}
              ($ :span camera-error)))

          ($ :input {:id "input-camera-upload-mobile"
                     :ref camera-ref
                     :type "file"
                     :accept "image/*"
                     :capture "environment"
                     :class "hidden"
                     :on-change (fn [e]
                                  (when-let [file (-> e .-target .-files (aget 0))]
                                    (set-camera-error! nil)
                                    (rf/dispatch [:mobile/upload-receipt file])
                                    (set! (.-value (.-target e)) "")))})
          ($ :input {:id "input-gallery-upload-mobile"
                     :ref gallery-ref
                     :type "file"
                     :accept "image/*"
                     :class "hidden"
                     :on-change (fn [e]
                                  (when-let [file (-> e .-target .-files (aget 0))]
                                    (set-camera-error! nil)
                                    (rf/dispatch [:mobile/upload-receipt file])
                                    (set! (.-value (.-target e)) "")))})

          (when camera-open?
            ($ :div {:id "panel-live-camera-upload-mobile"
                     :class "bg-base-100 rounded-2xl shadow-sm overflow-hidden border border-base-300"}
              ($ :div {:class "relative bg-black aspect-[3/4]"}
                ($ :video {:id "video-receipt-camera-mobile"
                           :ref live-video-ref
                           :class "w-full h-full object-cover"
                           :autoPlay true
                           :muted true
                           :playsInline true})
                (when camera-starting?
                  ($ :div {:class "absolute inset-0 bg-black/60 flex flex-col items-center justify-center gap-3 text-white"}
                    ($ :span {:class "ds-loading ds-loading-spinner ds-loading-lg"})
                    ($ :p {:class "text-sm"} "Opening camera..."))))
              ($ :div {:class "p-4 space-y-3"}
                ($ :p {:class "text-sm text-base-content/70"}
                  (if torch-available?
                    (if torch-enabled?
                      "Capture uses flash when supported. Torch is on for preview."
                      "Capture uses flash when supported. Turn on torch if you need preview light.")
                    "Capture uses flash when supported on this device/browser."))
                ($ :div {:class (str "grid gap-2 "
                                  (if torch-available?
                                    "grid-cols-1 sm:grid-cols-3"
                                    "grid-cols-1 sm:grid-cols-2"))}
                  (when torch-available?
                    ($ :button {:id "btn-torch-upload-mobile"
                                :type "button"
                                :class (str "ds-btn "
                                         (if torch-enabled?
                                           "ds-btn-warning"
                                           "ds-btn-outline"))
                                :disabled busy?
                                :on-click toggle-torch!}
                      (if torch-enabled?
                        "Torch On"
                        "Turn On Torch")))
                  ($ :button {:id "btn-capture-upload-mobile"
                              :type "button"
                              :class "ds-btn ds-btn-primary"
                              :disabled busy?
                              :on-click capture-live-photo!}
                    (if capturing-photo?
                      "Capturing..."
                      "Capture Receipt"))
                  ($ :button {:id "btn-close-camera-upload-mobile"
                              :type "button"
                              :class "ds-btn ds-btn-ghost"
                              :disabled loading?
                              :on-click close-live-camera!}
                    "Close Camera")))))

          ($ capture-button
            {:id "btn-take-photo-upload-mobile"
             :icon-path "M6.827 6.175A2.31 2.31 0 015.186 7.23c-.38.054-.757.112-1.134.175C2.999 7.58 2.25 8.507 2.25 9.574V18a2.25 2.25 0 002.25 2.25h15A2.25 2.25 0 0021.75 18V9.574c0-1.067-.75-1.994-1.802-2.169a47.865 47.865 0 00-1.134-.175 2.31 2.31 0 01-1.64-1.055l-.822-1.316a2.192 2.192 0 00-1.736-1.039 48.774 48.774 0 00-5.232 0 2.192 2.192 0 00-1.736 1.039l-.821 1.316z M16.5 12.75a4.5 4.5 0 11-9 0 4.5 4.5 0 019 0z"
             :label (t :mobile/take-photo "Take Photo")
             :sublabel (t :mobile/take-photo-sub "Use the rear camera and fire flash when supported")
             :disabled? (or busy? camera-open?)
             :on-click open-live-camera!})

          ($ capture-button
            {:id "btn-gallery-upload-mobile"
             :icon-path "M2.25 15.75l5.159-5.159a2.25 2.25 0 013.182 0l5.159 5.159m-1.5-1.5l1.409-1.409a2.25 2.25 0 013.182 0l2.909 2.909M3.75 21h16.5A2.25 2.25 0 0022.5 18.75V5.25A2.25 2.25 0 0020.25 3H3.75A2.25 2.25 0 001.5 5.25v13.5A2.25 2.25 0 003.75 21z"
             :label (t :mobile/from-gallery "Choose from Gallery")
             :sublabel (t :mobile/from-gallery-sub "Select existing photo")
             :disabled? busy?
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
