-- FORWARD
CREATE TRIGGER payers_updated_at
       BEFORE UPDATE ON payers
       FOR EACH ROW
       EXECUTE FUNCTION update_updated_at_column();

-- BACKWARD
DROP TRIGGER IF EXISTS payers_updated_at ON payers;