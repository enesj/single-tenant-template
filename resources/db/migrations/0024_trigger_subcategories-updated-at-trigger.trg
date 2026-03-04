-- FORWARD
CREATE TRIGGER subcategories_updated_at
       BEFORE UPDATE ON subcategories
       FOR EACH ROW
       EXECUTE FUNCTION update_updated_at_column();

-- BACKWARD
DROP TRIGGER IF EXISTS subcategories_updated_at ON subcategories;