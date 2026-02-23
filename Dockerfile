FROM gradle:8.12-jdk21 AS builder
WORKDIR /app
COPY . .
RUN gradle :o3000y-app:shadowJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache libstdc++
WORKDIR /app
COPY --from=builder /app/o3000y-app/build/libs/o3000y-app-*.jar /app/o3000y.jar
RUN mkdir -p /data
EXPOSE 4317 8080
ENV O3000Y_DATA_PATH=/data
ENTRYPOINT ["java", "-jar", "/app/o3000y.jar"]
