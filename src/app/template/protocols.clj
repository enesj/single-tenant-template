(ns app.template.protocols
  "Core protocols for template infrastructure services.

  These protocols define the contracts for reusable SaaS infrastructure
  components that can be implemented by any domain.")

(defprotocol BusinessService
  "Generic business service lifecycle management"
  (initialize [this]
    "Initialize service with required dependencies
    Returns: initialized service instance")
  (cleanup [this]
    "Cleanup service resources and connections
    Returns: service instance"))

;; Removed duplicate

(defprotocol AuthenticationService
  "Template authentication service interface"
  (authenticate [this credentials]
    "Authenticate user with various credential types (email/password, OAuth token)
    Returns: {:success? boolean :user map :token string :error string}")
  (create-session [this user-id tenant-id]
    "Create new user session with tenant context
    Returns: {:token string :expires-at instant}")
  (validate-session [this token]
    "Validate session token and return user/tenant context
    Returns: {:valid? boolean :user map :tenant map :error string}")
  (logout [this token]
    "Invalidate session token
    Returns: {:success? boolean}"))

(defprotocol TenantService
  "Template tenant management interface"
  (create-tenant [this tenant-data]
    "Create new tenant with default settings
    Returns: {:success? boolean :tenant map :error string}")
  (get-tenant [this tenant-id]
    "Retrieve tenant by ID
    Returns: tenant map or nil")
  (update-tenant [this tenant-id updates]
    "Update tenant information
    Returns: {:success? boolean :tenant map :error string}")
  (delete-tenant [this tenant-id]
    "Delete tenant and all associated data
    Returns: {:success? boolean :error string}")
  (list-tenants [this filters pagination]
    "List tenants with filtering and pagination
    Returns: {:tenants [map] :total int :page int}"))

(defprotocol EntityCRUDService
  "Generic entity CRUD interface for metadata-driven operations"
  (create-entity [this entity-type data context]
    "Create new entity using metadata definitions
    Returns: {:success? boolean :entity map :error string}")
  (get-entity [this entity-type id context]
    "Retrieve entity by ID within tenant context
    Returns: entity map or nil")
  (update-entity [this entity-type id updates context]
    "Update entity with validation and casting
    Returns: {:success? boolean :entity map :error string}")
  (delete-entity [this entity-type id context]
    "Delete entity by ID
    Returns: {:success? boolean :error string}")
  (list-entities [this entity-type filters pagination context]
    "List entities with filtering, pagination, and joins
    Returns: {:entities [map] :total int :page int}")
  (validate-entity-crud [this entity-type data context]
    "Validate entity data against metadata schema
    Returns: {:valid? boolean :errors [string] :validated-data map}"))

(defprotocol UserService
  "Template user management interface"
  (create-user [this user-data tenant-id]
    "Create new user within tenant
    Returns: {:success? boolean :user map :error string}")
  (get-user [this user-id]
    "Retrieve user by ID
    Returns: user map or nil")
  (update-user [this user-id updates]
    "Update user information
    Returns: {:success? boolean :user map :error string}")
  (list-users [this tenant-id filters pagination]
    "List users within tenant
    Returns: {:users [map] :total int :page int}")
  (invite-user [this email tenant-id role]
    "Send user invitation to tenant
    Returns: {:success? boolean :invitation map :error string}"))

(defprotocol InvitationService
  "Template invitation management interface"
  (create-invitation [this email tenant-id role]
    "Create user invitation
    Returns: {:success? boolean :invitation map :error string}")
  (accept-invitation [this token user-data]
    "Accept invitation and create user account
    Returns: {:success? boolean :user map :error string}")
  (revoke-invitation [this invitation-id]
    "Revoke pending invitation
    Returns: {:success? boolean :error string}")
  (list-invitations [this tenant-id]
    "List pending invitations for tenant
    Returns: {:invitations [map]}"))

(defprotocol ValidationService
  "Template validation service interface"
  (validate-field [this entity-type field-name value context]
    "Validate individual field value
    Returns: {:valid? boolean :error string :coerced-value any}")
  (validate-entity [this entity-type data context]
    "Validate complete entity data
    Returns: {:valid? boolean :errors map :validated-data map}")
  (get-validation-schema [this entity-type]
    "Get validation schema for entity type
    Returns: validation schema map"))

(defprotocol MetadataService
  "Template metadata service interface"
  (get-entity-schema [this entity-type]
    "Get complete schema definition for entity type
    Returns: schema map with fields, constraints, relations")
  (get-field-metadata [this entity-type field-name]
    "Get metadata for specific field
    Returns: field metadata map")
  (list-entity-types [this]
    "List all available entity types
    Returns: [keyword]")
  (get-ui-metadata [this entity-type view-type]
    "Get UI metadata for entity (form, list, detail views)
    Returns: UI configuration map"))
