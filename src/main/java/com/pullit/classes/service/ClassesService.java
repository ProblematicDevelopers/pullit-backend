package com.pullit.classes.service;

import com.pullit.classes.dto.request.ClassCreateRequest;
import com.pullit.classes.dto.request.ClassJoinRequest;
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
import com.pullit.exam.dto.response.UserExamSchoolResponse;

import java.util.List;

public interface ClassesService {

    /**
     * 클래스 ID로 특정 클래스의 상세 정보를 조회
     */
    ClassDetailResponse getClassDetailById(Long classId);

    List<UserExamSchoolResponse> getExamsByClassId(Long classId);

    UserExamSchoolResponse getExamsByClassIdAndExamId(Long classId, Long examId);

    LiveExamAttemptResponse createOrGetAttempt(Long userId, LiveExamAttemptRequest request);

    CbtExamResponse getLiveExam(Long examId, Long userId);

    AttemptAnswerResponse getAttemptAnswers(Long attemptId, Long userId);

    RedisUpdateResponse updateRedisData(Long attemptId, RedisUpdateRequest request, Long userId);

    RedisDataResponse getRedisData(Long attemptId, Long userId);

    RedisMigrationResponse migrateRedisToDatabase(Long attemptId, RedisMigrationRequest request, Long userId);

    // 학급 생성
    ClassCreateResponse createClass(ClassCreateRequest request, Long teacherId);

    // 학생 초대
    StudentInvitationResponse inviteStudents(Long classId, StudentInvitationRequest request, Long teacherId);

    // 초대 코드로 가입
    ClassDetailResponse joinClassByInviteCode(ClassJoinRequest request, Long studentId);

    // 초대 코드 재생성
    String regenerateInviteCode(Long classId, Long teacherId);

    // 학급에서 학생 제거
    void removeStudentFromClass(Long classId, Long studentId);

    // 학급 멤버 확인 (Student의 classGroupID 체크)
    boolean isClassMember(Long classId, Long userId);

    // 학급 소유자 확인
    boolean isClassOwner(Long classId, Long teacherId);

    // 초대 코드 생성 유틸리티
    String generateUniqueInviteCode();

    // 같은 학교의 초대 가능한 학생 목록 조회
    List<StudentInfoResponse> getAvailableStudentsInSameSchool(Long teacherId, String search, Long grade);

    // 초대 코드 조회 또는 생성
    String getOrCreateInviteCode(Long classId, Long teacherId);

    // 테스트용: 첫 번째 teacher ID 조회
    java.util.Optional<Long> getFirstTeacherId();
}
