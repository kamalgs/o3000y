plugins {
    id("o3000y.java-conventions")
    application
}

description = "Application assembly — Guice wiring, Main entry point"

application {
    mainClass.set("dev.o3000y.app.Main")
}

dependencies {
    implementation(project(":o3000y-model"))
    implementation(project(":o3000y-ingestion-api"))
    implementation(project(":o3000y-ingestion-core"))
    implementation(project(":o3000y-ingestion-grpc"))
    implementation(project(":o3000y-storage-api"))
    implementation(project(":o3000y-storage-parquet"))
    implementation(project(":o3000y-storage-local"))
    implementation(project(":o3000y-query-engine"))
    implementation(project(":o3000y-query-rest"))
    implementation(libs.guice)
    implementation(libs.arrow.memory.netty)
    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)
}
