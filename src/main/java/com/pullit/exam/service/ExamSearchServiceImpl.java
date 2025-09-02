package com.pullit.exam.service;

import com.pullit.common.annotation.LoggingTrace;
import com.pullit.common.annotation.RedisCacheable;
import com.pullit.common.annotation.RedisCacheEvict;
import com.pullit.exam.dto.request.ExamSearchRequest;
import com.pullit.exam.dto.response.ExamCountBySubjectResponse;
import com.pullit.exam.dto.response.ExamWithItemsResponse;
import com.pullit.exam.dto.response.UnifiedExamResponse;
import com.pullit.exam.entity.Exam;
import com.pullit.exam.entity.ExamItem;
import com.pullit.exam.repository.ExamRepository;
import com.pullit.exam.repository.ExamSearchRepository;
import com.pullit.exam.repository.UserExamRepository;
import com.pullit.item.entity.Subject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
// import org.springframework.cache.annotation.Cacheable; // Spring Cache 대신 RedisCacheable 사용
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly=true)
public class ExamSearchServiceImpl implements ExamSearchService {
    private final ExamSearchRepository examSearchRepository;
    private final ExamRepository examRepository;
    private final UserExamRepository userExamRepository;
    private final EntityManager entityManager;

    /**
     * 통합 검색 메인 메서드
     * - Exam과 UserExam 모두 검색
     * - 다양한 필터 조건 적용
     */
    @Override
    @RedisCacheable(
        key = "'exam:search:' + " +
              "(#request.keyword != null ? #request.keyword : 'none') + ':' + " +
              "(#request.gradeCode != null ? #request.gradeCode : 'all') + ':' + " +
              "(#request.areaCode != null ? #request.areaCode : 'all') + ':' + " +
              "(#request.termCode != null ? #request.termCode : 'all') + ':' + " +
              "(#request.subjectId != null ? #request.subjectId : 'all') + ':' + " +
              "(#request.visibility != null ? #request.visibility : 'all') + ':' + " +
              "'page:' + #pageable.getPageNumber() + ':size:' + #pageable.getPageSize()",
        ttl = 30,  // 30분 TTL (검색 결과는 적당한 시간 캐싱)
        condition = "#request != null"
    )
    public Page<UnifiedExamResponse> searchExams(ExamSearchRequest request, Pageable pageable) {
        log.debug("통합 시험 검색 시작: {}", request);

        // 검색 조건 검증
        validateSearchRequest(request);

        // 통합 검색 실행
        Page<UnifiedExamResponse> result = examSearchRepository.searchUnified(request, pageable);

        log.debug("통합 시험 검색 완료: {} 건", result.getTotalElements());
        return result;
    }

    /**
     * 키워드 빠른 검색 (자동완성용)
     * - 캐싱으로 응답 속도 향상
     */
    @Override
    @RedisCacheable(
        key = "'exam:quickSearch:' + #keyword + ':' + #limit",
        ttl = 10,  // 10분 TTL (자동완성은 짧게)
        condition = "#keyword != null && #keyword.length() >= 2"
    )
    @LoggingTrace(level = LoggingTrace.LogLevel.INFO, logExecutionTime = true, logParameters = true)
    public List<UnifiedExamResponse> quickSearch(String keyword, int limit) {
        log.info("[인덱스 없음] 빠른 검색 시작: keyword={}, limit={}", keyword, limit);

        // 키워드 최소 길이 체크
        if (keyword == null || keyword.trim().length() < 2) {
            return List.of();
        }

        // limit 범위 체크 (최대 20개)
        int searchLimit = Math.min(limit, 20);

        return examSearchRepository.quickSearch(keyword.trim(), searchLimit);
    }

