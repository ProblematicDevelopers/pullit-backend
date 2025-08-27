package com.pullit.filehistory.entity;

import com.pullit.item.entity.ItemMetadata;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ocr_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ocr_history_id", nullable = false)
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pdf_image_id")
    private PdfImage pdfImage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private ItemMetadata itemMetadata;

    @Column(name = "position_x")
    private String positionX;
    @Column(name = "position_y")
    private String positionY;
    @Column(name = "size_x")
    private String sizeX;
    @Column(name = "size_y")
    private String sizeY;

}
