#!/usr/bin/env bb

(ns scripts.bb.web.google-refresh-token
  (:require
    [clojure.data.json :as json]
    [clojure.string :as str])
  (:import
    [java.io BufferedReader InputStreamReader]
    [java.net ServerSocket Socket SocketTimeoutException URI URLDecoder URLEncoder]
    [java.net.http HttpClient HttpRequest HttpRequest$BodyPublishers HttpResponse$BodyHandlers]
    [java.nio.charset StandardCharsets]
    [java.time Duration]))

(def ^:private default-scope "https://mail.google.com/")
(def ^:private default-port 8787)
(def ^:private default-callback-path "/oauth/google/callback")
(def ^:private token-endpoint "https://oauth2.googleapis.com/token")

(defn- env
  [k]
  (some-> (System/getenv k) str/trim not-empty))

(defn- urlencode
  [s]
  (URLEncoder/encode (str s) (.name StandardCharsets/UTF_8)))

(defn- urldecode
  [s]
  (URLDecoder/decode (str s) (.name StandardCharsets/UTF_8)))

(defn- parse-int
  [s default]
  (try
    (Integer/parseInt (str s))
    (catch Exception _
      default)))

(defn- die!
  [msg]
  (binding [*out* *err*]
    (println msg))
  (System/exit 2))

(defn- usage!
  ([] (usage! nil))
  ([msg]
   (when msg
     (binding [*out* *err*]
       (println msg)
       (println)))
   (println "Generate a Gmail API refresh token without OAuth Playground.")
   (println)
   (println "Usage:")
   (println "  bb scripts/bb/web/google_refresh_token.clj authorize-url [options]")
   (println "  bb scripts/bb/web/google_refresh_token.clj exchange-code [options]")
   (println "  bb scripts/bb/web/google_refresh_token.clj local-flow [options]")
   (println)
   (println "Commands:")
   (println "  authorize-url   Print the Google consent URL for your own OAuth client")
   (println "  exchange-code   Exchange an authorization code for tokens")
   (println "  local-flow      Start a local callback server, wait for Google redirect,")
   (println "                  and exchange the code automatically")
   (println)
   (println "Options:")
   (println "  --client-id ID          Defaults to GOOGLE_OAUTH_CLIENT_ID")
   (println "  --client-secret SEC     Defaults to GOOGLE_OAUTH_CLIENT_SECRET")
   (println "  --scope URL             Defaults to https://mail.google.com/")
   (println "  --redirect-uri URI      Explicit redirect URI")
   (println "  --port N                Used by local-flow. Default: 8787")
   (println "  --callback-path PATH    Used by local-flow. Default: /oauth/google/callback")
   (println "  --code CODE             Required for exchange-code")
   (println "  --timeout-seconds N     Used by local-flow. Default: 180")
   (println "  --help                  Show this help")
   (println)
   (println "Examples:")
   (println "  bb scripts/bb/web/google_refresh_token.clj authorize-url \\")
   (println "    --client-id \"$GOOGLE_OAUTH_CLIENT_ID\" \\")
   (println "    --redirect-uri \"http://127.0.0.1:8787/oauth/google/callback\"")
   (println)
   (println "  bb scripts/bb/web/google_refresh_token.clj local-flow")
   (println)
   (println "  bb scripts/bb/web/google_refresh_token.clj exchange-code \\")
   (println "    --code \"CODE_FROM_GOOGLE\" \\")
   (println "    --redirect-uri \"http://127.0.0.1:8787/oauth/google/callback\"")
   (System/exit 0)))

(defn- parse-args
  [args]
  (loop [[arg & more] args
         parsed {:positionals []}]
    (cond
      (nil? arg) parsed
      (= "--help" arg) (assoc parsed :help? true)
      (str/starts-with? arg "--")
      (let [[value & more*] more]
        (when (nil? value)
          (die! (str arg " requires a value")))
        (recur more*
          (case arg
            "--client-id"       (assoc parsed :client-id value)
            "--client-secret"   (assoc parsed :client-secret value)
            "--scope"           (assoc parsed :scope value)
            "--redirect-uri"    (assoc parsed :redirect-uri value)
            "--port"            (assoc parsed :port (parse-int value default-port))
            "--callback-path"   (assoc parsed :callback-path value)
            "--code"            (assoc parsed :code value)
            "--timeout-seconds" (assoc parsed :timeout-seconds (parse-int value 180))
            (die! (str "Unknown option: " arg)))))
      :else
      (recur more (update parsed :positionals conj arg)))))

