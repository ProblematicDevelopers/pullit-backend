package com.pullit.cbt.service;

import com.pullit.cbt.dto.request.CbtExamCreateRequest;
import com.pullit.cbt.dto.request.CbtAttemptRequest;
import com.pullit.cbt.dto.request.RedisUpdateRequest;
import com.pullit.cbt.dto.request.RedisMigrationRequest;
import com.pullit.cbt.dto.response.CbtExamResponse;
import com.pullit.cbt.dto.response.CbtAttemptResponse;
import com.pullit.cbt.dto.response.CbtExamItemResponse;
import com.pullit.cbt.dto.response.AttemptAnswerResponse;
import com.pullit.cbt.dto.response.AttemptQuestionAnswerResponse;
import com.pullit.cbt.dto.response.RedisUpdateResponse;
import com.pullit.cbt.dto.response.RedisDataResponse;
import com.pullit.cbt.dto.response.RedisMigrationResponse;
import com.pullit.exam.entity.UserExam;
import com.pullit.exam.entity.UserExamItem;
import com.pullit.exam.enums.ExamVisibility;
import com.pullit.exam.repository.UserExamRepository;
import com.pullit.item.dao.ItemMetadataRepository;
import com.pullit.item.dao.ItemHtmlDataRepository;
import com.pullit.item.dao.SubjectRepository;
import com.pullit.item.dto.response.SubjectResponse;
import com.pullit.cbt.dto.response.CbtCandidateItemResponse;
import com.pullit.item.entity.ItemMetadata;
import com.pullit.item.entity.Subject;
import com.pullit.cbt.entity.AttemptExam;
import com.pullit.cbt.entity.AttemptExamQuestion;
import com.pullit.cbt.repository.AttemptExamRepository;
import com.pullit.cbt.repository.AttemptExamQuestionRepository;
import com.pullit.user.entity.User;
import com.pullit.user.repository.UserRepository;
import com.pullit.common.cache.service.RedisCacheService;
import com.pullit.common.constants.CacheConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CbtServiceImpl implements CbtService {
    private final SubjectRepository subjectRepository;
    private final UserExamRepository userExamRepository;
    private final ItemMetadataRepository itemMetadataRepository;
    private final ItemHtmlDataRepository itemHtmlDataRepository;
    private final AttemptExamRepository attemptExamRepository;
    private final AttemptExamQuestionRepository attemptExamQuestionRepository;
    private final UserRepository userRepository;
    private final RedisCacheService redisCacheService;

    @Override
    public Long createExam(Long userId, CbtExamCreateRequest request) {
        Long subjectId = request.getSubjectId();
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("과목을 찾을 수 없습니다. id=" + subjectId));
        SubjectResponse s = SubjectResponse.from(subject);

        Date today = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String formatted = sdf.format(today);

        String examName = "[CBT]" + s.getSchoolLevelName() + " > " + s.getAreaName() + " > " + s.getSubjectName() + " "
                + formatted;
        System.out.println("Request timeLimit: " + request.getTimeLimit());
        UserExam exam = UserExam.builder()
                .examName(examName)
                .examType("CBT")
                .totalItems(request.getQuestionCount())
                .timeLimit(request.getTimeLimit())
                .gradeCode(s.getGradeCode())
                .gradeName(s.getGradeName())
                .termCode(s.getTermCode())
                .termName(s.getTermName())
                .areaName(s.getAreaName())
                .areaCode(s.getAreaCode())
                .visibility(ExamVisibility.PRIVATE)
                .build();
        System.out.println("Created exam timeLimit: " + exam.getTimeLimit());
        try {
            UserExam u = userExamRepository.save(exam);
            return u.getId();
        } catch (Exception e) {
            // 저장에 실패한 경우 예외 메시지와 함께 null 반환 또는 예외 재던지기
            // 필요에 따라 로그를 남길 수도 있음
            throw new RuntimeException("시험 저장에 실패했습니다: " + e.getMessage(), e);
        }
    }

    @Override
    public UserExam addExamItem(Long examId, CbtExamCreateRequest request) {
        // 문제를 난이도별로 고루 분포하게 뽑아서 UserExamItem으로 추가하는 로직
        // (1) ItemMetadata에서 largeChapterId, mediumChapters 조건에 맞는 문제 추출
        // (2) 난이도별로 분포 고려하여 request.getQuestionCount()만큼 선정
        // (3) UserExamItem 생성 후 UserExam에 추가

        // 필요한 repository, entity import는 클래스 상단에서 처리되어 있다고 가정

        // 1. 시험 엔티티 조회
        UserExam userExam = userExamRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("시험을 찾을 수 없습니다. id=" + examId));

        // 2. ItemMetadataRepository 필요 (여기서는 itemMetadataRepository가 주입되어 있다고 가정)
        // 실제 환경에서는 @Autowired 또는 생성자 주입 필요
        // 예시: private final ItemMetadataRepository itemMetadataRepository;

        // 3. 요청에서 chapter 조건 추출
        List<CbtExamCreateRequest.SelectedChapter> selectedChapters = request.getSelectedChapters();
        int totalCount = request.getQuestionCount();

        // 4. 조건에 맞는 모든 문제 조회
        List<Long> mediumChapterCodes = new java.util.ArrayList<>();
        List<Long> largeChapterCodes = new java.util.ArrayList<>();
        for (CbtExamCreateRequest.SelectedChapter chapter : selectedChapters) {
            if (chapter.getLargeChapterId() != null && !chapter.getLargeChapterId().isEmpty()) {
                try {
                    largeChapterCodes.add(Long.parseLong(chapter.getLargeChapterId()));
                } catch (NumberFormatException ignore) {
                }
            }
            if (chapter.getMediumChapters() != null) {
                mediumChapterCodes.addAll(chapter.getMediumChapters());
            }
        }

        // 후보 아이템을 경량 DTO로 조회
        List<CbtCandidateItemResponse> candidates = itemMetadataRepository
                .findCandidateItems(request.getSubjectId(), largeChapterCodes, mediumChapterCodes);

        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("조건에 맞는 문제가 없습니다.");
        }

        // 5. 난이도별로 분포 계산
        // 난이도 코드 추출 (예: "EASY", "MEDIUM", "HARD" 등)
        Map<Long, List<CbtCandidateItemResponse>> difficultyMap = new java.util.HashMap<>();
        for (CbtCandidateItemResponse item : candidates) {
            Long diff = item.getDifficultyCode() != null ? item.getDifficultyCode() : -1L;
            difficultyMap.computeIfAbsent(diff, k -> new java.util.ArrayList<>()).add(item);
        }

        // 난이도별로 균등 분포 (남은 문제는 랜덤하게 채움)
        List<CbtCandidateItemResponse> selectedItems = new java.util.ArrayList<>();
        int diffTypes = difficultyMap.size();
        int baseCount = totalCount / diffTypes;
        int remain = totalCount % diffTypes;

        java.util.Random random = new java.util.Random();
        for (List<CbtCandidateItemResponse> diffList : difficultyMap.values()) {
            java.util.Collections.shuffle(diffList, random);
        }

        // 각 난이도에서 baseCount만큼 뽑기
        for (List<CbtCandidateItemResponse> diffList : difficultyMap.values()) {
            int pick = Math.min(baseCount, diffList.size());
            selectedItems.addAll(diffList.subList(0, pick));
        }

        // 남은 개수는 난이도 상관없이 랜덤하게 채움
        List<CbtCandidateItemResponse> remainPool = new java.util.ArrayList<>();
        for (List<CbtCandidateItemResponse> diffList : difficultyMap.values()) {
            if (diffList.size() > baseCount) {
                remainPool.addAll(diffList.subList(baseCount, diffList.size()));
            }
        }
        java.util.Collections.shuffle(remainPool, random);
        for (int i = 0; i < remain && i < remainPool.size(); i++) {
            selectedItems.add(remainPool.get(i));
        }

        // 만약 문제 수가 부족하면 candidates에서 랜덤하게 채움
        while (selectedItems.size() < totalCount && selectedItems.size() < candidates.size()) {
            CbtCandidateItemResponse extra = candidates.get(random.nextInt(candidates.size()));
            if (!selectedItems.contains(extra)) {
                selectedItems.add(extra);
            }
        }

        // 6. UserExamItem 생성 및 시험에 추가
        int order = userExam.getExamItems() != null ? userExam.getExamItems().size() + 1 : 1;
        for (CbtCandidateItemResponse item : selectedItems) {
            UserExamItem examItem = UserExamItem.builder()
                    .userExam(userExam)
                    .itemId(item.getItemId())
                    .subjectId(item.getSubjectId())
                    .itemOrder(order++)
                    .points(item.getDifficultyCode().intValue()) // 배점은 필요시 설정
                    .build();
            userExam.addExamItem(examItem);
        }

        // 7. 저장
        return userExamRepository.save(userExam);
    }

    @Override
    public CbtExamResponse getCbtExam(Long examId, Long userId) {
        UserExam userExam = userExamRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("시험을 찾을 수 없습니다. id=" + examId));
        
        // 사용자 권한 확인 (자신의 시험이거나 공개된 시험만 조회 가능)
        if (!userExam.getCreatedBy().equals(userId) && userExam.getVisibility() != ExamVisibility.PUBLIC) {
            throw new IllegalArgumentException("해당 시험에 대한 접근 권한이 없습니다.");
        }
        
        List<CbtExamItemResponse> examItems = userExam.getExamItems().stream()
                .map(item -> {
                    // ItemHtmlData 조회
                    var itemHtmlData = itemHtmlDataRepository.findByItemId(item.getItemId()).orElse(null);
                    
                    // ItemMetadata 조회하여 문제 유형 확인
                    var itemMetadata = itemMetadataRepository.findById(item.getItemId()).orElse(null);
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
                                    questionType = "SHORT_ANSWER_UNORDERED"; // 단답 무순형
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
                            .questionText(itemHtmlData != null ? itemHtmlData.getQuestion() : "")
                            .questionType(questionType)
                            .passage(itemHtmlData != null ? itemHtmlData.getPassage() : null)
                            .passageHtml(itemHtmlData != null ? itemHtmlData.getPassageHtml() : null)
                            .question(itemHtmlData != null ? itemHtmlData.getQuestion() : null)
                            .questionHtml(itemHtmlData != null ? itemHtmlData.getQuestionHtml() : null)
                            .choices(itemHtmlData != null ? itemHtmlData.getChoicesAsList() : null)
                            .answer(itemHtmlData != null ? itemHtmlData.getAnswer() : null)
                            .answerHtml(itemHtmlData != null ? itemHtmlData.getAnswerHtml() : null)
                            .explainText(itemHtmlData != null ? itemHtmlData.getExplainText() : null)
                            .explainHtml(itemHtmlData != null ? itemHtmlData.getExplainHtml() : null)
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
    public CbtAttemptResponse createOrGetAttempt(Long userId, CbtAttemptRequest request) {
        Long examId = request.getExamId();
        
        // 시험 존재 확인
        UserExam userExam = userExamRepository.findById(examId)
                .orElseThrow(() -> new IllegalArgumentException("시험을 찾을 수 없습니다. id=" + examId));
        
        // 사용자 존재 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. id=" + userId));
        
        // 기존 진행 중인 시도 확인
        AttemptExam existingAttempt = attemptExamRepository.findByUserAndExamAndStatus(
                user, userExam, AttemptExam.AttemptStatus.IN_PROGRESS);
        
        if (existingAttempt != null) {
            // 기존 진행 중인 시도가 있으면 반환
            return CbtAttemptResponse.builder()
                    .attemptId(existingAttempt.getId())
                    .examId(existingAttempt.getExam().getId())
                    .status(existingAttempt.getStatus().name())
                    .userId(existingAttempt.getUser().getId())
                    .remainTime(existingAttempt.getRemainTime())
                    .startTime(existingAttempt.getStartedAt() != null ? existingAttempt.getStartedAt().toString() : null)
                    .endTime(existingAttempt.getCompletedAt() != null ? existingAttempt.getCompletedAt().toString() : null)
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
                .remainTime(userExam.getTimeLimit() != null ? userExam.getTimeLimit() * 60 : null) // 분을 초로 변환
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
        
        return CbtAttemptResponse.builder()
                .attemptId(savedAttempt.getId())
                .examId(savedAttempt.getExam().getId())
                .status(savedAttempt.getStatus().name())
                .userId(savedAttempt.getUser().getId())
                .remainTime(savedAttempt.getRemainTime())
                .startTime(savedAttempt.getStartedAt() != null ? savedAttempt.getStartedAt().toString() : null)
                .endTime(savedAttempt.getCompletedAt() != null ? savedAttempt.getCompletedAt().toString() : null)
                .build();
    }

    @Override
    public AttemptAnswerResponse getAttemptAnswers(Long attemptId, Long userId) {
        // AttemptExam 조회
        AttemptExam attemptExam = attemptExamRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("시험 시도를 찾을 수 없습니다. id=" + attemptId));
        
        // 권한 확인 (자신의 시도만 조회 가능)
        if (!attemptExam.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("해당 시험 시도에 대한 접근 권한이 없습니다.");
        }
        
        // 답안 목록 조회
        List<AttemptExamQuestion> attemptQuestions = attemptExamQuestionRepository.findByAttemptExamId(attemptId);
        
        List<AttemptQuestionAnswerResponse> answers = attemptQuestions.stream()
                .map(question -> AttemptQuestionAnswerResponse.builder()
                        .questionId(question.getId())
                        .itemId(question.getExamItem().getItemId())
                        .itemOrder(question.getExamItem().getItemOrder())
                        .userAnswer(question.getUserAnswer())
                        .isCorrect(question.getIsCorrect())
                        .duration(question.getDuration())
                        .points(question.getPoints())
                        .answeredAt(question.getAnsweredAt() != null ? question.getAnsweredAt().toString() : null)
                        .isAnswered(question.isAnswered())
                        .build())
                .toList();
        
        // 통계 계산
        int totalQuestions = answers.size();
        int answeredQuestions = (int) answers.stream().filter(AttemptQuestionAnswerResponse::getIsAnswered).count();
        int correctAnswers = (int) answers.stream().filter(AttemptQuestionAnswerResponse::getIsCorrect).count();
        int totalScore = answers.stream()
                .filter(AttemptQuestionAnswerResponse::getIsCorrect)
                .mapToInt(answer -> answer.getPoints() != null ? answer.getPoints() : 0)
                .sum();
        int maxScore = answers.stream()
                .mapToInt(answer -> answer.getPoints() != null ? answer.getPoints() : 0)
                .sum();
        
        return AttemptAnswerResponse.builder()
                .attemptId(attemptId)
                .examId(attemptExam.getExam().getId())
                .examName(attemptExam.getExam().getExamName())
                .status(attemptExam.getStatus().name())
                .answers(answers)
                .totalQuestions(totalQuestions)
                .answeredQuestions(answeredQuestions)
                .correctAnswers(correctAnswers)
                .totalScore(totalScore)
                .maxScore(maxScore)
                .build();
    }

    @Override
    public RedisUpdateResponse updateRedisData(Long attemptId, RedisUpdateRequest request, Long userId) {
        // AttemptExam 조회 및 권한 확인
        AttemptExam attemptExam = attemptExamRepository.findById(attemptId)
                .orElseThrow(() -> new IllegalArgumentException("시험 시도를 찾을 수 없습니다. id=" + attemptId));
        
        // 권한 확인 (자신의 시도만 수정 가능)
        if (!attemptExam.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("해당 시험 시도에 대한 접근 권한이 없습니다.");
        }
        
        try {
            // Redis 키 생성 (attempt:attemptId:key 형태)
            String redisKey = redisCacheService.createCompositeKey(
                "attempt", 
                attemptId.toString(), 
                request.getKey() != null ? request.getKey() : "data"
            );
            
            // Redis에 데이터 저장
            if (request.getExpiration() != null && request.getExpiration() > 0) {
                // 만료 시간이 설정된 경우
                redisCacheService.put(redisKey, request.getValue(), request.getExpiration(), java.util.concurrent.TimeUnit.SECONDS);
            } else {
                // 기본 TTL 사용
                redisCacheService.put(redisKey, request.getValue(), CacheConstants.MEDIUM_TTL_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
            }
            
            // 추가 데이터가 있는 경우 Hash로 저장
            if (request.getData() != null && !request.getData().isEmpty()) {
                String hashKey = redisCacheService.createCompositeKey("attempt", attemptId.toString(), "hash");
                for (Map.Entry<String, Object> entry : request.getData().entrySet()) {
                    redisCacheService.put(hashKey + ":" + entry.getKey(), entry.getValue(), 
                        request.getExpiration() != null ? request.getExpiration() : CacheConstants.MEDIUM_TTL_SECONDS, 
                        java.util.concurrent.TimeUnit.SECONDS);
                }
            }
            
            return RedisUpdateResponse.builder()
                    .key(redisKey)
                    .value(request.getValue())
                    .success(true)
                    .message("Redis 데이터 업데이트 성공")
                    .expiration(request.getExpiration() != null ? request.getExpiration() : CacheConstants.MEDIUM_TTL_SECONDS)
                    .build();
                    
        } catch (Exception e) {
            return RedisUpdateResponse.builder()
                    .key(request.getKey())
                    .value(request.getValue())
                    .success(false)
                    .message("Redis 데이터 업데이트 실패: " + e.getMessage())
                    .expiration(null)
                    .build();
                 }
     }

     @Override
     public RedisDataResponse getRedisData(Long attemptId, Long userId) {
         // AttemptExam 조회 및 권한 확인
         AttemptExam attemptExam = attemptExamRepository.findById(attemptId)
                 .orElseThrow(() -> new IllegalArgumentException("시험 시도를 찾을 수 없습니다. id=" + attemptId));
         
         // 권한 확인 (자신의 시도만 조회 가능)
         if (!attemptExam.getUser().getId().equals(userId)) {
             throw new IllegalArgumentException("해당 시험 시도에 대한 접근 권한이 없습니다.");
         }
         
         try {
             Map<String, Object> redisData = new java.util.HashMap<>();
             
             // attempt:attemptId:* 패턴으로 모든 키 조회
             String keyPattern = redisCacheService.createCompositeKey("attempt", attemptId.toString(), "*");
             
             // Redis에서 패턴과 일치하는 키들을 찾아서 데이터 조회
             // 주요 키들을 직접 조회하는 방식으로 구현
             String[] commonKeys = {"data", "currentQuestion", "startTime", "remainingTime", "progress"};
             
             for (String key : commonKeys) {
                 String redisKey = redisCacheService.createCompositeKey("attempt", attemptId.toString(), key);
                 Object value = redisCacheService.get(redisKey, Object.class);
                 if (value != null) {
                     redisData.put(key, value);
                 }
             }
             
             // Hash 데이터도 조회
             String hashKeyPattern = redisCacheService.createCompositeKey("attempt", attemptId.toString(), "hash");
             String[] hashKeys = {"startTime", "remainingTime", "currentQuestion", "answers", "status"};
             
             for (String hashKey : hashKeys) {
                 String fullHashKey = hashKeyPattern + ":" + hashKey;
                 Object value = redisCacheService.get(fullHashKey, Object.class);
                 if (value != null) {
                     redisData.put("hash_" + hashKey, value);
                 }
             }
             
             return RedisDataResponse.builder()
                     .attemptId(attemptId)
                     .data(redisData)
                     .success(true)
                     .message("Redis 데이터 조회 성공")
                     .totalKeys(redisData.size())
                     .build();
                     
         } catch (Exception e) {
             return RedisDataResponse.builder()
                     .attemptId(attemptId)
                     .data(new java.util.HashMap<>())
                     .success(false)
                     .message("Redis 데이터 조회 실패: " + e.getMessage())
                     .totalKeys(0)
                     .build();
                  }
     }

     @Override
     public RedisMigrationResponse migrateRedisToDatabase(Long attemptId, RedisMigrationRequest request, Long userId) {
         // AttemptExam 조회 및 권한 확인
         AttemptExam attemptExam = attemptExamRepository.findById(attemptId)
                 .orElseThrow(() -> new IllegalArgumentException("시험 시도를 찾을 수 없습니다. id=" + attemptId));
         
         // 권한 확인 (자신의 시도만 마이그레이션 가능)
         if (!attemptExam.getUser().getId().equals(userId)) {
             throw new IllegalArgumentException("해당 시험 시도에 대한 접근 권한이 없습니다.");
         }
         
         try {
             int migratedQuestions = 0;
             
             // AttemptExamQuestion들을 조회
             List<AttemptExamQuestion> attemptQuestions = attemptExamQuestionRepository.findByAttemptExamId(attemptId);
             
             // 각 문제별로 Redis 데이터를 DB에 업데이트
             for (AttemptExamQuestion attemptQuestion : attemptQuestions) {
                 String questionId = attemptQuestion.getExamItem().getItemOrder().toString();
                 System.out.println("questionId: " + questionId);
                 
                 // 답변 업데이트
                 if (request.getQuestionAnswers() != null && request.getQuestionAnswers().containsKey(questionId)) {
                     String userAnswer = request.getQuestionAnswers().get(questionId);
                     attemptQuestion.setUserAnswer(userAnswer);
                     
                     // 정답 여부 확인 (ItemHtmlData에서 정답 조회)
                     var itemHtmlData = itemHtmlDataRepository.findByItemId(attemptQuestion.getExamItem().getItemId()).orElse(null);
                     if (itemHtmlData != null && itemHtmlData.getAnswer() != null) {
                         boolean isCorrect = itemHtmlData.getAnswer().equals(userAnswer);
                         attemptQuestion.setIsCorrect(isCorrect);
                         
                         // 정답인 경우 포인트 부여, 오답인 경우 0점
                         attemptQuestion.setPoints(isCorrect ? attemptQuestion.getPoints() : 0);
                     }
                     
                     // 답변 시간 설정
                     attemptQuestion.setAnsweredAt(java.time.LocalDateTime.now());
                 }
                 
                 // 소요 시간 업데이트
                 if (request.getQuestionTimes() != null && request.getQuestionTimes().containsKey(questionId)) {
                     Integer duration = request.getQuestionTimes().get(questionId);
                     attemptQuestion.setDuration(duration);
                 }
                 
                 // DB에 저장
                 attemptExamQuestionRepository.save(attemptQuestion);
                 migratedQuestions++;
             }
             
             if( request.getStatus().equals("DONE")) {
                // AttemptExam 상태를 DONE으로 변경
                attemptExam.complete();
             }
             
             // 총 시험 시간이 제공된 경우 저장
             if (request.getRemainingTime() != null) {
                //  AttemptExam에 총 시간 필드가 있다면 저장
                 attemptExam.setRemainTime(request.getRemainingTime());
             }
             
             attemptExamRepository.save(attemptExam);
             
             // Redis에서 관련 데이터 삭제 (선택사항)
             try {
                 String keyPattern = redisCacheService.createCompositeKey("attempt", attemptId.toString(), "*");
                 // Redis 키 삭제 로직 (실제 구현에서는 RedisTemplate의 delete 메서드 사용)
                 // redisCacheService.deleteByPattern(keyPattern);
             } catch (Exception e) {
                 // Redis 삭제 실패는 무시 (DB 마이그레이션은 성공)
                 System.err.println("Redis 데이터 삭제 실패: " + e.getMessage());
             }
             
             return RedisMigrationResponse.builder()
                     .attemptId(attemptId)
                     .success(true)
                     .message("Redis 데이터 DB 마이그레이션 성공")
                     .migratedQuestions(migratedQuestions)
                     .remainingTime(request.getRemainingTime())
                     .completedAt(attemptExam.getCompletedAt().toString())
                     .build();
                     
         } catch (Exception e) {
             return RedisMigrationResponse.builder()
                     .attemptId(attemptId)
                     .success(false)
                     .message("Redis 데이터 DB 마이그레이션 실패: " + e.getMessage())
                     .migratedQuestions(0)
                     .remainingTime(request.getRemainingTime())
                     .completedAt(null)
                     .build();
         }
     }

 }
