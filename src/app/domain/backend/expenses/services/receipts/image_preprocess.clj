(ns app.domain.backend.expenses.services.receipts.image-preprocess
  "Image preprocessing for receipt OCR.

  Pipeline (best-effort by default):
  1) Convert HEIC/HEIF to JPEG
  2) Convert images to monochrome (default: grayscale)
  3) Crop receipt from image (best-effort heuristic)

  The OCR worker uses this to improve text extraction quality before sending the
  image to Mistral OCR.

  Implementation uses ImageMagick (\"magick\" preferred, falls back to \"convert\").
  When the tool is unavailable or processing fails, we fall back to the original
  bytes for non-HEIC images. HEIC can be configured to fail fast.

  Env toggles:
  - RECEIPT_OCR_PREPROCESS_ENABLED=true|false (default true)
  - RECEIPT_OCR_PREPROCESS_CROP_ENABLED=true|false (default true)
  - RECEIPT_OCR_PREPROCESS_MONO_MODE=grayscale|bilevel (default grayscale)
  - RECEIPT_OCR_PREPROCESS_MAX_DIM=2200 (default 2200)
  - RECEIPT_OCR_PREPROCESS_HEIC_STRICT=true|false (default true)"
  (:require
    [clojure.java.io :as io]
    [clojure.java.shell :as shell]
    [clojure.string :as str]
    [taoensso.timbre :as log])
  (:import
    [java.nio.file Files]))

(def ^:dynamic *sh*
  "Command runner used by this module.

  A function like clojure.java.shell/sh. Overridable for tests."
  (fn [& args] (apply shell/sh args)))

