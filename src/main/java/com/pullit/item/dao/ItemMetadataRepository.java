package com.pullit.item.dao;

import com.pullit.item.entity.ItemMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemMetadataRepository extends JpaRepository<ItemMetadata, Long>, ItemMetadataRepositoryCustom {
    // 기본 쿼리들
    Optional<ItemMetadata> findByItemId(Long itemId);

    List<ItemMetadata> findBySubject_SubjectId(Long subjectId);

    @Query("SELECT COUNT(i) FROM ItemMetadata i WHERE i.subject.subjectId = :subjectId")
    Long countBySubjectId(@Param("subjectId") Long subjectId);

    @Query("SELECT COUNT(i) FROM ItemMetadata i WHERE i.subject.subjectId = :subjectId AND i.chapterHierarchy.largeChapter.code = :chapterId")
    Long countBySubjectIdAndLargeChapterId(@Param("subjectId") Long subjectId, @Param("chapterId") Long chapterId);

    // 이미지 데이터가 있는 문항만 조회
    @Query("SELECT i FROM ItemMetadata i WHERE i.subject.subjectId = :subjectId AND i.hasImageData = true")
    List<ItemMetadata> findBySubjectIdWithImages(@Param("subjectId") Long subjectId);

    // 지문별 문항 조회
    List<ItemMetadata> findByPassageId(Long passageId);
}
