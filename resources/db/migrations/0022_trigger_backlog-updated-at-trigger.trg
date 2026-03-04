-- FORWARD
CREATE TRIGGER backlog_updated_at
       BEFORE UPDATE ON backlog
       FOR EACH ROW
       EXECUTE FUNCTION update_updated_at_column();

-- BACKWARD
DROP TRIGGER IF EXISTS backlog_updated_at ON backlog;