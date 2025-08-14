package com.pullit.chapter.repository;

import com.pullit.chapter.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Integer> {
    List<Chapter> findBySubject_SubjectId(Long subjectId);

    List<Chapter> findBySubject_SubjectIdOrderByLargeChapter_CodeAscMediumChapter_CodeAscSmallChapter_CodeAscTopicChapter_CodeAsc(Long subjectId);

}
