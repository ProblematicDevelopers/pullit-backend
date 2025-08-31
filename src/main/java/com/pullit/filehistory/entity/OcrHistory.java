package com.pullit.filehistory.entity;

import com.pullit.item.entity.ItemMetadata;
import com.pullit.itemprocess.entity.ProcessedItem;
import com.pullit.itemprocess.enums.AreaType;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_item_id")
    private ProcessedItem processedItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "area_type", length = 20)
    private AreaType areaType;

    @Lob
    @Column(name = "ocr_text", columnDefinition = "TEXT")
    private String ocrText;

    @Lob
    @Column(name = "edited_text", columnDefinition = "TEXT")
    private String editedText;

    @Column(name = "original_image_url", length = 500)
    private String originalImageUrl;

    @Column(name = "position_x")
    private String positionX;
    @Column(name = "position_y")
    private String positionY;
    @Column(name = "size_x")
    private String sizeX;
    @Column(name = "size_y")
    private String sizeY;

}
