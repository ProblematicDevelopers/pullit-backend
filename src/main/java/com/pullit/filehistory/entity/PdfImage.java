package com.pullit.filehistory.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "pdf_image")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PdfImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "pdf_image_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_history_id")
    private FileHistory fileHistory;

    @OneToMany(mappedBy = "pdfImage")
    private List<OcrHistory> ocrHistories;

}
