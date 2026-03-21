(ns app.template.frontend.api
  "Frontend API configuration and endpoint management.")

;; API version configuration
(def current-api-version "v1")

;; Base API path with version
(def api-base (str "/api/" current-api-version))

;; API endpoint helpers
(defn versioned-endpoint
  "Creates a versioned API endpoint path.
   Examples:
   (versioned-endpoint \"/config\") => \"/api/v1/config\"
   (versioned-endpoint \"/items\") => \"/api/v1/items\""
  [path]
  (str api-base path))

;; Common API endpoints
(def endpoints
  {:config (versioned-endpoint "/config")
   :health (versioned-endpoint "/health")
   :metrics (versioned-endpoint "/metrics")
   :models-data (versioned-endpoint "/models-data")})

(defn entity-endpoint
  "Creates an entity-specific API endpoint.
   Examples:
   (entity-endpoint \"items\") => \"/api/v1/entities/items\"
   (entity-endpoint \"items\" 42) => \"/api/v1/entities/items/42\""
  ([entity-name]
   (versioned-endpoint (str "/entities/" entity-name)))
  ([entity-name id]
   (versioned-endpoint (str "/entities/" entity-name "/" id))))

(defn batch-endpoint
  "Creates a batch operation endpoint.
   Examples:
   (batch-endpoint \"items\" \"delete\") => \"/api/v1/entities/items/batch\"
   (batch-endpoint \"items\" \"update\") => \"/api/v1/entities/items/batch\""
  [entity-name _operation]
  (versioned-endpoint (str "/entities/" entity-name "/batch")))

(defn validate-endpoint
  "Creates a validation endpoint.
   Example:
   (validate-endpoint \"items\") => \"/api/v1/entities/items/validate\""
  [entity-name]
  (versioned-endpoint (str "/entities/" entity-name "/validate")))
