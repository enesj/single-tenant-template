-- FORWARD
CREATE TRIGGER countries_updated_at
       BEFORE UPDATE ON countries
       FOR EACH ROW
       EXECUTE FUNCTION update_updated_at_column();

-- BACKWARD
DROP TRIGGER IF EXISTS countries_updated_at ON countries;