    /**
     * 사용자 접근 가능한 모든 시험 조회
     * - 본인 생성 시험
     * - PUBLIC 시험
     * - SCHOOL 시험 (같은 학교) - Teacher 엔티티 구현 후
     */
    @Override
    public Page<UnifiedExamResponse> getAccessibleExams(Long userId, Pageable pageable) {
        log.debug("사용자 {} 접근 가능 시험 조회", userId);

        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 필수입니다");
        }

        return examSearchRepository.findAccessibleExams(userId, pageable);
    }

    /**
     * 최근 생성된 시험 조회
     * - 캐싱으로 자주 조회되는 데이터 최적화
     */
    @Override
    @RedisCacheable(
        key = "'exam:recent:' + #limit",
        ttl = 30,  // 30분 TTL (최근 시험은 자주 변경)
        condition = "#limit > 0 && #limit <= 50"
    )
    @LoggingTrace(level = LoggingTrace.LogLevel.INFO, logExecutionTime = true)
    public List<UnifiedExamResponse> getRecentExams(int limit) {
        log.info("[인덱스 없음] 최근 시험 조회: limit={}", limit);

        // limit 범위 체크 (최대 50개)
        int searchLimit = Math.min(limit, 50);

        return examSearchRepository.findRecentExams(searchLimit);
    }

    /**
     * 시험 타입별 검색
     * - TESTWIZARD: Exam 테이블만
     * - USER_CREATED: UserExam 테이블만
     * - ALL: 모두 (기본값)
     */
    @Override
    public Page<UnifiedExamResponse> searchByType(
            ExamSearchRequest request,
            String examType,
            Pageable pageable) {

        log.debug("타입별 검색: type={}", examType);

        // examType 검증
        String validExamType = validateExamType(examType);

        return examSearchRepository.searchByType(request, validExamType, pageable);
    }

    /**
     * 대단원별 시험 검색 (Exam만)
     * - UserExam은 chapter 정보 없음
     */
    @Override
    @RedisCacheable(
        key = "'exam:byChapter:' + #largeChapterCode + ':' + #pageable.pageNumber + ':' + #pageable.pageSize",
        ttl = 30,  // 30분 TTL
        condition = "#largeChapterCode != null"
    )
    public Page<UnifiedExamResponse> searchByChapter(Long largeChapterCode, Pageable pageable) {
        log.debug("대단원별 검색: chapterCode={}", largeChapterCode);

        if (largeChapterCode == null) {
            throw new IllegalArgumentException("대단원 코드는 필수입니다");
        }

        return examSearchRepository.findByLargeChapter(largeChapterCode, pageable);
    }

    /**
     * 과목별 시험 개수 통계
     */
    @Override
    @RedisCacheable(
        key = "'exam:countBySubject:' + (#subjectId != null ? #subjectId : 'all')",
        ttl = 60,  // 1시간 TTL (통계는 자주 변경되지 않음)
        timeUnit = java.util.concurrent.TimeUnit.MINUTES
    )
    public List<ExamCountBySubjectResponse> getExamCountBySubject(Long subjectId) {
        log.debug("과목별 시험 개수 조회: subjectId={}", subjectId);

        return examSearchRepository.countExamsBySubject(subjectId);
    }

    /**
     * 고급 검색
     * - 복잡한 조건 조합
     * - 삭제된 항목 포함 옵션
     */
    @Override
    public Page<UnifiedExamResponse> advancedSearch(
            ExamSearchRequest request,
            boolean includeDeleted,
            Pageable pageable) {

        log.debug("고급 검색: includeDeleted={}", includeDeleted);

        // 검색 조건 검증
        validateSearchRequest(request);

        return examSearchRepository.advancedSearch(request, includeDeleted, pageable);
    }

    /**
     * 인기 시험 조회
     * - 추후 조회수, 사용 횟수 등 통계 테이블 구현 후 개발
     */
    @Override
    public List<UnifiedExamResponse> getPopularExams(int limit) {
        log.debug("인기 시험 조회: limit={}", limit);

        // TODO: 통계 테이블 구현 후 실제 로직 구현
        // 임시로 최근 시험 반환
        return getRecentExams(limit);
    }

    /**
     * 추천 시험 조회
     * - 추후 사용자 프로필 기반 추천 알고리즘 구현
     */
    @Override
    public List<UnifiedExamResponse> getRecommendedExams(Long userId, int limit) {
        log.debug("추천 시험 조회: userId={}, limit={}", userId, limit);

        // TODO: 추천 알고리즘 구현
        // 임시로 최근 PUBLIC 시험 반환
        ExamSearchRequest request = ExamSearchRequest.builder()
                .visibility(com.pullit.exam.enums.ExamVisibility.PUBLIC)
                .build();

        Page<UnifiedExamResponse> result = examSearchRepository.searchUnified(
                request,
                Pageable.ofSize(limit)
        );

        return result.getContent();
    }

    /**
     * 내가 생성한 시험 목록 조회
     * - 현재 사용자가 생성한 모든 시험 조회 (Exam, UserExam 모두 포함)
     */
    @Override
    @Transactional(readOnly = true)
    public Page<UnifiedExamResponse> getMyExams(Long userId, Pageable pageable) {
        log.debug("내 시험 목록 조회: userId={}", userId);

        // ExamSearchRequest를 사용하여 createdBy로 필터링
        ExamSearchRequest request = ExamSearchRequest.builder()
                .createdBy(userId)  // 생성자 ID로 필터링
                .build();

        // 기존의 searchUnified 메서드를 활용하여 Exam과 UserExam 모두 검색
        Page<UnifiedExamResponse> result = examSearchRepository.searchUnified(request, pageable);
        
        log.info("내 시험 목록 조회 완료: userId={}, totalElements={}", userId, result.getTotalElements());
        
        return result;
    }

    // ===== Private Helper Methods =====

    /**
     * 검색 요청 검증
     */
    private void validateSearchRequest(ExamSearchRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("검색 조건은 필수입니다");
        }

        // 날짜 범위 검증
        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getStartDate().isAfter(request.getEndDate())) {
                throw new IllegalArgumentException("시작일은 종료일보다 이전이어야 합니다");
            }
        }
    }

    /**
     * 시험 타입 검증
     */
    private String validateExamType(String examType) {
        if (examType == null || examType.trim().isEmpty()) {
            return "ALL";
        }

        String upperType = examType.toUpperCase();
        if (!List.of("TESTWIZARD", "USER_CREATED", "ALL").contains(upperType)) {
            log.warn("잘못된 examType: {}, ALL로 대체", examType);
            return "ALL";
        }

        return upperType;
    }

    /**
     * 필터 옵션 조회
     * - 실제 데이터베이스에서 학년, 과목, 학기, 교과서 옵션을 조회
     */
    @Override
    @RedisCacheable(
        key = "'exam:filterOptions'",
        ttl = 120,  // 2시간 TTL (필터 옵션은 거의 변경 안됨)
        timeUnit = java.util.concurrent.TimeUnit.MINUTES
    )
    @LoggingTrace(level = LoggingTrace.LogLevel.INFO, logExecutionTime = true)
    @SuppressWarnings("unchecked")
    public Map<String, Object> getFilterOptions() {
        log.warn("[인덱스 없음] 필터 옵션 조회 - FULL TABLE SCAN 예상!");
        
        StopWatch stopWatch = new StopWatch("FilterOptions");
        
        Map<String, Object> filterOptions = new HashMap<>();
        
        try {
            // 학년 옵션 조회
            stopWatch.start("학년 조회");
            String gradeQuery = """
                SELECT DISTINCT s.grade_code, s.grade_name, COUNT(e.exam_id) as exam_count
                FROM subjects s 
                LEFT JOIN exams e ON s.subject_id = e.subject_id 
                WHERE s.grade_code IS NOT NULL 
                GROUP BY s.grade_code, s.grade_name 
                ORDER BY s.grade_code
                """;
            
            Query gradeNativeQuery = entityManager.createNativeQuery(gradeQuery);
            List<Object[]> gradeResults = gradeNativeQuery.getResultList();
            
            List<Map<String, Object>> grades = gradeResults.stream()
                .map(row -> Map.of(
                    "code", row[0],
                    "name", row[1],
                    "count", ((Number) row[2]).intValue()
                ))
                .toList();
            stopWatch.stop();
            log.info("학년 조회 완료: {} 건", grades.size());

            // 과목 옵션 조회 (area_code 기준)
            stopWatch.start("과목 조회");
            String subjectQuery = """
                SELECT DISTINCT s.area_code, s.area_name, COUNT(e.exam_id) as exam_count
                FROM subjects s 
                LEFT JOIN exams e ON s.subject_id = e.subject_id 
                WHERE s.area_code IS NOT NULL 
                GROUP BY s.area_code, s.area_name 
                ORDER BY s.area_code
                """;
                
            Query subjectNativeQuery = entityManager.createNativeQuery(subjectQuery);
            List<Object[]> subjectResults = subjectNativeQuery.getResultList();
            
            List<Map<String, Object>> subjects = subjectResults.stream()
                .map(row -> Map.of(
                    "code", row[0],
                    "name", row[1],
                    "count", ((Number) row[2]).intValue()
                ))
                .toList();
            stopWatch.stop();
            log.info("과목 조회 완료: {} 건", subjects.size());

            // 학기 옵션 조회
            stopWatch.start("학기 조회");
            String termQuery = """
                SELECT DISTINCT s.term_code, s.term_name, COUNT(e.exam_id) as exam_count
                FROM subjects s 
                LEFT JOIN exams e ON s.subject_id = e.subject_id 
                WHERE s.term_code IS NOT NULL AND s.term_code != '99'
                GROUP BY s.term_code, s.term_name 
                ORDER BY s.term_code
                """;
                
            Query termNativeQuery = entityManager.createNativeQuery(termQuery);
            List<Object[]> termResults = termNativeQuery.getResultList();
            
            List<Map<String, Object>> terms = termResults.stream()
                .map(row -> Map.of(
                    "code", row[0],
                    "name", row[1],
                    "count", ((Number) row[2]).intValue()
                ))
                .toList();
            stopWatch.stop();
            log.info("학기 조회 완료: {} 건", terms.size());

            // 교과서 옵션 조회 (최신 데이터만)
            stopWatch.start("교과서 조회");
            String textbookQuery = """
                SELECT s.subject_id, s.subject_name, s.area_code, s.grade_code, COUNT(e.exam_id) as exam_count
                FROM subjects s
                LEFT JOIN exams e ON s.subject_id = e.subject_id
                WHERE s.subject_id IS NOT NULL 
                GROUP BY s.subject_id, s.subject_name, s.area_code, s.grade_code 
                ORDER BY s.area_code, s.grade_code, s.subject_name
                """;
                
            Query textbookNativeQuery = entityManager.createNativeQuery(textbookQuery);
            List<Object[]> textbookResults = textbookNativeQuery.getResultList();
            
            List<Map<String, Object>> textbooks = textbookResults.stream()
                .map(row -> Map.of(
                    "id", ((Number) row[0]).longValue(),
                    "name", row[1],
                    "areaCode", row[2],
                    "gradeCode", row[3],
                    "count", ((Number) row[4]).intValue()
                ))
                .toList();
            stopWatch.stop();
            log.info("교과서 조회 완료: {} 건", textbooks.size());

            log.info("필터 옵션 상세 실행 시간:\n{}", stopWatch.prettyPrint());

            filterOptions.put("grades", grades);
            filterOptions.put("subjects", subjects);
            filterOptions.put("terms", terms);
            filterOptions.put("textbooks", textbooks);
            
            log.debug("필터 옵션 조회 완료: 학년={}, 과목={}, 학기={}, 교과서={}", 
                grades.size(), subjects.size(), terms.size(), textbooks.size());
            
        } catch (Exception e) {
            log.error("필터 옵션 조회 중 오류 발생", e);
            // 오류 시 기본값 반환
            filterOptions.put("grades", List.of());
            filterOptions.put("subjects", List.of());
            filterOptions.put("terms", List.of());
            filterOptions.put("textbooks", List.of());
        }
        
        return filterOptions;
    }

    /**
     * 특정 시험지의 문항 ID 목록만 조회합니다
     * 성능 최적화를 위해 ID만 직접 조회
     * 
     * @param examId 시험지 ID
     * @return 문항 ID 목록
     */
    @Override
    @RedisCacheable(
        key = "'exam:itemIds:' + #examId",
        ttl = 60,  // 1시간 TTL
        timeUnit = java.util.concurrent.TimeUnit.MINUTES,
        condition = "#examId != null"
    )
    @LoggingTrace(level = LoggingTrace.LogLevel.INFO, logExecutionTime = true)
    public List<Long> getExamItemIds(Long examId) {
        log.info("[인덱스 없음] 시험지 문항 ID 조회: examId={}", examId);
        
        if (examId == null) {
            log.warn("examId가 null입니다");
            return new ArrayList<>();
        }
        
        try {
            // JPQL을 사용하여 직접 itemId만 조회 (성능 최적화)
            String jpql = "SELECT ei.item.itemId FROM ExamItem ei " +
                         "WHERE ei.exam.id = :examId " +
                         "AND ei.item IS NOT NULL " +
                         "ORDER BY ei.itemNo";
            
            List<Long> itemIds = entityManager.createQuery(jpql, Long.class)
                    .setParameter("examId", examId)
                    .getResultList();
            
            log.debug("시험지 문항 ID 목록 조회 완료: examId={}, count={}", 
                    examId, itemIds.size());
            
            return itemIds != null ? itemIds : new ArrayList<>();
            
        } catch (Exception e) {
            log.error("시험지 문항 ID 조회 중 오류: examId={}", examId, e);
            throw new RuntimeException("시험지 문항 ID 조회 실패", e);
        }
    }

    /**
     * 특정 시험지의 문항 정보를 조회합니다
     * 편집 모드에서 기존 시험지의 문항들을 불러올 때 사용
     * 
     * @param examId 시험지 ID
     * @return 시험지와 문항 정보
     */
    @Override
    @RedisCacheable(
        key = "'exam:withItems:' + #examId",
        ttl = 60,  // 1시간 TTL  
        timeUnit = java.util.concurrent.TimeUnit.MINUTES,
        condition = "#examId != null"
    )
    @Transactional(readOnly = true)
    public ExamWithItemsResponse getExamWithItems(Long examId) {
        log.debug("시험지 문항 정보 조회 시작: examId={}", examId);
        
        if (examId == null) {
            log.warn("examId가 null입니다");
            return null;
        }
        
        try {
            // 1. Exam 엔티티와 연관 데이터를 한 번에 조회 (N+1 문제 방지)
            Exam exam = examRepository.findByIdWithFullDetails(examId)
                    .orElse(null);
            
            if (exam == null) {
                log.warn("시험지를 찾을 수 없음: examId={}", examId);
                return null;
            }
            
            // 2. ExamItem 목록에서 itemId 추출 (itemNo 순서대로 정렬)
            List<Long> itemIds = exam.getExamItems().stream()
                    .sorted((a, b) -> {
                        Integer aNo = a.getItemNo() != null ? a.getItemNo() : 0;
                        Integer bNo = b.getItemNo() != null ? b.getItemNo() : 0;
                        return aNo.compareTo(bNo);
                    })
                    .map(examItem -> {
                        if (examItem.getItem() != null) {
                            return examItem.getItem().getItemId();
                        }
                        return null;
                    })
                    .filter(id -> id != null)
                    .collect(Collectors.toList());
            
            // 3. Subject에서 학년/과목 정보 추출
            String gradeName = "";
            String gradeCode = "";
            String areaName = "";
            String areaCode = "";
            String subjectName = "";
            Long subjectId = null;
            
            if (exam.getSubject() != null) {
                Subject subject = exam.getSubject();
                subjectId = subject.getSubjectId();
                subjectName = subject.getSubjectName();
                
                // Subject 엔티티의 StringCodeNamePair에서 정보 추출
                if (subject.getGrade() != null) {
                    gradeName = subject.getGrade().getName();
                    gradeCode = subject.getGrade().getCode();
                }
                
                if (subject.getArea() != null) {
                    areaName = subject.getArea().getName();
                    areaCode = subject.getArea().getCode();
                }
            }
            
            // 4. Response 객체 생성
            ExamWithItemsResponse response = ExamWithItemsResponse.builder()
                    .examId(exam.getId())
                    .examName(exam.getExamName())
                    .itemCount(exam.getItemCount() != null ? exam.getItemCount() : itemIds.size())
                    .itemIds(itemIds)
                    .gradeCode(gradeCode)
                    .gradeName(gradeName)
                    .areaCode(areaCode)
                    .areaName(areaName)
                    .subjectId(subjectId)
                    .subjectName(subjectName)
                    .build();
            
            log.debug("시험지 문항 정보 조회 완료: examId={}, itemCount={}, itemIds={}", 
                    examId, itemIds.size(), itemIds);
            
            return response;
            
        } catch (Exception e) {
            log.error("시험지 문항 정보 조회 중 오류 발생: examId={}", examId, e);
            throw new RuntimeException("시험지 문항 정보 조회 실패", e);
        }
    }
    
    /**
     * 시험 개수 통계 조회 (Redis 캐싱 적용)
     * - TestWizard, UserCreated, 공개범위별 개수 조회
     */
    @Override
    @RedisCacheable(
        key = "'exam:counts:' + " +
              "(#request.gradeCode != null ? #request.gradeCode : 'all') + ':' + " +
              "(#request.areaCode != null ? #request.areaCode : 'all') + ':' + " +
              "(#request.termCode != null ? #request.termCode : 'all') + ':' + " +
              "(#request.subjectId != null ? #request.subjectId : 'all')",
        ttl = 60,  // 60분 캐싱 (카운트는 자주 변하지 않음)
        condition = "#request != null"
    )
    public Map<String, Long> getExamCounts(ExamSearchRequest request) {
        log.debug("시험 개수 통계 조회 시작: {}", request);
        
        Map<String, Long> countData = new HashMap<>();
        
        try {
            // TestWizard 시험 개수 조회 (Exam은 subject 기준으로만 조회 가능)
            Long testWizardCount = examRepository.countByConditions(
                request.getSubjectId()
            );
            
            // 사용자 생성 시험 개수 조회
            Long userCreatedCount = userExamRepository.countUserExamsByConditions(
                request.getGradeCode(),
                request.getAreaCode(),
                request.getTermCode()
            );
            
            // 공개범위별 개수 조회 (TestWizard + UserExam)
            Long publicTestWizard = examRepository.countByVisibilityAndConditions(
                com.pullit.exam.enums.ExamVisibility.PUBLIC,
                request.getSubjectId()
            );
            
            Long publicUserCreated = userExamRepository.countUserExamsByVisibilityAndConditions(
                com.pullit.exam.enums.ExamVisibility.PUBLIC,
                request.getGradeCode(),
                request.getAreaCode(),
                request.getTermCode()
            );
            
            Long schoolTestWizard = examRepository.countByVisibilityAndConditions(
                com.pullit.exam.enums.ExamVisibility.SCHOOL,
                request.getSubjectId()
            );
            
            Long schoolUserCreated = userExamRepository.countUserExamsByVisibilityAndConditions(
                com.pullit.exam.enums.ExamVisibility.SCHOOL,
                request.getGradeCode(),
                request.getAreaCode(),
                request.getTermCode()
            );
            
            Long privateTestWizard = examRepository.countByVisibilityAndConditions(
                com.pullit.exam.enums.ExamVisibility.PRIVATE,
                request.getSubjectId()
            );
            
            Long privateUserCreated = userExamRepository.countUserExamsByVisibilityAndConditions(
                com.pullit.exam.enums.ExamVisibility.PRIVATE,
                request.getGradeCode(),
                request.getAreaCode(),
                request.getTermCode()
            );
            
            // 데이터 구성
            countData.put("testWizardCount", testWizardCount != null ? testWizardCount : 0L);
            countData.put("userCreatedCount", userCreatedCount != null ? userCreatedCount : 0L);
            countData.put("totalCount", (testWizardCount != null ? testWizardCount : 0L) + 
                                        (userCreatedCount != null ? userCreatedCount : 0L));
            countData.put("publicCount", (publicTestWizard != null ? publicTestWizard : 0L) + 
                                         (publicUserCreated != null ? publicUserCreated : 0L));
            countData.put("schoolCount", (schoolTestWizard != null ? schoolTestWizard : 0L) + 
                                         (schoolUserCreated != null ? schoolUserCreated : 0L));
            countData.put("privateCount", (privateTestWizard != null ? privateTestWizard : 0L) + 
                                          (privateUserCreated != null ? privateUserCreated : 0L));
            
            log.info("시험 개수 통계 조회 완료: totalCount={}, testWizard={}, userCreated={}",
                    countData.get("totalCount"), countData.get("testWizardCount"), countData.get("userCreatedCount"));
            
        } catch (Exception e) {
            log.error("시험 개수 통계 조회 중 오류 발생", e);
            // 오류 시 기본값 반환
            countData.put("totalCount", 0L);
            countData.put("testWizardCount", 0L);
            countData.put("userCreatedCount", 0L);
            countData.put("publicCount", 0L);
            countData.put("schoolCount", 0L);
            countData.put("privateCount", 0L);
        }
        
        return countData;
    }
    
    /**
     * 전체 문항 개수 조회 (Redis 캐싱 적용)
     */
    @Override
    @RedisCacheable(
        key = "'exam:questionCount:' + " +
              "(#request.gradeCode != null ? #request.gradeCode : 'all') + ':' + " +
              "(#request.areaCode != null ? #request.areaCode : 'all') + ':' + " +
              "(#request.termCode != null ? #request.termCode : 'all') + ':' + " +
              "(#request.subjectId != null ? #request.subjectId : 'all')",
        ttl = 60,  // 60분 캐싱
        condition = "#request != null"
    )
    public Long getTotalQuestionCount(ExamSearchRequest request) {
        log.debug("전체 문항 개수 조회 시작: {}", request);
        
        try {
            // TestWizard 시험의 문항 수 (Exam은 subject 기준으로만 조회 가능)
            Long testWizardQuestions = examRepository.countTotalQuestions(
                request.getSubjectId()
            );
            
            // 사용자 생성 시험의 문항 수
            Long userCreatedQuestions = userExamRepository.countUserExamQuestions(
                request.getGradeCode(),
                request.getAreaCode(),
                request.getTermCode()
            );
            
            Long totalQuestions = (testWizardQuestions != null ? testWizardQuestions : 0L) + 
                                 (userCreatedQuestions != null ? userCreatedQuestions : 0L);
            
            log.info("전체 문항 개수 조회 완료: total={}, testWizard={}, userCreated={}",
                    totalQuestions, testWizardQuestions, userCreatedQuestions);
            
            return totalQuestions;
            
        } catch (Exception e) {
            log.error("전체 문항 개수 조회 중 오류 발생", e);
            return 0L;
        }
    }
}
