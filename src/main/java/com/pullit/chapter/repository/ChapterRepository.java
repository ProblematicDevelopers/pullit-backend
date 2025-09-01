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

    @Query("""
        SELECT 
            CASE 
                WHEN c.largeChapter.code = :chapterId THEN c.largeChapter.name
                WHEN c.mediumChapter.code = :chapterId THEN c.mediumChapter.name  
                WHEN c.smallChapter.code = :chapterId THEN c.smallChapter.name
                WHEN c.topicChapter.code = :chapterId THEN c.topicChapter.name
                ELSE NULL
            END 
        FROM Chapter c 
        WHERE c.largeChapter.code = :chapterId 
           OR c.mediumChapter.code = :chapterId
           OR c.smallChapter.code = :chapterId  
           OR c.topicChapter.code = :chapterId
        """)
    List<String> findChapterNamesByCode(@Param("chapterId") Long chapterId);
    
    default String findChapterNameByCode(Long chapterId) {
        List<String> names = findChapterNamesByCode(chapterId);
        return names.stream()
                .filter(name -> name != null && !name.trim().isEmpty())
                .findFirst()
                .orElse(null);
    }
}
