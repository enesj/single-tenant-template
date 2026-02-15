-- FORWARD
CREATE TRIGGER store_aliases_updated_at
       BEFORE UPDATE ON store_aliases
       FOR EACH ROW
       EXECUTE FUNCTION update_updated_at_column();

-- BACKWARD
DROP TRIGGER IF EXISTS store_aliases_updated_at ON store_aliases;