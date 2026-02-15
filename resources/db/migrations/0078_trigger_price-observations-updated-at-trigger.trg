-- FORWARD
CREATE TRIGGER price_observations_updated_at
       BEFORE UPDATE ON price_observations
       FOR EACH ROW
       EXECUTE FUNCTION update_updated_at_column();

-- BACKWARD
DROP TRIGGER IF EXISTS price_observations_updated_at ON price_observations;