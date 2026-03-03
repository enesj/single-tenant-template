-- FORWARD
CREATE CONSTRAINT TRIGGER tenants_require_active_owner
       AFTER INSERT ON tenants
       DEFERRABLE INITIALLY DEFERRED
       FOR EACH ROW
       EXECUTE FUNCTION enforce_tenant_single_active_owner();

-- BACKWARD
DROP TRIGGER IF EXISTS tenants_require_active_owner ON tenants;