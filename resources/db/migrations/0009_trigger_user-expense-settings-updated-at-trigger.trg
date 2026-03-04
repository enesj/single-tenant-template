-- FORWARD
CREATE TRIGGER user_expense_settings_updated_at
       BEFORE UPDATE ON user_expense_settings
       FOR EACH ROW
       EXECUTE FUNCTION update_updated_at_column();

-- BACKWARD
DROP TRIGGER IF EXISTS user_expense_settings_updated_at ON user_expense_settings;