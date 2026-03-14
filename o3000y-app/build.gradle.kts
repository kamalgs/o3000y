plugins {
    id("o3000y.java-conventions")
    application
    alias(libs.plugins.shadow)
}

description = "Application assembly — Guice wiring, Main entry point"

application {
    mainClass.set("dev.o3000y.app.Main")
}

tasks.jar {
    archiveClassifier.set("thin")
}

tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
}

dependencies {
    implementation(project(":o3000y-model"))
    implementation(project(":o3000y-ingestion-api"))
    implementation(project(":o3000y-ingestion-core"))
    implementation(project(":o3000y-ingestion-grpc"))
    implementation(project(":o3000y-storage-api"))
    implementation(project(":o3000y-storage-ducklake"))
    implementation(project(":o3000y-query-engine"))
    implementation(project(":o3000y-query-rest"))
    implementation(libs.guice)
    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)
}
