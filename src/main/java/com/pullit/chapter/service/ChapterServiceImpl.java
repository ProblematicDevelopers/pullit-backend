package com.pullit.chapter.service;

import com.pullit.chapter.dto.response.*;
import com.pullit.chapter.repository.ChapterRepository;
import com.pullit.chapter.entity.Chapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class ChapterServiceImpl implements ChapterService {
    private final ChapterRepository chapterRepository;

    @Override
    public List<Chapter> findAll() {
        return List.of();
    }

    @Override
    public List<ChapterResponse> findAllChapterOnly() {
        return chapterRepository.findAll().stream()
                .map(ChapterResponse::from)
                .toList();
    }

    @Override

    public List<ChapterResponse> findBySubjectId(Long subjectId) {
        return chapterRepository.findBySubject_SubjectId(subjectId).stream()
                .map(ChapterResponse::from)
                .toList();
    }

    @Override
    public List<LargeNode> findTreeBySubjectId(Long subjectId) {
        List<Chapter> rows = chapterRepository.findBySubject_SubjectIdOrderByLargeChapter_CodeAscMediumChapter_CodeAscSmallChapter_CodeAscTopicChapter_CodeAsc(subjectId);
        // 중/소는 (부모-자식 연결을 위해) 맵만 보조적으로 씁니다.
        Map<Long, LargeNode> largeMap = new LinkedHashMap<>();
        Map<Long, MediumNode> mediumMap = new LinkedHashMap<>();
        Map<Long, SmallNode> smallMap  = new LinkedHashMap<>();

        for (Chapter c : rows) {
            if (c.getLargeChapter() == null || c.getLargeChapter().getCode() == null) {
                continue;
            }
            Long lId = c.getLargeChapter().getCode();
            LargeNode l = largeMap.get(lId);
            if (l == null) {
                l = LargeNode.builder()
                        .id(lId)
                        .name(c.getLargeChapter().getName())
                        .children(new ArrayList<>())
                        .build();
                largeMap.put(lId, l);
            }

            if (c.getMediumChapter() != null && c.getMediumChapter().getCode() != null) {
                Long mId = c.getMediumChapter().getCode();
                MediumNode m = mediumMap.get(mId);
                if (m == null) {
                    m = MediumNode.builder()
                            .id(mId)
                            .name(c.getMediumChapter().getName())
                            .children(new ArrayList<>())
                            .build();
                    l.getChildren().add(m);
                    mediumMap.put(mId, m);
                }
                if (c.getSmallChapter() != null && c.getSmallChapter().getCode() != null) {
                    Long sId = c.getSmallChapter().getCode();
                    SmallNode s = smallMap.get(sId);
                    if (s == null) {
                        s = SmallNode.builder()
                                .id(sId)
                                .name(c.getSmallChapter().getName())
                                .topics(new ArrayList<>())
                                .build();
                        m.getChildren().add(s);
                        smallMap.put(sId, s);
                    }
                    if (c.getTopicChapter() != null && c.getTopicChapter().getCode() != null) {
                        s.getTopics().add(
                                TopicNode.builder()
                                        .id(c.getTopicChapter().getCode())
                                        .name(c.getTopicChapter().getName())
                                        .build()
                        );
                    }
                }
            }
        }
        return new ArrayList<>(largeMap.values());
    }
}
