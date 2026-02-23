plugins {
    id("o3000y.java-conventions")
}

description = "gRPC OTLP ingestion transport"

dependencies {
    api(project(":o3000y-ingestion-api"))
    implementation(project(":o3000y-ingestion-core"))
    implementation(libs.guice)
    implementation(libs.grpc.netty.shaded)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.stub)
    implementation(libs.grpc.services)
    implementation(libs.opentelemetry.proto)
    implementation(libs.protobuf.java)
    implementation(libs.slf4j.api)
    compileOnly(libs.javax.annotation.api)

    testImplementation(libs.grpc.testing)
    testImplementation(libs.grpc.inprocess)
    testImplementation(libs.logback.classic)
}
