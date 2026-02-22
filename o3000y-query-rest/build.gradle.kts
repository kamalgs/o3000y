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

    testImplementation(project(":o3000y-storage-parquet"))
    testImplementation(project(":o3000y-testing-fixtures"))
    testImplementation(libs.hadoop.common)
    testImplementation(libs.logback.classic)
}
