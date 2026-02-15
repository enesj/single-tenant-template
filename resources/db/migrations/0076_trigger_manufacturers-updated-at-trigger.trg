-- FORWARD
CREATE TRIGGER manufacturers_updated_at
       BEFORE UPDATE ON manufacturers
       FOR EACH ROW
       EXECUTE FUNCTION update_updated_at_column();

-- BACKWARD
DROP TRIGGER IF EXISTS manufacturers_updated_at ON manufacturers;