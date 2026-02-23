plugins {
    id("o3000y.java-conventions")
}

description = "Ingestion core — SpanBuffer batching implementation"

dependencies {
    api(project(":o3000y-ingestion-api"))
    api(project(":o3000y-storage-api"))
    implementation(libs.slf4j.api)
    testImplementation(libs.logback.classic)
}
