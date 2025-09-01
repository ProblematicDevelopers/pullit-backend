-- Make class_group_id and grade nullable in students table
-- This allows students to register without being assigned to a class initially
-- Teachers can invite them to classes later

ALTER TABLE students 
    MODIFY COLUMN class_group_id BIGINT NULL,
    MODIFY COLUMN grade BIGINT NULL;