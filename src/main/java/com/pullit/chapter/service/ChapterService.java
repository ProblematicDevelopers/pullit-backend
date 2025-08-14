package com.pullit.chapter.service;

import com.pullit.chapter.dto.response.ChapterResponse;
import com.pullit.chapter.dto.response.LargeNode;
import com.pullit.chapter.entity.Chapter;
import com.pullit.item.entity.Subject;

import java.util.List;

public interface ChapterService {
    List<Chapter> findAll();
    List<ChapterResponse> findAllChapterOnly();
    List<ChapterResponse> findBySubjectId(Long subjectId);
    List<LargeNode> findTreeBySubjectId(Long subjectId);

}
