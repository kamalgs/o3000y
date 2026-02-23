FROM gradle:8.12-jdk21 AS builder
WORKDIR /app

# 1) Copy only build config files — this layer caches dependency downloads
COPY settings.gradle.kts build.gradle.kts gradle/libs.versions.toml ./
COPY buildSrc/ buildSrc/
COPY o3000y-model/build.gradle.kts o3000y-model/build.gradle.kts
COPY o3000y-ingestion-api/build.gradle.kts o3000y-ingestion-api/build.gradle.kts
COPY o3000y-ingestion-core/build.gradle.kts o3000y-ingestion-core/build.gradle.kts
COPY o3000y-ingestion-grpc/build.gradle.kts o3000y-ingestion-grpc/build.gradle.kts
COPY o3000y-storage-api/build.gradle.kts o3000y-storage-api/build.gradle.kts
COPY o3000y-storage-parquet/build.gradle.kts o3000y-storage-parquet/build.gradle.kts
COPY o3000y-storage-local/build.gradle.kts o3000y-storage-local/build.gradle.kts
COPY o3000y-query-engine/build.gradle.kts o3000y-query-engine/build.gradle.kts
COPY o3000y-query-rest/build.gradle.kts o3000y-query-rest/build.gradle.kts
COPY o3000y-testing-fixtures/build.gradle.kts o3000y-testing-fixtures/build.gradle.kts
COPY o3000y-testing-integration/build.gradle.kts o3000y-testing-integration/build.gradle.kts
COPY o3000y-testing-system/build.gradle.kts o3000y-testing-system/build.gradle.kts
COPY o3000y-app/build.gradle.kts o3000y-app/build.gradle.kts

# 2) Download all dependencies (cached unless build files change)
RUN gradle dependencies --no-daemon || true

# 3) Copy source and build (only this layer re-runs on code changes)
COPY . .
RUN gradle :o3000y-app:shadowJar --no-daemon -x test -x spotlessCheck

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/o3000y-app/build/libs/o3000y-app-*.jar /app/o3000y.jar
RUN mkdir -p /data
EXPOSE 4317 8080
ENV O3000Y_DATA_PATH=/data
ENTRYPOINT ["java", "-jar", "/app/o3000y.jar"]
