package com.pullit.classes.service;

import com.pullit.cbt.dto.request.RedisMigrationRequest;
import com.pullit.cbt.dto.request.RedisUpdateRequest;
import com.pullit.cbt.dto.response.AttemptAnswerResponse;
import com.pullit.cbt.dto.response.CbtExamResponse;
import com.pullit.cbt.dto.response.CbtExamItemResponse;
import com.pullit.cbt.dto.response.RedisDataResponse;
import com.pullit.cbt.dto.response.RedisMigrationResponse;
import com.pullit.cbt.dto.response.RedisUpdateResponse;
import com.pullit.cbt.entity.AttemptExam;
import com.pullit.cbt.entity.AttemptExamQuestion;
import com.pullit.cbt.repository.AttemptExamQuestionRepository;
import com.pullit.cbt.repository.AttemptExamRepository;
import com.pullit.item.dao.ItemHtmlDataRepository;
import com.pullit.item.dao.ItemMetadataRepository;
import com.pullit.classes.dto.request.LiveExamAttemptRequest;
import com.pullit.classes.dto.response.ClassDetailResponse;
import com.pullit.classes.dto.response.LiveExamAttemptResponse;
import com.pullit.classes.dto.response.StudentInfoResponse;
import com.pullit.classes.dto.response.TeacherInfoResponse;
import com.pullit.classes.entity.Classes;
import com.pullit.classes.repository.ClassRepository;
import com.pullit.exam.dto.response.UserExamSchoolResponse;
import com.pullit.exam.entity.UserExam;
import com.pullit.exam.entity.UserExamItem;
import com.pullit.exam.enums.ExamVisibility;
import com.pullit.exam.repository.UserExamRepository;
import com.pullit.student.entity.Student;
import com.pullit.student.repository.StudentRepository;
import com.pullit.teacher.entity.Teacher;
import com.pullit.teacher.repository.TeacherRepository;
import com.pullit.user.dto.response.UserResponse;
import com.pullit.user.entity.User;
import com.pullit.user.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Classes", description = "클래스 API")
public class ClassesServiceImpl implements ClassesService {

        private final ClassRepository classRepository;
        private final StudentRepository studentRepository;
        private final TeacherRepository teacherRepository;
        private final UserExamRepository userExamRepository;
        private final UserRepository userRepository;
        private final AttemptExamRepository attemptExamRepository;
        private final AttemptExamQuestionRepository attemptExamQuestionRepository;
        private final ItemHtmlDataRepository itemHtmlDataRepository;
        private final ItemMetadataRepository itemMetadataRepository;

        @Override
        @Operation(summary = "클래스 ID로 특정 클래스의 상세 정보를 조회", description = "클래스 ID로 특정 클래스의 상세 정보를 조회")
        public ClassDetailResponse getClassDetailById(Long userId) {
                log.info("Getting class detail for class ID: {}", userId);
                Student userStudent = studentRepository.findByUserId(userId);
                Long classId = userStudent.getClassGroupID();
                Classes classEntity = classRepository.findById(classId)
                                .orElseThrow(() -> new RuntimeException("클래스를 찾을 수 없습니다. ID: " + classId));

                return convertToClassDetailResponse(classEntity);
        }

        @Override
        public List<UserExamSchoolResponse> getExamsByClassId(Long classId) {
                List<UserExam> exams = userExamRepository
                                .findByClassIdAndVisibilityAndExamDateGreaterThanEqualOrderByExamDateAsc(classId,
                                                ExamVisibility.SCHOOL, LocalDate.now());
                return exams.stream()
                                .map(e -> {
                                        // 생성자 정보 조회
                                        User creator = userRepository.findById(e.getCreatedBy())
                                                        .orElseThrow(() -> new RuntimeException(
                                                                        "시험 생성자를 찾을 수 없습니다. ID: " + e.getCreatedBy()));
                                        UserResponse creatorResponse = UserResponse.from(creator);

                                        return UserExamSchoolResponse.builder()
                                                        .id(e.getId())
                                                        .examName(e.getExamName())
                                                        .gradeCode(e.getGradeCode())
                                                        .gradeName(e.getGradeName())
                                                        .termCode(e.getTermCode())
                                                        .termName(e.getTermName())
                                                        .areaCode(e.getAreaCode())
                                                        .areaName(e.getAreaName())
                                                        .examType(e.getExamType())
                                                        .visibility(e.getVisibility().name())
                                                        .pdfUrl(e.getPdfUrl())
                                                        .answerPdfUrl(e.getAnswerPdfUrl())
                                                        .timeLimit(e.getTimeLimit())
                                                        .examDate(e.getExamDate())
                                                        .description(e.getDescription())
                                                        .totalItems(e.getTotalItems())
                                                        .createdBy(creatorResponse)
                                                        .build();
                                })
                                .collect(Collectors.toList());

        }

