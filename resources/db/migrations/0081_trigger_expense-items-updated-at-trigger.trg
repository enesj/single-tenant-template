-- FORWARD
CREATE TRIGGER expense_items_updated_at
       BEFORE UPDATE ON expense_items
       FOR EACH ROW
       EXECUTE FUNCTION update_updated_at_column();

-- BACKWARD
DROP TRIGGER IF EXISTS expense_items_updated_at ON expense_items;