(ns app.domain.backend.expenses.integrations.mistral-ocr.batch
  "Mistral Batch API functions for OCR processing."
  (:require
    [cheshire.core :as json]
    [clojure.java.io :as io]
    [clojure.string :as str]
    [taoensso.timbre :as log]
    [app.domain.backend.expenses.integrations.mistral-ocr.config :as config]
    [app.domain.backend.expenses.integrations.mistral-ocr.http :as http])
  (:import
    [java.io File]
    [java.time Instant]))

(defn- terminal-batch-status? [status]
  (contains? #{"SUCCESS" "FAILED" "CANCELLED" "CANCELED"}
    (some-> status str/trim str/upper-case)))

(defn- parse-jsonl-lines
  "Parse a JSONL string into a seq of Clojure maps.

  Ignores blank lines."
  [s]
  (->> (str/split-lines (or s ""))
    (map str/trim)
    (remove str/blank?)
    (map #(json/parse-string % true))))

(defn- write-jsonl-temp-file!
  "Write JSONL lines to a temp file and return the File.

  Caller is responsible for deleting it."
  [lines]
  (let [^File f (File/createTempFile "mistral-ocr-batch-" ".jsonl")]
    (with-open [w (io/writer f)]
      (doseq [line lines]
        (.write w (json/generate-string line))
        (.write w "\n")))
    f))

(defn- upload-batch-input-file!
  "Upload an input JSONL to Mistral Files API (purpose=batch) and return file id."
  [cfg ^File f]
  (let [resp (http/request-with-retries!
               cfg
               :post
               (http/files-url cfg)
               {:multipart [{:name "purpose" :content "batch"}
                            {:name "file" :content f :filename (.getName f)}]})
        resp-json (or (http/parse-json-body (:body resp))
                    (throw (http/response->ex "Mistral file upload returned non-JSON" resp)))
        file-id (or (:id resp-json) (:file_id resp-json))]
    (when-not (seq file-id)
      (throw (ex-info "Mistral file upload missing id" {:type :mistral/file-upload-missing-id
                                                        :response resp-json})))
    file-id))

(defn- create-batch-job!
  "Create a batch job for the given endpoint and input file."
  [cfg endpoint input-file-id]
  (let [body {:input_files [input-file-id]
              :model (or (:model cfg) (config/default-model-name))
              :endpoint endpoint}
        resp (http/request-with-retries!
               cfg
               :post
               (http/batch-jobs-url cfg)
               {:content-type :json
                :body (json/generate-string body)})
        resp-json (or (http/parse-json-body (:body resp))
                    (throw (http/response->ex "Mistral batch job create returned non-JSON" resp)))]
    (when-not (seq (:id resp-json))
      (throw (ex-info "Mistral batch job create missing id" {:type :mistral/batch-create-missing-id
                                                             :response resp-json})))
    resp-json))

(defn- get-batch-job!
  [cfg job-id]
  (let [resp (http/request-with-retries! cfg :get (http/batch-job-url cfg job-id) {})]
    (or (http/parse-json-body (:body resp))
      (throw (http/response->ex "Mistral batch job get returned non-JSON" resp)))))

(defn- await-batch-job!
  "Poll a batch job until it reaches a terminal state or timeout.

  Returns the final job map."
  [cfg job-id]
  (let [poll-ms (long (max 250 (or (:batch-poll-ms cfg) 2000)))
        timeout-ms (long (max 1000 (or (:batch-timeout-ms cfg) 600000)))
        deadline (+ (System/currentTimeMillis) timeout-ms)]
    (loop []
      (let [job (get-batch-job! cfg job-id)
            status (:status job)]
        (cond
          (terminal-batch-status? status) job

          (> (System/currentTimeMillis) deadline)
          (throw (ex-info "Mistral batch job timed out"
                   {:type :mistral/batch-timeout
                    :job-id job-id
                    :last-status status
                    :timeout-ms timeout-ms}))

          :else
          (do
            (Thread/sleep poll-ms)
            (recur)))))))

(defn- download-file-content!
  "Download a file from Mistral Files API and return it as text."
  [cfg file-id]
  (let [resp (http/request-with-retries! cfg :get (http/file-content-url cfg file-id) {:as :text})]
    (:body resp)))

(defn- batch-line->ocr-response
  "Normalize a single JSONL output line from Mistral Batch.

  Returns:
  - {:custom-id <string> :resp-json <map>} on success
  - {:custom-id <string> :error <map>} on per-request error or non-2xx
  - {:error <map>} if the line can't be attributed to a request"
  [line]
  (let [custom-id (some-> (:custom_id line) str)
        err (:error line)
        response (:response line)]
    (cond
      (not (seq custom-id))
      {:error {:type :mistral/batch-missing-custom-id
               :line line}}

      err
      {:custom-id custom-id
       :error {:type :mistral/batch-request-error
               :error err
               :line (dissoc line :response)}}

      :else
      (let [status-code (:status_code response)
            body (:body response)
            body-json (cond
                        (map? body) body
                        (string? body) (or (http/parse-json-body body) {:raw body})
                        :else {:raw body})]
        (if (and (integer? status-code) (<= 200 status-code 299))
          {:custom-id custom-id
           :resp-json body-json}
          {:custom-id custom-id
           :error {:type :mistral/batch-non-2xx
                   :status status-code
                   :body-snippet (http/safe-body-snippet (when (string? body) body))
                   :line (dissoc line :response)}})))))

(defn- resp-json->extract-result
  [cfg resp-json]
  (let [markdown (http/pages->markdown resp-json)]
    {:raw resp-json
     :extraction nil
     :parsed-markdown markdown
     :received-at (str (Instant/now))
     :model (or (:model resp-json) (:model cfg) (config/default-model-name))}))

(defn ocr-extract-batch!
  "Run Mistral OCR extraction via Batch API (markdown-only).

  requests: seq of {:custom-id <string> :bytes <byte-array> :content-type <string|nil>}

  Returns:
  {:results {<custom-id> <extract-result>}
   :errors {<custom-id> <error-map>}
   :jobs [<job-map> ...]}"
  [cfg requests]
  (when-not (:batch-enabled? cfg)
    (throw (ex-info "Batch OCR is disabled" {:type :mistral/batch-disabled})))
  (let [max-n (long (max 1 (or (:batch-max-requests cfg) 50)))
        requests (vec requests)
        groups (partition-all max-n requests)]
    (reduce
      (fn [acc group]
        (let [lines (mapv
                      (fn [{:keys [custom-id bytes content-type]}]
                        {:custom_id (str custom-id)
                         :body {:model (or (:model cfg) (config/default-model-name))
                                :document (http/build-document-map bytes content-type)}})
                      group)
              ^File input-file (write-jsonl-temp-file! lines)
              job (try
                    (let [file-id (upload-batch-input-file! cfg input-file)
                          job (create-batch-job! cfg "/v1/ocr" file-id)
                          job-id (:id job)
                          final (await-batch-job! cfg job-id)
                          out-id (:output_file final)
                          err-id (:error_file final)]
                      (when (or (= "FAILED" (some-> (:status final) str/upper-case))
                              (and (not (seq out-id)) (seq err-id)))
                        (throw (ex-info "Mistral batch job failed" {:type :mistral/batch-job-failed
                                                                    :job final
                                                                    :error-file err-id})))
                      (when-not (seq out-id)
                        (throw (ex-info "Mistral batch job missing output_file" {:type :mistral/batch-missing-output-file
                                                                                 :job final})))
                      (let [out-text (download-file-content! cfg out-id)
                            out-lines (parse-jsonl-lines out-text)
                            parsed (mapv batch-line->ocr-response out-lines)
                            ok-lines (filter :resp-json parsed)
                            err-lines (filter :error parsed)
                            results (into {}
                                      (map (fn [{:keys [custom-id resp-json]}]
                                             [custom-id (resp-json->extract-result cfg resp-json)]))
                                      ok-lines)
                            errors (into {}
                                     (keep (fn [{:keys [custom-id error]}]
                                             (when (seq custom-id)
                                               [custom-id error])))
                                     err-lines)]
                        (doseq [unkeyed (keep :error (remove :custom-id err-lines))]
                          (log/warn "Unattributed batch output line" unkeyed))
                        {:job final
                         :results results
                         :errors errors}))
                    (finally
                      (try (.delete input-file) (catch Exception _ nil))))
              group-ids (set (map (comp str :custom-id) group))
              results (:results job)
              missing (seq (remove #(or (contains? results %) (contains? (:errors job) %)) group-ids))
              errors (merge
                       (:errors job)
                       (into {}
                         (map (fn [cid]
                                [cid {:type :mistral/batch-missing-result
                                      :message "Missing result in batch output"}]))
                         (or missing [])))]
          (-> acc
            (update :jobs conj (:job job))
            (update :results merge results)
            (update :errors merge errors))))
      {:jobs [] :results {} :errors {}}
      groups)))
