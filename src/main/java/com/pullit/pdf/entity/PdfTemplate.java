package com.pullit.pdf.entity;

import com.pullit.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import javax.annotation.processing.SupportedAnnotationTypes;

@Entity
@Table(name = "pdf_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"schema"})
public class PdfTemplate extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_id")
    private Long id;

    @Column(name = "template_name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "template_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TemplateType type;

    @Column(name = "schema_json", columnDefinition = "JSON", nullable = false)
    private String schema;

    @Column(name = "page_size", length = 20)
    private String pageSize = "A4";

    @Column(name = "page_orientation", length = 20)
    private String pageOrientation = "PORTRAIT";

    @Column(name = "margins", columnDefinition = "JSON")
    private String margins;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "usage_count")
    private Integer usageCount = 0;

    @Column(name = "version")
    private Integer version = 1;


    /**
     * 사용 횟수 증가 메서드
     */
    public void incrementUsageCount() {
        this.usageCount = (this.usageCount == null ? 0 : this.usageCount) + 1;
    }

    /**
     * 버전 증가 메서드
     */
    public void incrementVersion() {
        this.version = (this.version == null ? 1 : this.version) + 1;
    }

}
