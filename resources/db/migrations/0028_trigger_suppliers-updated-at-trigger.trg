-- FORWARD
CREATE TRIGGER suppliers_updated_at
       BEFORE UPDATE ON suppliers
       FOR EACH ROW
       EXECUTE FUNCTION update_updated_at_column();

-- BACKWARD
DROP TRIGGER IF EXISTS suppliers_updated_at ON suppliers;