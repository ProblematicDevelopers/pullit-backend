package com.pullit.common.constants;

public class CryptoConstants {

    private CryptoConstants() {}

    // AES 관련
    public static final String AES_ALGORITHM = "AES";
    public static final String AES_CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    public static final int AES_KEY_SIZE = 256;                    // 256비트
    public static final int AES_IV_LENGTH = 12;                    // GCM 모드 IV 길이
    public static final int AES_TAG_LENGTH = 128;                  // GCM 인증 태그 길이
    public static final int AES_SALT_LENGTH = 16;                  // Salt 길이

    // RSA 관련
    public static final String RSA_ALGORITHM = "RSA";
    public static final String RSA_CIPHER_ALGORITHM = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
    public static final String RSA_SIGNATURE_ALGORITHM = "SHA256withRSA";
    public static final int RSA_KEY_SIZE_2048 = 2048;              // 2048비트
    public static final int RSA_KEY_SIZE_4096 = 4096;              // 4096비트
    public static final int RSA_OAEP_PADDING_OVERHEAD = 42;        // OAEP 패딩 오버헤드

    // PBKDF2 관련
    public static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    public static final int PBKDF2_ITERATIONS = 10000;             // 반복 횟수
    public static final int PBKDF2_KEY_LENGTH = 256;               // 키 길이 (비트)

    // 해시 알고리즘
    public static final String SHA256_ALGORITHM = "SHA-256";
    public static final String SHA512_ALGORITHM = "SHA-512";
    public static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    public static final String HMAC_SHA512_ALGORITHM = "HmacSHA512";

    // BCrypt 관련
    public static final int BCRYPT_STRENGTH = 12;                  // BCrypt 강도

    // PEM 형식
    public static final String PEM_PUBLIC_KEY_BEGIN = "-----BEGIN PUBLIC KEY-----";
    public static final String PEM_PUBLIC_KEY_END = "-----END PUBLIC KEY-----";
    public static final String PEM_PRIVATE_KEY_BEGIN = "-----BEGIN PRIVATE KEY-----";
    public static final String PEM_PRIVATE_KEY_END = "-----END PRIVATE KEY-----";
    public static final int PEM_LINE_LENGTH = 64;                  // PEM 줄 길이

    // 기본 설정
    public static final int DEFAULT_SALT_LENGTH = 16;              // 기본 Salt 길이
    public static final int DEFAULT_TOKEN_LENGTH = 32;             // 기본 토큰 길이
    public static final int DEFAULT_SECURE_RANDOM_LENGTH = 16;     // 기본 난수 길이

    // 에러 메시지
    public static final String ERROR_ENCRYPTION_FAILED = "암호화 처리 중 오류 발생";
    public static final String ERROR_DECRYPTION_FAILED = "복호화 처리 중 오류 발생";
    public static final String ERROR_KEY_GENERATION_FAILED = "키 생성 실패";
    public static final String ERROR_SIGNATURE_FAILED = "서명 생성 실패";
    public static final String ERROR_VERIFICATION_FAILED = "서명 검증 실패";
    public static final String ERROR_DATA_TOO_LARGE = "데이터가 너무 큽니다. 최대 %d바이트";

    // 로그 메시지
    public static final String LOG_KEY_GENERATED = "RSA 키 쌍 생성 완료 ({}비트)";
    public static final String LOG_SIGNATURE_VERIFIED = "서명 검증 결과: {}";
    public static final String LOG_ENCRYPTION_SUCCESS = "암호화 성공: {} bytes";
    public static final String LOG_DECRYPTION_SUCCESS = "복호화 성공: {} bytes";

}
