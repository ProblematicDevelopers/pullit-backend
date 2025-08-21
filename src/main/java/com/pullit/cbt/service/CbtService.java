package com.pullit.cbt.service;

import com.pullit.cbt.dto.request.CbtExamCreateRequest;
import com.pullit.cbt.dto.request.CbtAttemptRequest;
import com.pullit.cbt.dto.request.RedisUpdateRequest;
import com.pullit.cbt.dto.request.RedisMigrationRequest;
import com.pullit.cbt.dto.response.CbtExamResponse;
import com.pullit.cbt.dto.response.CbtAttemptResponse;
import com.pullit.cbt.dto.response.AttemptAnswerResponse;
import com.pullit.cbt.dto.response.RedisUpdateResponse;
import com.pullit.cbt.dto.response.RedisDataResponse;
import com.pullit.cbt.dto.response.RedisMigrationResponse;
import com.pullit.exam.entity.UserExam;

public interface CbtService {
    Long createExam(Long userId, CbtExamCreateRequest request);

    UserExam addExamItem(Long examId, CbtExamCreateRequest request);
    
    CbtExamResponse getCbtExam(Long examId, Long userId);
    
    CbtAttemptResponse createOrGetAttempt(Long userId, CbtAttemptRequest request);
    
    AttemptAnswerResponse getAttemptAnswers(Long attemptId, Long userId);
    
    RedisUpdateResponse updateRedisData(Long attemptId, RedisUpdateRequest request, Long userId);
    
    RedisDataResponse getRedisData(Long attemptId, Long userId);
    
    RedisMigrationResponse migrateRedisToDatabase(Long attemptId, RedisMigrationRequest request, Long userId);
}
