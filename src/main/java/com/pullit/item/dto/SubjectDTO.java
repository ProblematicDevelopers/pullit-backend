package com.pullit.item.dto;

import com.pullit.common.embedded.StringCodeNamePair;
import com.pullit.item.entity.Subject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubjectDTO {
    private Long subjectId;
    private String subjectName;
    private String subjectThumbnail;

    private String curriculumCode;
    private String curriculumName;

    private String schoolLevelCode;
    private String schoolLevelName;

    private String gradeCode;
    private String gradeName;

    private String termCode;
    private String termName;

    private String areaCode;
    private String areaName;

    public static SubjectDTO from(Subject s) {
        if (s == null) return null;

        return SubjectDTO.builder()
                .subjectId(s.getSubjectId())
                .subjectName(s.getSubjectName())
                .subjectThumbnail(s.getSubjectThumbnail())
                .curriculumCode(codeOf(s.getCurriculum()))
                .curriculumName(nameOf(s.getCurriculum()))
                .schoolLevelCode(codeOf(s.getSchoolLevel()))
                .schoolLevelName(nameOf(s.getSchoolLevel()))
                .gradeCode(codeOf(s.getGrade()))
                .gradeName(nameOf(s.getGrade()))
                .termCode(codeOf(s.getTerm()))
                .termName(nameOf(s.getTerm()))
                .areaCode(codeOf(s.getArea()))
                .areaName(nameOf(s.getArea()))
                .build();
    }

    /** 리스트 매핑 유틸 */
    public static List<SubjectDTO> fromList(List<Subject> subjects) {
        if (subjects == null) return List.of();
        return subjects.stream()
                .filter(Objects::nonNull)
                .map(SubjectDTO::from)
                .collect(Collectors.toList());
    }

    // -------- 내부 유틸 --------
    private static String codeOf(StringCodeNamePair pair) {
        return pair != null ? pair.getCode() : null;
    }

    private static String nameOf(StringCodeNamePair pair) {
        return pair != null ? pair.getName() : null;
    }
}
