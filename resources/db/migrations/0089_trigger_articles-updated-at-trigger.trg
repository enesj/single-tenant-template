-- FORWARD
CREATE TRIGGER articles_updated_at
       BEFORE UPDATE ON articles
       FOR EACH ROW
       EXECUTE FUNCTION update_updated_at_column();

-- BACKWARD
DROP TRIGGER IF EXISTS articles_updated_at ON articles;