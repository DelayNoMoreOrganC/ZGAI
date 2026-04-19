-- Check all tables in H2 database
SELECT table_name, table_type
FROM information_schema.tables
WHERE table_schema = 'PUBLIC'
ORDER BY table_name;
