package com.pullit.chapter.repository;

import com.pullit.chapter.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Integer> {
    List<Chapter> findBySubject_SubjectId(Long subjectId);

    List<Chapter> findBySubject_SubjectIdOrderByLargeChapter_CodeAscMediumChapter_CodeAscSmallChapter_CodeAscTopicChapter_CodeAsc(Long subjectId);
    
    @Query("SELECT c FROM Chapter c WHERE c.subject.subjectId = :subjectId")
    List<Chapter> findChaptersBySubjectId(@Param("subjectId") Long subjectId);
    
    @Query(value = "SELECT * FROM chapters WHERE subject_id = :subjectId", nativeQuery = true)
    List<Chapter> findChaptersBySubjectIdNative(@Param("subjectId") Long subjectId);

    String findNameById(Long id);
}
