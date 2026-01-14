(ns app.domain.backend.expenses.handlers.receipt-upload
  "Receipt upload handlers.

  These endpoints accept multipart uploads (FormData) and create a `receipts` row
  that can later be processed by the out-of-band receipt OCR worker.

  Storage: local filesystem under `upload/stripes/`.
  The stored `storage_key` is the generated filename (relative), so the worker
  can resolve it using `--storage-base-dir upload/stripes`."
  (:require
    [app.domain.backend.expenses.services.receipts :as receipts]
    [app.domain.backend.expenses.handlers.user-expenses.helpers :as h]
    [app.template.backend.routes.admin.utils :as admin-utils]
    [app.template.backend.utils.adapters.database :as db-adapter]
    [cheshire.core :as json]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [ring.util.response :as response]
    [taoensso.timbre :as log])
  (:import
    [java.nio.file Files]
    [java.util UUID]))

(def ^:private max-file-size-bytes (* 10 1024 1024))

(defn- file-prop
  [m k]
  (or (get m k) (get m (name k))))

(defn- uploaded-file
  "Extract a parsed multipart file map from a Ring request.

  Supports both string and keyword keys (depending on middleware)."
  [request]
  (or (get-in request [:multipart-params :file])
    (get-in request [:multipart-params "file"])
    (get-in request [:params :file])
    (get-in request [:params "file"])))

(defn- safe-extension
  [filename content-type]
  (let [ext (some->> filename
              (re-find #"\.[A-Za-z0-9]+$")
              str/lower-case)]
    (cond
      ext ext
      (= content-type "application/pdf") ".pdf"
      (and (string? content-type) (str/starts-with? content-type "image/"))
      (str "." (subs content-type (count "image/")))
      :else "")))

(defn- ensure-upload-dir!
  []
  (let [dir (io/file "upload" "stripes")]
    (when-not (.exists dir)
      (.mkdirs dir))
    dir))

(defn- store-uploaded-file!
  [{:keys [tempfile filename content-type size] :as file-map}]
  (let [tempfile (or tempfile (file-prop file-map :tempfile))
        filename (or filename (file-prop file-map :filename))
        content-type (or content-type (file-prop file-map :content-type))
        size (or size (file-prop file-map :size))]
    (when-not (instance? java.io.File tempfile)
      (throw (ex-info "Invalid upload payload" {:status 400
                                                :file-keys (vec (keys file-map))})))

    (let [bytes (Files/readAllBytes (.toPath ^java.io.File tempfile))
          size (long (or size (alength bytes)))]
      (when (and max-file-size-bytes (> size (long max-file-size-bytes)))
        (throw (ex-info "File too large (max 10MB)" {:status 400
                                                     :size-bytes size
                                                     :max-bytes max-file-size-bytes})))

      (let [ext (safe-extension filename content-type)
            storage-key (str (UUID/randomUUID) ext)
            out-file (io/file (ensure-upload-dir!) storage-key)]
        (Files/write (.toPath out-file) bytes (into-array java.nio.file.OpenOption []))
        {:storage_key storage-key
         :bytes bytes
         :file_size size
         :original_filename filename
         :content_type content-type}))))

(defn- create-receipt-from-upload!
  [db request]
  (let [file (uploaded-file request)]
    (when-not (map? file)
      (throw (ex-info "Missing multipart file param 'file'" {:status 400
                                                             :content-type (get-in request [:headers "content-type"])
                                                             :has-multipart-params? (contains? request :multipart-params)
                                                             :multipart-keys (some-> request :multipart-params keys vec)
                                                             :param-keys (some-> request :params keys vec)})))
    (let [{:keys [storage_key bytes original_filename content_type file_size]} (store-uploaded-file! file)
          user-id (h/get-user-id request)
          result (receipts/upload-receipt! db {:user_id user-id
                                               :storage_key storage_key
                                               :bytes bytes
                                               :original_filename original_filename
                                               :content_type content_type
                                               :file_size file_size})]
      ;; If the receipt is a duplicate, delete the just-uploaded file so we don't
      ;; accumulate orphaned files under upload/stripes/.
      (when (:duplicate? result)
        (try
          (let [uploaded-file (io/file (ensure-upload-dir!) storage_key)]
            (Files/deleteIfExists (.toPath uploaded-file)))
          (catch Exception e
            (log/warn e "Failed to delete duplicate uploaded receipt file" {:storage_key storage_key}))))
      result)))

(def ^:private to-app db-adapter/to-app)

(defn user-upload-handler
  "POST /api/v1/expenses/upload

  Returns {:data <receipt> :duplicate? <bool>}.
  - 201 when a new receipt is created
  - 200 when the upload is a duplicate of an existing receipt"
  [db]
  (fn [request]
    (if-let [forbidden (h/ensure-role request h/receipts-write-roles "Only members, admins, and owners can upload receipts")]
      forbidden
      (try
        (let [{:keys [receipt duplicate?]} (create-receipt-from-upload! db request)
              duplicate? (boolean duplicate?)]
          (-> (response/response (json/generate-string {:data (to-app receipt)
                                                        :duplicate? duplicate?}))
            (response/content-type "application/json")
            (response/status (if duplicate? 200 201))))
        (catch clojure.lang.ExceptionInfo e
          (let [{:keys [status]} (ex-data e)
                status (or status 500)]
            (when (= status 500)
              (log/error e "Upload receipt failed"))
            (-> (response/response (json/generate-string {:error (or (.getMessage e) "Upload failed")
                                                          :details (dissoc (ex-data e) :status)}))
              (response/content-type "application/json")
              (response/status status))))
        (catch Exception e
          (log/error e "Upload receipt failed")
          (-> (response/response (json/generate-string {:error "Upload failed"}))
            (response/content-type "application/json")
            (response/status 500)))))))

(defn admin-upload-handler
  "POST /admin/api/expenses/upload

  Returns {:success true :receipt <receipt> :duplicate? <bool>} (200) on success."
  [db]
  (admin-utils/with-error-handling
    (fn [request]
      (let [{:keys [receipt duplicate?]} (create-receipt-from-upload! db request)]
        (admin-utils/success-response {:receipt (to-app receipt)
                                       :duplicate? (boolean duplicate?)})))
    "Failed to upload receipt"))
