package com.pullit.classes.controller;

import com.pullit.auth.authentication.CustomUserDetails;
import com.pullit.classes.dto.request.ClassCreateRequest;
import com.pullit.classes.dto.request.ClassJoinRequest;
import com.pullit.classes.dto.request.ClassUpdateRequest;
import com.pullit.classes.dto.request.StudentInvitationRequest;
import com.pullit.classes.dto.response.ClassCreateResponse;
import com.pullit.cbt.dto.request.RedisMigrationRequest;
import com.pullit.cbt.dto.request.RedisUpdateRequest;
import com.pullit.cbt.dto.response.AttemptAnswerResponse;
import com.pullit.cbt.dto.response.CbtExamResponse;
import com.pullit.cbt.dto.response.RedisDataResponse;
import com.pullit.cbt.dto.response.RedisMigrationResponse;
import com.pullit.cbt.dto.response.RedisUpdateResponse;
import com.pullit.classes.dto.request.LiveExamAttemptRequest;
import com.pullit.classes.dto.response.ClassDetailResponse;
import com.pullit.classes.dto.response.StudentInvitationResponse;
import com.pullit.classes.dto.response.StudentInfoResponse;
import com.pullit.classes.dto.response.LiveExamAttemptResponse;
import com.pullit.classes.service.ClassesService;
import com.pullit.common.annotation.AuthUser;
import com.pullit.common.dto.response.ApiResponse;
import com.pullit.common.exception.BusinessException;
import com.pullit.common.exception.ErrorCode;
import com.pullit.exam.dto.response.UserExamSchoolResponse;
import com.pullit.exam.enums.ExamVisibility;
import com.pullit.notification.annotation.NotificationTrigger;
import com.pullit.notification.enums.NotificationType;
import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/classes")
@RequiredArgsConstructor
@Tag(name = "Classes", description = "클래스 API")
public class ClassesController {

    private final ClassesService classesService;

    /**
     * 현재 로그인한 사용자의 클래스 정보 조회
     * 선생님인 경우 담당 클래스를, 학생인 경우 소속 클래스를 반환
     */
    @GetMapping("/myclass")
    @Operation(summary = "나의 클래스 정보 조회", description = "선생님은 담당 클래스, 학생은 소속 클래스 정보를 조회합니다")
    public ResponseEntity<ApiResponse<ClassDetailResponse>> getMyClass(
            @AuthUser CustomUserDetails userDetails) {
        Long userId = userDetails.getUserId();
        
        ClassDetailResponse classDetail;
        if (userDetails.isTeacher()) {
            // 선생님인 경우 담당 클래스 조회
            classDetail = classesService.getTeacherClass(userId);
        } else {
            // 학생인 경우 소속 클래스 조회
            classDetail = classesService.getClassDetailById(userId);
        }
        
        return ResponseEntity.ok(ApiResponse.success(classDetail));
    }

    @GetMapping("/{classId}/exams")
    @Operation(summary = "클래스 ID로 클래스의 공개 시험 목록 조회", description = "클래스 ID로 클래스의 공개 시험 목록 조회")
    public ResponseEntity<ApiResponse<List<UserExamSchoolResponse>>> getExamsByClassId(
            @PathVariable Long classId) {
        List<UserExamSchoolResponse> exams = classesService.getExamsByClassId(classId);
        return ResponseEntity.ok(ApiResponse.success(exams));
    }

    @GetMapping("/{classId}/exams/{examId}")
    @Operation(summary = "클래스 ID로 클래스의 공개 시험 목록 조회", description = "클래스 ID로 클래스의 공개 시험 목록 조회")
    public ResponseEntity<ApiResponse<UserExamSchoolResponse>> getExamsByClassIdAndExamId(
            @PathVariable Long classId,
            @PathVariable Long examId) {
        UserExamSchoolResponse exam = classesService.getExamsByClassIdAndExamId(classId, examId);
        return ResponseEntity.ok(ApiResponse.success(exam));
    }

