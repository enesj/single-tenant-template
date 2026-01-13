(ns app.shared.http.core
  (:require [ajax.core :as ajax]))

(def ^:private default-headers
  {"Content-Type" "application/json"})

(defn build-xhrio-request
  "Build a standardized :http-xhrio request map.

  This is a low-level helper used by higher-level request builders.
  It centralizes default JSON formats and safe header handling.

  Notes:
  - If :body is present (e.g. FormData), we avoid forcing Content-Type.
  - If the provided :format declares {:content-type false}, we also avoid
    adding Content-Type.
  - If no headers remain after merging, :headers is omitted.

  Accepted keys (common subset):
  :method :uri :params :body :format :response-format :timeout :on-success :on-failure
  :headers (map) :default-headers (map, optional)"
  [{:keys [method uri params body format response-format headers timeout on-success on-failure default-headers]
    :or {headers {}}}]
  (let [format (or format (ajax/json-request-format))
        response-format (or response-format (ajax/json-response-format {:keywords? true}))
        use-default-headers? (and (some? default-headers)
                               (nil? body)
                               (not (false? (:content-type format)))
                               (not (contains? headers "Content-Type")))
        final-headers (merge (when use-default-headers? default-headers) headers)
        base {:method method
              :uri uri
              :format format
              :response-format response-format}
        base (cond-> base
               (some? timeout) (assoc :timeout timeout)
               (some? on-success) (assoc :on-success on-success)
               (some? on-failure) (assoc :on-failure on-failure)
               (some? params) (assoc :params params)
               (some? body) (assoc :body body)
               (seq final-headers) (assoc :headers final-headers))]
    base))

(defn build-request
  ;; Legacy request builder (kept for backwards compatibility).
  ;;
  ;; Prefer `build-xhrio-request` and the higher-level helpers:
  ;; - `app.admin.frontend.utils.http/*`
  ;; - `app.template.frontend.api.http/*`
  ;;
  ;; Historically this reads a token from app-db for admin requests.
  ;; Callers may also pass {:token "..."} in opts to avoid relying on app-db.
  ([db context method endpoint-path]
   (build-request db context method endpoint-path nil))
  ([db context method endpoint-path opts]
   (let [admin-token (when (= context :admin)
                       (or (:token opts)
                         (get-in db [:admin/token])))
         auth-headers (when admin-token
                        {"x-admin-token" admin-token})
         base-path (if (= context :admin)
                     "/admin/api"
                     "/api")
         headers (merge (get opts :headers {}) auth-headers)]
     (build-xhrio-request
       (-> opts
         (assoc :method method
                :uri (str base-path endpoint-path)
                :default-headers default-headers)
         (cond-> (seq headers) (assoc :headers headers)))))))
