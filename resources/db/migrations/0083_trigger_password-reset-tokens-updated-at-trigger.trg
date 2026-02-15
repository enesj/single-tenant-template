-- FORWARD
CREATE TRIGGER password_reset_tokens_updated_at
       BEFORE UPDATE ON password_reset_tokens
       FOR EACH ROW
       EXECUTE FUNCTION update_updated_at_column();

-- BACKWARD
DROP TRIGGER IF EXISTS password_reset_tokens_updated_at ON password_reset_tokens;