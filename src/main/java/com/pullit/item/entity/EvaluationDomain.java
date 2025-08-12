package com.pullit.item.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "evaluation_domains")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString(exclude = {"subject"})

public class EvaluationDomain {
    @Id
    @Column(name = "domain_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long domainId;

    @Column(name = "domain_name", length = 100, nullable = false)
    private String domainName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    public boolean belongsToSubject(Long subjectId) {
        return subject != null && subject.getSubjectId().equals(subjectId);
    }

    public String getFullDomainName() {
        if (subject != null && subject.getSubjectName() != null) {
            return subject.getSubjectName() + " - " + domainName;
        }
        return domainName;
    }

    public boolean isNamedAs(String name) {
        return domainName != null && domainName.equals(name);
    }

    public static EvaluationDomain create(String domainName, Subject subject) {
        return EvaluationDomain.builder()
                .domainName(domainName)
                .subject(subject)
                .build();
    }

}
