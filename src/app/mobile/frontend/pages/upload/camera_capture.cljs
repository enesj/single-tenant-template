(ns app.mobile.frontend.pages.upload.camera-capture
  "Camera capture UI components: capture button, toast banner, camera page."
  (:require
    [app.mobile.frontend.pages.upload.camera-utils
     :as cu
     :refer [apply-camera-zoom! apply-default-camera-zoom! apply-torch!
             attach-stream! camera-constraints camera-error-message
             camera-zoom-range capture-current-frame! detect-flash-support!
             first-video-track format-camera-zoom-label
             format-camera-zoom-range-label media-devices-supported?
             min-preview-zoom max-preview-zoom clamp-number
             point-distance point-midpoint stop-stream!
             torch-supported? try-capture-with-flash!]]
    [app.template.frontend.i18n :refer [use-t]]
    [re-frame.core :as rf]
    [uix.core :refer [$ defui use-effect use-ref use-state]]
    [uix.re-frame :refer [use-subscribe]]))

;; ========================================================================
;; Small shared components
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

(defui toast-banner []
  (let [t (use-t)
        toast (use-subscribe [:mobile/toast])]
    ;; Auto-dismiss after 3s -- hook called unconditionally per React rules
    (use-effect
      (fn []
        (when toast
          (let [timer (js/setTimeout #(rf/dispatch [:mobile/clear-toast]) 3000)]
            #(js/clearTimeout timer))))
      [toast])
    (when toast
      ($ :div {:class "fixed top-4 left-4 right-4 z-50 ds-alert ds-alert-success shadow-lg"}
        ($ :span (if (keyword? toast) (t toast) toast))))))

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
                disabled? "border-white/10 bg-black/20 text-white/35"
                active? "border-amber-300 bg-amber-500/72"
                :else "border-white/20 bg-black/28 hover:bg-black/38"))}
    ($ :svg {:class "h-6 w-6" :fill "none" :viewBox "0 0 24 24" :stroke-width "1.75" :stroke "currentColor"}
      ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :d icon-path}))))

;; ========================================================================
;; Camera capture page
;; ========================================================================

