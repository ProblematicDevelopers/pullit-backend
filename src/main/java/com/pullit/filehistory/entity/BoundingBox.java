package com.pullit.filehistory.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BoundingBox {
    
    // 정규화 좌표 (0~1 범위 권장)
    @Column(name = "norm_x")
    private Double normalizedX;
    
    @Column(name = "norm_y")
    private Double normalizedY;
    
    @Column(name = "norm_width")
    private Double normalizedWidth;
    
    @Column(name = "norm_height")
    private Double normalizedHeight;
    
    // 렌더 컨텍스트
    @Column(name = "page_no")
    private Integer pageNo;
    
    @Column(name = "scale_factor")
    private Double scale;
    
    @Column(name = "rotation")
    private Double rotation;
    
    @Column(name = "canvas_width")
    private Integer canvasWidth;
    
    @Column(name = "canvas_height")
    private Integer canvasHeight;
    
    // 원본 픽셀 좌표 (참고용)
    @Column(name = "pixel_x")
    private Double pixelX;
    
    @Column(name = "pixel_y")
    private Double pixelY;
    
    @Column(name = "pixel_width")
    private Double pixelWidth;
    
    @Column(name = "pixel_height")
    private Double pixelHeight;
}