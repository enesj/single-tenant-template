-- FORWARD
CREATE TRIGGER article_aliases_updated_at
       BEFORE UPDATE ON article_aliases
       FOR EACH ROW
       EXECUTE FUNCTION update_updated_at_column();

-- BACKWARD
DROP TRIGGER IF EXISTS article_aliases_updated_at ON article_aliases;