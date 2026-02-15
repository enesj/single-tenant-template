-- FORWARD
CREATE TRIGGER cities_updated_at
       BEFORE UPDATE ON cities
       FOR EACH ROW
       EXECUTE FUNCTION update_updated_at_column();

-- BACKWARD
DROP TRIGGER IF EXISTS cities_updated_at ON cities;