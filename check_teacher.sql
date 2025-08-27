-- Check latest users and teachers
SELECT 'Latest Users:' as info;
SELECT id, username, email, role, created_date FROM users ORDER BY id DESC LIMIT 5;

SELECT 'Latest Teachers:' as info;
SELECT user_id, school_name, area_code, area_name, created_date FROM teachers ORDER BY user_id DESC LIMIT 5;

-- Check specific user
SELECT 'User 34 details:' as info;
SELECT * FROM users WHERE id = 34;

SELECT 'Teacher for User 34:' as info;
SELECT * FROM teachers WHERE user_id = 34;