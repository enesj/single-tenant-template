-- FORWARD
CREATE CONSTRAINT TRIGGER tenant_memberships_require_active_owner
       AFTER INSERT OR DELETE OR UPDATE OF tenant_id, role, status ON tenant_memberships
       DEFERRABLE INITIALLY DEFERRED
       FOR EACH ROW
       EXECUTE FUNCTION enforce_tenant_single_active_owner();

-- BACKWARD
DROP TRIGGER IF EXISTS tenant_memberships_require_active_owner ON tenant_memberships;