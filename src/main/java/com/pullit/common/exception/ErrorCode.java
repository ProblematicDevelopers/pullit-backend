package com.pullit.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ========== 공통 에러 (COMMON) ==========
    SUCCESS("COMMON_001", "정상 처리되었습니다.", HttpStatus.OK),
    INVALID_INPUT("COMMON_002", "잘못된 입력값입니다.", HttpStatus.BAD_REQUEST),
    METHOD_NOT_ALLOWED("COMMON_003", "허용되지 않은 HTTP 메소드입니다.", HttpStatus.METHOD_NOT_ALLOWED),
    INTERNAL_SERVER_ERROR("COMMON_004", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_TYPE_VALUE("COMMON_005", "잘못된 타입의 값입니다.", HttpStatus.BAD_REQUEST),
    ENTITY_NOT_FOUND("COMMON_006", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    INVALID_JSON("COMMON_007", "JSON 형식이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    MISSING_REQUEST_PARAMETER("COMMON_008", "필수 요청 파라미터가 누락되었습니다.", HttpStatus.BAD_REQUEST),
    TOO_MANY_REQUESTS("COMMON_009", "너무 많은 요청입니다. 잠시 후 다시 시도해주세요.", HttpStatus.TOO_MANY_REQUESTS),

    // ========== 인증/인가 에러 (AUTH) ==========
    UNAUTHORIZED("AUTH_001", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("AUTH_002", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    INVALID_TOKEN("AUTH_003", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
    EXPIRED_TOKEN("AUTH_004", "만료된 토큰입니다.", HttpStatus.UNAUTHORIZED),
    INVALID_CREDENTIALS("AUTH_005", "아이디 또는 비밀번호가 올바르지 않습니다.", HttpStatus.UNAUTHORIZED),
    ACCOUNT_LOCKED("AUTH_006", "계정이 잠겨있습니다.", HttpStatus.FORBIDDEN),
    ACCOUNT_DISABLED("AUTH_007", "비활성화된 계정입니다.", HttpStatus.FORBIDDEN),
    EMAIL_NOT_VERIFIED("AUTH_008", "이메일 인증이 필요합니다.", HttpStatus.FORBIDDEN),

    // ========== 사용자 관련 에러 (USER) ==========
    USER_NOT_FOUND("USER_001", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_EMAIL("USER_002", "이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT),
    DUPLICATE_USERNAME("USER_003", "이미 사용 중인 사용자명입니다.", HttpStatus.CONFLICT),
    INVALID_PASSWORD("USER_004", "비밀번호 형식이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    PASSWORD_MISMATCH("USER_005", "비밀번호가 일치하지 않습니다.", HttpStatus.BAD_REQUEST),
    PROFILE_UPDATE_FAILED("USER_006", "프로필 업데이트에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // ========== 파일 관련 에러 (FILE) ==========
    FILE_NOT_FOUND("FILE_001", "파일을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    FILE_UPLOAD_FAILED("FILE_002", "파일 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_FILE_TYPE("FILE_003", "허용되지 않은 파일 형식입니다.", HttpStatus.BAD_REQUEST),
    FILE_SIZE_EXCEEDED("FILE_004", "파일 크기가 제한을 초과했습니다.", HttpStatus.BAD_REQUEST),
    FILE_DELETE_FAILED("FILE_005", "파일 삭제에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // ========== 비즈니스 로직 에러 (BUSINESS) ==========
    INVALID_BUSINESS_LOGIC("BIZ_001", "비즈니스 로직 오류가 발생했습니다.", HttpStatus.BAD_REQUEST),
    DUPLICATE_RESOURCE("BIZ_002", "이미 존재하는 리소스입니다.", HttpStatus.CONFLICT),
    RESOURCE_NOT_AVAILABLE("BIZ_003", "리소스를 사용할 수 없습니다.", HttpStatus.CONFLICT),
    INVALID_STATUS_TRANSITION("BIZ_004", "유효하지 않은 상태 변경입니다.", HttpStatus.BAD_REQUEST),
    QUOTA_EXCEEDED("BIZ_005", "할당량을 초과했습니다.", HttpStatus.TOO_MANY_REQUESTS),


    // ========== 외부 API 관련 에러 (EXTERNAL) ==========
    EXTERNAL_API_ERROR("EXT_001", "외부 API 호출 중 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY),
    EXTERNAL_API_TIMEOUT("EXT_002", "외부 API 응답 시간이 초과되었습니다.", HttpStatus.GATEWAY_TIMEOUT),
    EXTERNAL_API_UNAVAILABLE("EXT_003", "외부 API를 사용할 수 없습니다.", HttpStatus.SERVICE_UNAVAILABLE);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

}
