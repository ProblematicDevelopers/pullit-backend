FROM openjdk:17-jdk-slim AS builder

WORKDIR /app

# Gradle wrapper 복사
COPY gradlew .
COPY gradle gradle

# 의존성 캐싱을 위해 build.gradle 먼저 복사
COPY build.gradle .
COPY settings.gradle .

# 실행 권한 부여
RUN chmod +x ./gradlew

# 의존성 다운로드
RUN ./gradlew dependencies --no-daemon

# 소스 코드 복사
COPY src src

# 애플리케이션 빌드
RUN ./gradlew build -x test --no-daemon

# 런타임 이미지
FROM openjdk:17-jdk-slim

WORKDIR /app

# 빌드된 JAR 파일만 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 헬스체크
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "app.jar"]