        @Override
        public UserExamSchoolResponse getExamsByClassIdAndExamId(Long classId, Long examId) {
                UserExam exam = userExamRepository.findByIdAndClassId(examId, classId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "시험을 찾을 수 없습니다. examId=" + examId + " classId=" + classId));

                // 생성자 정보 조회
                User creator = userRepository.findById(exam.getCreatedBy())
                                .orElseThrow(() -> new RuntimeException(
                                                "시험 생성자를 찾을 수 없습니다. ID: " + exam.getCreatedBy()));
                UserResponse creatorResponse = UserResponse.from(creator);

                return UserExamSchoolResponse.builder()
                                .id(exam.getId())
                                .examName(exam.getExamName())
                                .gradeCode(exam.getGradeCode())
                                .gradeName(exam.getGradeName())
                                .termCode(exam.getTermCode())
                                .termName(exam.getTermName())
                                .areaCode(exam.getAreaCode())
                                .areaName(exam.getAreaName())
                                .examType(exam.getExamType())
                                .visibility(exam.getVisibility().name())
                                .pdfUrl(exam.getPdfUrl())
                                .answerPdfUrl(exam.getAnswerPdfUrl())
                                .timeLimit(exam.getTimeLimit())
                                .examDate(exam.getExamDate())
                                .description(exam.getDescription())
                                .totalItems(exam.getTotalItems())
                                .createdBy(creatorResponse)
                                .build();
        }

        @Override
        @Transactional
        public LiveExamAttemptResponse createOrGetAttempt(Long userId, LiveExamAttemptRequest request) {
                Long examId = request.getExamId();
                Long classId = request.getClassId();

                // 시험 존재 확인
                UserExam userExam = userExamRepository.findByIdAndClassId(examId, classId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "시험을 찾을 수 없습니다. id=" + examId + " classId=" + classId));
                System.out.println("userExam: " + userExam);

                // 사용자 존재 확인
                User user = userRepository.findById(userId)
                                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id=" + userId));
                System.out.println("user: " + user);

                // 기존 진행 중인 시도 확인
                AttemptExam existingAttempt = attemptExamRepository.findByUserAndExamAndStatus(user, userExam,
                                AttemptExam.AttemptStatus.IN_PROGRESS);
                if (existingAttempt != null) {
                        // 기존 진행 중인 시도가 있으면 반환
                        return LiveExamAttemptResponse.builder()
                                        .attemptId(existingAttempt.getId())
                                        .examId(existingAttempt.getExam().getId())
                                        .status(existingAttempt.getStatus().name())
                                        .userId(existingAttempt.getUser().getId())
                                        .remainTime(existingAttempt.getRemainTime())
                                        .startTime(existingAttempt.getStartedAt() != null
                                                        ? existingAttempt.getStartedAt().toString()
                                                        : null)
                                        .endTime(existingAttempt.getCompletedAt() != null
                                                        ? existingAttempt.getCompletedAt().toString()
                                                        : null)
                                        .build();
                }
                System.out.println("userExam.getTimeLimit(): " + userExam.getTimeLimit());
                System.out.println("userExam ID: " + userExam.getId());
                System.out.println("userExam Name: " + userExam.getExamName());
                System.out.println("userExam Type: " + userExam.getExamType());
                System.out.println("userExam Total Items: " + userExam.getTotalItems());
                // 새로운 시도 생성
                AttemptExam newAttempt = AttemptExam.builder()
                                .user(user)
                                .exam(userExam)
                                .status(AttemptExam.AttemptStatus.IN_PROGRESS)
                                .startedAt(java.time.LocalDateTime.now())
                                .remainTime(userExam.getTimeLimit() != null ? userExam.getTimeLimit() * 60 : null) // 분을
                                                                                                                   // 초로
                                                                                                                   // 변환
                                .build();

                AttemptExam savedAttempt = attemptExamRepository.save(newAttempt);

                // 시험 문제 수만큼 AttemptExamQuestion 미리 생성
                for (UserExamItem examItem : userExam.getExamItems()) {
                        AttemptExamQuestion attemptQuestion = AttemptExamQuestion.builder()
                                        .attemptExam(savedAttempt)
                                        .examItem(examItem)
                                        .userAnswer(null) // 아직 답변하지 않음
                                        .isCorrect(false)
                                        .duration(0)
                                        .points(examItem.getPoints())
                                        .answeredAt(null)
                                        .build();

                        attemptExamQuestionRepository.save(attemptQuestion);
                }

                return LiveExamAttemptResponse.builder()
                                .attemptId(savedAttempt.getId())
                                .examId(savedAttempt.getExam().getId())
                                .status(savedAttempt.getStatus().name())
                                .userId(savedAttempt.getUser().getId())
                                .remainTime(savedAttempt.getRemainTime())
                                .startTime(savedAttempt.getStartedAt() != null ? savedAttempt.getStartedAt().toString()
                                                : null)
                                .endTime(savedAttempt.getCompletedAt() != null
                                                ? savedAttempt.getCompletedAt().toString()
                                                : null)
                                .build();
        }

        @Override
        public CbtExamResponse getLiveExam(Long examId, Long userId) {
                UserExam userExam = userExamRepository.findById(examId)
                                .orElseThrow(() -> new IllegalArgumentException("시험을 찾을 수 없습니다. id=" + examId));
                Student student = studentRepository.findByUserId(userId);
                if (student == null) {
                        throw new IllegalArgumentException("학생을 찾을 수 없습니다. id=" + userId);
                }
                Long classId = student.getClassGroupID();
                // 사용자 권한 확인 (자신의 시험이거나 공개된 시험만 조회 가능)
                if (classId != userExam.getClassId()
                                && userExam.getVisibility() != ExamVisibility.SCHOOL) {
                        throw new IllegalArgumentException("해당 시험에 대한 접근 권한이 없습니다.");
                }

                List<CbtExamItemResponse> examItems = userExam.getExamItems().stream()
                                .map(item -> {
                                        // ItemHtmlData 조회
                                        var itemHtmlData = itemHtmlDataRepository.findByItemId(item.getItemId())
                                                        .orElse(null);

                                        // ItemMetadata 조회하여 문제 유형 확인
                                        var itemMetadata = itemMetadataRepository.findById(item.getItemId())
                                                        .orElse(null);
                                        String questionType = "FREE_CHOICE"; // 기본값

                                        if (itemMetadata != null && itemMetadata.getQuestionForm() != null) {
                                                Long questionFormCode = itemMetadata.getQuestionForm().getCode();
                                                if (questionFormCode != null) {
                                                        switch (questionFormCode.intValue()) {
                                                                case 10:
                                                                        questionType = "FREE_CHOICE"; // 자유 선지형
                                                                        break;
                                                                case 50:
                                                                        questionType = "FIVE_CHOICE"; // 5지 선택
                                                                        break;
                                                                case 60:
                                                                        questionType = "SHORT_ANSWER_ORDERED"; // 단답 유순형
                                                                        break;
                                                                case 61:
                                                                        questionType = "SHORT_ANSWER_UNORDERED"; // 단답
                                                                                                                 // 무순형
                                                                        break;
                                                                default:
                                                                        questionType = "FREE_CHOICE"; // 기본값
                                                                        break;
                                                        }
                                                }
                                        }

                                        return CbtExamItemResponse.builder()
                                                        .itemId(item.getItemId())
                                                        .subjectId(item.getSubjectId())
                                                        .itemOrder(item.getItemOrder())
                                                        .points(item.getPoints())
                                                        .questionText(itemHtmlData != null ? itemHtmlData.getQuestion()
                                                                        : "")
                                                        .questionType(questionType)
                                                        .passage(itemHtmlData != null ? itemHtmlData.getPassage()
                                                                        : null)
                                                        .passageHtml(itemHtmlData != null
                                                                        ? itemHtmlData.getPassageHtml()
                                                                        : null)
                                                        .question(itemHtmlData != null ? itemHtmlData.getQuestion()
                                                                        : null)
                                                        .questionHtml(itemHtmlData != null
                                                                        ? itemHtmlData.getQuestionHtml()
                                                                        : null)
                                                        .choices(itemHtmlData != null ? itemHtmlData.getChoicesAsList()
                                                                        : null)
                                                        .answer(itemHtmlData != null ? itemHtmlData.getAnswer() : null)
                                                        .answerHtml(itemHtmlData != null ? itemHtmlData.getAnswerHtml()
                                                                        : null)
                                                        .explainText(itemHtmlData != null
                                                                        ? itemHtmlData.getExplainText()
                                                                        : null)
                                                        .explainHtml(itemHtmlData != null
                                                                        ? itemHtmlData.getExplainHtml()
                                                                        : null)
                                                        .build();
                                })
                                .toList();

                return CbtExamResponse.builder()
                                .examId(userExam.getId())
                                .examName(userExam.getExamName())
                                .examType(userExam.getExamType())
                                .totalItems(userExam.getTotalItems())
                                .timeLimit(userExam.getTimeLimit())
                                .gradeName(userExam.getGradeName())
                                .termName(userExam.getTermName())
                                .areaName(userExam.getAreaName())
                                .visibility(userExam.getVisibility().name())
                                .examItems(examItems)
                                .build();
        }

        @Override
        public AttemptAnswerResponse getAttemptAnswers(Long attemptId, Long userId) {
                return null;
        }

        @Override
        public RedisUpdateResponse updateRedisData(Long attemptId, RedisUpdateRequest request, Long userId) {
                return null;
        }

        @Override
        public RedisDataResponse getRedisData(Long attemptId, Long userId) {
                return null;
        }

        @Override
        public RedisMigrationResponse migrateRedisToDatabase(Long attemptId, RedisMigrationRequest request,
                        Long userId) {
                return null;
        }

        private ClassDetailResponse convertToClassDetailResponse(Classes classEntity) {
                // 담당 교사 정보 조회
                TeacherInfoResponse teacherInfo = getTeacherInfo(classEntity.getTeacherId());

                // 클래스에 속한 학생들 정보 조회 (임시로 빈 리스트 반환)
                List<StudentInfoResponse> students = getStudentsInClass(classEntity.getClassId());

                return ClassDetailResponse.builder()
                                .classId(classEntity.getClassId())
                                .className(classEntity.getClassName())
                                .classGrade(classEntity.getClassGrade())
                                .classSubject(classEntity.getClassSubject())
                                .createdDate(classEntity.getCreatedDate())
                                .updatedDate(classEntity.getUpdatedDate())
                                .teacher(teacherInfo)
                                .students(students)
                                .totalStudents((long) students.size())
                                .totalTeachers(1L)
                                .build();
        }

        private List<StudentInfoResponse> getStudentsInClass(Long classId) {
                log.info("Getting students for class ID: {}", classId);

                List<Student> students = studentRepository.findByClassGroupID(classId);
                log.info("Found {} students for class ID: {}", students.size(), classId);

                // StudentInfoResponse로 변환
                List<StudentInfoResponse> studentInfoResponses = students.stream()
                                .map(student -> {
                                        // User 정보 조회
                                        var user = studentRepository.findUserByStudentId(student.getUserId())
                                                        .orElse(null);
                                        return StudentInfoResponse.builder()
                                                        .studentId(student.getUserId())
                                                        .studentName(user != null ? user.getFullName() : "Unknown")
                                                        .email(user != null ? user.getEmail() : "")
                                                        .phoneNumber(user != null ? user.getPhone() : "")
                                                        .grade(student.getGrade())
                                                        .studentNumber(student.getStudentNo().toString())
                                                        .enrolledDate(student.getCreatedDate().toLocalDate())
                                                        .status("OFFLINE")
                                                        .build();
                                })
                                .collect(Collectors.toList());

                return studentInfoResponses;
        }

        private TeacherInfoResponse getTeacherInfo(Long teacherId) {
                if (teacherId == null) {
                        return null;
                }

                Teacher teacher = teacherRepository.findByUserIdWithUser(teacherId)
                                .orElse(null);

                if (teacher == null) {
                        log.warn("Teacher not found for ID: {}", teacherId);
                        return null;
                }

                return TeacherInfoResponse.builder()
                                .teacherId(teacher.getUserId())
                                .teacherName(teacher.getUser().getFullName())
                                .email(teacher.getUser().getEmail())
                                .phoneNumber(teacher.getUser().getPhone())
                                .subject(teacher.getAreaDisplayName())
                                .build();
        }
}
