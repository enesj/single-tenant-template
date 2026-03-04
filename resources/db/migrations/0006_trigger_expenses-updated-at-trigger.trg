-- FORWARD
CREATE TRIGGER expenses_updated_at
       BEFORE UPDATE ON expenses
       FOR EACH ROW
       EXECUTE FUNCTION update_updated_at_column();

-- BACKWARD
DROP TRIGGER IF EXISTS expenses_updated_at ON expenses;