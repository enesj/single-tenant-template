(ns app.domain.backend.expenses.services.receipts.storage
  "File storage, hashing, and low-level database helpers for receipts."
  (:require
    [buddy.core.codecs :as codecs]
    [buddy.core.hash :as hash]
    [cheshire.core :as json]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [honey.sql :as sql]
    [next.jdbc :as jdbc]
    [next.jdbc.result-set :as rs]
    [taoensso.timbre :as log])
  (:import
    [java.nio.file Files]
    [java.util UUID]))

(def ^:private local-receipt-storage-base-dir
  (io/file "upload" "stripes"))

(defn compute-file-hash
  "Compute SHA-256 hex digest for uploaded file bytes."
  [bytes]
  (some-> bytes hash/sha256 codecs/bytes->hex))

(defn check-duplicate
  "Return existing receipt with the same file_hash, if any."
  [db file-hash]
  (when file-hash
    (jdbc/execute-one!
      db
      (sql/format {:select [:*]
                   :from [:receipts]
                   :where [:= :file_hash file-hash]
                   :limit 1})
      {:builder-fn rs/as-unqualified-lower-maps})))

(defn jsonb-value
  "Convert a Clojure value into a HoneySQL expression that writes JSONB.

  Accepts either a Clojure value (map/vector/etc) or a pre-encoded JSON string."
  [x]
  (cond
    (nil? x) nil
    (and (vector? x) (= :cast (first x))) x
    (string? x) [:cast x :jsonb]
    :else [:cast (json/generate-string x) :jsonb]))

(defn receipt-status-cast [status]
  (when status
    [:cast status :receipt_status]))

(defn resolve-local-receipt-file
  "Return a java.io.File for a local receipt storage key (relative to `upload/stripes/`).

  Returns nil when:
  - storage-key is blank
  - the resolved file does not exist

  Throws ex-info with :status 400 when the path is unsafe (escapes base dir)."
  [storage-key]
  (let [k (some-> storage-key str/trim not-empty)]
    (when k
      (try
        (let [base-path (.toPath local-receipt-storage-base-dir)
              resolved (.normalize (.resolve base-path k))]
          (when-not (.startsWith resolved base-path)
            (throw (ex-info "Unsafe storage_key path" {:status 400 :storage_key k})))
          (let [f (.toFile resolved)]
            (when (.exists f) f)))
        (catch clojure.lang.ExceptionInfo e
          (throw (ex-info (ex-message e)
                   (assoc (ex-data e) :status 400)
                   e)))))))

(defn delete-receipt-file!
  "Delete the receipt file from disk if it exists.
   
   Handles both regular files (upload/stripes/<storage_key>) and exported files
   (upload/stripes/exported/<storage_key>).

   Logs errors but does not throw - deletion should succeed even if file cleanup fails."
  [receipt]
  (when-let [storage-key (:storage_key receipt)]
    (try
      (when-let [f (resolve-local-receipt-file storage-key)]
        (Files/deleteIfExists (.toPath f))
        (log/info "Deleted receipt file" {:receipt-id (:id receipt)
                                          :storage-key storage-key
                                          :path (.getAbsolutePath f)}))
      (catch Exception e
        (log/warn e "Failed to delete receipt file" {:receipt-id (:id receipt)
                                                     :storage-key storage-key})))))

(defn upload-receipt!
  "Insert a new receipt record. Expects at least :storage_key and either
   :file_hash or :bytes to hash. Returns {:duplicate? bool :receipt {...}}."
  [db {:keys [user_id storage_key file_hash bytes original_filename content_type file_size] :as data}]
  (let [hash (or file_hash (compute-file-hash bytes))]
    (when-not storage_key
      (throw (ex-info "storage_key is required" {:data data})))
    (when-not hash
      (throw (ex-info "file_hash or bytes required" {:data data})))

    (if-let [existing (check-duplicate db hash)]
      {:duplicate? true :receipt existing}
      (let [row {:id (UUID/randomUUID)
                 :user_id user_id
                 :storage_key storage_key
                 :file_hash hash
                 :original_filename original_filename
                 :content_type content_type
                 :file_size file_size
                 :status "uploaded"}
            sql-map {:insert-into :receipts
                     :values [(update row :status #(vector :cast % :receipt_status))]
                     :returning [:*]}]
        {:duplicate? false
         :receipt (jdbc/execute-one! db (sql/format sql-map) {:builder-fn rs/as-unqualified-lower-maps})}))))
