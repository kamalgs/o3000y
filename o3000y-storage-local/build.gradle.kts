plugins {
    id("o3000y.java-conventions")
}

description = "Local filesystem storage writer"

dependencies {
    api(project(":o3000y-storage-api"))
    implementation(project(":o3000y-storage-parquet"))
    implementation(libs.guice)
    implementation(libs.slf4j.api)

    testImplementation(libs.duckdb.jdbc)
    testImplementation(libs.logback.classic)
}
