plugins {
    id("o3000y.java-conventions")
}

description = "Shared test fixtures — SpanFixtures, ProtoFixtures, DuckLakeTestHelper"

dependencies {
    api(project(":o3000y-model"))
    implementation(project(":o3000y-storage-ducklake"))
    implementation(libs.opentelemetry.proto)
    implementation(libs.protobuf.java)
}
