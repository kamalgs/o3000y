FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN chmod +x gradlew && ./gradlew --no-daemon installDist

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/install/tracequery/ ./
EXPOSE 7070
ENV DATA_DIR=/data/traces
ENTRYPOINT ["bin/tracequery"]
