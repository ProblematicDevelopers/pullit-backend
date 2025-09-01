-- Manual migration script to fix exam_assignments foreign key
-- Run this script in your MySQL database to update the foreign key constraint

USE pullit; -- Replace with your database name if different

-- Check current foreign key constraints
SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM 
    INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE 
    TABLE_NAME = 'exam_assignments' 
    AND CONSTRAINT_SCHEMA = DATABASE()
    AND REFERENCED_TABLE_NAME IS NOT NULL;

-- Drop the existing foreign key constraint
ALTER TABLE exam_assignments 
DROP FOREIGN KEY FK8bsu0n1s2ciaks8aqn1gjn4jr;

-- Drop any other foreign key that might reference exams table
-- (Run only if there are other constraints found in the check above)
-- ALTER TABLE exam_assignments DROP FOREIGN KEY [constraint_name];

-- Add new foreign key constraint to reference user_exams table
ALTER TABLE exam_assignments 
ADD CONSTRAINT FK_exam_assignments_user_exams 
FOREIGN KEY (exam_id) REFERENCES user_exams(id) ON DELETE CASCADE ON UPDATE CASCADE;

-- Verify the new constraint
SELECT 
    CONSTRAINT_NAME,
    TABLE_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    REFERENCED_COLUMN_NAME
FROM 
    INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE 
    TABLE_NAME = 'exam_assignments' 
    AND CONSTRAINT_SCHEMA = DATABASE()
    AND REFERENCED_TABLE_NAME = 'user_exams';

-- Check if index exists, if not create one
CREATE INDEX IF NOT EXISTS idx_exam_assignments_user_exam_id ON exam_assignments(exam_id);

COMMIT;