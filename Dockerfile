# syntax=docker/dockerfile:1.6

# =========================
# Build ステージ
# =========================
FROM eclipse-temurin:21-jdk AS backend-build

WORKDIR /workspace

# Maven 依存定義
COPY pom.xml ./
COPY mvnw ./mvnw
COPY .mvn ./.mvn
RUN chmod +x mvnw

# Maven 依存キャッシュ
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp dependency:go-offline || true

# ---- アプリケーションソース ----
COPY . .
RUN chmod +x mvnw

# ---- ビルド ----
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -ntp clean package -DskipTests

# =========================
# Runtime ステージ
# =========================
FROM eclipse-temurin:21-jre-alpine AS runtime

# JVM / タイムゾーン設定
ENV TZ=Asia/Tokyo \
    APP_HOME=/app \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 \
-XX:+ExitOnOutOfMemoryError \
-Djava.security.egd=file:/dev/./urandom"

WORKDIR ${APP_HOME}

# Healthcheck 用
RUN apk add --no-cache curl

# jar のみコピー
COPY --from=backend-build /workspace/target/*.jar app.jar

EXPOSE 8080

# =========================
# Health Check
# =========================
# Spring Boot Actuator を前提
HEALTHCHECK --interval=30s --timeout=3s --start-period=30s --retries=3 \
  CMD curl -fs http://localhost:8080/actuator/health \
  | grep '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
