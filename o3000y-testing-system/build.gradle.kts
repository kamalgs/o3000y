plugins {
    id("o3000y.java-conventions")
}

description = "End-to-end system tests"

dependencies {
    testImplementation(project(":o3000y-model"))
    testImplementation(project(":o3000y-app"))
    testImplementation(project(":o3000y-testing-fixtures"))
    testImplementation(project(":o3000y-ingestion-grpc"))
    testImplementation(project(":o3000y-query-engine"))
    testImplementation(project(":o3000y-query-rest"))
    testImplementation(libs.guice)
    testImplementation(libs.grpc.netty.shaded)
    testImplementation(libs.grpc.protobuf)
    testImplementation(libs.grpc.stub)
    testImplementation(libs.opentelemetry.proto)
    testImplementation(libs.protobuf.java)
    testImplementation(libs.jackson.databind)
    testImplementation(libs.duckdb.jdbc)
    testImplementation(libs.arrow.memory.netty)
    testImplementation(libs.logback.classic)
}
