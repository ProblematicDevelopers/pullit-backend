package com.pullit.domain.assignment.entity;

import com.pullit.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "submissions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Submission extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;
    
    @Column(nullable = false)
    private Long studentId;
    
    @Column(nullable = false)
    private String studentName;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubmissionStatus status;
    
    @Column
    private LocalDateTime submittedAt;
    
    @Column
    private Integer score;
    
    @Column(columnDefinition = "TEXT")
    private String feedback;
    
    @Column
    private Boolean isLate;
    
    @OneToMany(mappedBy = "submission", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubmissionFile> files = new ArrayList<>();
    
    public enum SubmissionStatus {
        NOT_SUBMITTED,
        SUBMITTED,
        GRADING,
        GRADED,
        RETURNED
    }
    
    public void submit() {
        this.status = SubmissionStatus.SUBMITTED;
        this.submittedAt = LocalDateTime.now();
        this.isLate = assignment.isOverdue();
    }
    
    public void grade(Integer score, String feedback) {
        this.status = SubmissionStatus.GRADED;
        this.score = score;
        this.feedback = feedback;
    }
}