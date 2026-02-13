(ns app.domain.backend.expenses.services.receipts.image-preprocess-test
  (:require
    [app.domain.backend.expenses.services.receipts.image-preprocess :as image-preprocess]
    [clojure.test :refer [deftest is testing]]))

(deftest prepare-for-preview-non-image-is-noop
  (let [b (byte-array [1 2])]
    (is (= {:bytes b
            :content-type "application/octet-stream"
            :preprocessed? false}
          (image-preprocess/prepare-for-preview {:bytes b
                                                 :content-type "application/octet-stream"
                                                 :filename "x.bin"})))))

(deftest prepare-for-preview-non-heic-image-converts-to-jpeg-when-available
  (testing "non-HEIC images are converted to a cropped/resized JPEG preview"
    (let [calls (atom [])
          jpg-bytes (byte-array [9 8 7 6])
          in-bytes (byte-array [1 2 3])
          res (binding [image-preprocess/*imagemagick-binary* "magick"
                        image-preprocess/*sh*
                        (fn [& args]
                          (swap! calls conj args)
                          (let [out-path (last args)]
                            (java.nio.file.Files/write
                              (.toPath (java.io.File. ^String out-path))
                              ^bytes jpg-bytes
                              (into-array java.nio.file.OpenOption [])))
                          {:exit 0 :out "" :err ""})]
                (image-preprocess/prepare-for-preview {:bytes in-bytes
                                                       :content-type "image/png"
                                                       :filename "x.png"}))
          cmd (first (first @calls))
          args (mapcat identity (map rest @calls))]
      (is (= "magick" cmd))
      (is (= "image/jpeg" (:content-type res)))
      (is (true? (:preprocessed? res)))
      (is (= (seq jpg-bytes) (seq (:bytes res))))
      ;; sanity: ensure we still apply the crop heuristic
      (is (some #(= "-trim" %) args)))))

(deftest prepare-for-preview-heic-without-imagemagick-throws-415
  ;; Force ImageMagick to appear "unavailable" even if it's installed locally.
  (binding [image-preprocess/*imagemagick-binary* ""]
    (try
      (image-preprocess/prepare-for-preview {:bytes (byte-array [1 2])
                                             :content-type "image/heic"
                                             :filename "x.heic"})
      (is false "Expected ExceptionInfo")
      (catch clojure.lang.ExceptionInfo e
        (is (= 415 (:status (ex-data e))))
        (is (= :receipt/heic-preview-unavailable (:type (ex-data e))))))))

(deftest prepare-for-preview-heic-applies-best-effort-crop-args
  (let [calls (atom [])
        jpg-bytes (byte-array [9 8 7 6])
        in-bytes (byte-array [1 2 3])
        res (binding [image-preprocess/*imagemagick-binary* "magick"
                      image-preprocess/*sh*
                      (fn [& args]
                        (swap! calls conj args)
                        (let [out-path (last args)]
                          (java.nio.file.Files/write
                            (.toPath (java.io.File. ^String out-path))
                            ^bytes jpg-bytes
                            (into-array java.nio.file.OpenOption [])))
                        {:exit 0 :out "" :err ""})]
              (image-preprocess/prepare-for-preview {:bytes in-bytes
                                                     :content-type "image/heic"
                                                     :filename "x.heic"}))
        cmd (first (first @calls))
        args (mapcat identity (map rest @calls))]
    (is (= "magick" cmd))
    (is (= "image/jpeg" (:content-type res)))
    (is (true? (:preprocessed? res)))
    (is (= (seq jpg-bytes) (seq (:bytes res))))
    (is (some #(= "-trim" %) args))
    (is (some #(= "-auto-level" %) args))))

(deftest prepare-for-ocr-does-not-apply-crop-args
  (let [calls (atom [])
        jpg-bytes (byte-array [9 8 7 6])
        in-bytes (byte-array [1 2 3])
        res (binding [image-preprocess/*imagemagick-binary* "magick"
                      image-preprocess/*sh*
                      (fn [& args]
                        (swap! calls conj args)
                        (let [out-path (last args)]
                          (java.nio.file.Files/write
                            (.toPath (java.io.File. ^String out-path))
                            ^bytes jpg-bytes
                            (into-array java.nio.file.OpenOption [])))
                        {:exit 0 :out "" :err ""})]
              (image-preprocess/prepare-for-ocr {:bytes in-bytes
                                                 :content-type "image/png"
                                                 :filename "x.png"}))
        cmd (first (first @calls))
        args (mapcat identity (map rest @calls))]
    (is (= "magick" cmd))
    (is (= "image/jpeg" (:content-type res)))
    (is (true? (:preprocessed? res)))
    (is (= (seq jpg-bytes) (seq (:bytes res))))
    ;; OCR preprocessing no longer applies trim/crop flags.
    (is (not (some #(= "-trim" %) args)))
    (is (some #(= "-auto-level" %) args))))
