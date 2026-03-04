CREATE SEQUENCE IF NOT EXISTS backlog_number_seq;
SELECT setval('backlog_number_seq',
  COALESCE((SELECT MAX(number) FROM backlog), 1),
  (SELECT EXISTS (SELECT 1 FROM backlog)));
ALTER SEQUENCE backlog_number_seq OWNED BY backlog.number;
ALTER TABLE backlog ALTER COLUMN number SET DEFAULT nextval('backlog_number_seq');
