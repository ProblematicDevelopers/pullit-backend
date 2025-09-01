-- Migration to change exam_assignments foreign key from exams to user_exams table

-- First, drop the existing foreign key constraint
ALTER TABLE exam_assignments 
DROP FOREIGN KEY FK8bsu0n1s2ciaks8aqn1gjn4jr;

-- Add new foreign key constraint to reference user_exams table
ALTER TABLE exam_assignments 
ADD CONSTRAINT FK_exam_assignments_user_exams 
FOREIGN KEY (exam_id) REFERENCES user_exams(id) ON DELETE CASCADE ON UPDATE CASCADE;

-- Add index for better query performance
CREATE INDEX idx_exam_assignments_user_exam_id ON exam_assignments(exam_id);