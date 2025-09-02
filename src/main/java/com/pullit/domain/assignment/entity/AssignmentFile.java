package com.pullit.domain.assignment.entity;

import com.pullit.domain.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "assignment_files")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AssignmentFile extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private Assignment assignment;
    
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
    
    @Column
    private String uploaderId;
}