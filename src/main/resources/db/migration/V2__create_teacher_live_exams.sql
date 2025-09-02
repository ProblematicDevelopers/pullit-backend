-- 선생님이 생성한 실시간 시험 전용 테이블
CREATE TABLE teacher_live_exams (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exam_name VARCHAR(500) NOT NULL,
    class_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    exam_type VARCHAR(50) DEFAULT 'LIVE_EXAM',
    exam_status VARCHAR(20) DEFAULT 'CREATED', -- CREATED, STARTED, ENDED
    
    -- 시험 정보
    total_items INT DEFAULT 0,
    total_points INT DEFAULT 100,
    time_limit INT, -- 분 단위
    
    -- 시험 일정
    scheduled_date DATE,
    scheduled_time TIME,
    started_at TIMESTAMP NULL,
    ended_at TIMESTAMP NULL,
    
    -- 메타데이터
    description TEXT,
    grade_code VARCHAR(10),
    term_code VARCHAR(10),
    subject_code VARCHAR(20),
    
    -- 감사 필드
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    
    -- 외래키 제약
    CONSTRAINT fk_live_exam_class FOREIGN KEY (class_id) REFERENCES classes(id),
    CONSTRAINT fk_live_exam_teacher FOREIGN KEY (teacher_id) REFERENCES users(id),
    
    -- 인덱스
    INDEX idx_class_status (class_id, exam_status),
    INDEX idx_scheduled_date (scheduled_date),
    INDEX idx_teacher_id (teacher_id)
);

-- 실시간 시험 문제 연결 테이블
CREATE TABLE teacher_live_exam_items (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    live_exam_id BIGINT NOT NULL,
    user_exam_item_id BIGINT NOT NULL,
    item_order INT NOT NULL,
    points INT DEFAULT 5,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT fk_live_exam_items FOREIGN KEY (live_exam_id) REFERENCES teacher_live_exams(id) ON DELETE CASCADE,
    CONSTRAINT fk_exam_item FOREIGN KEY (user_exam_item_id) REFERENCES user_exam_items(id),
    
    UNIQUE KEY uk_exam_item_order (live_exam_id, item_order)
);

-- 실시간 시험 알림 테이블
CREATE TABLE exam_notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    class_id BIGINT NOT NULL,
    exam_id BIGINT NOT NULL,
    exam_type VARCHAR(20) NOT NULL, -- LIVE_EXAM, CBT
    notification_type VARCHAR(30) NOT NULL, -- EXAM_CREATED, EXAM_STARTED, EXAM_ENDED
    message TEXT,
    
    -- 수신자 관리
    target_role VARCHAR(20) DEFAULT 'STUDENT', -- STUDENT, TEACHER, ALL
    is_read BOOLEAN DEFAULT FALSE,
    read_by TEXT, -- JSON array of user IDs who read this
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NULL,
    
    CONSTRAINT fk_notification_class FOREIGN KEY (class_id) REFERENCES classes(id),
    
    INDEX idx_class_notification (class_id, created_at DESC),
    INDEX idx_unread (class_id, is_read)
);

-- user_exams 테이블에 exam_source 컬럼 추가 (기존 테이블 수정)
ALTER TABLE user_exams 
ADD COLUMN exam_source VARCHAR(20) DEFAULT 'STUDENT_CBT' AFTER exam_type,
ADD INDEX idx_exam_source (exam_source, created_by);

-- 시험 타입별 뷰 생성
CREATE VIEW v_student_cbt_exams AS
SELECT * FROM user_exams 
WHERE exam_source = 'STUDENT_CBT' 
AND deleted_date IS NULL;

CREATE VIEW v_teacher_live_exams AS
SELECT 
    tle.*,
    u.full_name as teacher_name,
    c.class_name,
    c.grade,
    c.class_number
FROM teacher_live_exams tle
JOIN users u ON tle.teacher_id = u.id
JOIN classes c ON tle.class_id = c.id
WHERE tle.exam_status != 'DELETED';