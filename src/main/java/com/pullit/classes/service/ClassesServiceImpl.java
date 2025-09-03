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
import com.pullit.classes.dto.request.ClassCreateRequest;
import com.pullit.classes.dto.request.ClassJoinRequest;
import com.pullit.classes.dto.request.ClassUpdateRequest;
import com.pullit.classes.dto.request.StudentInvitationRequest;
import com.pullit.classes.dto.response.ClassCreateResponse;
import com.pullit.classes.dto.response.ClassDetailResponse;
import com.pullit.classes.dto.response.LiveExamAttemptResponse;
import com.pullit.classes.dto.response.StudentInfoResponse;
import com.pullit.classes.dto.response.StudentInvitationResponse;
import com.pullit.classes.dto.response.TeacherInfoResponse;
import com.pullit.classes.entity.ClassInvitation;
import com.pullit.classes.entity.Classes;
import com.pullit.classes.repository.ClassInvitationRepository;
import com.pullit.classes.repository.ClassRepository;
import com.pullit.common.embedded.StringCodeNamePair;
import com.pullit.common.exception.BusinessException;
import com.pullit.common.exception.ErrorCode;
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
import com.pullit.user.entity.UserRole;
import com.pullit.user.repository.UserRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
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
    private final ClassInvitationRepository classInvitationRepository;

    private final AttemptExamRepository attemptExamRepository;
    private final AttemptExamQuestionRepository attemptExamQuestionRepository;
    private final ItemHtmlDataRepository itemHtmlDataRepository;
    private final ItemMetadataRepository itemMetadataRepository;

    @Override
    @Operation(summary = "클래스 ID로 특정 클래스의 상세 정보를 조회", description = "클래스 ID로 특정 클래스의 상세 정보를 조회")
    public ClassDetailResponse getClassDetailById(Long userId) {
        log.info("Getting class detail for user ID: {}", userId);

        // 먼저 학생 정보를 조회
        Student userStudent = studentRepository.findByUserId(userId);

        // 학생 정보가 없으면 null 반환 또는 예외 처리
        if (userStudent == null) {
            log.warn("No student found for user ID: {}", userId);
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "학생 정보를 찾을 수 없습니다.");
        }

        Long classId = userStudent.getClassGroupID();

        // 학생이 클래스에 속해있지 않은 경우
        if (classId == null) {
            log.info("Student {} is not enrolled in any class", userId);
            return null; // 또는 빈 응답 반환
        }

        Classes classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "클래스를 찾을 수 없습니다. ID: " + classId));

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

        // 기존 완료된 시도 확인
        AttemptExam existingDoneAttempt = attemptExamRepository.findByUserAndExamAndStatus(user, userExam,
                AttemptExam.AttemptStatus.DONE);
        if (existingDoneAttempt != null) {
            // 완료된 시험이 있으면 완료 상태로 반환
            return LiveExamAttemptResponse.builder()
                    .attemptId(existingDoneAttempt.getId())
                    .examId(existingDoneAttempt.getExam().getId())
                    .status(AttemptExam.AttemptStatus.DONE) // 완료 상태 표시
                    .userId(existingDoneAttempt.getUser().getId())
                    .remainTime(0) // 완료된 시험이므로 남은 시간 0
                    .startTime(existingDoneAttempt.getStartedAt() != null
                            ? existingDoneAttempt.getStartedAt().toString()
                            : null)
                    .endTime(existingDoneAttempt.getCompletedAt() != null
                            ? existingDoneAttempt.getCompletedAt().toString()
                            : null)
                    .message("이미 완료된 시험입니다.") // 완료 메시지 추가
                    .build();
        }

        // 기존 진행 중인 시도 확인
        AttemptExam existingAttempt = attemptExamRepository.findByUserAndExamAndStatus(user, userExam,
                AttemptExam.AttemptStatus.IN_PROGRESS);
        if (existingAttempt != null) {
            // 기존 진행 중인 시도가 있으면 반환
            return LiveExamAttemptResponse.builder()
                    .attemptId(existingAttempt.getId())
                    .examId(existingAttempt.getExam().getId())
                    .status(AttemptExam.AttemptStatus.IN_PROGRESS)
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
                .status(AttemptExam.AttemptStatus.IN_PROGRESS)
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
                .classGrade(classEntity.getClassGrade() != null ? classEntity.getClassGrade().getCode() : null)
                .classSubject(classEntity.getClassSubject() != null ? classEntity.getClassSubject().getCode() : null)
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
                            .grade(student.getGrade() != null ? convertGradeCodeToNumber(student.getGrade().getCode()) : null)
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

    @Override
    @Transactional
    public ClassCreateResponse createClass(ClassCreateRequest request, Long teacherId) {
        // 1. StringCodeNamePair 생성
        StringCodeNamePair gradePair = StringCodeNamePair.builder()
                .code(request.getClassGrade())
                .name(getGradeName(request.getClassGrade()))
                .build();

        StringCodeNamePair subjectPair = StringCodeNamePair.builder()
                .code(request.getClassSubject())
                .name(getSubjectName(request.getClassSubject()))
                .build();

        // 2. Classes 엔티티 생성
        Classes newClass = Classes.builder()
                .teacherId(teacherId)
                .className(request.getClassName())
                .classGrade(gradePair)
                .classSubject(subjectPair)
                .build();

        Classes savedClass = classRepository.save(newClass);

        // 2. 초대 코드 생성
        String inviteCode = null;
        if (request.getGenerateInviteCode()) {
            inviteCode = generateUniqueInviteCode();

            ClassInvitation invitation = ClassInvitation.builder()
                    .classId(savedClass.getClassId())
                    .inviteCode(inviteCode)
                    .expiresAt(LocalDateTime.now().plusDays(30))
                    .createdBy(teacherId)
                    .build();

            classInvitationRepository.save(invitation);
        }

        // 3. Teacher 정보 조회
        Teacher teacher = teacherRepository.findByUserIdWithUser(teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "선생님 정보를 찾을 수 없습니다"));

        // 4. 응답 생성
        return ClassCreateResponse.builder()
                .classId(savedClass.getClassId())
                .className(savedClass.getClassName())
                .classGrade(savedClass.getClassGrade().getCode())
                .classSubject(savedClass.getClassSubject().getCode())
                .classSubjectName(savedClass.getClassSubject().getName())
                .inviteCode(inviteCode)
                .createdDate(savedClass.getCreatedDate())
                .teacher(ClassCreateResponse.TeacherInfo.builder()
                        .userId(teacher.getUserId())
                        .fullName(teacher.getUser().getFullName())
                        .email(teacher.getUser().getEmail())
                        .schoolName(teacher.getSchool() != null ? teacher.getSchool().getSchoolName() : null)
                        .build())
                .build();
    }

    @Override
    @Transactional
    public StudentInvitationResponse inviteStudents(Long classId, StudentInvitationRequest request, Long teacherId) {
        // 1. 학급 존재 확인
        Classes classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "학급을 찾을 수 없습니다"));

        // 2. 초대 결과 리스트
        List<StudentInvitationResponse.InvitationResult> results = new ArrayList<>();
        List<Long> invitedStudentIds = new ArrayList<>();  // 성공적으로 초대된 학생들의 ID
        int successCount = 0;
        int failedCount = 0;

        // 3. 각 학생 초대 처리
        for (StudentInvitationRequest.StudentInviteInfo studentInfo : request.getStudents()) {
            try {
                // 이메일로 사용자 찾기
                User user = userRepository.findByEmail(studentInfo.getEmail())
                        .orElse(null);

                if (user == null) {
                    results.add(StudentInvitationResponse.InvitationResult.builder()
                            .email(studentInfo.getEmail())
                            .success(false)
                            .message("사용자를 찾을 수 없습니다")
                            .status(StudentInvitationResponse.InvitationStatus.USER_NOT_FOUND)
                            .build());
                    failedCount++;
                    continue;
                }

                // 학생 권한 확인
                if (user.getRole() != UserRole.STUDENT) {
                    results.add(StudentInvitationResponse.InvitationResult.builder()
                            .email(studentInfo.getEmail())
                            .success(false)
                            .message("학생 계정이 아닙니다")
                            .status(StudentInvitationResponse.InvitationStatus.NOT_STUDENT)
                            .build());
                    failedCount++;
                    continue;
                }

                // 학생 정보 조회
                Student student = studentRepository.findByUserId(user.getId());
                if (student == null) {
                    // 학생 레코드가 없으면 생성
                    student = Student.builder()
                            .userId(user.getId())
                            .user(user)
                            .build();
                    student = studentRepository.save(student);
                }

                // 이미 학급에 속해있는지 확인
                if (student.getClassGroupID() != null) {
                    if (student.getClassGroupID().equals(classId)) {
                        results.add(StudentInvitationResponse.InvitationResult.builder()
                                .email(studentInfo.getEmail())
                                .success(false)
                                .message("이미 이 학급의 멤버입니다")
                                .status(StudentInvitationResponse.InvitationStatus.ALREADY_MEMBER)
                                .build());
                    } else {
                        results.add(StudentInvitationResponse.InvitationResult.builder()
                                .email(studentInfo.getEmail())
                                .success(false)
                                .message("이미 다른 학급에 속해있습니다")
                                .status(StudentInvitationResponse.InvitationStatus.ERROR)
                                .build());
                    }
                    failedCount++;
                    continue;
                }

                // 학급에 추가
                student.setClassGroupID(classId);
                if (studentInfo.getStudentNo() != null) {
                    student.setStudentNo(studentInfo.getStudentNo());
                } else {
                    Long maxStudentNo = studentRepository.findMaxStudentNoByClassId(classId)
                            .orElse(0L);
                    student.setStudentNo(maxStudentNo + 1);
                }
                // 학년 정보 설정 (StringCodeNamePair 타입)
                student.setGrade(classEntity.getClassGrade());
                studentRepository.save(student);

                // 성공적으로 초대된 학생의 ID를 리스트에 추가
                invitedStudentIds.add(user.getId());

                results.add(StudentInvitationResponse.InvitationResult.builder()
                        .email(studentInfo.getEmail())
                        .success(true)
                        .message("초대가 완료되었습니다")
                        .status(StudentInvitationResponse.InvitationStatus.SENT)
                        .build());
                successCount++;

            } catch (Exception e) {
                log.error("학생 초대 중 오류 발생: {}", studentInfo.getEmail(), e);
                results.add(StudentInvitationResponse.InvitationResult.builder()
                        .email(studentInfo.getEmail())
                        .success(false)
                        .message("초대 중 오류가 발생했습니다")
                        .status(StudentInvitationResponse.InvitationStatus.ERROR)
                        .build());
                failedCount++;
            }
        }

        return StudentInvitationResponse.builder()
                .classId(classId)
                .className(classEntity.getClassName())
                .totalInvited(request.getStudents().size())
                .successCount(successCount)
                .failedCount(failedCount)
                .results(results)
                .invitedStudentIds(invitedStudentIds)  // 초대된 학생 ID 리스트 추가
                .build();
    }

    @Override
    @Transactional
    public ClassDetailResponse joinClassByInviteCode(ClassJoinRequest request, Long studentId) {
        // 1. 초대 코드 유효성 확인
        ClassInvitation invitation = classInvitationRepository
                .findActiveByInviteCode(request.getInviteCode())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_INPUT, "유효하지 않은 초대 코드입니다"));

        if (!invitation.isUsable()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "만료되었거나 사용할 수 없는 초대 코드입니다");
        }

        // 2. 학생 정보 조회
        Student student = studentRepository.findByUserId(studentId);
        if (student == null) {
            // 학생 레코드가 없으면 생성
            User user = userRepository.findById(studentId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다"));

            student = Student.builder()
                    .userId(studentId)
                    .user(user)
                    .build();
            student = studentRepository.save(student);
        }

        if (student.getClassGroupID() != null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "이미 다른 학급에 소속되어 있습니다");
        }

        // 3. 학급 정보 조회
        Classes classEntity = classRepository.findById(invitation.getClassId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "학급을 찾을 수 없습니다"));

        // 4. 학생의 classGroupID 업데이트
        student.setClassGroupID(invitation.getClassId());
        // 학년 코드를 숫자로 변환 (07 -> 1, 08 -> 2, 09 -> 3)
        if (classEntity.getClassGrade() != null) {
            // 학년 정보 설정 (StringCodeNamePair 타입)
            student.setGrade(classEntity.getClassGrade());
        }

        // 학번 설정
        if (request.getStudentNo() != null) {
            student.setStudentNo(request.getStudentNo());
        } else {
            Long maxStudentNo = studentRepository.findMaxStudentNoByClassId(invitation.getClassId())
                    .orElse(0L);
            student.setStudentNo(maxStudentNo + 1);
        }

        studentRepository.save(student);

        // 5. 초대 코드 사용 횟수 증가
        invitation.incrementUsage();
        classInvitationRepository.save(invitation);

        // 6. 학급 상세 정보 반환
        return convertToClassDetailResponse(classEntity);
    }

    @Override
    @Transactional
    public String regenerateInviteCode(Long classId, Long teacherId) {
        // 1. 기존 초대 코드 비활성화
        classInvitationRepository.deactivateAllByClassId(classId);

        // 2. 새로운 초대 코드 생성
        String newInviteCode = generateUniqueInviteCode();

        ClassInvitation invitation = ClassInvitation.builder()
                .classId(classId)
                .inviteCode(newInviteCode)
                .expiresAt(LocalDateTime.now().plusDays(30))
                .createdBy(teacherId)
                .isActive(true)
                .build();

        classInvitationRepository.save(invitation);

        return newInviteCode;
    }

    @Override
    @Transactional
    public void removeStudentFromClass(Long classId, Long studentId) {
        Student student = studentRepository.findByUserId(studentId);
        if (student == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "학생을 찾을 수 없습니다");
        }

        if (!classId.equals(student.getClassGroupID())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "해당 학생은 이 학급의 멤버가 아닙니다");
        }

        student.setClassGroupID(null);
        student.setStudentNo(null);
        studentRepository.save(student);
    }

    @Override
    public boolean isClassMember(Long classId, Long userId) {
        Student student = studentRepository.findByUserId(userId);
        return student != null && classId.equals(student.getClassGroupID());
    }

    @Override
    public boolean isClassOwner(Long classId, Long teacherId) {
        return classRepository.findById(classId)
                .map(c -> c.getTeacherId().equals(teacherId))
                .orElse(false);
    }

    @Override
    public String generateUniqueInviteCode() {
        SecureRandom random = new SecureRandom();
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        int length = 8;

        String code;
        do {
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                sb.append(characters.charAt(random.nextInt(characters.length())));
            }
            code = sb.toString();
        } while (classInvitationRepository.existsByInviteCode(code));

        return code;
    }

    private String getSubjectName(String subjectCode) {
        Map<String, String> subjectMap = new HashMap<>();
        // DB 코드 매핑
        subjectMap.put("KO", "국어");
        subjectMap.put("MA", "수학");
        subjectMap.put("EN", "영어");
        subjectMap.put("SC", "과학");
        subjectMap.put("SO", "사회");

        return subjectMap.getOrDefault(subjectCode, subjectCode);
    }

    private String getGradeName(String gradeCode) {
        Map<String, String> gradeMap = new HashMap<>();
        // DB 코드 매핑
        gradeMap.put("07", "1학년");
        gradeMap.put("08", "2학년");
        gradeMap.put("09", "3학년");

        return gradeMap.getOrDefault(gradeCode, gradeCode);
    }

    private Long convertGradeCodeToNumber(String gradeCode) {
        // 학년 코드를 숫자로 변환 (07 -> 1, 08 -> 2, 09 -> 3)
        switch (gradeCode) {
            case "07":
                return 1L;
            case "08":
                return 2L;
            case "09":
                return 3L;
            default:
                return 1L;
        }
    }

    private String convertGradeNumberToCode(Long gradeNumber) {
        // 숫자 학년을 코드로 변환 (1 -> 07, 2 -> 08, 3 -> 09)
        if (gradeNumber == null) return null;
        switch (gradeNumber.intValue()) {
            case 1:
                return "07";
            case 2:
                return "08";
            case 3:
                return "09";
            default:
                return null;
        }
    }

    @Override
    public List<StudentInfoResponse> getAvailableStudentsInSameSchool(Long teacherId, String search, Long grade) {
        // 1. 선생님의 학교 정보 조회
        Teacher teacher = teacherRepository.findByUserIdWithUser(teacherId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "선생님 정보를 찾을 수 없습니다"));

        if (teacher.getSchool() == null) {
            return new ArrayList<>();
        }

        Long schoolId = teacher.getSchool().getId();

        // 2. 같은 학교의 학생 조회 (학급 미배정 학생만)
        List<Student> availableStudents;
        if (grade != null) {
            String gradeCode = convertGradeNumberToCode(grade);
            if (gradeCode != null) {
                availableStudents = studentRepository.findBySchoolIdAndGrade_CodeAndClassGroupIDIsNull(schoolId, gradeCode);
            } else {
                availableStudents = studentRepository.findBySchoolIdAndClassGroupIDIsNull(schoolId);
            }
        } else {
            availableStudents = studentRepository.findBySchoolIdAndClassGroupIDIsNull(schoolId);
        }

        // 3. StudentInfoResponse로 변환 및 필터링
        return availableStudents.stream()
                .map(student -> {
                    User user = student.getUser();
                    if (user == null) {
                        user = userRepository.findById(student.getUserId()).orElse(null);
                    }

                    if (user == null) return null;

                    // 검색 조건 필터링
                    if (search != null && !search.isEmpty()) {
                        String searchLower = search.toLowerCase();
                        if (!user.getFullName().toLowerCase().contains(searchLower) &&
                                !user.getEmail().toLowerCase().contains(searchLower)) {
                            return null;
                        }
                    }

                    return StudentInfoResponse.builder()
                            .studentId(student.getUserId())
                            .studentName(user.getFullName())
                            .email(user.getEmail())
                            .phoneNumber(user.getPhone())
                            .grade(student.getGrade() != null ? convertGradeCodeToNumber(student.getGrade().getCode()) : null)
                            .studentNumber(student.getStudentNo() != null ? student.getStudentNo().toString() : "")
                            .enrolledDate(student.getCreatedDate() != null ? student.getCreatedDate().toLocalDate() : null)
                            .status("AVAILABLE")
                            .build();
                })
                .filter(response -> response != null)
                .collect(Collectors.toList());
    }

    @Override
    public String getOrCreateInviteCode(Long classId, Long teacherId) {
        // 1. 기존 활성 초대 코드 조회
        Optional<ClassInvitation> existingInvitation = classInvitationRepository
                .findByClassIdAndIsActiveTrue(classId);

        if (existingInvitation.isPresent() && existingInvitation.get().isUsable()) {
            return existingInvitation.get().getInviteCode();
        }

        // 2. 기존 코드가 없거나 만료되었으면 새로 생성
        return regenerateInviteCode(classId, teacherId);
    }

    @Override
    public Optional<Long> getFirstTeacherId() {
        // 데이터베이스에서 첫 번째 teacher를 조회
        List<Teacher> teachers = teacherRepository.findAll();
        if (!teachers.isEmpty()) {
            return Optional.of(teachers.get(0).getUserId());
        }
        return Optional.empty();
    }
    
    @Override
    public ClassDetailResponse getTeacherClass(Long teacherId) {
        log.info("Getting class for teacher ID: {}", teacherId);
        
        // 선생님이 담당하는 클래스 조회
        Classes classEntity = classRepository.findFirstByTeacherId(teacherId)
                .orElse(null);
        
        if (classEntity == null) {
            log.info("No class found for teacher ID: {}", teacherId);
            return null;
        }
        
        return convertToClassDetailResponse(classEntity);
    }
    
    @Override
    @Transactional
    public ClassDetailResponse updateClass(Long classId, ClassUpdateRequest request, Long teacherId) {
        log.info("Updating class ID: {} by teacher ID: {}", classId, teacherId);
        
        // 1. 클래스 조회
        Classes classEntity = classRepository.findById(classId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "클래스를 찾을 수 없습니다. ID: " + classId));
        
        // 2. 권한 확인 (클래스 담당 교사인지)
        if (!classEntity.getTeacherId().equals(teacherId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "해당 클래스를 수정할 권한이 없습니다");
        }
        
        // 3. 클래스 정보 업데이트
        classEntity.setClassName(request.getClassName());
        
        // 학년 정보 업데이트
        if (request.getClassGrade() != null) {
            StringCodeNamePair gradePair = StringCodeNamePair.builder()
                    .code(request.getClassGrade())
                    .name(getGradeName(request.getClassGrade()))
                    .build();
            classEntity.setClassGrade(gradePair);
        }
        
        // 과목 정보 업데이트
        if (request.getClassSubject() != null) {
            StringCodeNamePair subjectPair = StringCodeNamePair.builder()
                    .code(request.getClassSubject())
                    .name(getSubjectName(request.getClassSubject()))
                    .build();
            classEntity.setClassSubject(subjectPair);
        }
        
        // 4. 저장
        Classes updatedClass = classRepository.save(classEntity);
        
        // 5. 응답 반환
        return convertToClassDetailResponse(updatedClass);
    }
}
