plugins {
    id("o3000y.java-conventions")
    application
}

description = "Load generator — sends synthetic OTLP traces via gRPC"

application {
    mainClass.set("dev.o3000y.loadgen.LoadGenMain")
}

dependencies {
    implementation(project(":o3000y-model"))
    implementation(project(":o3000y-testing-fixtures"))
    implementation(libs.grpc.netty.shaded)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.stub)
    implementation(libs.opentelemetry.proto)
    implementation(libs.protobuf.java)
    implementation(libs.slf4j.api)
    runtimeOnly(libs.logback.classic)
}
