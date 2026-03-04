-- FORWARD
CREATE TRIGGER expense_categories_updated_at
       BEFORE UPDATE ON expense_categories
       FOR EACH ROW
       EXECUTE FUNCTION update_updated_at_column();

-- BACKWARD
DROP TRIGGER IF EXISTS expense_categories_updated_at ON expense_categories;