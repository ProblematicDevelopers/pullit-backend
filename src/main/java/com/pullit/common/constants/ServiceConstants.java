package com.pullit.common.constants;

import com.pullit.common.exception.BusinessException;
import com.pullit.common.exception.ErrorCode;

//서비스 내에서 사용하는 상수들
public class ServiceConstants {
    private ServiceConstants() {
        throw new BusinessException(ErrorCode.CONSTANTS_NO_INSTANCES);
    }

    // ========== FileHistory ProblemSourceOption==========
    // response 코드
    public static final String CBT_CODE = "CBT";
    public static final String TEXTBOOK_CODE = "TEXTBOOK";

    // 표시명
    public static final String CBT_NAME = "CBT(기출문제)";
    public static final String TEXTBOOK_NAME = "교과서";

    // 설명
    public static final String CBT_DESC = "기출문제 시험지 기반 가공";
    public static final String TEXTBOOK_DESC = "교과서 문제/지문 기반 가공";

    // ========== 필요시 더 추가 ==========
}
