(ns app.backend.webserver
  (:require
    [app.backend.routes :as routes]
    [org.httpkit.server :as http-kit]
    [taoensso.timbre :as log])
  (:import
    (java.io Closeable)))

(defn wrap-service-container [handler service-container]
  (fn [request]
    (handler (assoc request :service-container service-container))))

(defn create-webserver [host port database service-container]
  ;; Extract config and models-data from service container (eliminating need for separate parameters)
  (let [routes (-> (routes/app-routes database service-container) ; Pass service container to routes
                 (wrap-service-container service-container)) ; Only need service container middleware now
        server (http-kit/run-server routes
                 {:port port
                  :host host
                  :error-logger (fn [text ex]
                                  ;; Suppress expected coercion failures during tests

                                  (let [test-mode? (= port 8081)
                                        coercion-failure? (and (string? text)
                                                            (or (.contains text "Request coercion failed")
                                                              (.contains text "Exception:Request coercion failed")))
                                        should-suppress? (and test-mode? coercion-failure?)]
                                    ;; Debug logging to understand what's happening
                                    (when (and (string? text) (.contains text "Request coercion failed"))
                                      (log/info "DEBUG: Port:" port "Test mode?" test-mode? "Text:" (str text) "Should suppress?" should-suppress?))
                                    (when-not should-suppress?
                                      ;; Log comprehensive error information
                                      (if ex
                                        (do
                                          (log/error (str text))
                                          (log/error "Exception Class:" (str (.getClass ex)))
                                          (log/error "Exception Message:" (str (.getMessage ex)))
                                          ;; Extract coercion error details from exception data
                                          (when-let [ex-data (select-keys (ex-data ex) [:value :errors])]
                                            (log/error "Exception Data:" (pr-str ex-data))
                                            ;; If this is a coercion error, show the details
                                            (when (and (:coercion ex-data) (:humanized ex-data))
                                              (log/error "Coercion Error:" (:humanized ex-data))))
                                          (when (.getCause ex)
                                            (log/error "Exception Cause:" (str (.getCause ex)))
                                            (log/error "Stack Trace:")
                                            (doseq [element (take 10 (.getStackTrace ex))] ; Limit stack trace to first 10 lines
                                              (log/error "  at " (str element)))))
                                        (log/error (str text))))))})]
    (log/info "Server started on port" port)
    (reify
      Closeable
      (close [_]
        (server)
        (log/info "Server stopped")))))
