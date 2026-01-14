(ns app.template.protocols
  "Core protocols for template infrastructure services.

  These protocols define the contracts for reusable SaaS infrastructure
  components that can be implemented by any domain.")

(defprotocol BusinessService
  "Generic business service lifecycle management"
  (cleanup [this]
    "Cleanup service resources and connections
    Returns: service instance"))

(defprotocol ^:private AuthenticationService
  "Template authentication service interface"
  (authenticate [this credentials]
    "Authenticate user with various credential types (email/password, OAuth token)
    Returns: {:success? boolean :user map :token string :error string}"))

(defprotocol ^:private EntityCRUDService
  "Generic entity CRUD interface for metadata-driven operations"
  (create-entity [this entity-type data context]
    "Create new entity using metadata definitions
    Returns: {:success? boolean :entity map :error string}")
  (get-entity [this entity-type id context]
    "Retrieve entity by ID within tenant context
    Returns: entity map or nil"))

(defprotocol ^:private ValidationService
  "Template validation service interface"
  (validate-entity [this entity-type data context]
    "Validate complete entity data
    Returns: {:valid? boolean :errors map :validated-data map}"))

(comment
  ;; Keep these protocol vars referenced so clojure-lsp/clj-kondo doesn't flag
  ;; them as unused-private-var while they remain useful contracts/examples.
  AuthenticationService
  EntityCRUDService
  ValidationService)
