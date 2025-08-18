package com.pullit.chapter.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pullit.chapter.entity.Chapter;
import com.pullit.common.embedded.StringCodeNamePair;
import com.pullit.item.embedded.CodeNamePair;
import lombok.*;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChapterResponse {

    private Long id;

    // Subject info (순환 방지: 엔티티 대신 식별자/이름만)
    private Long subjectId;
    private String subjectName;

    // Curriculum
    private String curriculumCode;
    private String curriculumName;

    // Chapter hierarchy
    private Long largeChapterId;
    private String largeChapterName;

    private Long mediumChapterId;
    private String mediumChapterName;

    private Long smallChapterId;
    private String smallChapterName;

    private Long topicChapterId;
    private String topicChapterName;

    // Convenience fields
    private String chapterPath;   // ex) "대단원 > 중단원 > 소단원 > 주제"
    private Integer chapterDepth; // 0~4

    /** 단일 매핑 */
    public static ChapterResponse from(Chapter c) {
        if (c == null) return null;

        return ChapterResponse.builder()
                .id(c.getId())
                .subjectId(c.getSubject() != null ? c.getSubject().getSubjectId() : null)
                .subjectName(c.getSubjectName())
                .curriculumCode(codeOf(c.getCurriculum()))
                .curriculumName(nameOf(c.getCurriculum()))
                .largeChapterId(codeOf(c.getLargeChapter()))
                .largeChapterName(nameOf(c.getLargeChapter()))
                .mediumChapterId(codeOf(c.getMediumChapter()))
                .mediumChapterName(nameOf(c.getMediumChapter()))
                .smallChapterId(codeOf(c.getSmallChapter()))
                .smallChapterName(nameOf(c.getSmallChapter()))
                .topicChapterId(codeOf(c.getTopicChapter()))
                .topicChapterName(nameOf(c.getTopicChapter()))
                .chapterPath(c.getChapterPath())
                .chapterDepth(c.getChapterDepth())
                .build();
    }

    /** 리스트 매핑 */
    public static List<ChapterResponse> fromList(List<Chapter> chapters) {
        if (chapters == null) return List.of();
        return chapters.stream()
                .filter(Objects::nonNull)
                .map(ChapterResponse::from)
                .collect(Collectors.toList());
    }

    // ---------- 내부 유틸 ----------
    private static String codeOf(StringCodeNamePair pair) {
        return pair != null ? pair.getCode() : null;
    }
    private static String nameOf(StringCodeNamePair pair) {
        return pair != null ? pair.getName() : null;
    }
    private static Long codeOf(CodeNamePair pair) {
        return pair != null ? pair.getCode() : null; // CodeNamePair의 code가 Long 기준
    }
    private static String nameOf(CodeNamePair pair) {
        return pair != null ? pair.getName() : null;
    }
}