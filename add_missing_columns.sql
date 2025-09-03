-- calendar_events 테이블에 class_id 컬럼 추가
ALTER TABLE calendar_events 
ADD COLUMN class_id BIGINT,
ADD CONSTRAINT fk_calendar_events_class 
    FOREIGN KEY (class_id) 
    REFERENCES classes(id) 
    ON DELETE SET NULL;

-- 인덱스 추가 (선택사항, 성능 향상을 위해)
CREATE INDEX idx_calendar_events_class_id ON calendar_events(class_id);