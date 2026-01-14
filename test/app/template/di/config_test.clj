(ns app.template.di.config-test
  (:require
    [app.template.di.config :as di-config]
    [app.template.di.container :as di]
    [clojure.test :refer [deftest is testing]]))

(defrecord StoppableSvc [id events]
  di/Lifecycle
  (init [svc _container] svc)
  (start [svc _container]
    (swap! events conj [:start id])
    svc)
  (stop [svc _container]
    (swap! events conj [:stop id])
    svc))

(deftest stop-services-stops-di-container-test
  (testing "stop-services! shuts down DI-managed services when :di-container exists"
    (let [events (atom [])
          container (-> (di/create-container {})
                      (di/register-service!
                        (di/create-simple-service :x (fn [_] (->StoppableSvc :x events)))))
          container (-> container di/initialize-services! di/start-services!)
          services {:di-container container}]

      (is (= [[:start :x]] @events))

      (di-config/stop-services! services)

      (is (= [[:start :x] [:stop :x]] @events)))))
