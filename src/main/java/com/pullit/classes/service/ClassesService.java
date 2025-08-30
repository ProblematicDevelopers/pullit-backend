package com.pullit.classes.service;

import com.pullit.cbt.dto.request.RedisMigrationRequest;
import com.pullit.cbt.dto.request.RedisUpdateRequest;
import com.pullit.cbt.dto.response.AttemptAnswerResponse;
import com.pullit.cbt.dto.response.CbtExamResponse;
import com.pullit.cbt.dto.response.RedisDataResponse;
import com.pullit.cbt.dto.response.RedisMigrationResponse;
import com.pullit.cbt.dto.response.RedisUpdateResponse;
import com.pullit.classes.dto.request.LiveExamAttemptRequest;
import com.pullit.classes.dto.response.ClassDetailResponse;
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
}
