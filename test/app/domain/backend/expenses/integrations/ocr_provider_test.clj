(ns app.domain.backend.expenses.integrations.ocr-provider-test
  (:require
    [app.domain.backend.expenses.integrations.llamaparse :as llamaparse]
    [app.domain.backend.expenses.integrations.mistral-ocr :as mistral-ocr]
    [app.domain.backend.expenses.integrations.ocr-provider :as ocr-provider]
    [clojure.test :refer [deftest is]]))

(deftest selected-workflow-defaults-to-mistral
  (is (= :mistral (ocr-provider/selected-workflow {:getenv (constantly nil)})))
  (is (= :mistral (ocr-provider/selected-workflow {:getenv (fn [_] "")}))))

(deftest selected-workflow-parses-valid-values
  (is (= :mistral (ocr-provider/selected-workflow {:getenv (fn [_] "mistral")})))
  (is (= :llamaparse (ocr-provider/selected-workflow {:getenv (fn [_] "llamaparse")})))
  (is (= :llamaparse (ocr-provider/selected-workflow {:getenv (fn [_] "\"llamaparse\"")})))
  (is (= :llamaparse (ocr-provider/selected-workflow {:getenv (fn [_] "'llamaparse'")})))
  (is (= :llamaparse (ocr-provider/selected-workflow {:getenv (fn [_] "llama-parse")})))
  (is (= :mistral (ocr-provider/selected-workflow {:getenv (fn [_] "unknown")}))))

(deftest build-provider-selects-llamaparse-when-configured
  (with-redefs [ocr-provider/selected-workflow (fn [& _] :llamaparse)
                llamaparse/build-config (fn [_] {:enabled? true :api-key "lk"})
                llamaparse/ocr-parse! (fn [cfg req] {:provider "llamaparse" :cfg cfg :req req})
                llamaparse/ocr-extract! (fn [cfg req] {:provider "llamaparse" :cfg cfg :req req})]
    (let [provider (ocr-provider/build-provider {:llamaparse {:api-key "lk"}})
          parse-res ((:parse! provider) {:bytes (.getBytes "x")})
          extract-res ((:extract! provider) {:bytes (.getBytes "x")})]
      (is (= :llamaparse (:provider provider)))
      (is (= "LlamaParse" (:provider-name provider)))
      (is (= true (:enabled? provider)))
      (is (= "lk" (:api-key provider)))
      (is (= "llamaparse" (:provider parse-res)))
      (is (= "llamaparse" (:provider extract-res))))))

(deftest build-provider-defaults-to-mistral
  (with-redefs [ocr-provider/selected-workflow (fn [& _] :mistral)
                mistral-ocr/build-config (fn [_] {:enabled? true :api-key "mk"})
                mistral-ocr/ocr-parse! (fn [cfg req] {:provider "mistral" :cfg cfg :req req})
                mistral-ocr/ocr-extract! (fn [cfg req] {:provider "mistral" :cfg cfg :req req})]
    (let [provider (ocr-provider/build-provider {:mistral {:api-key "mk"}})
          parse-res ((:parse! provider) {:bytes (.getBytes "x")})]
      (is (= :mistral (:provider provider)))
      (is (= "Mistral OCR" (:provider-name provider)))
      (is (= "mistral" (:provider parse-res))))))

(deftest provider-messages-are-provider-specific
  (is (= "Receipt OCR is disabled (set MISTRAL_OCR_ENABLED=true to enable)"
        (ocr-provider/disabled-message {:provider :mistral})))
  (is (= "Receipt OCR is disabled (set LLAMAPARSE_ENABLED=true to enable)"
        (ocr-provider/disabled-message {:provider :llamaparse})))
  (is (= "Receipt OCR is not configured (missing MISTRAL_API_KEY)"
        (ocr-provider/missing-api-key-message {:provider :mistral})))
  (is (= "Receipt OCR is not configured (missing LLAMA_CLOUD_API_KEY)"
        (ocr-provider/missing-api-key-message {:provider :llamaparse}))))
