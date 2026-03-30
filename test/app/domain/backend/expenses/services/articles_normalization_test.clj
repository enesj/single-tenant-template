(ns app.domain.backend.expenses.services.articles-normalization-test
  (:require
    [app.domain.backend.expenses.services.articles.normalization :as normalization]
    [clojure.test :refer [deftest is testing]]))

(deftest normalize-alias-label-transliterates-bosnian-diacritics
  (testing "single word"
    (is (= "caj" (normalization/normalize-alias-label "ČAJ"))))

  (testing "mixed bosnian letters"
    (is (= "caj-djumbir-sumski-zar-cevap"
          (normalization/normalize-alias-label "Čaj Đumbir Šumski Žar Ćevap")))))

(deftest normalize-article-key-transliterates-bosnian-diacritics
  (is (= "cokolada-djanduja"
        (normalization/normalize-article-key "Čokolada Đanduja"))))

(deftest normalization-still-removes-non-alphanumerics-after-transliteration
  (is (= "caj-menta-20"
        (normalization/normalize-alias-label "Čaj / menta! 20%")))
  (is (= "sok-zuti"
        (normalization/normalize-article-key "Sok žuti"))))
