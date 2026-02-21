# ---- Build Stage ----
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

# Gradle wrapper + 설정 먼저 복사 (의존성 캐시 레이어)
COPY gradlew settings.gradle build.gradle gradle.properties ./
COPY gradle ./gradle
# 의존성 사전 다운로드 (캐시 레이어 목적 — 일부 의존성은 소스 없이 resolve 불가하므로 실패 허용)
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# 소스 복사 및 빌드
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test \
    && cp $(ls build/libs/*.jar | grep -v plain | head -1) build/libs/app.jar

# ---- Runtime Stage ----
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app

COPY --from=build /app/build/libs/app.jar app.jar
RUN apk add --no-cache curl \
    && addgroup -S appgroup && adduser -S appuser -G appgroup \
    && mkdir -p /app/logs && chown appuser:appgroup /app/logs
USER appuser

EXPOSE 8080 8081

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]