(defn- env-truthy?
  [k default-val]
  (let [v (some-> (System/getenv k) str str/trim str/lower-case)]
    (cond
      (nil? v) default-val
      (contains? #{"1" "true" "yes" "y" "on"} v) true
      (contains? #{"0" "false" "no" "n" "off"} v) false
      :else default-val)))

(defn- env->pos-int
  [k default-val]
  (try
    (let [v (some-> (System/getenv k) str str/trim not-empty)]
      (if v
        (max 1 (Long/parseLong v))
        default-val))
    (catch Exception _
      default-val)))

(defn preprocess-enabled? [] (env-truthy? "RECEIPT_OCR_PREPROCESS_ENABLED" true))

(defn- crop-enabled? [] (env-truthy? "RECEIPT_OCR_PREPROCESS_CROP_ENABLED" true))

(defn- heic-strict? [] (env-truthy? "RECEIPT_OCR_PREPROCESS_HEIC_STRICT" true))

(defn- mono-mode
  []
  (let [v (some-> (System/getenv "RECEIPT_OCR_PREPROCESS_MONO_MODE") str str/trim str/lower-case)]
    (cond
      (= v "bilevel") :bilevel
      :else :grayscale)))

(defn- max-dim
  []
  (long (env->pos-int "RECEIPT_OCR_PREPROCESS_MAX_DIM" 2200)))

(defn- safe-sh
  "Run a command and capture failures as a result map.

  Returns the same map as shell/sh, plus optional :exception."
  [& args]
  (try
    (apply *sh* args)
    (catch Exception e
      {:exit 127
       :out ""
       :err (or (.getMessage e) (str e))
       :exception e})))

(defn- command-available?
  [cmd]
  (try
    (let [res (safe-sh cmd "-version")]
      (and (integer? (:exit res)) (zero? (:exit res))))
    (catch Exception _
      false)))

(defonce ^:private imagemagick-binary
  (delay
    (cond
      (command-available? "magick") "magick"
      (command-available? "convert") "convert"
      :else nil)))

(def ^:dynamic *imagemagick-binary*
  "Override ImageMagick binary detection (used mainly in tests).

  When nil, the default detection logic is used."
  nil)

(defn- imagemagick-binary*
  []
  (or *imagemagick-binary* @imagemagick-binary))

(defn- imagemagick!
  "Run ImageMagick with provided args (excluding binary)."
  [& args]
  (let [bin (imagemagick-binary*)]
    (when-not (seq bin)
      (throw (ex-info "ImageMagick is not available (install `magick` or `convert`)"
               {:type :receipt/imagemagick-missing
                :attempted ["magick" "convert"]})))
    (apply safe-sh bin args)))

(defn- content-type->ext
  [content-type]
  (cond
    (not (string? content-type)) nil
    (str/includes? content-type "pdf") ".pdf"
    (str/starts-with? content-type "image/")
    (str "." (subs content-type (count "image/")))
    :else nil))

(defn- filename->ext
  [filename]
  (some->> filename
    str
    str/trim
    (re-find #"\.[A-Za-z0-9]+$")
    str/lower-case))

(defn- image-like?
  [{:keys [content-type filename]}]
  (or (and (string? content-type)
        (str/starts-with? (str/lower-case content-type) "image/"))
    (contains? #{".png" ".jpg" ".jpeg" ".gif" ".webp" ".bmp" ".tif" ".tiff" ".heic" ".heif"}
      (filename->ext filename))))

(defn- pdf?
  [{:keys [content-type filename]}]
  (or (and (string? content-type)
        (str/includes? (str/lower-case content-type) "pdf"))
    (= ".pdf" (filename->ext filename))))

(defn- heic?
  [{:keys [content-type filename]}]
  (let [ct (some-> content-type str str/trim str/lower-case)
        ext (filename->ext filename)]
    (or (= ct "image/heic")
      (= ct "image/heif")
      (= ext ".heic")
      (= ext ".heif"))))

(defn- temp-file!
  [suffix]
  (let [p (Files/createTempFile "receipt-ocr-" (or suffix "") (make-array java.nio.file.attribute.FileAttribute 0))
        f (.toFile p)]
    (.deleteOnExit f)
    f))

(defn- ensure-input-file!
  "Return a java.io.File for input.

  Prefers :path when it exists; otherwise writes :bytes to a temp file."
  [{:keys [path bytes content-type filename]}]
  (let [f (when (seq (some-> path str str/trim))
            (io/file path))]
    (cond
      (and f (.exists f)) f

      (instance? (Class/forName "[B") bytes)
      (let [suffix (or (filename->ext filename)
                     (content-type->ext content-type)
                     ".bin")
            tmp (temp-file! suffix)]
        (Files/write (.toPath tmp) ^bytes bytes (into-array java.nio.file.OpenOption []))
        tmp)

      :else
      (throw (ex-info "Missing receipt bytes/path for preprocessing"
               {:type :receipt/preprocess-missing-input
                :has-bytes? (boolean bytes)
                :path path
                :content-type content-type
                :filename filename})))))

(defn- build-magick-args
  "Build ImageMagick args for input -> output conversion."
  [{:keys [input-path output-path]}]
  (let [max-dim (max-dim)
        crop? (crop-enabled?)
        mono (mono-mode)
        ;; A conservative best-effort crop heuristic. This is intentionally
        ;; non-destructive: if the background isn't uniform, -trim might do
        ;; nothing (which is fine).
        trim-args (when crop?
                    ["-fuzz" "10%"
                     "-trim" "+repage"
                     "-bordercolor" "white"
                     "-border" "20"])
        mono-args (case mono
                    :bilevel ["-colorspace" "Gray" "-threshold" "55%" "-type" "Bilevel"]
                    :grayscale ["-colorspace" "Gray"])]
    (vec
      (concat
        [input-path
         "-auto-orient"
         "-strip"
         ;; tame huge scans/photos
         "-resize" (str max-dim "x" max-dim ">")
         ;; mild contrast helps receipts with poor lighting and improves trimming
         "-auto-level"]
        ;; Crop BEFORE monochrome conversion (helps -trim on colored/noisy backgrounds)
        trim-args
        mono-args
        ["-quality" "90"
         output-path]))))

(defn- build-preview-magick-args
  "Build ImageMagick args for a browser preview conversion.

  Intentionally minimal: preserve colors, only orient/strip/resize and (best-effort)
  trim/crop, then encode as JPEG.

  This is used to make images previewable in-browser while also trying to make the
  receipt occupy most of the preview frame."
  [{:keys [input-path output-path]}]
  (let [max-dim (max-dim)
        ;; A conservative best-effort crop heuristic. This is intentionally
        ;; non-destructive: if the background isn't uniform, -trim might do
        ;; nothing (which is fine). We use a slightly higher fuzz + tiny median
        ;; to cope better with speckled backgrounds.
        trim-args ["-median" "1"
                   "-fuzz" "20%"
                   "-trim" "+repage"
                   "-bordercolor" "white"
                   "-border" "20"]]
    (vec
      (concat
        [input-path
         "-auto-orient"
         "-strip"
         "-resize" (str max-dim "x" max-dim ">")
         ;; mild contrast helps receipts with poor lighting and can improve trimming
         "-auto-level"]
        trim-args
        ["-quality" "90"
         output-path]))))

(defn prepare-for-preview
  "Prepare a receipt file for browser preview.

  - PDFs are returned unchanged.
  - Non-image inputs are returned unchanged.
  - For images, we best-effort generate a resized/cropped JPEG preview using
    ImageMagick.

  Why JPEG? Browser compatibility + smaller payloads.

  Input map keys:
  - :bytes (optional when :path is present)
  - :path (optional when :bytes is present)
  - :content-type (original)
  - :filename (original)

  Returns:
  {:bytes <bytes>
   :content-type <string>
   :preprocessed? <bool>}

  Throws (status 415) when preview conversion is requested for HEIC/HEIF but
  ImageMagick is unavailable, or when HEIC/HEIF conversion fails."
  [{:keys [bytes path content-type filename] :as req}]
  (cond
    (pdf? req)
    {:bytes bytes :content-type content-type :preprocessed? false}

    (not (image-like? req))
    {:bytes bytes :content-type content-type :preprocessed? false}

    :else
    (let [heic? (heic? req)]
      (when (and heic? (not (seq (imagemagick-binary*))))
        (throw (ex-info "HEIC preview requires ImageMagick (magick/convert)"
                 {:type :receipt/heic-preview-unavailable
                  :status 415
                  :attempted ["magick" "convert"]
                  :path path
                  :content-type content-type
                  :filename filename})))

      ;; If ImageMagick isn't available, fall back to original bytes for non-HEIC.
      (if-not (seq (imagemagick-binary*))
        {:bytes bytes :content-type content-type :preprocessed? false}
        (let [input (ensure-input-file! req)
              output (temp-file! ".jpg")
              started (System/nanoTime)
              args (build-preview-magick-args {:input-path (.getAbsolutePath input)
                                               :output-path (.getAbsolutePath output)})
              res (apply imagemagick! args)
              duration-ms (/ (- (System/nanoTime) started) 1000000.0)]
          (when-not (zero? (:exit res))
            (if heic?
              (throw (ex-info "HEIC preview conversion failed"
                       {:type :receipt/heic-preview-failed
                        :status 415
                        :exit (:exit res)
                        :stderr (:err res)
                        :stdout (:out res)
                        :input (.getAbsolutePath input)
                        :output (.getAbsolutePath output)
                        :content-type content-type
                        :filename filename}))
              (do
                (log/warn "Receipt preview conversion failed; falling back to original"
                  {:exit (:exit res)
                   :stderr (:err res)
                   :stdout (:out res)
                   :input (.getAbsolutePath input)
                   :content-type content-type
                   :filename filename})
                {:bytes bytes :content-type content-type :preprocessed? false})))

          (let [out-bytes (Files/readAllBytes (.toPath output))]
            (log/info "Receipt converted for preview"
              {:duration-ms duration-ms
               :output-bytes (alength ^bytes out-bytes)
               :input-path (.getAbsolutePath input)})
            {:bytes out-bytes
             :content-type "image/jpeg"
             :preprocessed? true}))))))

(defn prepare-for-ocr
  "Prepare an uploaded receipt for OCR.

  Input map keys:
  - :bytes (optional when :path is present)
  - :path (optional when :bytes is present)
  - :content-type (original)
  - :filename (original)

  Returns:
  {:bytes <processed-bytes>
   :content-type <string>
   :preprocessed? <bool>}

  Notes:
  - PDFs are returned unchanged.
  - Non-image inputs are returned unchanged.
  - For images, output is always JPEG when preprocessing succeeds."
  [{:keys [bytes path content-type filename] :as req}]
  (cond
    (not (preprocess-enabled?))
    {:bytes bytes :content-type content-type :preprocessed? false}

    (pdf? req)
    {:bytes bytes :content-type content-type :preprocessed? false}

    (not (image-like? req))
    {:bytes bytes :content-type content-type :preprocessed? false}

    :else
    (let [input (ensure-input-file! req)
          output (temp-file! ".jpg")
          heic? (heic? req)
          started (System/nanoTime)]
      (try
        ;; If strict HEIC is enabled, fail fast when no ImageMagick is available.
        (when (and heic? (heic-strict?) (not (seq (imagemagick-binary*))))
          (throw (ex-info "HEIC preprocessing requires ImageMagick (magick/convert)"
                   {:type :receipt/heic-preprocess-unavailable
                    :path path
                    :content-type content-type
                    :filename filename})))

        (let [args (build-magick-args {:input-path (.getAbsolutePath input)
                                       :output-path (.getAbsolutePath output)})
              res (apply imagemagick! args)
              duration-ms (/ (- (System/nanoTime) started) 1000000.0)]
          (when-not (zero? (:exit res))
            (throw (ex-info "Image preprocessing failed"
                     {:type :receipt/preprocess-failed
                      :exit (:exit res)
                      :stderr (:err res)
                      :stdout (:out res)
                      :heic? heic?
                      :input (.getAbsolutePath input)
                      :output (.getAbsolutePath output)})))

          (let [out-bytes (Files/readAllBytes (.toPath output))]
            (log/info "Receipt image preprocessed for OCR"
              {:duration-ms duration-ms
               :heic? heic?
               :mono-mode (name (mono-mode))
               :crop-enabled? (crop-enabled?)
               :input-path (.getAbsolutePath input)
               :output-bytes (alength ^bytes out-bytes)})
            {:bytes out-bytes
             :content-type "image/jpeg"
             :preprocessed? true}))
        (catch Exception e
          (let [details (ex-data e)
                msg (or (.getMessage e) (str (class e)))]
            (if (and heic? (heic-strict?))
              (do
                (log/warn e "Receipt HEIC preprocessing failed (strict)" {:message msg :details details})
                (throw e))
              (do
                (log/warn e "Receipt preprocessing failed; falling back to original bytes"
                  {:message msg
                   :details details
                   :content-type content-type
                   :filename filename
                   :path path})
                {:bytes bytes
                 :content-type content-type
                 :preprocessed? false}))))))))
