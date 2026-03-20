-- Fix enum typos: backlog_status
ALTER TYPE backlog_status RENAME VALUE 'In progres' TO 'In progress';
ALTER TYPE backlog_status RENAME VALUE 'Need improvments' TO 'Need improvements';

-- Fix enum typos: backlog_type
ALTER TYPE backlog_type RENAME VALUE 'Improvment' TO 'Improvement';
