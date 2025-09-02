package com.pullit.domain.assignment.entity;

import com.pullit.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "submission_files")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SubmissionFile extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;
    
    @Column(nullable = false)
    private String originalFileName;
    
    @Column(nullable = false)
    private String storedFileName;
    
    @Column(nullable = false)
    private String filePath;
    
    @Column
    private String fileType;
    
    @Column
    private Long fileSize;
}