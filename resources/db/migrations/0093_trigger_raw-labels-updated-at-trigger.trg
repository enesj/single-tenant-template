-- FORWARD
CREATE TRIGGER raw_labels_updated_at
       BEFORE UPDATE ON raw_labels
       FOR EACH ROW
       EXECUTE FUNCTION update_updated_at_column();

-- BACKWARD
DROP TRIGGER IF EXISTS raw_labels_updated_at ON raw_labels;