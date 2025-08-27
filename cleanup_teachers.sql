-- Check and cleanup duplicate teacher records

-- Show existing teachers
SELECT t.user_id, t.school_id, t.area_code, t.area_name, u.username 
FROM teachers t 
LEFT JOIN users u ON t.user_id = u.id;

-- Delete teacher records that don't have corresponding users
DELETE t FROM teachers t
LEFT JOIN users u ON t.user_id = u.id
WHERE u.id IS NULL;

-- If you want to clean all teacher records to start fresh (BE CAREFUL!)
-- TRUNCATE TABLE teachers;