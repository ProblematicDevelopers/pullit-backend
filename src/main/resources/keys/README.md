# RSA 키 생성 방법
  
개발 환경에서 사용할 RSA 키 쌍을 생성하는 방법입니다.

## 키 생성 명령어

```bash
# 1. 개인키 생성 (2048 비트)
openssl genrsa -out private-key.pem 2048

# 2. 공개키 추출
openssl rsa -in private-key.pem -pubout -out public-key.pem
```

## 주의사항

- **절대로 실제 키를 Git에 커밋하지 마세요**
- 개발용 키만 이 디렉토리에 저장하세요
- 프로덕션 환경에서는 환경변수나 별도의 보안 저장소를 사용하세요
- .gitignore에 *.pem 파일이 추가되어 있는지 확인하세요