(defn- redirect-uri
  [{:keys [redirect-uri port callback-path]}]
  (or redirect-uri
    (str "http://127.0.0.1:" (or port default-port) (or callback-path default-callback-path))))

(defn- auth-url
  [{:keys [client-id scope] :as opts}]
  (when-not (seq client-id)
    (die! "Missing client ID. Pass --client-id or set GOOGLE_OAUTH_CLIENT_ID."))
  (let [redirect (redirect-uri opts)
        scope*   (or scope default-scope)
        params   [["client_id" client-id]
                  ["redirect_uri" redirect]
                  ["response_type" "code"]
                  ["scope" scope*]
                  ["access_type" "offline"]
                  ["prompt" "consent"]]]
    (str "https://accounts.google.com/o/oauth2/v2/auth?"
      (str/join "&" (map (fn [[k v]] (str k "=" (urlencode v))) params)))))

(defn- parse-query-string
  [raw-query]
  (if (str/blank? raw-query)
    {}
    (->> (str/split raw-query #"&")
      (keep (fn [part]
              (let [[k v] (str/split part #"=" 2)]
                (when (seq k)
                  [(urldecode k) (some-> v urldecode)]))))
      (into {}))))

(defn- http-client
  []
  (-> (HttpClient/newBuilder)
    (.connectTimeout (Duration/ofSeconds 15))
    (.build)))

(defn- token-request-form
  [{:keys [client-id client-secret code] :as opts}]
  (let [redirect (redirect-uri opts)]
    (when-not (seq client-id)
      (die! "Missing client ID. Pass --client-id or set GOOGLE_OAUTH_CLIENT_ID."))
    (when-not (seq client-secret)
      (die! "Missing client secret. Pass --client-secret or set GOOGLE_OAUTH_CLIENT_SECRET."))
    (when-not (seq code)
      (die! "Missing code. Pass --code CODE."))
    {"client_id" client-id
     "client_secret" client-secret
     "code" code
     "grant_type" "authorization_code"
     "redirect_uri" redirect}))

(defn- form-encode
  [m]
  (->> m
    (map (fn [[k v]] (str (urlencode k) "=" (urlencode v))))
    (str/join "&")))

(defn- exchange-code!
  [opts]
  (let [body      (form-encode (token-request-form opts))
        request   (-> (HttpRequest/newBuilder)
                    (.uri (URI/create token-endpoint))
                    (.timeout (Duration/ofSeconds 20))
                    (.header "Content-Type" "application/x-www-form-urlencoded")
                    (.POST (HttpRequest$BodyPublishers/ofString body))
                    (.build))
        response  (.send (http-client) request (HttpResponse$BodyHandlers/ofString))
        status    (.statusCode response)
        raw-body  (.body response)
        parsed    (json/read-str raw-body :key-fn keyword)]
    (when-not (= 200 status)
      (throw (ex-info "Failed to exchange authorization code for tokens"
               {:status status
                :body parsed})))
    parsed))

(defn- callback-payload
  [target]
  (let [uri    (URI/create (str "http://127.0.0.1" target))
        params (parse-query-string (.getRawQuery uri))]
    (cond
      (seq (get params "error"))
      {:error (get params "error")
       :error-description (get params "error_description")}

      (seq (get params "code"))
      {:code (get params "code")}

      :else
      {:error "missing_code"
       :error-description "Google callback did not include an authorization code."})))

(defn- write-http-response!
  [^Socket socket html]
  (let [body-bytes    (.getBytes html (.name StandardCharsets/UTF_8))
        header-bytes  (.getBytes
                        (str "HTTP/1.1 200 OK\r\n"
                          "Content-Type: text/html; charset=utf-8\r\n"
                          "Content-Length: " (alength body-bytes) "\r\n"
                          "Connection: close\r\n"
                          "\r\n")
                        (.name StandardCharsets/UTF_8))]
    (with-open [out (.getOutputStream socket)]
      (.write out header-bytes)
      (.write out body-bytes)
      (.flush out))))

(defn- read-request-target
  [^Socket socket]
  (let [reader (BufferedReader. (InputStreamReader. (.getInputStream socket) StandardCharsets/UTF_8))
        request-line (.readLine reader)]
    (when (str/blank? request-line)
      (die! "Received an empty HTTP request on the local callback port."))
    (let [[method target _] (str/split request-line #" " 3)]
      (when-not (= "GET" method)
        (die! (str "Unexpected callback HTTP method: " method)))
      target)))

(defn- await-callback!
  [{:keys [port callback-path timeout-seconds]}]
  (with-open [server-socket (ServerSocket. (int (or port default-port)))]
    (.setReuseAddress server-socket true)
    (.setSoTimeout server-socket (* 1000 (int (or timeout-seconds 180))))
    (with-open [socket (.accept server-socket)]
      (let [target (read-request-target socket)
            uri    (URI/create (str "http://127.0.0.1" target))
            path   (.getPath uri)]
        (when-not (= path (or callback-path default-callback-path))
          (die! (str "Received callback on unexpected path: " path)))
        (write-http-response!
          socket
          (str "<!doctype html><html><body style='font-family: sans-serif; padding: 2rem;'>"
            "<h2>Google OAuth callback received</h2>"
            "<p>You can close this tab and return to the terminal.</p>"
            "</body></html>"))
        (callback-payload target)))))

(defn- print-token-result
  [result]
  (println "Token exchange succeeded.")
  (println)
  (println "Save this in Railway as GMAIL_REFRESH_TOKEN:")
  (println)
  (println (or (:refresh_token result) "<no refresh_token returned>"))
  (println)
  (when-not (:refresh_token result)
    (println "Google did not return a refresh_token.")
    (println "This usually means the account already granted consent for this client/scope.")
    (println "Retry with prompt=consent, or revoke the app access and try again."))
  (println)
  (println "Sender account guidance:")
  (println "Use the same Gmail account for SMTP_FROM_EMAIL and for the Google consent you just approved."))

(defn- run-authorize-url!
  [opts]
  (println (auth-url opts)))

(defn- run-exchange-code!
  [opts]
  (print-token-result (exchange-code! opts)))

(defn- run-local-flow!
  [opts]
  (let [timeout-seconds (or (:timeout-seconds opts) 180)]
    (println "Add this redirect URI to your Google OAuth client if it is not already allowed:")
    (println (redirect-uri opts))
    (println)
    (println "Open this URL in your browser and complete consent:")
    (println)
    (println (auth-url opts))
    (println)
    (println (str "Waiting up to " timeout-seconds " seconds for Google callback..."))
    (try
      (let [callback-result (await-callback! opts)]
        (when-let [error (:error callback-result)]
          (die! (str "Google returned an error: " error
                  (when-let [desc (:error-description callback-result)]
                    (str " (" desc ")")))))
        (print-token-result (exchange-code! (assoc opts :code (:code callback-result)))))
      (catch SocketTimeoutException _
        (die! (str "Timed out waiting for Google callback.\n"
                "\n"
                "What to check:\n"
                "1. Open the printed Google URL in a browser on this same laptop.\n"
                "2. Make sure your OAuth client allows this redirect URI:\n"
                "   " (redirect-uri opts) "\n"
                "3. If Google shows a redirect_uri_mismatch error, fix the OAuth client redirect list first.\n"
                "\n"
                "Manual fallback:\n"
                "1. Run: bb google-refresh-token authorize-url\n"
                "2. Complete consent in the browser.\n"
                "3. Copy the 'code' query param from the final redirect URL.\n"
                "4. Run: bb google-refresh-token exchange-code --code \"PASTE_CODE_HERE\" --redirect-uri \"" (redirect-uri opts) "\""))))))

(defn -main
  [& args]
  (let [{:keys [positionals help?]
         :as parsed} (parse-args args)
        command      (first positionals)
        opts         (merge {:client-id (env "GOOGLE_OAUTH_CLIENT_ID")
                             :client-secret (env "GOOGLE_OAUTH_CLIENT_SECRET")
                             :scope default-scope
                             :port default-port
                             :callback-path default-callback-path
                             :timeout-seconds 180}
                       parsed)]
    (when (or help? (nil? command))
      (usage!))
    (case command
      "authorize-url" (run-authorize-url! opts)
      "exchange-code" (run-exchange-code! opts)
      "local-flow"    (run-local-flow! opts)
      (usage! (str "Unknown command: " command)))))

(apply -main *command-line-args*)
