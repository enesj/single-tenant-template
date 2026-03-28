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
                payload (select-keys body [:raw_label :raw_label_normalized :unit :supplier_id :article_id])
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

(defn batch-delete-article-aliases-handler
  "Batch delete article aliases (admin/owner only).

  Expects JSON body like:
  {:ids [<uuid> ...]}

  Returns:
  {:data {:deleted-count n :deleted-ids [...] :errors [...]}}"
  [db]
  (fn [request]
    (if-let [_user-id (h/get-user-id request)]
      (if-let [forbidden (h/ensure-role request power-user-roles
                           "Only admins and owners can delete article aliases")]
        forbidden
        (try
          (let [body (h/read-body-params request)
                raw-ids (or (:ids body)
                          (:article_alias_ids body)
                          (:article-alias-ids body)
                          (:articleAliasIds body)
                          [])
                ids (->> raw-ids (map h/try-parse-uuid) (filter some?) vec)]
            (cond
              (empty? raw-ids)
              (h/json-response {:error "No article alias ids provided"} 400)

              (empty? ids)
              (h/json-response {:error "One or more article alias ids are invalid"} 400)

              :else
              (let [delete! (:delete! article-aliases/service)
                    deleted-ids (atom [])
                    errors (atom [])]
                (doseq [alias-id ids]
                  (try
                    (if (boolean (delete! db alias-id))
                      (swap! deleted-ids conj (str alias-id))
                      (swap! errors conj {:id (str alias-id)
                                          :error "not found"}))
                    (catch Exception e
                      (swap! errors conj {:id (str alias-id)
                                          :error (.getMessage e)}))))
                (h/json-response {:data {:deleted-count (count @deleted-ids)
                                         :deleted-ids (vec @deleted-ids)
                                         :errors (vec @errors)}}))))
          (catch Exception e
            (log/error e "Failed to batch delete article aliases")
            (h/json-response {:error "Failed to delete article aliases"} 500))))
      (h/unauthorized-response))))