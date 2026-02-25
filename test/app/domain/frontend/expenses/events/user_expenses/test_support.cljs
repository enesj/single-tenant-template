(ns app.domain.frontend.expenses.events.user-expenses.test-support
  (:require
    [app.domain.frontend.expenses.events.user-expenses]
    app.template.frontend.events.list.batch
    app.template.frontend.events.list.crud
    [app.template.frontend.helpers-test :as helpers]
    [goog.object :as gobj]
    [re-frame.core :as rf]
    [re-frame.db :as rf-db]))

(defonce captured-http-requests (atom []))

(defn install-fx-stubs! []
  (rf/reg-fx :http-xhrio (fn [req]
                           (swap! captured-http-requests conj req)
                           nil))
  (rf/reg-fx :dispatch-later (fn [_] nil)))

(defn reset-db! []
  (install-fx-stubs!)
  (reset! captured-http-requests [])
  (reset! rf-db/app-db helpers/valid-test-db-state))

(defn last-http-request []
  (last @captured-http-requests))

(defn req-method [req]
  (let [m (or (:method req) (gobj/get req "method"))]
    (cond-> m (string? m) keyword)))

(defn req-uri [req]
  (or (:uri req) (gobj/get req "uri")))

(defn req-body [req]
  (or (:body req) (gobj/get req "body")))

(defn req-params [req]
  (or (:params req) (gobj/get req "params")))

(defn req-ids [req]
  (or (get-in (req-params req) [:ids])
    (let [body (req-body req)]
      (when (string? body)
        (try
          (-> (js/JSON.parse body)
            (js->clj :keywordize-keys true)
            :ids)
          (catch :default _ nil))))))

(defn req-format-content-type [req]
  (let [fmt (or (:format req) (gobj/get req "format"))]
    (or (get fmt :content-type) (gobj/get fmt "content-type"))))

(defn valid-receipt [id]
  {:id id
   :supplier-guess-supplier {:id "supplier-1"}
   :payer-id "payer-1"
   :raw-extract-json {:extraction {:purchased-at "2026-01-01T10:00:00Z"
                                   :totals {:total-amount 5.25
                                            :currency "BAM"}
                                   :items [{:raw-label "Bread"
                                            :line-total 5.25}]}}})
