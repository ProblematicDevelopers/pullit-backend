package com.pullit.itemprocess.enums;

import com.pullit.common.constants.ServiceConstants;
import com.pullit.common.exception.BusinessException;
import com.pullit.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum OcrAreaCode {
    //clova
    // url: clova invoke-url
    // header X-OCR-SECRET, json? (이건 더 찾아보자

    //mathpix
    //url: mathpix url
    //header app_id, app_key
    MA("수학", ServiceConstants.OCR_MATHPIX),
    KO("국어", ServiceConstants.OCR_CLOVA),
    EN("영어", ServiceConstants.OCR_CLOVA),
    SO("사회", ServiceConstants.OCR_CLOVA),
    HS("역사", ServiceConstants.OCR_CLOVA),
    SC("과학", ServiceConstants.OCR_MATHPIX);

    private final String title;
    private final String ocrTitle;

    public static OcrAreaCode fromString(String code) {
        if (code == null) {
            throw new BusinessException(ErrorCode.CONSTANTS_NO_INSTANCES);
        }
        return Arrays.stream(OcrAreaCode.values())
                .filter(area -> area.name().equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new BusinessException(ErrorCode.CONSTANTS_NO_INSTANCES));
    }
    public boolean isClova() {
        return ServiceConstants.OCR_CLOVA.equals(this.ocrTitle);
    }

    public boolean isMathpix() {
        return ServiceConstants.OCR_MATHPIX.equals(this.ocrTitle);
    }
}
