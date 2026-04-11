(ns app.mobile.frontend.pages.upload-test
  (:require
    [app.mobile.frontend.pages.upload :as sut]
    [cljs.test :refer [deftest is testing]]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(defn- fake-track [capabilities]
  #js {:getCapabilities (fn [] capabilities)})

(defn- fake-error [name]
  (let [err (js/Error.)]
    (set! (.-name err) name)
    err))

(deftest torch-supported-detects-available-capabilities
  (testing "returns true only when the track exposes torch support"
    (is (true? (sut/torch-supported? (fake-track #js {:torch true}))))
    (is (false? (sut/torch-supported? (fake-track #js {:torch false}))))
    (is (false? (sut/torch-supported? #js {})))
    (is (false? (sut/torch-supported? nil)))))

(deftest flash-supported-detects-photo-capabilities
  (testing "flash support comes from ImageCapture photo capabilities"
    (is (= #{"flash" "auto"}
          (sut/supported-fill-light-modes #js {:fillLightMode #js ["flash" "auto"]})))
    (is (true? (sut/flash-supported? #js {:fillLightMode #js ["off" "flash"]})))
    (is (false? (sut/flash-supported? #js {:fillLightMode #js ["off"]})))
    (is (false? (sut/flash-supported? #js {})))))

(deftest default-flash-enabled-follows-availability
  (testing "camera capture starts with flash enabled whenever the browser reports flash support"
    (is (true? (sut/default-flash-enabled? true)))
    (is (false? (sut/default-flash-enabled? false)))
    (is (false? (sut/default-flash-enabled? nil)))))

(deftest native-camera-capture-only-falls-back-when-live-preview-is-unavailable
  (testing "flash attempts stay in the custom UI and native capture is reserved for camera fallback"
    (is (false? (sut/device-flash-mode? false true)))
    (is (false? (sut/device-flash-mode? true true)))
    (is (false? (sut/native-camera-capture? false false true)))
    (is (true? (sut/native-camera-capture? true false false)))
    (is (false? (sut/native-camera-capture? false true false)))))

(deftest preferred-camera-zoom-clamps-to-supported-range
  (testing "default live zoom uses hardware zoom only when the track exposes a supported range"
    (is (= {:min 1 :max 3 :step 0.5}
          (sut/camera-zoom-range #js {:zoom #js {:min 1 :max 3 :step 0.5}})))
    (is (= 1.5
          (sut/preferred-camera-zoom #js {:zoom #js {:min 1 :max 3}} 1.5)))
    (is (= 2
          (sut/preferred-camera-zoom #js {:zoom #js {:min 2 :max 4}} 1.5)))
    (is (= 3
          (sut/preferred-camera-zoom #js {:zoom #js {:min 1 :max 3}} 4)))
    (is (nil? (sut/preferred-camera-zoom #js {} 1.5)))))

(deftest zoom-helpers-keep-overlay-independent
  (testing "pinch helpers clamp zoom and compute geometry for media-only transforms"
    (is (= 4 (sut/clamp-number 10 1 4)))
    (is (= 1 (sut/clamp-number -2 1 4)))
    (is (= 5 (sut/point-distance {:x 0 :y 0} {:x 3 :y 4})))
    (is (= {:x 5 :y 10}
          (sut/point-midpoint {:x 0 :y 0} {:x 10 :y 20})))))

(deftest camera-error-message-covers-common-failures
  (testing "maps browser camera errors to friendly fallback guidance"
    (is (= :mobile/camera-err-not-allowed
          (sut/camera-error-message (fake-error "NotAllowedError"))))
    (is (= :mobile/camera-err-not-found
          (sut/camera-error-message (fake-error "NotFoundError"))))
    (is (= :mobile/camera-err-default
          (sut/camera-error-message (fake-error "SomethingElse"))))))

(deftest bounded-capture-dimensions-keeps-images-small
  (testing "large captures are downscaled while smaller ones are left alone"
    (is (= {:width 1600 :height 900}
          (sut/bounded-capture-dimensions 4032 2268)))
    (is (= {:width 800 :height 600}
          (sut/bounded-capture-dimensions 800 600)))))

(deftest upload-failure-surfaces-server-error
  (testing "mobile upload failures preserve backend error text"
    (reset! rf-db/app-db {})
    (rf/dispatch-sync [:mobile/upload-receipt-failure {:response {:error "File too large (max 10MB)"}}])
    (is (= "File too large (max 10MB)"
          (get-in @rf-db/app-db [:mobile :upload :error])))))

(deftest upload-events-invoke-optional-camera-callbacks
  (testing "camera review actions can react to upload success and failure"
    (reset! rf-db/app-db {})
    (let [success-response {:duplicate? true}
          success-result (atom nil)
          error-result (atom nil)]
      (rf/dispatch-sync [:mobile/upload-receipt-success {:on-success #(reset! success-result %)} success-response])
      (rf/dispatch-sync [:mobile/upload-receipt-failure {:on-error #(reset! error-result %)} {:response {:error "Upload failed badly"}}])
      (is (= success-response @success-result))
      (is (= "Upload failed badly" @error-result)))))
