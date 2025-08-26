package com.pullit.item.dao;

import com.pullit.item.dto.request.ItemSearchRequest;
import com.pullit.item.entity.ItemMetadata;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ItemMetadataRepositoryImpl implements ItemMetadataRepositoryCustom {
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    public Page<ItemMetadata> searchItems(ItemSearchRequest request, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        
        // Count Query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<ItemMetadata> countRoot = countQuery.from(ItemMetadata.class);
        countQuery.select(cb.count(countRoot));
        
        List<Predicate> countPredicates = buildPredicates(request, cb, countRoot);
        if (!countPredicates.isEmpty()) {
            countQuery.where(cb.and(countPredicates.toArray(new Predicate[0])));
        }
        
        Long total = entityManager.createQuery(countQuery).getSingleResult();
        
        // Data Query
        CriteriaQuery<ItemMetadata> dataQuery = cb.createQuery(ItemMetadata.class);
        Root<ItemMetadata> dataRoot = dataQuery.from(ItemMetadata.class);
        dataQuery.select(dataRoot);
        
        // Fetch joins for performance
        dataRoot.fetch("subject", JoinType.LEFT);
        dataRoot.fetch("imageData", JoinType.LEFT);
        
        List<Predicate> dataPredicates = buildPredicates(request, cb, dataRoot);
        if (!dataPredicates.isEmpty()) {
            dataQuery.where(cb.and(dataPredicates.toArray(new Predicate[0])));
        }
        
        // Apply sorting
        applySorting(request, cb, dataQuery, dataRoot);
        
        TypedQuery<ItemMetadata> typedQuery = entityManager.createQuery(dataQuery);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());
        
        List<ItemMetadata> content = typedQuery.getResultList();
        
        return new PageImpl<>(content, pageable, total);
    }
    
    private List<Predicate> buildPredicates(ItemSearchRequest request, CriteriaBuilder cb, Root<ItemMetadata> root) {
        List<Predicate> predicates = new ArrayList<>();
        
        // 필수: 교과서 ID
        predicates.add(cb.equal(root.get("subject").get("subjectId"), request.getSubjectId()));
        
        // 대단원 필터 (ChapterHierarchy 구조 고려)
        if (request.getLargeChapterIds() != null && !request.getLargeChapterIds().isEmpty()) {
            predicates.add(root.get("chapterHierarchy").get("largeChapter").get("code").in(request.getLargeChapterIds()));
        }
        
        // 중단원 필터
        if (request.getMediumChapterIds() != null && !request.getMediumChapterIds().isEmpty()) {
            predicates.add(root.get("chapterHierarchy").get("mediumChapter").get("code").in(request.getMediumChapterIds()));
        }
        
        // 소단원 필터
        if (request.getSmallChapterIds() != null && !request.getSmallChapterIds().isEmpty()) {
            predicates.add(root.get("chapterHierarchy").get("smallChapter").get("code").in(request.getSmallChapterIds()));
        }
        
        // 토픽 필터
        if (request.getTopicChapterIds() != null && !request.getTopicChapterIds().isEmpty()) {
            predicates.add(root.get("chapterHierarchy").get("topicChapter").get("code").in(request.getTopicChapterIds()));
        }
        
        // 난이도 필터
        if (request.getDifficultyCode() != null && !request.getDifficultyCode().isEmpty()) {
            predicates.add(root.get("difficulty").get("code").in(request.getDifficultyCode()));
        }
        
        // 문제 유형 필터
        if (request.getQuestionFormCode() != null && !request.getQuestionFormCode().isEmpty()) {
            predicates.add(root.get("questionForm").get("code").in(request.getQuestionFormCode()));
        }
        
        // 이미지 포함 여부
        if (request.getHasImage() != null) {
            predicates.add(cb.equal(root.get("hasImageData"), request.getHasImage()));
        }
        
        // HTML 포함 여부
        if (request.getHasHtml() != null) {
            predicates.add(cb.equal(root.get("hasHtmlData"), request.getHasHtml()));
        }
        
        // 지문 ID
        if (request.getPassageId() != null) {
            predicates.add(cb.equal(root.get("passageId"), request.getPassageId()));
        }
        
        // 키워드 검색 (챕터명에서 검색)
        if (request.hasKeyword()) {
            String keyword = "%" + request.getKeyword().toLowerCase() + "%";
            Predicate keywordPredicate = cb.or(
                cb.like(cb.lower(root.get("chapterHierarchy").get("largeChapter").get("name")), keyword),
                cb.like(cb.lower(root.get("chapterHierarchy").get("mediumChapter").get("name")), keyword),
                cb.like(cb.lower(root.get("chapterHierarchy").get("smallChapter").get("name")), keyword),
                cb.like(cb.lower(root.get("chapterHierarchy").get("topicChapter").get("name")), keyword)
            );
            predicates.add(keywordPredicate);
        }
        
        return predicates;
    }
    
    private void applySorting(ItemSearchRequest request, CriteriaBuilder cb, 
                              CriteriaQuery<ItemMetadata> query, Root<ItemMetadata> root) {
        String sortBy = request.getSortBy();
        boolean isAsc = "ASC".equalsIgnoreCase(request.getSortOrder());
        
        Order order;
        switch (sortBy) {
            case "difficultyCode":
                order = isAsc ? cb.asc(root.get("difficulty").get("code")) 
                              : cb.desc(root.get("difficulty").get("code"));
                break;
            case "largeChapterId":
                order = isAsc ? cb.asc(root.get("chapterHierarchy").get("largeChapter").get("code"))
                              : cb.desc(root.get("chapterHierarchy").get("largeChapter").get("code"));
                break;
            case "itemId":
            default:
                order = isAsc ? cb.asc(root.get("itemId")) 
                              : cb.desc(root.get("itemId"));
                break;
        }
        
        query.orderBy(order);
    }
    
    @Override
    public Map<Long, Long> countItemsByChapters(Long subjectId, List<Long> chapterIds) {
        // 대단원별 카운트
        String largeSql = "SELECT i.chapterHierarchy.largeChapter.code, COUNT(i) " +
                         "FROM ItemMetadata i " +
                         "WHERE i.subject.subjectId = :subjectId " +
                         "AND i.chapterHierarchy.largeChapter.code IN :chapterIds " +
                         "GROUP BY i.chapterHierarchy.largeChapter.code";
        
        // 중단원별 카운트
        String mediumSql = "SELECT i.chapterHierarchy.mediumChapter.code, COUNT(i) " +
                          "FROM ItemMetadata i " +
                          "WHERE i.subject.subjectId = :subjectId " +
                          "AND i.chapterHierarchy.mediumChapter.code IN :chapterIds " +
                          "GROUP BY i.chapterHierarchy.mediumChapter.code";
        
        // 대단원 결과 조회
        List<Object[]> largeResults = entityManager.createQuery(largeSql, Object[].class)
                .setParameter("subjectId", subjectId)
                .setParameter("chapterIds", chapterIds)
                .getResultList();
        
        // 중단원 결과 조회
        List<Object[]> mediumResults = entityManager.createQuery(mediumSql, Object[].class)
                .setParameter("subjectId", subjectId)
                .setParameter("chapterIds", chapterIds)
                .getResultList();
        
        // 결과 합치기
        Map<Long, Long> resultMap = largeResults.stream()
                .collect(Collectors.toMap(
                    r -> (Long) r[0],
                    r -> (Long) r[1]
                ));
        
        mediumResults.forEach(r -> {
            Long chapterId = (Long) r[0];
            Long count = (Long) r[1];
            resultMap.put(chapterId, count);
        });
        
        return resultMap;
    }
    
    @Override
    public Map<Long, Long> countItemsByDifficulty(Long subjectId) {
        String jpql = "SELECT i.difficulty.code, COUNT(i) " +
                      "FROM ItemMetadata i " +
                      "WHERE i.subject.subjectId = :subjectId " +
                      "GROUP BY i.difficulty.code";
        
        List<Object[]> results = entityManager.createQuery(jpql, Object[].class)
                .setParameter("subjectId", subjectId)
                .getResultList();
        
        return results.stream()
                .collect(Collectors.toMap(
                    r -> (Long) r[0],
                    r -> (Long) r[1]
                ));
    }
    
    @Override
    public Map<Long, Long> countItemsByQuestionForm(Long subjectId) {
        String jpql = "SELECT i.questionForm.code, COUNT(i) " +
                      "FROM ItemMetadata i " +
                      "WHERE i.subject.subjectId = :subjectId " +
                      "GROUP BY i.questionForm.code";
        
        List<Object[]> results = entityManager.createQuery(jpql, Object[].class)
                .setParameter("subjectId", subjectId)
                .getResultList();
        
        return results.stream()
                .collect(Collectors.toMap(
                    r -> (Long) r[0],
                    r -> (Long) r[1]
                ));
    }
    
    @Override
    public List<ItemMetadata> findSimilarItems(Long itemId, int limit) {
        // 먼저 대상 문항 조회
        ItemMetadata targetItem = entityManager.find(ItemMetadata.class, itemId);
        
        if (targetItem == null) {
            return new ArrayList<>();
        }
        
        String jpql = "SELECT i FROM ItemMetadata i " +
                      "WHERE i.itemId != :itemId " +
                      "AND i.subject.subjectId = :subjectId " +
                      "AND i.chapterHierarchy.largeChapter.code = :largeChapterId " +
                      "AND i.difficulty.code = :difficultyCode " +
                      "AND i.questionForm.code = :questionFormCode " +
                      "ORDER BY i.itemId";
        
        return entityManager.createQuery(jpql, ItemMetadata.class)
                .setParameter("itemId", itemId)
                .setParameter("subjectId", targetItem.getSubject().getSubjectId())
                .setParameter("largeChapterId", targetItem.getChapterHierarchy().getLargeChapter().getCode())
                .setParameter("difficultyCode", targetItem.getDifficulty().getCode())
                .setParameter("questionFormCode", targetItem.getQuestionForm().getCode())
                .setMaxResults(limit)
                .getResultList();
    }
    
    @Override
    public Map<Long, Long> countItemsBySubjects(List<Long> subjectIds) {
        if (subjectIds == null || subjectIds.isEmpty()) {
            return new HashMap<>();
        }
        
        String jpql = "SELECT i.subject.subjectId, COUNT(i) " +
                      "FROM ItemMetadata i " +
                      "WHERE i.subject.subjectId IN :subjectIds " +
                      "GROUP BY i.subject.subjectId";
        
        List<Object[]> results = entityManager.createQuery(jpql, Object[].class)
                .setParameter("subjectIds", subjectIds)
                .getResultList();
        
        return results.stream()
                .collect(Collectors.toMap(
                    r -> (Long) r[0],
                    r -> (Long) r[1]
                ));
    }

    @Override
    public List<ItemMetadata> findRandomItemsWithPassageGrouping(Long subjectId, List<Long> chapterIds, Long difficultyCode, int limit, boolean independentOnly) {
        String jpql;
        if (independentOnly) {
            // 독립 문항만 선택 (passageId가 null인 경우)
            jpql = "SELECT i FROM ItemMetadata i " +
                    "WHERE i.subject.subjectId = :subjectId " +
                    "AND (" +
                    "  i.chapterHierarchy.largeChapter.code IN :chapterIds " +
                    "  OR i.chapterHierarchy.mediumChapter.code IN :chapterIds " +
                    "  OR i.chapterHierarchy.smallChapter.code IN :chapterIds " +
                    "  OR i.chapterHierarchy.topicChapter.code IN :chapterIds" +
                    ") " +
                    "AND i.difficulty.code = :difficultyCode " +
                    "AND i.passageId IS NULL " +
                    "ORDER BY FUNCTION('RAND')";
        } else {
            // 지문 그룹 선택 (각 지문에서 대표 문항 하나씩만 선택)
            jpql = "SELECT i FROM ItemMetadata i " +
                    "WHERE i.subject.subjectId = :subjectId " +
                    "AND i.itemId IN (" +
                    "  SELECT MIN(i2.itemId) FROM ItemMetadata i2 " +
                    "  WHERE i2.subject.subjectId = :subjectId " +
                    "  AND (" +
                    "    i2.chapterHierarchy.largeChapter.code IN :chapterIds " +
                    "    OR i2.chapterHierarchy.mediumChapter.code IN :chapterIds " +
                    "    OR i2.chapterHierarchy.smallChapter.code IN :chapterIds " +
                    "    OR i2.chapterHierarchy.topicChapter.code IN :chapterIds" +
                    "  ) " +
                    "  AND i2.difficulty.code = :difficultyCode " +
                    "  AND i2.passageId IS NOT NULL " +
                    "  GROUP BY i2.passageId" +
                    ") " +
                    "ORDER BY FUNCTION('RAND')";
        }

        return entityManager.createQuery(jpql, ItemMetadata.class)
                .setParameter("subjectId", subjectId)
                .setParameter("chapterIds", chapterIds)
                .setParameter("difficultyCode", difficultyCode)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    public Map<String, Long> countSelectionUnitsByDifficulty(Long subjectId, List<Long> chapterIds, Long difficultyCode) {
        Map<String, Long> result = new HashMap<>();

        // 독립 문항 수
        String independentSql = "SELECT COUNT(i) FROM ItemMetadata i " +
                "WHERE i.subject.subjectId = :subjectId " +
                "AND (" +
                "  i.chapterHierarchy.largeChapter.code IN :chapterIds " +
                "  OR i.chapterHierarchy.mediumChapter.code IN :chapterIds " +
                "  OR i.chapterHierarchy.smallChapter.code IN :chapterIds " +
                "  OR i.chapterHierarchy.topicChapter.code IN :chapterIds" +
                ") " +
                "AND i.difficulty.code = :difficultyCode " +
                "AND i.passageId IS NULL";

        Long independentCount = entityManager.createQuery(independentSql, Long.class)
                .setParameter("subjectId", subjectId)
                .setParameter("chapterIds", chapterIds)
                .setParameter("difficultyCode", difficultyCode)
                .getSingleResult();

        // 지문 그룹 수
        String passageGroupSql = "SELECT COUNT(DISTINCT i.passageId) FROM ItemMetadata i " +
                "WHERE i.subject.subjectId = :subjectId " +
                "AND (" +
                "  i.chapterHierarchy.largeChapter.code IN :chapterIds " +
                "  OR i.chapterHierarchy.mediumChapter.code IN :chapterIds " +
                "  OR i.chapterHierarchy.smallChapter.code IN :chapterIds " +
                "  OR i.chapterHierarchy.topicChapter.code IN :chapterIds" +
                ") " +
                "AND i.difficulty.code = :difficultyCode " +
                "AND i.passageId IS NOT NULL";

        Long passageGroupCount = entityManager.createQuery(passageGroupSql, Long.class)
                .setParameter("subjectId", subjectId)
                .setParameter("chapterIds", chapterIds)
                .setParameter("difficultyCode", difficultyCode)
                .getSingleResult();

        result.put("independent", independentCount);
        result.put("passageGroup", passageGroupCount);
        result.put("total", independentCount + passageGroupCount);

        return result;    }

    @Override
    public List<ItemMetadata> findItemsByPassageIds(Long subjectId, List<Long> passageIds) {
        if (passageIds == null || passageIds.isEmpty()) {
            return new ArrayList<>();
        }

        String jpql = "SELECT i FROM ItemMetadata i " +
                "WHERE i.subject.subjectId = :subjectId " +
                "AND i.passageId IN :passageIds " +
                "ORDER BY i.passageId, i.itemId";

        return entityManager.createQuery(jpql, ItemMetadata.class)
                .setParameter("subjectId", subjectId)
                .setParameter("passageIds", passageIds)
                .getResultList();
    }

    @Override
    public Map<Long, Long> getPassageRepresentativeDifficulty(Long subjectId, List<Long> chapterIds) {
        // 각 지문의 대표 난이도 (첫 번째 문항의 난이도 사용)
        String jpql = "SELECT i.passageId, MIN(i.difficulty.code) " +
                "FROM ItemMetadata i " +
                "WHERE i.subject.subjectId = :subjectId " +
                "AND (" +
                "  i.chapterHierarchy.largeChapter.code IN :chapterIds " +
                "  OR i.chapterHierarchy.mediumChapter.code IN :chapterIds " +
                "  OR i.chapterHierarchy.smallChapter.code IN :chapterIds " +
                "  OR i.chapterHierarchy.topicChapter.code IN :chapterIds" +
                ") " +
                "AND i.passageId IS NOT NULL " +
                "GROUP BY i.passageId";

        List<Object[]> results = entityManager.createQuery(jpql, Object[].class)
                .setParameter("subjectId", subjectId)
                .setParameter("chapterIds", chapterIds)
                .getResultList();

        return results.stream()
                .collect(Collectors.toMap(
                        r -> (Long) r[0],
                        r -> (Long) r[1]
                ));
    }
}