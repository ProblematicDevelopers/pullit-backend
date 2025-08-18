package com.pullit.item.dao;

import com.pullit.item.dto.request.ItemSearchRequest;
import com.pullit.item.entity.ItemMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

public interface ItemMetadataRepositoryCustom {
    
    /**
     * 동적 검색 쿼리
     * @param request 검색 조건
     * @param pageable 페이징 정보
     * @return 검색 결과
     */
    Page<ItemMetadata> searchItems(ItemSearchRequest request, Pageable pageable);
    
    /**
     * 챕터별 문항 수 집계
     * @param subjectId 교과서 ID
     * @param chapterIds 챕터 ID 목록
     * @return 챕터ID별 문항 수 맵
     */
    Map<Long, Long> countItemsByChapters(Long subjectId, List<Long> chapterIds);
    
    /**
     * 난이도별 문항 수 집계
     * @param subjectId 교과서 ID
     * @return 난이도코드별 문항 수 맵
     */
    Map<Long, Long> countItemsByDifficulty(Long subjectId);
    
    /**
     * 문제 유형별 문항 수 집계
     * @param subjectId 교과서 ID
     * @return 문제유형코드별 문항 수 맵
     */
    Map<Long, Long> countItemsByQuestionForm(Long subjectId);
    
    /**
     * 유사 문항 검색 (같은 챕터, 난이도, 문제유형)
     * @param itemId 기준 문항 ID
     * @param limit 최대 결과 수
     * @return 유사 문항 목록
     */
    List<ItemMetadata> findSimilarItems(Long itemId, int limit);
    
    /**
     * 교과서별 전체 문항 수 조회
     * @param subjectIds 교과서 ID 목록
     * @return 교과서ID별 문항 수 맵
     */
    Map<Long, Long> countItemsBySubjects(List<Long> subjectIds);
}