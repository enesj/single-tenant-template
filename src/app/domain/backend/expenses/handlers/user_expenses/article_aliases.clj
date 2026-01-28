(ns app.domain.backend.expenses.handlers.user-expenses.article-aliases
  "User-facing article alias handlers (admin/owner only)."
  (:require
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [app.domain.backend.expenses.services.article-aliases :as article-aliases]
    [taoensso.timbre :as log]))

(def ^:private power-user-roles
  #{"admin" "owner"})

(defn update-article-alias-handler
  "Update article alias fields (admin/owner only)."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request power-user-roles
                           "Only admins and owners can update article aliases")]
        forbidden
        (try
          (let [alias-id (h/try-parse-uuid (get-in request [:path-params :id]))
                body (h/read-body-params request)
                payload (select-keys body [:raw_label :raw_label_normalized :supplier_id :article_id])
                update! (:update! article-aliases/service)]
            (cond
              (nil? alias-id)
              (h/not-found-response "Article alias not found")

              (empty? payload)
              (h/json-response {:error "No article alias fields provided"} 400)

              :else
              (if-let [updated (update! db alias-id payload)]
                (h/json-response {:data updated})
                (h/not-found-response "Article alias not found"))))
          (catch Exception e
            (log/error e "Failed to update article alias" {:alias-id (get-in request [:path-params :id])})
            (h/json-response {:error "Failed to update article alias"} 500))))
      (h/unauthorized-response))))

(defn delete-article-alias-handler
  "Delete article alias (admin/owner only)."
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request power-user-roles
                           "Only admins and owners can delete article aliases")]
        forbidden
        (try
          (let [alias-id (h/try-parse-uuid (get-in request [:path-params :id]))
                delete! (:delete! article-aliases/service)
                deleted? (when alias-id (delete! db alias-id))]
            (if deleted?
              (h/json-response {:data {:deleted true}})
              (h/not-found-response "Article alias not found")))
          (catch Exception e
            (log/error e "Failed to delete article alias" {:alias-id (get-in request [:path-params :id])})
            (h/json-response {:error "Failed to delete article alias"} 500))))
      (h/unauthorized-response))))