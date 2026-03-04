-- FORWARD
CREATE TRIGGER admin_sessions_updated_at
       BEFORE UPDATE ON admin_sessions
       FOR EACH ROW
       EXECUTE FUNCTION update_updated_at_column();

-- BACKWARD
DROP TRIGGER IF EXISTS admin_sessions_updated_at ON admin_sessions;