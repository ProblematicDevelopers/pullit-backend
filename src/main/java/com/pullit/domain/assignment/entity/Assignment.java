package com.pullit.domain.assignment.entity;

import com.pullit.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "assignments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Assignment extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(columnDefinition = "TEXT")
    private String description;
    
    @Column(nullable = false)
    private Long teacherId;
    
    @Column(nullable = false)
    private LocalDateTime dueDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssignmentStatus status;
    
    @Column
    private Integer maxScore;
    
    @Column
    private Boolean allowLateSubmission;
    
    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AssignmentFile> files = new ArrayList<>();
    
    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AssignmentClass> assignmentClasses = new ArrayList<>();
    
    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Submission> submissions = new ArrayList<>();
    
    public enum AssignmentStatus {
        DRAFT,
        PUBLISHED,
        CLOSED,
        GRADING,
        COMPLETED
    }
    
    public void publish() {
        this.status = AssignmentStatus.PUBLISHED;
    }
    
    public void close() {
        this.status = AssignmentStatus.CLOSED;
    }
    
    public boolean isOverdue() {
        return LocalDateTime.now().isAfter(this.dueDate);
    }
}