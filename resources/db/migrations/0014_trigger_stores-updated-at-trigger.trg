-- FORWARD
CREATE TRIGGER stores_updated_at
       BEFORE UPDATE ON stores
       FOR EACH ROW
       EXECUTE FUNCTION update_updated_at_column();

-- BACKWARD
DROP TRIGGER IF EXISTS stores_updated_at ON stores;