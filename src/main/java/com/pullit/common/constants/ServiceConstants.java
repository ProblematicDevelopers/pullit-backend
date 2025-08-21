package com.pullit.common.constants;

import com.pullit.common.exception.BusinessException;
import com.pullit.common.exception.ErrorCode;

//서비스 내에서 사용하는 상수들
public class ServiceConstants {
    private ServiceConstants() {
        throw new BusinessException(ErrorCode.CONSTANTS_NO_INSTANCES);
    }

    // ========== ItemProcess==========
    public static final String OCR_CLOVA = "CLOVA";
    public static final String OCR_MATHPIX = "CLOVA";


    // ========== 필요시 더 추가 ==========
}
