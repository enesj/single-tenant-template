(ns app.shared.http.core
  (:require [ajax.core :as ajax]))

(def ^:private default-headers
  {"Content-Type" "application/json"})

(defn build-request
  [db context method endpoint-path & [opts]]
  (let [admin-token (when (= context :admin)
                      (get-in db [:admin/token]))
        auth-headers (when admin-token
                       {"x-admin-token" admin-token})
        base-path (if (= context :admin)
                    "/admin/api"
                    "/api")]
    (merge {:method method
            :uri (str base-path endpoint-path)
            :headers (merge default-headers auth-headers (get opts :headers {}))
            :format (ajax/json-request-format)
            :response-format (ajax/json-response-format {:keywords? true})}
      opts)))
