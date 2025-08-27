package com.pullit.filehistory.entity;

import com.pullit.common.entity.BaseTimeEntity;
import com.pullit.common.file.entity.FileMetadata;
import com.pullit.item.entity.Subject;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "file_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileHistory extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_history_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_metadata_id", nullable = false)
    private FileMetadata fileMetadata;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @OneToMany(mappedBy = "fileHistory")
    private List<PdfImage> pdfImages;

    @Column(name = "file_history_name")
    private String fileHistoryName;

    @Column(name = "img_count")
    private int imgCount;

    @Column(name = "img_order")
    private String imgOrder;

    @Column(name = "created_by")
    private String createdBy;
}
