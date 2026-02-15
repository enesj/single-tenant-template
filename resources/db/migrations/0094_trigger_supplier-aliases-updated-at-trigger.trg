-- FORWARD
CREATE TRIGGER supplier_aliases_updated_at
       BEFORE UPDATE ON supplier_aliases
       FOR EACH ROW
       EXECUTE FUNCTION update_updated_at_column();

-- BACKWARD
DROP TRIGGER IF EXISTS supplier_aliases_updated_at ON supplier_aliases;