(ns app.template.frontend.utils.debounce-test
  (:require
    [app.template.frontend.utils.debounce :as debounce]
    [cljs.test :refer-macros [deftest is testing async]]))

(deftest test-debounce
  (testing "debounce function delays execution"
    (async done
      (let [call-count (atom 0)
            increment-count #(swap! call-count inc)
            debounced (debounce/debounce increment-count 100)]

        ;; Call the debounced function multiple times rapidly
        (debounced)
        (debounced)
        (debounced)

        ;; Should not have been called yet
        (is (= @call-count 0))

        ;; Wait for debounce period to pass
        (js/setTimeout
          (fn []
            ;; Should have been called exactly once
            (is (= @call-count 1))
            (done))
          150)))))

(deftest test-debounce-with-cancel-cancellation
  (testing "debounce-with-cancel can be cancelled"
    (async done
      (let [call-count (atom 0)
            increment-count #(swap! call-count inc)
            {:keys [debounced cancel]} (debounce/debounce-with-cancel increment-count 100)]

        ;; Call the debounced function
        (debounced)

        ;; Cancel before it executes
        (cancel)

        ;; Wait for what would have been the debounce period
        (js/setTimeout
          (fn []
            ;; Should not have been called
            (is (= @call-count 0))
            (done))
          150)))))

(deftest test-debounce-with-cancel-execution
  (testing "debounce-with-cancel executes after delay without cancellation"
    (async done
      (let [call-count (atom 0)
            increment-count #(swap! call-count inc)
            {:keys [debounced]} (debounce/debounce-with-cancel increment-count 100)]

        ;; Call the debounced function
        (debounced)

        ;; Wait for debounce period
        (js/setTimeout
          (fn []
            ;; Should have been called once
            (is (= @call-count 1))
            (done))
          150)))))

(deftest test-use-debounced-callback
  (testing "use-debounced-callback creates a stable debounced function"
    (async done
      (let [call-count (atom 0)
            increment-count #(swap! call-count inc)
            debounced (debounce/use-debounced-callback increment-count 100 [])]

        ;; Call multiple times
        (debounced)
        (debounced)
        (debounced)

        ;; Should not execute immediately
        (is (= @call-count 0))

        ;; Wait for debounce period
        (js/setTimeout
          (fn []
            ;; Should have executed once
            (is (= @call-count 1))

            ;; Call again and wait
            (debounced)
            (js/setTimeout
              (fn []
                ;; Should have executed twice total
                (is (= @call-count 2))
                (done))
              150))
          150)))))

(deftest test-debounce-with-arguments
  (testing "debounced function passes arguments correctly"
    (async done
      (let [result (atom nil)
            save-args (fn [a b c] (reset! result [a b c]))
            debounced (debounce/debounce save-args 100)]

        ;; Call with arguments
        (debounced 1 2 3)

        ;; Wait for execution
        (js/setTimeout
          (fn []
            ;; Should have received all arguments
            (is (= @result [1 2 3]))
            (done))
          150)))))

(deftest test-rapid-calls-reset-timer
  (testing "rapid calls reset the debounce timer"
    (async done
      (let [call-count (atom 0)
            increment-count #(swap! call-count inc)
            debounced (debounce/debounce increment-count 50)]

        ;; Call at different intervals
        (debounced)

        ;; Call again after 30ms (before debounce period)
        (js/setTimeout #(debounced) 30)

        ;; Check after original debounce period would have ended
        (js/setTimeout
          (fn []
            ;; Should not have been called yet (timer was reset)
            (is (= @call-count 0))

            ;; Wait for new debounce period to end with some buffer
            (js/setTimeout
              (fn []
                ;; Now should have been called once
                (is (= @call-count 1))
                (done))
              80))
          60)))))
