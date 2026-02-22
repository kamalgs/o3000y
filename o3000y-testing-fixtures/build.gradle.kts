plugins {
    id("o3000y.java-conventions")
}

description = "Shared test fixtures — SpanFixtures, ProtoFixtures, ParquetTestHelper"

dependencies {
    api(project(":o3000y-model"))
    implementation(project(":o3000y-storage-parquet"))
    implementation(libs.opentelemetry.proto)
    implementation(libs.protobuf.java)
    implementation(libs.arrow.vector)
    implementation(libs.arrow.memory.netty)
}
