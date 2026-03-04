-- FORWARD
CREATE TRIGGER email_verification_tokens_updated_at
       BEFORE UPDATE ON email_verification_tokens
       FOR EACH ROW
       EXECUTE FUNCTION update_updated_at_column();

-- BACKWARD
DROP TRIGGER IF EXISTS email_verification_tokens_updated_at ON email_verification_tokens;