(defui camera-capture-page []
  (let [t (use-t)
        loading? (use-subscribe [:mobile/upload-loading?])
        live-video-ref (use-ref nil)
        live-stream-ref (use-ref nil)
        native-camera-ref (use-ref nil)
        active-pointers-ref (use-ref {})
        gesture-ref (use-ref nil)
        [camera-starting? set-camera-starting!] (use-state true)
        [capturing-photo? set-capturing-photo!] (use-state false)
        [camera-error set-camera-error!] (use-state nil)
        [camera-error-dismissed? set-camera-error-dismissed!] (use-state false)
        [torch-available? set-torch-available!] (use-state false)
        [torch-enabled? set-torch-enabled!] (use-state false)
        [flash-available? set-flash-available!] (use-state false)
        [flash-enabled? set-flash-enabled!] (use-state (cu/default-flash-enabled? nil))
        [native-fallback? set-native-fallback!] (use-state false)
        [device-flash-native-mode? set-device-flash-native-mode!] (use-state false)
        [camera-hardware-zoom set-camera-hardware-zoom!] (use-state 1.0)
        [camera-hardware-zoom-range set-camera-hardware-zoom-range!] (use-state nil)
        [captured-file set-captured-file!] (use-state nil)
        [captured-preview-url set-captured-preview-url!] (use-state nil)
        [preview-zoom set-preview-zoom!] (use-state 1.0)
        [preview-pan set-preview-pan!] (use-state {:x 0 :y 0})
        busy? (boolean (or loading? camera-starting? capturing-photo?))
        zoomed? (> preview-zoom min-preview-zoom)
        device-flash? (and device-flash-native-mode? flash-enabled?)
        native-capture-mode? (or native-fallback? device-flash-native-mode?)
        lens-controls-available? (and (map? camera-hardware-zoom-range) (not native-capture-mode?))
        flash-pill-label (if flash-enabled?
                           (t :mobile/flash-on)
                           (t :mobile/flash-off))
        reset-preview-transform! (fn []
                                   (reset! active-pointers-ref {})
                                   (reset! gesture-ref nil)
                                   (set-preview-zoom! 1.0)
                                   (set-preview-pan! {:x 0 :y 0}))
        clear-camera-error! (fn []
                              (set-camera-error! nil)
                              (set-camera-error-dismissed! false))
        show-camera-error! (fn [message]
                             (set-camera-error! message)
                             (set-camera-error-dismissed! false))
        clear-captured-file! (fn []
                               (set-captured-file! nil)
                               (set-captured-preview-url! nil))
        change-camera-hardware-zoom! (fn [direction]
                                       (when-let [track (first-video-track @live-stream-ref)]
                                         (when-let [{min-zoom :min max-zoom :max step :step} camera-hardware-zoom-range]
                                           (let [step (or step 0.5)
                                                 next-zoom (-> (+ camera-hardware-zoom (* direction step))
                                                             (max min-zoom)
                                                             (min max-zoom))]
                                             (when (not= next-zoom camera-hardware-zoom)
                                               (apply-camera-zoom! track next-zoom
                                                 set-camera-hardware-zoom!
                                                 (fn [_]
                                                   (show-camera-error! "Camera zoom could not be changed on this device/browser."))))))))
        queue-captured-file! (fn [file]
                               (set-capturing-photo! false)
                               (clear-camera-error!)
                               (set-captured-file! file)
                               (if (and (exists? js/URL) (fn? (.-createObjectURL js/URL)))
                                 (set-captured-preview-url! (.createObjectURL js/URL file))
                                 (set-captured-preview-url! nil)))
        stop-live-stream! (fn []
                            (when-let [stream @live-stream-ref]
                              (stop-stream! stream)
                              (reset! live-stream-ref nil))
                            (when-let [video-el @live-video-ref]
                              (set! (.-srcObject video-el) nil))
                            (reset! active-pointers-ref {})
                            (reset! gesture-ref nil))
        restart-live-preview! (fn []
                                (if (media-devices-supported?)
                                  (let [media-devices (.-mediaDevices js/navigator)]
                                    (set-camera-starting! true)
                                    (set-capturing-photo! false)
                                    (clear-camera-error!)
                                    (set-native-fallback! false)
                                    (set-camera-hardware-zoom! 1.0)
                                    (set-camera-hardware-zoom-range! nil)
                                    (stop-live-stream!)
                                    (reset! active-pointers-ref {})
                                    (reset! gesture-ref nil)
                                    (set-preview-zoom! 1.0)
                                    (set-preview-pan! {:x 0 :y 0})
                                    (-> (.getUserMedia media-devices camera-constraints)
                                      (.then (fn [stream]
                                               (reset! live-stream-ref stream)
                                               (when-let [video-el @live-video-ref]
                                                 (attach-stream! video-el stream))
                                               (let [track (first-video-track stream)
                                                     torch? (torch-supported? track)]
                                                 (set-camera-hardware-zoom-range! (some-> track .getCapabilities camera-zoom-range))
                                                 (apply-default-camera-zoom! track set-camera-hardware-zoom!)
                                                 (set-torch-available! torch?)
                                                 (set-torch-enabled! false)
                                                 (detect-flash-support! track
                                                   (fn [flash?]
                                                     (set-flash-available! flash?)
                                                     (set-flash-enabled! (cu/default-flash-enabled? flash?))
                                                     (set-camera-starting! false))))))
                                      (.catch (fn [err]
                                                (stop-live-stream!)
                                                (set-camera-starting! false)
                                                (set-native-fallback! true)
                                                (set-device-flash-native-mode! false)
                                                (set-camera-hardware-zoom! 1.0)
                                                (set-camera-hardware-zoom-range! nil)
                                                (set-torch-available! false)
                                                (set-torch-enabled! false)
                                                (set-flash-available! false)
                                                (set-flash-enabled! (cu/default-flash-enabled? nil))
                                                (show-camera-error! (camera-error-message err))))))
                                  (do
                                    (set-camera-starting! false)
                                    (set-native-fallback! true)
                                    (set-device-flash-native-mode! false)
                                    (set-camera-hardware-zoom! 1.0)
                                    (set-camera-hardware-zoom-range! nil)
                                    (set-torch-available! false)
                                    (set-torch-enabled! false)
                                    (set-flash-available! false)
                                    (set-flash-enabled! (cu/default-flash-enabled? nil))
                                    (show-camera-error! :mobile/camera-unavailable))))
        trigger-native-camera! (fn []
                                 (when-let [el @native-camera-ref]
                                   (.click el)))
        reopen-native-camera! (fn []
                                (js/setTimeout trigger-native-camera! 150))
        return-to-upload! (fn []
                            (clear-captured-file!)
                            (set-device-flash-native-mode! false)
                            (set-camera-hardware-zoom! 1.0)
                            (set-camera-hardware-zoom-range! nil)
                            (stop-live-stream!)
                            (reset-preview-transform!)
                            (rf/dispatch [:mobile/navigate "/m/upload"]))
        current-points (fn []
                         (vals @active-pointers-ref))
        begin-drag! (fn [pointer]
                      (if zoomed?
                        (reset! gesture-ref {:mode :drag
                                             :pointer-id (:id pointer)
                                             :start-point {:x (:x pointer) :y (:y pointer)}
                                             :start-pan preview-pan})
                        (reset! gesture-ref nil)))
        begin-pinch! (fn []
                       (let [[p1 p2] (take 2 (current-points))]
                         (if (and p1 p2)
                           (let [distance (point-distance p1 p2)]
                             (if (> distance 0)
                               (reset! gesture-ref {:mode :pinch
                                                    :start-distance distance
                                                    :start-midpoint (point-midpoint p1 p2)
                                                    :start-zoom preview-zoom
                                                    :start-pan preview-pan})
                               (reset! gesture-ref nil)))
                           (reset! gesture-ref nil))))
        sync-gesture-after-release! (fn []
                                      (let [points (vec (current-points))]
                                        (cond
                                          (>= (count points) 2) (begin-pinch!)
                                          (= (count points) 1) (begin-drag! (first points))
                                          :else (reset! gesture-ref nil))))
        handle-preview-pointer-down! (fn [e]
                                       (let [pointer {:id (.-pointerId e)
                                                      :x (.-clientX e)
                                                      :y (.-clientY e)}]
                                         (.preventDefault e)
                                         (try
                                           (.setPointerCapture (.-currentTarget e) (.-pointerId e))
                                           (catch :default _ nil))
                                         (swap! active-pointers-ref assoc (:id pointer) pointer)
                                         (if (>= (count @active-pointers-ref) 2)
                                           (begin-pinch!)
                                           (begin-drag! pointer))))
        handle-preview-pointer-move! (fn [e]
                                       (let [pointer-id (.-pointerId e)]
                                         (when (contains? @active-pointers-ref pointer-id)
                                           (.preventDefault e)
                                           (swap! active-pointers-ref assoc pointer-id {:id pointer-id
                                                                                        :x (.-clientX e)
                                                                                        :y (.-clientY e)})
                                           (let [{:keys [mode pointer-id start-point start-pan start-distance start-midpoint start-zoom]} @gesture-ref]
                                             (case mode
                                               :drag (when (= pointer-id (.-pointerId e))
                                                       (let [dx (- (.-clientX e) (:x start-point))
                                                             dy (- (.-clientY e) (:y start-point))]
                                                         (set-preview-pan! {:x (+ (:x start-pan) dx)
                                                                            :y (+ (:y start-pan) dy)})))
                                               :pinch (let [[p1 p2] (take 2 (current-points))]
                                                        (when (and p1 p2 (> start-distance 0))
                                                          (let [distance (point-distance p1 p2)
                                                                midpoint (point-midpoint p1 p2)
                                                                next-zoom (clamp-number (* start-zoom (/ distance start-distance))
                                                                            min-preview-zoom
                                                                            max-preview-zoom)
                                                                dx (- (:x midpoint) (:x start-midpoint))
                                                                dy (- (:y midpoint) (:y start-midpoint))]
                                                            (set-preview-zoom! next-zoom)
                                                            (set-preview-pan! {:x (+ (:x start-pan) dx)
                                                                               :y (+ (:y start-pan) dy)}))))
                                               nil)))))
        handle-preview-pointer-up! (fn [e]
                                     (let [pointer-id (.-pointerId e)]
                                       (swap! active-pointers-ref dissoc pointer-id)
                                       (sync-gesture-after-release!)))
        toggle-torch! (fn []
                        (when torch-available?
                          (when-let [track (first-video-track @live-stream-ref)]
                            (apply-torch! track
                              (not torch-enabled?)
                              (fn [enabled?]
                                (set-torch-enabled! enabled?)
                                (clear-camera-error!))
                              (fn [_]
                                (set-torch-available! false)
                                (set-torch-enabled! false)
                                (show-camera-error! :mobile/torch-unavailable))))))
        toggle-flash! (fn []
                        (let [next-enabled? (not flash-enabled?)]
                          (set-flash-enabled! next-enabled?)
                          (set-device-flash-native-mode! false)
                          (if next-enabled?
                            (if flash-available?
                              (clear-camera-error!)
                              (show-camera-error! :mobile/flash-native-hint))
                            (clear-camera-error!))))
        save-captured-file! (fn [file after-success]
                              (when file
                                (rf/dispatch [:mobile/upload-receipt
                                              file
                                              {:on-success (fn [_response]
                                                             (when (fn? after-success)
                                                               (after-success)))
                                               :on-error (fn [message]
                                                           (show-camera-error! message))}])))
        save-and-exit! (fn []
                         (save-captured-file! captured-file return-to-upload!))
        save-and-take-another! (fn []
                                 (when-let [file captured-file]
                                   (if native-capture-mode?
                                     (do
                                       (clear-captured-file!)
                                       (trigger-native-camera!)
                                       (save-captured-file! file nil))
                                     (do
                                       (clear-captured-file!)
                                       (restart-live-preview!)
                                       (save-captured-file! file nil)))))
        retake-photo! (fn []
                        (clear-captured-file!)
                        (if native-capture-mode?
                          (reopen-native-camera!)
                          (restart-live-preview!)))
        capture-live-photo! (fn []
                              (if native-capture-mode?
                                (trigger-native-camera!)
                                (when-let [video-el @live-video-ref]
                                  (set-capturing-photo! true)
                                  (let [track (first-video-track @live-stream-ref)
                                        on-success (fn [file]
                                                     (queue-captured-file! file))
                                        on-error (fn [message]
                                                   (set-capturing-photo! false)
                                                   (show-camera-error! message))]
                                    (if flash-enabled?
                                      (try-capture-with-flash! track video-el on-success on-error)
                                      (capture-current-frame! video-el on-success on-error))))))]

    (use-effect
      (fn []
        (fn []
          (when (and captured-preview-url (exists? js/URL) (fn? (.-revokeObjectURL js/URL)))
            (.revokeObjectURL js/URL captured-preview-url))))
      [captured-preview-url])

    (use-effect
      (fn []
        (if (media-devices-supported?)
          (let [cancelled? (atom false)
                media-devices (.-mediaDevices js/navigator)]
            (set-camera-starting! true)
            (set-camera-error! nil)
            (set-camera-error-dismissed! false)
            (set-native-fallback! false)
            (set-camera-hardware-zoom! 1.0)
            (set-camera-hardware-zoom-range! nil)
            (reset! active-pointers-ref {})
            (reset! gesture-ref nil)
            (set-preview-zoom! 1.0)
            (set-preview-pan! {:x 0 :y 0})
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
                             (set-camera-hardware-zoom-range! (some-> track .getCapabilities camera-zoom-range))
                             (apply-default-camera-zoom! track set-camera-hardware-zoom!)
                             (set-torch-available! torch?)
                             (set-torch-enabled! false)
                             (detect-flash-support! track
                               (fn [flash?]
                                 (set-flash-available! flash?)
                                 (set-flash-enabled! (cu/default-flash-enabled? flash?))
                                 (set-device-flash-native-mode! false)
                                 (set-camera-starting! false)))
                             (set-capturing-photo! false))))))
              (.catch (fn [err]
                        (when-not @cancelled?
                          (when-let [stream @live-stream-ref]
                            (stop-stream! stream)
                            (reset! live-stream-ref nil))
                          (when-let [video-el @live-video-ref]
                            (set! (.-srcObject video-el) nil))
                          (reset! active-pointers-ref {})
                          (reset! gesture-ref nil)
                          (set-camera-starting! false)
                          (set-native-fallback! true)
                          (set-device-flash-native-mode! false)
                          (set-camera-hardware-zoom! 1.0)
                          (set-camera-hardware-zoom-range! nil)
                          (set-torch-available! false)
                          (set-torch-enabled! false)
                          (set-flash-available! false)
                          (set-flash-enabled! (cu/default-flash-enabled? nil))
                          (set-camera-error! (camera-error-message err))
                          (set-camera-error-dismissed! false)))))
            (fn []
              (reset! cancelled? true)
              (when-let [stream @live-stream-ref]
                (stop-stream! stream)
                (reset! live-stream-ref nil))
              (when-let [video-el @live-video-ref]
                (set! (.-srcObject video-el) nil))
              (reset! active-pointers-ref {})
              (reset! gesture-ref nil)))
          (do
            (set-camera-starting! false)
            (set-native-fallback! true)
            (set-device-flash-native-mode! false)
            (set-camera-hardware-zoom! 1.0)
            (set-camera-hardware-zoom-range! nil)
            (set-flash-available! false)
            (set-flash-enabled! (cu/default-flash-enabled? nil))
            (set-torch-available! false)
            (set-torch-enabled! false)
            (set-camera-error! :mobile/camera-unavailable)
            (set-camera-error-dismissed! false)
            js/undefined)))
      [])

    (use-effect
      (fn []
        (when (and (nil? captured-file) (not native-capture-mode?))
          (when-let [stream @live-stream-ref]
            (when-let [video-el @live-video-ref]
              (attach-stream! video-el stream))))
        js/undefined)
      [captured-file native-capture-mode?])

    (use-effect
      (fn []
        (when (<= preview-zoom min-preview-zoom)
          (set-preview-pan! {:x 0 :y 0})
          (reset! gesture-ref nil))
        js/undefined)
      [preview-zoom])

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
                                (queue-captured-file! file)
                                (set! (.-value (.-target e)) "")))})
      ($ :div {:id "page-camera-capture-mobile"
               :class "fixed inset-0 z-[70] overflow-hidden bg-black text-white"}
        ($ :div {:id "camera-preview-stage-mobile"
                 :class "absolute inset-0 overflow-hidden"
                 :style #js {:touchAction "none"}
                 :on-pointer-down (when (and (not native-capture-mode?) (nil? captured-file)) handle-preview-pointer-down!)
                 :on-pointer-move (when (and (not native-capture-mode?) (nil? captured-file)) handle-preview-pointer-move!)
                 :on-pointer-up (when (and (not native-capture-mode?) (nil? captured-file)) handle-preview-pointer-up!)
                 :on-pointer-cancel (when (and (not native-capture-mode?) (nil? captured-file)) handle-preview-pointer-up!)}
          ($ :div {:class "absolute inset-0"
                   :style #js {:transform (str "translate(" (:x preview-pan) "px, " (:y preview-pan) "px) scale(" preview-zoom ")")
                               :transformOrigin "center center"
                               :willChange "transform"}}
            (cond
              captured-preview-url
              ($ :img {:id "img-captured-receipt-preview-mobile"
                       :src captured-preview-url
                       :alt "Captured receipt preview"
                       :class "h-full w-full object-contain bg-black"})

              captured-file
              ($ :div {:class "flex h-full items-center justify-center bg-black px-6 text-center"}
                ($ :p {:class "text-sm text-white/70"}
                  (t :mobile/captured-fallback-msg)))

              native-capture-mode?
              ($ :div {:class "flex h-full items-center justify-center bg-gradient-to-b from-neutral-900 to-black px-6 text-center"}
                ($ :div {:class "space-y-3"}
                  ($ :div {:class "mx-auto flex h-20 w-20 items-center justify-center rounded-full border border-white/20 bg-white/10"}
                    ($ :svg {:class "h-10 w-10" :fill "none" :viewBox "0 0 24 24" :stroke-width "1.5" :stroke "currentColor"}
                      ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :d "M6.827 6.175A2.31 2.31 0 015.186 7.23c-.38.054-.757.112-1.134.175C2.999 7.58 2.25 8.507 2.25 9.574V18a2.25 2.25 0 002.25 2.25h15A2.25 2.25 0 0021.75 18V9.574c0-1.067-.75-1.994-1.802-2.169a47.865 47.865 0 00-1.134-.175 2.31 2.31 0 01-1.64-1.055l-.822-1.316a2.192 2.192 0 00-1.736-1.039 48.774 48.774 0 00-5.232 0 2.192 2.192 0 00-1.736 1.039l-.821 1.316z M16.5 12.75a4.5 4.5 0 11-9 0 4.5 4.5 0 019 0z"})))
                  ($ :p {:class "text-lg font-semibold"}
                    (t :mobile/take-photo "Take Photo"))
                  ($ :p {:class "text-sm text-white/70"}
                    (if device-flash?
                      (t :mobile/native-flash-prompt)
                      (t :mobile/native-camera-prompt)))))

              :else
              ($ :video {:id "video-receipt-camera-mobile"
                         :ref live-video-ref
                         :class "h-full w-full object-cover"
                         :autoPlay true
                         :muted true
                         :playsInline true}))))

        (when camera-starting?
          ($ :div {:class "absolute inset-0 z-20 flex flex-col items-center justify-center gap-3 bg-black/45 backdrop-blur-sm"}
            ($ :span {:class "ds-loading ds-loading-spinner ds-loading-lg text-white"})
            ($ :p {:class "text-sm text-white/80"} (t :mobile/opening-camera))))

        ($ :div {:class "pointer-events-none absolute inset-x-0 top-0 z-30 flex items-start justify-between p-4"}
          ($ :div {:class "pointer-events-auto flex flex-col items-center gap-2"}
            ($ camera-icon-button
              {:id "btn-back-camera-mobile"
               :label (t :mobile/back)
               :icon-path "M15.75 19.5L8.25 12l7.5-7.5"
               :on-click return-to-upload!})
            ($ :span {:class "rounded-full bg-black/35 px-3 py-1 text-[11px] font-medium uppercase tracking-wide text-white/75"}
              (t :mobile/back)))
          ($ :div {:class "pointer-events-auto flex items-start gap-3"}
            ($ :div {:class "flex flex-col items-center gap-2"}
              ($ camera-icon-button
                {:id "btn-flash-upload-mobile"
                 :label (if flash-available?
                          (if flash-enabled? (t :mobile/flash-on) (t :mobile/flash-off))
                          (if device-flash? (t :mobile/iphone-flash-on) (t :mobile/iphone-flash-off)))
                 :icon-path "M12 3v10.5m0 0l3.75-3.75M12 13.5L8.25 9.75M5.25 15a6.75 6.75 0 1013.5 0c0-1.563-.53-3.002-1.42-4.148"
                 :active? flash-enabled?
                 :disabled? busy?
                 :on-click toggle-flash!})
              ($ :span {:class (str "rounded-full px-3 py-1 text-[11px] font-medium uppercase tracking-wide "
                                 (if flash-enabled?
                                   "bg-amber-500/85 text-black"
                                   "bg-black/35 text-white/75"))}
                flash-pill-label))
            ($ :div {:class "flex flex-col items-center gap-2"}
              ($ camera-icon-button
                {:id "btn-torch-upload-mobile"
                 :label (if torch-enabled? (t :mobile/torch-on) (t :mobile/torch-off))
                 :icon-path "M12 2.25c-1.311 0-2.53.568-3.375 1.49A4.48 4.48 0 007.5 6.75c0 .73.174 1.42.483 2.032.22.436.35.916.35 1.404v.564c0 .415.336.75.75.75h5.834a.75.75 0 00.75-.75v-.564c0-.488.13-.968.35-1.404.309-.611.483-1.302.483-2.032 0-1.17-.446-2.236-1.175-3.01A4.48 4.48 0 0012 2.25z M9.75 14.25h4.5m-4.125 2.25h3.75m-3 2.25h2.25"
                 :active? torch-enabled?
                 :disabled? (or busy? (not torch-available?))
                 :on-click toggle-torch!})
              ($ :span {:class (str "rounded-full px-3 py-1 text-[11px] font-medium uppercase tracking-wide "
                                 (if torch-enabled?
                                   "bg-amber-500/85 text-black"
                                   "bg-black/35 text-white/75"))}
                (if torch-enabled? (t :mobile/torch-on) (t :mobile/torch-off))))))

        (when (and camera-error (not camera-error-dismissed?))
          ($ :div {:id "alert-camera-upload-mobile"
                   :class "absolute left-4 right-4 top-28 z-30 flex items-start justify-between gap-3 rounded-2xl border border-white/15 bg-black/45 px-4 py-3 text-sm text-white/90 backdrop-blur"}
            ($ :span {:class "flex-1 leading-5"} (if (keyword? camera-error) (t camera-error) camera-error))
            ($ :button {:id "btn-close-camera-error-mobile"
                        :type "button"
                        :aria-label "Dismiss camera warning"
                        :class "rounded-full bg-white/10 p-2 text-white/80 transition hover:bg-white/15 hover:text-white"
                        :on-click #(set-camera-error-dismissed! true)}
              ($ :svg {:class "h-4 w-4" :fill "none" :viewBox "0 0 24 24" :stroke-width "2" :stroke "currentColor"}
                ($ :path {:stroke-linecap "round" :stroke-linejoin "round" :d "M6 18L18 6M6 6l12 12"})))))

        ($ :div {:class "pointer-events-none absolute inset-x-0 bottom-36 z-30 flex justify-center gap-2"}
          ($ :div {:class "pointer-events-auto flex items-center gap-2"}
            ($ camera-icon-button
              {:id "btn-lens-decrease-camera-mobile"
               :label "Decrease Lens Zoom"
               :icon-path "M18 12H6"
               :disabled? (or busy? (not lens-controls-available?))
               :on-click #(change-camera-hardware-zoom! -1)})
            ($ :span {:id "badge-camera-hardware-zoom-mobile"
                      :class "rounded-full bg-black/35 px-4 py-1.5 text-[11px] font-medium uppercase tracking-[0.18em] text-white/75"}
              (str (t :mobile/lens-label) " "
                (format-camera-zoom-label camera-hardware-zoom)
                " "
                (or (format-camera-zoom-range-label camera-hardware-zoom-range) "")))
            ($ camera-icon-button
              {:id "btn-lens-increase-camera-mobile"
               :label "Increase Lens Zoom"
               :icon-path "M12 6v12m6-6H6"
               :disabled? (or busy? (not lens-controls-available?))
               :on-click #(change-camera-hardware-zoom! 1)}))
          (when zoomed?
            ($ :span {:class "rounded-full bg-black/35 px-4 py-1.5 text-[11px] font-medium uppercase tracking-[0.18em] text-white/75"}
              (str (t :mobile/preview-label) " " (js/Math.round (* preview-zoom 100)) "%"))))

        ($ :div {:class "pointer-events-none absolute inset-x-0 bottom-8 z-30 flex items-end justify-center"}
          ($ :div {:class "pointer-events-auto flex flex-col items-center gap-3"}
            ($ :span {:class "rounded-full bg-black/35 px-4 py-1.5 text-[11px] font-medium uppercase tracking-[0.18em] text-white/75"}
              (cond
                capturing-photo? (t :mobile/status-capturing)
                captured-file (t :mobile/status-review-capture)
                device-flash? (t :mobile/iphone-flash)
                native-fallback? (t :mobile/status-device-camera)
                flash-enabled? (t :mobile/status-flash-ready)
                :else (t :mobile/status-camera-ready)))
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
                                   "border-neutral-300 bg-neutral-900"))}))))

        (when captured-file
          ($ :div {:id "modal-camera-next-action-mobile"
                   :class "absolute inset-0 z-40 flex items-end bg-black/18 px-4 pb-6 pt-20"}
            ($ :div {:class "mx-auto w-full max-w-sm rounded-3xl border border-white/10 bg-neutral-950/90 p-5 shadow-2xl"}
              ($ :p {:class "text-xs font-semibold uppercase tracking-[0.24em] text-white/45"}
                (t :mobile/receipt-captured-label))
              ($ :h2 {:class "mt-2 text-xl font-semibold text-white"}
                (t :mobile/take-another-or-exit))
              (when captured-preview-url
                ($ :div {:class "mt-4 overflow-hidden rounded-2xl border border-white/10 bg-black"}
                  ($ :img {:id "img-captured-receipt-audit-mobile"
                           :src captured-preview-url
                           :alt "Captured receipt audit preview"
                           :class "max-h-64 w-full object-contain bg-black"})))
              ($ :p {:class "mt-3 text-sm leading-6 text-white/70"}
                (if native-capture-mode?
                  (t :mobile/save-native-help)
                  (t :mobile/save-live-help)))
              (when device-flash?
                ($ :p {:class "mt-2 text-sm leading-6 text-amber-200/90"}
                  (t :mobile/iphone-flash-hint)))
              ($ :div {:class "mt-5 grid gap-3"}
                ($ :button {:id "btn-camera-save-another-mobile"
                            :type "button"
                            :disabled loading?
                            :on-click save-and-take-another!
                            :class "flex w-full items-center justify-center rounded-2xl bg-white/90 px-4 py-3 text-sm font-semibold text-black transition hover:bg-white disabled:cursor-not-allowed disabled:opacity-50"}
                  (t :mobile/use-and-take-another))
                ($ :button {:id "btn-camera-save-exit-mobile"
                            :type "button"
                            :disabled loading?
                            :on-click save-and-exit!
                            :class "flex w-full items-center justify-center rounded-2xl bg-amber-400/85 px-4 py-3 text-sm font-semibold text-black transition hover:bg-amber-300 disabled:cursor-not-allowed disabled:opacity-50"}
                  (t :mobile/use-and-exit))
                ($ :button {:id "btn-camera-cancel-mobile"
                            :type "button"
                            :disabled loading?
                            :on-click return-to-upload!
                            :class "flex w-full items-center justify-center rounded-2xl border border-white/15 bg-white/8 px-4 py-3 text-sm font-medium text-white/85 transition hover:bg-white/12 disabled:cursor-not-allowed disabled:opacity-50"}
                  (t :common/cancel "Cancel"))
                ($ :button {:id "btn-camera-retake-mobile"
                            :type "button"
                            :disabled loading?
                            :on-click retake-photo!
                            :class "flex w-full items-center justify-center rounded-2xl border border-white/15 bg-white/8 px-4 py-3 text-sm font-medium text-white/85 transition hover:bg-white/12 disabled:cursor-not-allowed disabled:opacity-50"}
                  (t :mobile/retake))))))))))
