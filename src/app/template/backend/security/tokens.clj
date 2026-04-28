(ns app.template.backend.security.tokens
  "Helpers for storing bearer-style tokens safely at rest.")

(defn hash-token
  "Derive the deterministic storage value for a bearer token.

  Raw tokens should be returned only at delivery boundaries such as browser
  sessions, reset URLs, and invitation emails. Database rows store this SHA-256
  hex value so a DB dump does not contain directly reusable bearer tokens. Nil
  remains nil so missing-token callers keep existing no-match behavior."
  [token]
  (when (some? token)
    (let [digest (.digest (java.security.MessageDigest/getInstance "SHA-256")
                   (.getBytes (str token) "UTF-8"))]
      (format "%064x" (java.math.BigInteger. 1 digest)))))