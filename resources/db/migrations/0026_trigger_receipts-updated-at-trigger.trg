-- FORWARD
CREATE TRIGGER receipts_updated_at
       BEFORE UPDATE ON receipts
       FOR EACH ROW
       EXECUTE FUNCTION update_updated_at_column();

-- BACKWARD
DROP TRIGGER IF EXISTS receipts_updated_at ON receipts;