package com.pullit.exam.entity;

import com.pullit.common.entity.FullAuditEntity;
import com.pullit.common.s3.dto.S3UploadResponse;
import com.pullit.exam.enums.ExamVisibility;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_exams")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_date IS NULL")
public class UserExam extends FullAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exam_id")
    private Long id;

    @Column(name = "exam_name", nullable = false, length = 500)
    private String examName;

    @Column(name = "grade_code", nullable = false, length = 10)
    private String gradeCode;

    @Column(name = "grade_name", length = 20)
    private String gradeName;

    // 학기 정보
    @Column(name = "term_code", length = 10)
    private String termCode;

    @Column(name = "term_name", length = 20)
    private String termName;

    @Column(name = "area_code", length = 10)
    private String areaCode;

    @Column(name = "area_name", length = 50)
    private String areaName;

    @Column(name = "exam_type", length = 20)
    private String examType;

    @Column(name = "total_items")
    @Builder.Default
    private Integer totalItems = 0;

    @Column(name = "total_points")
    @Builder.Default
    private Integer totalPoints = 100;


    @Column(name = "class_id")
    private Long classId;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", length = 20)
    @Builder.Default
    private ExamVisibility visibility = ExamVisibility.PRIVATE;

    @Column(name = "deleted_date")
    private LocalDateTime deletedDate;

    @Column(name = "deleted_by")
    private Long deletedBy;

    // PDF 관련 필드 추가
    @Column(name = "pdf_url", length = 500)
    private String pdfUrl;

    @Column(name = "answer_pdf_url", length = 500)
    private String answerPdfUrl;

    @Column(name = "pdf_generated_at")
    private LocalDateTime pdfGeneratedAt;

    // 시험 추가 정보
    @Column(name = "time_limit")
    private Integer timeLimit;  // 시험 시간(분)

    @Column(name = "exam_date")
    private LocalDate examDate;  // 시험 예정일

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;  // 시험 설명

    @Column(name = "pdf_s3_key", length = 500)
    private String pdfS3Key;

    @Column(name = "pdf_file_name", length = 255)
    private String pdfFileName;

    @Column(name ="pdf_file_size")
    private Long pdfFileSize;

    @Column(name="pdf_content_type", length = 500)
    private String pdfContentType;

    @Column(name = "answer_pdf_s3_key", length = 500)
    private String answerPdfS3Key;

    @Column(name = "answer_pdf_file_name", length = 255)
    private String answerPdfFileName;

    @Column(name = "answer_pdf_file_size")
    private Long answerPdfFileSize;

    @OneToMany(mappedBy = "userExam", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("itemOrder ASC")
    @Builder.Default
    private List<UserExamItem> examItems = new ArrayList<>();

    public void addExamItem(UserExamItem item) {
        examItems.add(item);
        item.setUserExam(this);
        this.totalItems = examItems.size();
        recalculateTotalPoints();
    }

    public void removeExamItem(UserExamItem item) {
        examItems.remove(item);
        item.setUserExam(null);
        this.totalItems = examItems.size();
        recalculateTotalPoints();
    }

    private void recalculateTotalPoints() {
        this.totalPoints = examItems.stream()
                .mapToInt(UserExamItem::getPoints)
                .sum();
    }

    // Soft Delete 메서드
    public void softDelete(Long deleterId) {
        this.deletedDate = LocalDateTime.now();
        this.deletedBy = deleterId;
    }

    public void restore() {
        this.deletedDate = null;
        this.deletedBy = null;
    }

    // PDF 관련 메서드 추가
    public void updatePdfUrl(String pdfUrl) {
        this.pdfUrl = pdfUrl;
        this.pdfGeneratedAt = LocalDateTime.now();
    }

    public void updateAnswerPdfUrl(String answerPdfUrl) {
        this.answerPdfUrl = answerPdfUrl;
    }

    public boolean hasPdf() {
        return pdfUrl != null && !pdfUrl.isEmpty();
    }

    public boolean hasAnswerPdf() {
        return answerPdfUrl != null && !answerPdfUrl.isEmpty();
    }

    public void updatePdfMetadata(S3UploadResponse response) {
        this.pdfUrl = response.getPublicUrl() != null ?
                response.getPublicUrl() : response.getPresignedUrl();
        this.pdfS3Key = response.getS3Key();
        this.pdfFileName = response.getFileName();
        this.pdfFileSize = response.getFileSize();
        this.pdfContentType = response.getContentType();
        this.pdfGeneratedAt = LocalDateTime.now();
    }

}
