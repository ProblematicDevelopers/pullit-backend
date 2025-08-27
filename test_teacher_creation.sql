-- Test script to check teacher creation issue

-- Check recent users with TEACHER role
SELECT 'Users with TEACHER role (last 10):' as info;
SELECT id, username, email, role, created_date 
FROM users 
WHERE role = 'TEACHER' 
ORDER BY id DESC 
LIMIT 10;

-- Check teacher table records
SELECT 'Teacher table records (last 10):' as info;
SELECT user_id, school_name, area_code, area_name, created_date 
FROM teachers 
ORDER BY user_id DESC 
LIMIT 10;

-- Find TEACHER users without teacher records
SELECT 'TEACHER users without teacher table entry:' as info;
SELECT u.id, u.username, u.email, u.created_date
FROM users u
WHERE u.role = 'TEACHER' 
AND NOT EXISTS (
    SELECT 1 FROM teachers t WHERE t.user_id = u.id
)
ORDER BY u.id DESC;

-- Check teacher table structure
SELECT 'Teacher table structure:' as info;
DESCRIBE teachers;

-- Check for any constraints or foreign keys
SELECT 'Teacher table constraints:' as info;
SELECT 
    CONSTRAINT_NAME,
    CONSTRAINT_TYPE,
    TABLE_NAME,
    REFERENCED_TABLE_NAME
FROM information_schema.TABLE_CONSTRAINTS
WHERE TABLE_NAME = 'teachers'
AND TABLE_SCHEMA = 'pullit';