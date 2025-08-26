package com.pullit.common.cache.service;

import com.pullit.common.annotation.RedisCacheEvict;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 캐시 무효화를 위한 전용 서비스
 * 데이터 변경 작업 후 관련된 모든 캐시를 무효화하는 메소드들을 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheInvalidationService {
    
    private final RedisCacheService cacheService;
    
    /**
     * Exam 관련 모든 캐시 무효화
     * Exam이 생성/수정/삭제될 때 호출
     */
    @RedisCacheEvict(pattern = "exam:*", allEntries = true)
    public void invalidateAllExamCache() {
        log.info("모든 Exam 캐시 무효화");
    }
    
    /**
     * 특정 Exam의 캐시만 무효화
     */
    @RedisCacheEvict(key = "'exam:withItems:' + #examId")
    public void invalidateExamCache(Long examId) {
        log.info("Exam 캐시 무효화: examId={}", examId);
        // 연관된 다른 캐시도 삭제
        cacheService.evict("exam:itemIds:" + examId);
    }
    
    /**
     * Item 관련 모든 캐시 무효화
     * Item이 생성/수정/삭제될 때 호출
     */
    @RedisCacheEvict(pattern = "item:*", allEntries = true)
    public void invalidateAllItemCache() {
        log.info("모든 Item 캐시 무효화");
    }
    
    /**
     * 특정 Item의 캐시만 무효화
     */
    @RedisCacheEvict(key = "'item:detail:' + #itemId")
    public void invalidateItemCache(Long itemId) {
        log.info("Item 캐시 무효화: itemId={}", itemId);
    }
    
    /**
     * 특정 Subject와 관련된 Item 캐시 무효화
     */
    public void invalidateItemCacheBySubject(Long subjectId) {
        log.info("Subject별 Item 캐시 무효화: subjectId={}", subjectId);
        // Subject와 관련된 모든 Item 캐시 패턴 삭제
        cacheService.evictByPattern("item:count:*:" + subjectId);
        cacheService.evictByPattern("item:search:*");  // 검색 캐시도 무효화
    }
    
    /**
     * Chapter 관련 캐시 무효화
     */
    @RedisCacheEvict(pattern = "chapter:*", allEntries = true)
    public void invalidateAllChapterCache() {
        log.info("모든 Chapter 캐시 무효화");
    }
    
    /**
     * 특정 Subject의 Chapter 캐시 무효화
     */
    @RedisCacheEvict(key = "'chapter:bySubject:' + #subjectId")
    public void invalidateChapterCacheBySubject(Long subjectId) {
        log.info("Subject별 Chapter 캐시 무효화: subjectId={}", subjectId);
        // 트리 구조 캐시도 삭제
        cacheService.evict("chapter:tree:" + subjectId);
    }
    
    /**
     * Subject 관련 캐시 무효화
     */
    @RedisCacheEvict(pattern = "subject:*", allEntries = true)
    public void invalidateAllSubjectCache() {
        log.info("모든 Subject 캐시 무효화");
    }
    
    /**
     * 필터 옵션 캐시 무효화
     * 데이터 구조가 변경될 때 호출
     */
    @RedisCacheEvict(key = "'exam:filterOptions'")
    public void invalidateFilterOptionsCache() {
        log.info("필터 옵션 캐시 무효화");
    }
    
    /**
     * 최근 시험 목록 캐시 무효화
     * 새로운 시험이 추가될 때 호출
     */
    public void invalidateRecentExamsCache() {
        log.info("최근 시험 목록 캐시 무효화");
        // 모든 limit별 캐시 삭제
        cacheService.evictByPattern("exam:recent:*");
    }
    
    /**
     * 빠른 검색 캐시 무효화
     * 시험 제목이 변경될 때 호출
     */
    public void invalidateQuickSearchCache() {
        log.info("빠른 검색 캐시 무효화");
        cacheService.evictByPattern("exam:quickSearch:*");
    }
    
    /**
     * 전체 캐시 무효화 (주의해서 사용)
     * 대량 데이터 마이그레이션이나 시스템 업데이트 시에만 사용
     */
    public void invalidateAllCache() {
        log.warn("전체 캐시 무효화 실행!");
        cacheService.evictByPattern("*");
    }
}