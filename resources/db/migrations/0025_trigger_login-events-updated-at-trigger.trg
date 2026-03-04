-- FORWARD
CREATE TRIGGER login_events_updated_at
       BEFORE UPDATE ON login_events
       FOR EACH ROW
       EXECUTE FUNCTION update_updated_at_column();

-- BACKWARD
DROP TRIGGER IF EXISTS login_events_updated_at ON login_events;