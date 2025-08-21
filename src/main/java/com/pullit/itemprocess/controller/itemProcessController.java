package com.pullit.itemprocess.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.pullit.common.constants.ServiceConstants;
import com.pullit.common.dto.response.ApiResponse;
import com.pullit.itemprocess.dto.request.ClovaRequest;
import com.pullit.itemprocess.dto.response.ClovaResponse;
import com.pullit.itemprocess.dto.response.MathpixResponse;
import com.pullit.itemprocess.enums.OcrAreaCode;
import com.pullit.itemprocess.service.OcrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@Tag(name = "Item Process API", description = "ocr 변환 API")
@RequiredArgsConstructor
@RestController()
@RequestMapping("/api/item-process")
public class itemProcessController {
    final private OcrService ocrService;

    @Operation(
            summary = "OCR 이미지 변환",
            description = "업로드한 이미지를 과목 코드(areaCode)에 따라 Clova OCR 또는 Mathpix OCR로 변환합니다.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "성공적으로 OCR 처리됨",
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "잘못된 요청 (예: areaCode 잘못됨)"
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "500",
                            description = "서버 오류"
                    )
            }
    )

    @PostMapping(value = "/trans-ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<String>> transOcr(
            @Parameter(description = "과목 코드 (예: KO, EN, SO, HS → Clova / MA, SC → Mathpix)", example = "MA", required = true)
            @RequestParam String areaCode,
            @Parameter(description = "OCR 처리할 이미지 파일", required = true)
            @RequestParam MultipartFile file) throws IOException {
        String ocrTitle = OcrAreaCode.fromString(areaCode).getOcrTitle();
        log.info("file: {}", file.getSize());
        log.info("ocrTitle: {}", ocrTitle);
        log.info("areaCode: {}", areaCode);
        String resultText = "";
        OcrAreaCode area = OcrAreaCode.fromString(areaCode);

        // 국/영/사/역/도 → Clova OCR
        if (area.isClova()) {
            log.debug("call Clova OCR");
            // MultipartFile → Base64 변환 or 업로드 후 URL 전달
            String base64 = java.util.Base64.getEncoder().encodeToString(file.getBytes());

            ClovaRequest req = new ClovaRequest();
            ClovaRequest.Image img = new ClovaRequest.Image();
            img.setFormat("png");
            img.setName(file.getOriginalFilename());
            img.setData(base64);
            img.setUrl(null); // URL 대신 data로 전송

            req.setImages(List.of(img));

            req.setLang("ko");

            req.setRequestId("req-" + System.currentTimeMillis());
            req.setResultType("string");
            req.setTimestamp(System.currentTimeMillis());
            req.setVersion("V1");

            ClovaResponse clovaRes = ocrService.callClovaOcrApi(req);
            log.info("clovaRes: {}", clovaRes);

            resultText = clovaRes.getImages().get(0).getFields().stream()
                    .map(ClovaResponse.Field::getInferText)
                    .reduce("", (a, b) -> a + " " + b);

            // 수/과 → Mathpix OCR
            }else if (area.isMathpix()) {
            log.debug("call Mathpix OCR");
            String base64 = "data:image/png;base64," + java.util.Base64.getEncoder().encodeToString(file.getBytes());

            MathpixResponse mathpixRes = ocrService.callMathpixOcrApi(base64);
            resultText = mathpixRes.getText();
        }
        log.info("resultText: {}", resultText);
        return ResponseEntity.ok(ApiResponse.success("OCR 결과", resultText));
    }
}

