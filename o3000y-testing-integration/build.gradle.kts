plugins {
    id("o3000y.java-conventions")
}

description = "Integration tests and architecture fitness tests"

dependencies {
    testImplementation(project(":o3000y-model"))
    testImplementation(project(":o3000y-ingestion-api"))
    testImplementation(project(":o3000y-ingestion-core"))
    testImplementation(project(":o3000y-ingestion-grpc"))
    testImplementation(project(":o3000y-storage-api"))
    testImplementation(project(":o3000y-storage-parquet"))
    testImplementation(project(":o3000y-storage-local"))
    testImplementation(project(":o3000y-query-engine"))
    testImplementation(project(":o3000y-query-rest"))
    testImplementation(project(":o3000y-testing-fixtures"))
    testImplementation(project(":o3000y-app"))
    testImplementation(libs.archunit.junit5)
    testImplementation(libs.duckdb.jdbc)
    testImplementation(libs.logback.classic)
}