    @PostMapping("/attempt")
    @Operation(summary = "실시간 시험 시도 생성 또는 기존 시도 조회", description = "실시간 시험 시도 생성 또는 기존 시도 조회")
    public ResponseEntity<ApiResponse<LiveExamAttemptResponse>> createOrGetAttempt(
            @AuthUser CustomUserDetails currentUser,
            @RequestBody LiveExamAttemptRequest request) {
        try {
            Long userId = currentUser.getUserId();
            LiveExamAttemptResponse response = classesService.createOrGetAttempt(userId, request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            // 잘못된 요청에 대한 예외 처리
            return ResponseEntity.badRequest().body(ApiResponse.error("401", "잘못된 요청입니다: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            // 기타 예외 처리
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("500", "CBT 시험 시도 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/exam/{examId}")
    @Operation(summary = "생성된 Cbt Exam 정보 불러오기", description = "생성된 Cbt Exam 정보 불러오기")
    public ResponseEntity<ApiResponse<CbtExamResponse>> getLiveExam(
            @AuthUser CustomUserDetails currentUser,
            @PathVariable Long examId) {
        try {
            Long userId = currentUser.getUserId();
            CbtExamResponse response = classesService.getLiveExam(examId, userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            // 잘못된 요청에 대한 예외 처리
            return ResponseEntity.badRequest().body(ApiResponse.error("401", "잘못된 요청입니다: " + e.getMessage()));
        } catch (Exception e) {
            // 기타 예외 처리
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("500", "CBT 시험지 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/attempt/{attemptId}/answers")
    @Operation(summary = "CBT 시험 시도 답안 조회", description = "CBT 시험 시도 답안 조회")
    public ResponseEntity<ApiResponse<AttemptAnswerResponse>> getAttemptAnswers(
            @AuthUser CustomUserDetails currentUser,
            @PathVariable Long attemptId) {
        try {
            Long userId = currentUser.getUserId();
            AttemptAnswerResponse response = classesService.getAttemptAnswers(attemptId, userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            // 잘못된 요청에 대한 예외 처리
            return ResponseEntity.badRequest().body(ApiResponse.error("401", "잘못된 요청입니다: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            // 기타 예외 처리
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("500", "CBT 시험 시도 답안 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/redis/{attemptId}")
    @Operation(summary = "CBT 시험 시도 Redis 데이터 업데이트", description = "CBT 시험 시도 Redis 데이터 업데이트")
    public ResponseEntity<ApiResponse<RedisUpdateResponse>> updateRedisData(
            @AuthUser CustomUserDetails currentUser,
            @PathVariable Long attemptId,
            @RequestBody RedisUpdateRequest request) {
        try {
            Long userId = currentUser.getUserId();
            RedisUpdateResponse response = classesService.updateRedisData(attemptId, request, userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            // 잘못된 요청에 대한 예외 처리
            return ResponseEntity.badRequest().body(ApiResponse.error("401", "잘못된 요청입니다: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            // 기타 예외 처리
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("500", "Redis 데이터 업데이트 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @GetMapping("/redis/{attemptId}")
    @Operation(summary = "CBT 시험 시도 Redis 데이터 조회", description = "CBT 시험 시도 Redis 데이터 조회")
    public ResponseEntity<ApiResponse<RedisDataResponse>> getRedisData(
            @AuthUser CustomUserDetails currentUser,
            @PathVariable Long attemptId) {
        try {
            Long userId = currentUser.getUserId();
            RedisDataResponse response = classesService.getRedisData(attemptId, userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            // 잘못된 요청에 대한 예외 처리
            return ResponseEntity.badRequest().body(ApiResponse.error("401", "잘못된 요청입니다: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            // 기타 예외 처리
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("500", "Redis 데이터 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    @PostMapping("/attempt/{attemptId}/migrate")
    @Operation(summary = "Redis 데이터를 DB로 마이그레이션", description = "CBT 시험 시도 Redis 데이터를 DB로 마이그레이션")
    public ResponseEntity<ApiResponse<RedisMigrationResponse>> migrateRedisToDatabase(
            @AuthUser CustomUserDetails currentUser,
            @PathVariable Long attemptId,
            @RequestBody RedisMigrationRequest request) {
        try {
            Long userId = currentUser.getUserId();
            RedisMigrationResponse response = classesService.migrateRedisToDatabase(attemptId, request, userId);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (IllegalArgumentException e) {
            // 잘못된 요청에 대한 예외 처리
            return ResponseEntity.badRequest().body(ApiResponse.error("401", "잘못된 요청입니다: " + e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            // 기타 예외 처리
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("500", "Redis 데이터 마이그레이션 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }


    @PostMapping
    @Operation(summary = "학급 생성", description = "선생님이 새로운 학급을 생성합니다")
    public ResponseEntity<ApiResponse<ClassCreateResponse>> createClass(
        @Valid @RequestBody ClassCreateRequest request,
        @AuthUser CustomUserDetails userDetails
    ) {
        // 1. 선생님 권한 확인
        if (!userDetails.isTeacher()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "선생님만 학급을 생성할 수 있습니다");
        }

        // 2. 서비스 호출
        ClassCreateResponse response = classesService.createClass(request, userDetails.getUserId());

        // 3. 응답
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // 테스트용 임시 엔드포인트 (테스트 계정으로 학급 생성)
    @PostMapping("/test")
    @Operation(summary = "테스트용 학급 생성", description = "테스트 계정으로 학급을 생성합니다")
    public ResponseEntity<ApiResponse<ClassCreateResponse>> createClassForTest(
        @Valid @RequestBody ClassCreateRequest request
    ) {
        // 테스트용 teacherId - 실제 DB에서 첫 번째 teacher를 사용
        Long testTeacherId = classesService.getFirstTeacherId()
            .orElse(4L); // 만약 teacher가 없으면 기본값 4L 사용

        log.info("테스트 학급 생성 요청 - teacherId: {}, className: {}, grade: {}, subject: {}",
                 testTeacherId, request.getClassName(), request.getClassGrade(), request.getClassSubject());

        ClassCreateResponse response = classesService.createClass(request, testTeacherId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{classId}/invitations")
    @Operation(summary = "학생 초대", description = "학급에 학생들을 초대합니다")
    @NotificationTrigger(
        type = NotificationType.CLASS_INVITATION,
        multipleUsers = true,
        userIdsExpression = "#result.body.data.invitedStudentIds",
        title = "'반 초대'",
        message = "#result.body.data.className + ' 반에 초대되었습니다'",
        targetUrl = "'/student/class-room/my-class'"
    )
    public ResponseEntity<ApiResponse<StudentInvitationResponse>> inviteStudents(
        @PathVariable Long classId,
        @Valid @RequestBody StudentInvitationRequest request,
        @AuthUser CustomUserDetails userDetails
    ) {
        // 1. 선생님 권한 확인
        if (!userDetails.isTeacher()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "선생님만 학생을 초대할 수 있습니다");
        }

        // 2. 학급 소유자 확인
        if (!classesService.isClassOwner(classId, userDetails.getUserId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "해당 학급의 담당 선생님이 아닙니다");
        }

        // 3. 서비스 호출
        StudentInvitationResponse response = classesService.inviteStudents(classId, request, userDetails.getUserId());

        // 4. 응답
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/join")
    @Operation(summary = "학급 가입", description = "학생이 초대 코드로 학급에 가입합니다")
    public ResponseEntity<ApiResponse<ClassDetailResponse>> joinClass(
        @Valid @RequestBody ClassJoinRequest request,
        @AuthUser CustomUserDetails userDetails
    ) {
        // 1. 학생 권한 확인
        if (!userDetails.isStudent()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "학생만 학급에 가입할 수 있습니다");
        }

        // 2. 서비스 호출
        ClassDetailResponse response = classesService.joinClassByInviteCode(request, userDetails.getUserId());

        // 3. 응답
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{classId}/invite-code")
    @Operation(summary = "초대 코드 재생성", description = "학급 초대 코드를 재생성합니다")
    public ResponseEntity<ApiResponse<Map<String, String>>> regenerateInviteCode(
        @PathVariable Long classId,
        @AuthUser CustomUserDetails userDetails
    ) {
        // 1. 선생님 권한 & 소유자 확인
        if (!userDetails.isTeacher()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "선생님만 초대 코드를 생성할 수 있습니다");
        }

        if (!classesService.isClassOwner(classId, userDetails.getUserId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "해당 학급의 담당 선생님이 아닙니다");
        }

        // 2. 서비스 호출
        String newInviteCode = classesService.regenerateInviteCode(classId, userDetails.getUserId());

        // 3. 응답
        Map<String, String> result = Map.of("inviteCode", newInviteCode);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/{classId}/students/{studentId}")
    @Operation(summary = "학생 제거", description = "학급에서 학생을 제거합니다")
    public ResponseEntity<ApiResponse<Void>> removeStudent(
        @PathVariable Long classId,
        @PathVariable Long studentId,
        @AuthUser CustomUserDetails userDetails
    ) {
        // 1. 선생님 권한 & 소유자 확인
        if (!userDetails.isTeacher()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "선생님만 학생을 제거할 수 있습니다");
        }

        if (!classesService.isClassOwner(classId, userDetails.getUserId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "해당 학급의 담당 선생님이 아닙니다");
        }

        // 2. 서비스 호출
        classesService.removeStudentFromClass(classId, studentId);

        // 3. 응답
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/available-students")
    @Operation(summary = "초대 가능한 학생 목록 조회", description = "같은 학교의 학급 미배정 학생 목록을 조회합니다")
    public ResponseEntity<ApiResponse<List<StudentInfoResponse>>> getAvailableStudents(
        @AuthUser CustomUserDetails userDetails,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) Long grade
    ) {
        // 1. 선생님 권한 확인
        if (!userDetails.isTeacher()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "선생님만 학생 목록을 조회할 수 있습니다");
        }

        // 2. 서비스 호출
        List<StudentInfoResponse> students = classesService.getAvailableStudentsInSameSchool(
            userDetails.getUserId(), search, grade);

        // 3. 응답
        return ResponseEntity.ok(ApiResponse.success(students));
    }

    @GetMapping("/{classId}/invite-code")
    @Operation(summary = "초대 코드 조회", description = "학급의 현재 초대 코드를 조회합니다")
    public ResponseEntity<ApiResponse<Map<String, String>>> getInviteCode(
        @PathVariable Long classId,
        @AuthUser CustomUserDetails userDetails
    ) {
        // 1. 선생님 권한 & 소유자 확인
        if (!userDetails.isTeacher()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "선생님만 초대 코드를 조회할 수 있습니다");
        }

        if (!classesService.isClassOwner(classId, userDetails.getUserId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "해당 학급의 담당 선생님이 아닙니다");
        }

        // 2. 서비스 호출
        String inviteCode = classesService.getOrCreateInviteCode(classId, userDetails.getUserId());

        // 3. 응답
        Map<String, String> result = Map.of(
            "inviteCode", inviteCode,
            "classId", classId.toString()
        );
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    @GetMapping("/teacher/my-class")
    @Operation(summary = "선생님의 담당 학급 조회", description = "선생님이 담당하는 학급 정보를 초대 코드와 함께 조회합니다")
    public ResponseEntity<ApiResponse<ClassDetailResponse>> getTeacherClass(
        @AuthUser CustomUserDetails userDetails
    ) {
        // 1. 선생님 권한 확인
        if (!userDetails.isTeacher()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "선생님만 조회할 수 있습니다");
        }
        
        // 2. 서비스 호출
        ClassDetailResponse classDetail = classesService.getTeacherClass(userDetails.getUserId());
        
        // 3. 응답
        return ResponseEntity.ok(ApiResponse.success(classDetail));
    }
    
    @PutMapping("/{classId}")
    @Operation(summary = "학급 정보 수정", description = "학급의 이름, 학년, 과목 정보를 수정합니다")
    public ResponseEntity<ApiResponse<ClassDetailResponse>> updateClass(
        @PathVariable Long classId,
        @Valid @RequestBody ClassUpdateRequest request,
        @AuthUser CustomUserDetails userDetails
    ) {
        // 1. 선생님 권한 확인
        if (!userDetails.isTeacher()) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "선생님만 학급 정보를 수정할 수 있습니다");
        }
        
        // 2. 서비스 호출
        ClassDetailResponse updatedClass = classesService.updateClass(classId, request, userDetails.getUserId());
        
        // 3. 응답
        return ResponseEntity.ok(ApiResponse.success(updatedClass));
    }
}
