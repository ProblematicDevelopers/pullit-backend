package com.pullit.dashboard.service;

import com.pullit.cbt.entity.AttemptExam;
import com.pullit.cbt.repository.AttemptExamRepository;
import com.pullit.classes.entity.Classes;
import com.pullit.classes.repository.ClassRepository;
import com.pullit.dashboard.dto.response.DashboardActivityResponse;
import com.pullit.dashboard.dto.response.DashboardScheduleResponse;
import com.pullit.dashboard.dto.response.DashboardStatsResponse;
import com.pullit.exam.entity.UserExam;
import com.pullit.exam.repository.UserExamRepository;
import com.pullit.schedule.entity.Schedule;
import com.pullit.schedule.repository.ScheduleRepository;
import com.pullit.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class DashboardService {
    
    private final UserExamRepository userExamRepository;
    private final AttemptExamRepository attemptExamRepository;
    private final ClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final ScheduleRepository scheduleRepository;
    
    // 최근 활동 조회
    public List<DashboardActivityResponse> getRecentActivities(Long userId, int limit) {
        List<DashboardActivityResponse> activities = new ArrayList<>();
        
        // 1. 최근 생성한 시험
        List<UserExam> recentExams = userExamRepository.findByCreatedByOrderByCreatedDateDesc(
            userId, PageRequest.of(0, limit/3)
        );
        
        for (UserExam exam : recentExams) {
            activities.add(DashboardActivityResponse.builder()
                .id(exam.getId())
                .type("exam_created")
                .title(exam.getExamName() + " 생성")
                .description(String.format("%d문항 - %s", exam.getTotalItems(), exam.getGradeName()))
                .activityTime(exam.getCreatedDate())
                .relativeTime(getRelativeTime(exam.getCreatedDate()))
                .iconType("exam")
                .relatedId(exam.getId())
                .build());
        }
        
        // 2. 최근 학생 시험 응시
        List<AttemptExam> recentAttempts = attemptExamRepository.findRecentAttemptsByTeacher(
            userId, PageRequest.of(0, limit/3)
        );
        
        for (AttemptExam attempt : recentAttempts) {
            String studentName = attempt.getUser() != null ? attempt.getUser().getFullName() : "알 수 없음";
            String examName = attempt.getExam() != null ? attempt.getExam().getExamName() : "시험";
            
            activities.add(DashboardActivityResponse.builder()
                .id(attempt.getId())
                .type("exam_attempted")
                .title("학생 시험 응시")
                .description(String.format("%s 학생 - %s", studentName, examName))
                .activityTime(attempt.getCreatedDate())
                .relativeTime(getRelativeTime(attempt.getCreatedDate()))
                .iconType("student")
                .relatedId(attempt.getId())
                .build());
        }
        
        // 3. 최근 채점 완료
        List<AttemptExam> recentGrades = attemptExamRepository.findRecentlyGraded(
            userId, PageRequest.of(0, limit/3)
        );
        
        for (AttemptExam grade : recentGrades) {
            String examName = grade.getExam() != null ? grade.getExam().getExamName() : "시험";
            String studentName = grade.getUser() != null ? grade.getUser().getFullName() : "학생";
            
            activities.add(DashboardActivityResponse.builder()
                .id(grade.getId())
                .type("grade_updated")
                .title(examName + " 채점 완료")
                .description(String.format("%s 학생 채점 완료", studentName))
                .activityTime(grade.getUpdatedDate())
                .relativeTime(getRelativeTime(grade.getUpdatedDate()))
                .iconType("grade")
                .relatedId(grade.getId())
                .build());
        }
        
        // 시간순 정렬 후 limit만큼 반환
        return activities.stream()
            .sorted((a, b) -> b.getActivityTime().compareTo(a.getActivityTime()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    // 예정된 일정 조회
    public List<DashboardScheduleResponse> getUpcomingSchedules(Long userId, int limit) {
        List<DashboardScheduleResponse> schedules = new ArrayList<>();
        
        // 1. 예정된 시험 조회
        List<UserExam> upcomingExams = userExamRepository.findUpcomingExams(
            userId, LocalDate.now(), PageRequest.of(0, limit/2)
        );
        
        for (UserExam exam : upcomingExams) {
            if (exam.getExamDate() != null) {
                schedules.add(DashboardScheduleResponse.builder()
                    .id(exam.getId())
                    .title(exam.getExamName())
                    .type("exam")
                    .date(exam.getExamDate())
                    .scheduledDateTime(exam.getExamDate().atStartOfDay())
                    .dateDisplay(getDateDisplay(exam.getExamDate()))
                    .timeDisplay("오전 9:00") // 기본값
                    .participants(getExamParticipants(exam.getId()))
                    .description(exam.getDescription())
                    .examId(exam.getId())
                    .status("upcoming")
                    .build());
            }
        }
        
        // 2. 캘린더 일정 조회
        List<Schedule> upcomingSchedules = scheduleRepository.findUpcomingSchedules(
            userId, LocalDateTime.now(), PageRequest.of(0, limit/2)
        );
        
        for (Schedule schedule : upcomingSchedules) {
            schedules.add(DashboardScheduleResponse.builder()
                .id(schedule.getId())
                .title(schedule.getTitle())
                .type(schedule.getType())
                .date(schedule.getScheduledDate().toLocalDate())
                .time(schedule.getScheduledDate().toLocalTime())
                .scheduledDateTime(schedule.getScheduledDate())
                .dateDisplay(getDateDisplay(schedule.getScheduledDate().toLocalDate()))
                .timeDisplay(formatTime(schedule.getScheduledDate().toLocalTime()))
                .participants(schedule.getParticipants())
                .description(schedule.getDescription())
                .status(schedule.getStatus())
                .build());
        }
        
        // 날짜순 정렬 후 limit만큼 반환
        return schedules.stream()
            .sorted((a, b) -> a.getScheduledDateTime().compareTo(b.getScheduledDateTime()))
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    // 대시보드 통계 조회
    public DashboardStatsResponse getDashboardStats(Long userId) {
        
        // 담당 학급 조회
        List<Classes> teacherClasses = classRepository.findByTeacherId(userId);
        Long classId = teacherClasses.isEmpty() ? null : teacherClasses.get(0).getClassId();
        
        // 통계 데이터 수집
        Long totalStudents = classId != null ? 
            studentRepository.countByClassId(classId) : 0L;
        
        Long activeExams = userExamRepository.countActiveExams(userId);
        Long createdExams = userExamRepository.countByCreatedBy(userId);
        Long totalQuestions = userExamRepository.getTotalQuestionsByTeacher(userId);
        
        Double averageGrade = attemptExamRepository.getAverageScoreByTeacher(userId);
        Integer todayClasses = scheduleRepository.countTodaySchedules(
            userId, LocalDate.now(), "class"
        );
        
        // WebSocket에서 온라인 학생 수는 별도로 처리
        Long onlineStudents = 0L; // Frontend에서 WebSocket으로 실시간 업데이트
        
        return DashboardStatsResponse.builder()
            .totalStudents(totalStudents)
            .activeExams(activeExams)
            .averageGrade(averageGrade != null ? averageGrade : 0.0)
            .todayClasses(todayClasses)
            .createdExams(createdExams)
            .totalQuestions(totalQuestions)
            .onlineStudents(onlineStudents)
            .build();
    }
    
    // 상대 시간 계산
    private String getRelativeTime(LocalDateTime dateTime) {
        LocalDateTime now = LocalDateTime.now();
        long minutes = ChronoUnit.MINUTES.between(dateTime, now);
        long hours = ChronoUnit.HOURS.between(dateTime, now);
        long days = ChronoUnit.DAYS.between(dateTime, now);
        
        if (minutes < 60) {
            return minutes + "분 전";
        } else if (hours < 24) {
            return hours + "시간 전";
        } else if (days < 7) {
            return days + "일 전";
        } else {
            return dateTime.format(DateTimeFormatter.ofPattern("MM월 dd일"));
        }
    }
    
    // 날짜 표시 형식
    private String getDateDisplay(LocalDate date) {
        LocalDate today = LocalDate.now();
        long daysUntil = ChronoUnit.DAYS.between(today, date);
        
        if (daysUntil == 0) {
            return "오늘";
        } else if (daysUntil == 1) {
            return "내일";
        } else if (daysUntil == 2) {
            return "모레";
        } else if (daysUntil <= 7) {
            return date.format(DateTimeFormatter.ofPattern("E요일"));
        } else if (daysUntil <= 14) {
            return "다음주";
        } else {
            return date.format(DateTimeFormatter.ofPattern("MM월 dd일"));
        }
    }
    
    // 시간 형식
    private String formatTime(LocalTime time) {
        return time.format(DateTimeFormatter.ofPattern("a h:mm"));
    }
    
    // 시험 참여자 수 조회
    private Integer getExamParticipants(Long examId) {
        return attemptExamRepository.countByExamId(examId);
    }
    
    // 학생용 - 예정된 시험 조회 (일반시험 + CBT 시험)
    public List<DashboardScheduleResponse> getStudentUpcomingExams(Long studentId, Long classId, int limit) {
        List<DashboardScheduleResponse> exams = new ArrayList<>();
        
        // 1. 일반 시험(UserExam) 조회 - 해당 학급에 할당된 시험
        if (classId != null) {
            List<UserExam> upcomingUserExams = userExamRepository.findUpcomingExamsByClass(
                classId, LocalDate.now(), PageRequest.of(0, limit/2)
            );
            
            for (UserExam exam : upcomingUserExams) {
                if (exam.getExamDate() != null) {
                    exams.add(DashboardScheduleResponse.builder()
                        .id(exam.getId())
                        .title(exam.getExamName())
                        .type("general_exam") // 일반시험 타입
                        .date(exam.getExamDate())
                        .scheduledDateTime(exam.getExamDate().atStartOfDay())
                        .dateDisplay(getDateDisplay(exam.getExamDate()))
                        .timeDisplay(exam.getTimeLimit() != null ? 
                            String.format("제한시간: %d분", exam.getTimeLimit()) : "제한시간 없음")
                        .description(String.format("%s %s - %d문항", 
                            exam.getGradeName(), exam.getAreaName(), exam.getTotalItems()))
                        .examId(exam.getId())
                        .status("upcoming")
                        .build());
                }
            }
        }
        
        // 2. CBT 시험 조회 - 현재는 AttemptExam에서 간접 조회
        // TODO: CbtExam 엔티티가 추가되면 직접 조회하도록 수정
        // 임시로 빈 리스트 반환 (CbtExam 엔티티가 없어서)
        // 실제 구현 시 CbtExam 엔티티와 Repository 생성 필요
        
        // 날짜순 정렬 후 limit만큼 반환
        return exams.stream()
            .sorted((a, b) -> a.getScheduledDateTime().compareTo(b.getScheduledDateTime()))
            .limit(limit)
            .collect(Collectors.toList());
    }
}