-- FORWARD
CREATE TRIGGER admins_updated_at
       BEFORE UPDATE ON admins
       FOR EACH ROW
       EXECUTE FUNCTION update_updated_at_column();

-- BACKWARD
DROP TRIGGER IF EXISTS admins_updated_at ON admins;