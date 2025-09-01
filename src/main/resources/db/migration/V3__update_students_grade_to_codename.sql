-- Update students table: change grade from BIGINT to code/name pair
-- Also make class_group_id nullable to allow students to register without being assigned to a class initially

ALTER TABLE students 
    DROP COLUMN IF EXISTS grade,
    ADD COLUMN grade_code VARCHAR(10) NULL,
    ADD COLUMN grade_name VARCHAR(50) NULL,
    MODIFY COLUMN class_group_id BIGINT NULL;