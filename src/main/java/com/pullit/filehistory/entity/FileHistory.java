package com.pullit.filehistory.entity;

import com.pullit.common.file.entity.FileMetadata;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

@Entity
@Table(name = "file_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_history_id", nullable = false)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id", nullable = false)
    private FileMetadata fileMetadataId;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "file_history_name")
    private String fileHistoryName;

    @Column(name = "img_count")
    private Long imgCount;

    @Column(name = "img_order")
    private String imgOrder;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_date")
    private Timestamp createdDate;

    @Column(name = "updated_date")
    private Timestamp updatedDate;

}
