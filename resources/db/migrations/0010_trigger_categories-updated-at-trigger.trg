-- FORWARD
CREATE TRIGGER categories_updated_at
       BEFORE UPDATE ON categories
       FOR EACH ROW
       EXECUTE FUNCTION update_updated_at_column();

-- BACKWARD
DROP TRIGGER IF EXISTS categories_updated_at ON categories;