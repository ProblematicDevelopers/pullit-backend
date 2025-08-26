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
     *
     * @param request  검색 조건
     * @param pageable 페이징 정보
     * @return 검색 결과
     */
    Page<ItemMetadata> searchItems(ItemSearchRequest request, Pageable pageable);

    /**
     * 챕터별 문항 수 집계
     *
     * @param subjectId  교과서 ID
     * @param chapterIds 챕터 ID 목록
     * @return 챕터ID별 문항 수 맵
     */
    Map<Long, Long> countItemsByChapters(Long subjectId, List<Long> chapterIds);

    /**
     * 난이도별 문항 수 집계
     *
     * @param subjectId 교과서 ID
     * @return 난이도코드별 문항 수 맵
     */
    Map<Long, Long> countItemsByDifficulty(Long subjectId);

    /**
     * 문제 유형별 문항 수 집계
     *
     * @param subjectId 교과서 ID
     * @return 문제유형코드별 문항 수 맵
     */
    Map<Long, Long> countItemsByQuestionForm(Long subjectId);

    /**
     * 유사 문항 검색 (같은 챕터, 난이도, 문제유형)
     *
     * @param itemId 기준 문항 ID
     * @param limit  최대 결과 수
     * @return 유사 문항 목록
     */
    List<ItemMetadata> findSimilarItems(Long itemId, int limit);

    /**
     * 교과서별 전체 문항 수 조회
     *
     * @param subjectIds 교과서 ID 목록
     * @return 교과서ID별 문항 수 맵
     */
    Map<Long, Long> countItemsBySubjects(List<Long> subjectIds);

    /**
     * 난이도별 랜덤 문항 조회
     * @param subjectId 교과서 id
     * @param chapterIds chapter id
     * @param difficultyCode  난이도
     * @param limit 개수제한
     * @param independentOnly 지문여부
     * @return Item List
     */
    List<ItemMetadata> findRandomItemsWithPassageGrouping(Long subjectId, List<Long> chapterIds, Long difficultyCode, int limit, boolean independentOnly);

    /**
     * 지문에 묶인 모든 문제 조회
     * @param subjectId 교과서 ID
     * @param passageIds 지문 ID목록
     * @return Item List
     */
    List<ItemMetadata> findItemsByPassageIds(Long subjectId, List<Long> passageIds);
    /**
     * 난이도별 선택 가능 단위 수 조회 (독립 문항 + 지문 그룹)
     */
    Map<String, Long> countSelectionUnitsByDifficulty(
            Long subjectId,
            List<Long> chapterIds,
            Long difficultyCode
    );

    /**
     * 난이도 조회
     * @param subjectId 교과서 ID
     * @param chapterIds chapter id
     * @return 대표난이도 조회
     */
    Map<Long, Long> getPassageRepresentativeDifficulty(Long subjectId, List<Long> chapterIds);
}