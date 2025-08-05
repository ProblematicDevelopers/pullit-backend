# GitHub Actions 설정 가이드

## 필요한 GitHub Secrets

Repository Settings > Secrets and variables > Actions에서 설정:

### 1. EC2_HOST
- EC2 인스턴스의 Public IP
- 예: `3.35.47.6`

### 2. EC2_USERNAME
- EC2 접속 유저명
- 예: `ubuntu`

### 3. EC2_SSH_KEY
- EC2 인스턴스 접속용 Private Key
- .pem 파일의 전체 내용을 복사
- 예:
```
-----BEGIN RSA PRIVATE KEY-----
MIIEpAIBAAKCAQEA...
...
-----END RSA PRIVATE KEY-----
```

## 브랜치 전략

```
feature/* → develop (PR 필수)
    ↓
develop → main (PR 필수, 2명 승인)
    ↓
main → 자동 배포
```

## 워크플로우

1. **feature 브랜치에서 개발**
   ```bash
   git checkout -b feature/add-login
   git push origin feature/add-login
   ```

2. **develop으로 PR 생성**
   - CI 테스트 통과 필수
   - 코드 리뷰 후 머지

3. **develop → main PR**
   - 2명 이상 승인 필요
   - 모든 테스트 통과 필수

4. **main 머지 시 자동 배포**
   - Docker 이미지 빌드
   - EC2로 전송 및 배포