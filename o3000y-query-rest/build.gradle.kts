plugins {
    id("o3000y.java-conventions")
}

description = "REST API for query service"

dependencies {
    api(project(":o3000y-query-engine"))
    implementation(libs.guice)
    implementation(libs.javalin)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.slf4j.api)

    testImplementation(libs.logback.classic)
}
