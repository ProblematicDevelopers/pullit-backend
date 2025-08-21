package com.pullit.itemprocess.controller;

import com.pullit.common.constants.ServiceConstants;
import com.pullit.common.dto.response.ApiResponse;
import com.pullit.itemprocess.enums.OcrAreaCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Tag(name = "Item Process API", description = "ocr 변환 API")
@RequiredArgsConstructor
@RestController()
@RequestMapping("/api/item-process")
public class itemProcessController {

    @Operation(summary = "변환하기위한 이미지 전달", description = "전달받은 이미지를 과목에 맞는 OCR로 전달합니다 ")
    @PostMapping(value = "/trans-ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> transOcr(String areaCode, @RequestParam MultipartFile file, HttpServletResponse response) {
        log.info("file: {}", file.getSize());
        String ocrTitle = OcrAreaCode.fromString(areaCode).getOcrTitle();
        log.info("ocrTitle: {}", ocrTitle);
        //국, 영, 역, 사, 도
        if (ocrTitle.equals(ServiceConstants.OCR_CLOVA)) {
            log.debug("clova");

        //수, 과
        } else if (ocrTitle.equals(ServiceConstants.OCR_MATHPIX)){
            log.debug("mathpix");
        }
        return ResponseEntity.ok(ApiResponse.success("", ""));
    }

}
