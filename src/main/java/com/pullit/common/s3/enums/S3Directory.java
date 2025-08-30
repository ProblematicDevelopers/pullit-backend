package com.pullit.common.s3.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@Getter
@RequiredArgsConstructor
public enum S3Directory {
    // PDF 관련
    PDF_EXAM("pdfs/exams/", "시험지 PDF"),
    PDF_REPORT("pdfs/reports/", "리포트 PDF"),
    PDF_TEMPLATE("pdfs/templates/", "PDF 템플릿"),

    // 이미지 관련
    IMAGE_PROFILE("images/profiles/", "프로필 이미지"),
    IMAGE_QUESTION("images/questions/", "문제 이미지"),
    IMAGE_LOGO("images/logos/", "로고 이미지"),
    OCR_IMAGES("images/ocr/", "OCR 처리 이미지"),

    // 엑셀 관련
    EXCEL_EXPORT("excels/exports/", "엑셀 내보내기"),
    EXCEL_IMPORT("excels/imports/", "엑셀 가져오기"),

    // 문서 파일
    DOCUMENT("documents/", "문서 파일"),
    
    // 임시 파일
    TEMP("temp/", "임시 파일"),
    EXAM_PDF("exam-pdf/", "시험지 PDF");

    private final String path;
    private final String description;

    /**
     * 날짜별 경로 생성
     * 예: pdfs/exams/2024/01/20/
     */
    public String getDateBasedPath() {
        LocalDate now = LocalDate.now();
        return String.format("%s%d/%02d/%02d/",
                this.path,
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth()
        );
    }
}
