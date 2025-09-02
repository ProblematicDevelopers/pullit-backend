package com.pullit.itemprocess.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pullit.itemprocess.entity.ProcessItemMetadata;

@Repository
public interface ProcessItemMetadataRepository extends JpaRepository<ProcessItemMetadata, Long> {
    Optional<ProcessItemMetadata> findBySourceItemId(Long sourceItemId);
    
    // 같은 subject의 지문별 문항 조회
    List<ProcessItemMetadata> findBySubject_SubjectIdAndPassageId(Long subjectId, Long passageId);
    
    // 같은 subject의 모든 문항 조회
    List<ProcessItemMetadata> findBySubject_SubjectId(Long subjectId);
    
    // 같은 subject의 모든 지문 그룹 ID 조회
    @Query("SELECT DISTINCT p.passageId FROM ProcessItemMetadata p WHERE p.subject.subjectId = :subjectId AND p.passageId IS NOT NULL")
    List<Long> findDistinctPassageIdsBySubjectId(@Param("subjectId") Long subjectId);
    
    // 지문별 문항 수 조회
    @Query("SELECT p.passageId, COUNT(p) FROM ProcessItemMetadata p WHERE p.subject.subjectId = :subjectId AND p.passageId IS NOT NULL GROUP BY p.passageId")
    List<Object[]> countItemsByPassageId(@Param("subjectId") Long subjectId);
    
    // 독립 문항 조회 (passageId가 null인 문항들)
    @Query("SELECT p FROM ProcessItemMetadata p WHERE p.subject.subjectId = :subjectId AND p.passageId IS NULL")
    List<ProcessItemMetadata> findIndependentItemsBySubjectId(@Param("subjectId") Long subjectId);
    
    // 지문 그룹의 대표 문항 조회 (각 지문에서 첫 번째 문항)
    @Query("SELECT p FROM ProcessItemMetadata p WHERE p.subject.subjectId = :subjectId AND p.passageId IS NOT NULL AND p.itemId IN (SELECT MIN(p2.itemId) FROM ProcessItemMetadata p2 WHERE p2.subject.subjectId = :subjectId AND p2.passageId IS NOT NULL GROUP BY p2.passageId)")
    List<ProcessItemMetadata> findPassageRepresentativesBySubjectId(@Param("subjectId") Long subjectId);
    
    // 난이도별 문항 조회
    @Query("SELECT p FROM ProcessItemMetadata p WHERE p.subject.subjectId = :subjectId AND p.difficulty.code = :difficultyCode")
    List<ProcessItemMetadata> findBySubjectIdAndDifficultyCode(@Param("subjectId") Long subjectId, @Param("difficultyCode") Long difficultyCode);
    
    // 문제 유형별 문항 조회
    @Query("SELECT p FROM ProcessItemMetadata p WHERE p.subject.subjectId = :subjectId AND p.questionForm.code = :questionFormCode")
    List<ProcessItemMetadata> findBySubjectIdAndQuestionFormCode(@Param("subjectId") Long subjectId, @Param("questionFormCode") Long questionFormCode);
    
    // 챕터별 문항 조회
    @Query("SELECT p FROM ProcessItemMetadata p WHERE p.subject.subjectId = :subjectId AND (" +
           "p.chapterHierarchy.largeChapter.code IN :chapterIds OR " +
           "p.chapterHierarchy.mediumChapter.code IN :chapterIds OR " +
           "p.chapterHierarchy.smallChapter.code IN :chapterIds OR " +
           "p.chapterHierarchy.topicChapter.code IN :chapterIds)")
    List<ProcessItemMetadata> findBySubjectIdAndChapterIds(@Param("subjectId") Long subjectId, @Param("chapterIds") List<Long> chapterIds);
    
    // HTML 데이터가 있는 문항 조회
    @Query("SELECT p FROM ProcessItemMetadata p WHERE p.subject.subjectId = :subjectId AND p.hasHtmlData = true")
    List<ProcessItemMetadata> findBySubjectIdWithHtmlData(@Param("subjectId") Long subjectId);
    
    // 이미지 데이터가 있는 문항 조회
    @Query("SELECT p FROM ProcessItemMetadata p WHERE p.subject.subjectId = :subjectId AND p.hasImageData = true")
    List<ProcessItemMetadata> findBySubjectIdWithImageData(@Param("subjectId") Long subjectId);
    
    // 최근 생성된 문항 조회
    @Query("SELECT p FROM ProcessItemMetadata p WHERE p.subject.subjectId = :subjectId ORDER BY p.createdDate DESC")
    List<ProcessItemMetadata> findRecentItemsBySubjectId(@Param("subjectId") Long subjectId);
    
    // 전체 문항 수 조회
    @Query("SELECT COUNT(p) FROM ProcessItemMetadata p WHERE p.subject.subjectId = :subjectId")
    Long countBySubjectId(@Param("subjectId") Long subjectId);
    
    // 난이도별 문항 수 조회
    @Query("SELECT p.difficulty.code, COUNT(p) FROM ProcessItemMetadata p WHERE p.subject.subjectId = :subjectId GROUP BY p.difficulty.code")
    List<Object[]> countBySubjectIdAndDifficulty(@Param("subjectId") Long subjectId);
    
    // 문제 유형별 문항 수 조회
    @Query("SELECT p.questionForm.code, COUNT(p) FROM ProcessItemMetadata p WHERE p.subject.subjectId = :subjectId GROUP BY p.questionForm.code")
    List<Object[]> countBySubjectIdAndQuestionForm(@Param("subjectId") Long subjectId);
}