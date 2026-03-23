(ns app.mobile.frontend.pages.upload-test
  (:require
    [app.mobile.frontend.pages.upload :as sut]
    [cljs.test :refer [deftest is testing]]))

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

(deftest camera-error-message-covers-common-failures
  (testing "maps browser camera errors to friendly fallback guidance"
    (is (= "Camera access was blocked. You can still use the device camera instead."
          (sut/camera-error-message (fake-error "NotAllowedError"))))
    (is (= "No rear camera was found on this device."
          (sut/camera-error-message (fake-error "NotFoundError"))))
    (is (= "Couldn't start the in-app camera. You can still use the device camera instead."
          (sut/camera-error-message (fake-error "SomethingElse"))))))
