(ns app.mobile.frontend.pages.upload.camera-utils
  "Camera utility functions and constants for mobile upload.
  Pure functions (no UI components, no re-frame side effects).")

;; ========================================================================
;; Camera constraints & stream helpers
;; ========================================================================

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

;; ========================================================================
;; Torch
;; ========================================================================

(defn torch-supported? [track]
  (try
    (boolean (some-> track .getCapabilities .-torch))
    (catch :default _
      false)))

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

;; ========================================================================
;; Camera error messages
;; ========================================================================

(defn camera-error-message [err]
  (case (some-> err .-name)
    "NotAllowedError" :mobile/camera-err-not-allowed
    "NotFoundError" :mobile/camera-err-not-found
    "NotReadableError" :mobile/camera-err-not-readable
    "OverconstrainedError" :mobile/camera-err-overconstrained
    "SecurityError" :mobile/camera-err-security
    "AbortError" :mobile/camera-err-abort
    :mobile/camera-err-default))

;; ========================================================================
;; Stream attach & file creation
;; ========================================================================

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

;; ========================================================================
;; Capture dimensions & frame capture
;; ========================================================================

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
      (on-error :mobile/preview-not-ready)
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
              (on-error :mobile/capture-failed)))
          "image/jpeg"
          0.85)))))

;; ========================================================================
;; Hardware zoom
;; ========================================================================

(def default-live-camera-zoom 1.5)

(defn camera-zoom-range [capabilities]
  (let [zoom (some-> capabilities .-zoom)
        min-zoom (some-> zoom .-min)
        max-zoom (some-> zoom .-max)]
    (when (and (number? min-zoom) (number? max-zoom))
      {:min min-zoom
       :max max-zoom
       :step (some-> zoom .-step)})))

(defn preferred-camera-zoom [capabilities desired-zoom]
  (when-let [{min-zoom :min max-zoom :max} (camera-zoom-range capabilities)]
    (-> desired-zoom (max min-zoom) (min max-zoom))))

(defn apply-camera-zoom! [track zoom on-success on-failure]
  (if (and track (number? zoom) (fn? (.-applyConstraints track)))
    (-> (.applyConstraints track #js {:advanced #js [#js {:zoom zoom}]})
      (.then (fn [_]
               (when (fn? on-success)
                 (on-success zoom))))
      (.catch (fn [err]
                (when (fn? on-failure)
                  (on-failure err)))))
    (when (fn? on-failure)
      (on-failure nil))))

(defn apply-default-camera-zoom! [track on-success]
  (let [capabilities (some-> track .getCapabilities)
        preferred-zoom (preferred-camera-zoom capabilities default-live-camera-zoom)]
    (cond
      (number? preferred-zoom)
      (apply-camera-zoom! track preferred-zoom
        (fn [zoom]
          (when (fn? on-success)
            (on-success zoom)))
        (fn [_]
          (when (fn? on-success)
            (on-success 1.0))))

      (fn? on-success)
      (on-success 1.0)

      :else nil)))

;; ========================================================================
;; Flash (ImageCapture API)
;; ========================================================================

(defn supported-fill-light-modes [capabilities]
  (let [modes (some-> capabilities .-fillLightMode)]
    (cond
      (nil? modes) #{}
      (string? modes) #{modes}
      (instance? js/Array modes) (set (array-seq modes))
      :else #{})))

(defn flash-supported? [capabilities]
  (contains? (supported-fill-light-modes capabilities) "flash"))

(defn default-flash-enabled?
  [_flash-available?]
  true)

(defn try-capture-with-flash! [track video-el on-success on-error]
  (let [fallback! #(capture-current-frame! video-el on-success on-error)
        on-photo-blob (fn [blob]
                        (if blob
                          (on-success (upload-file-from-blob blob))
                          (fallback!)))
        attempt-photo! (fn [image-capture]
                         (if (fn? (.-takePhoto image-capture))
                           (-> (.takePhoto image-capture #js {:fillLightMode "flash"})
                             (.then on-photo-blob)
                             (.catch (fn [_]
                                       (fallback!))))
                           (fallback!)))]
    (if (and track video-el (exists? js/ImageCapture))
      (try
        (attempt-photo! (js/ImageCapture. track))
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

;; ========================================================================
;; Flash/native mode helpers
;; ========================================================================

(defn device-flash-mode? [_flash-available? _flash-enabled?]
  false)

(defn native-camera-capture? [native-fallback? _flash-available? _flash-enabled?]
  (boolean native-fallback?))

;; ========================================================================
;; Preview zoom & gesture math
;; ========================================================================

(def min-preview-zoom 1.0)
(def max-preview-zoom 4.0)

(defn clamp-number [value min-value max-value]
  (-> value (max min-value) (min max-value)))

(defn point-distance [{x1 :x y1 :y} {x2 :x y2 :y}]
  (let [dx (- x2 x1)
        dy (- y2 y1)]
    (js/Math.sqrt (+ (* dx dx) (* dy dy)))))

(defn point-midpoint [{x1 :x y1 :y} {x2 :x y2 :y}]
  {:x (/ (+ x1 x2) 2)
   :y (/ (+ y1 y2) 2)})

;; ========================================================================
;; Format helpers
;; ========================================================================

(defn format-camera-zoom-label [zoom]
  (str (.toFixed (double (or zoom 1.0)) 1) "x"))

(defn format-camera-zoom-range-label [zoom-range]
  (when (and (map? zoom-range) (number? (:min zoom-range)) (number? (:max zoom-range)))
    (str "(range "
      (format-camera-zoom-label (:min zoom-range))
      "-"
      (format-camera-zoom-label (:max zoom-range))
      ")")))
