-- FORWARD
CREATE TRIGGER audit_logs_updated_at
       BEFORE UPDATE ON audit_logs
       FOR EACH ROW
       EXECUTE FUNCTION update_updated_at_column();

-- BACKWARD
DROP TRIGGER IF EXISTS audit_logs_updated_at ON audit_logs;