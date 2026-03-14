plugins {
    id("o3000y.java-conventions")
}

description = "DuckLake-based storage backend"

dependencies {
    api(project(":o3000y-storage-api"))
    api(project(":o3000y-model"))
    implementation(libs.duckdb.jdbc)
    implementation(libs.guice)
    implementation(libs.slf4j.api)
    implementation(libs.jackson.databind)

    testImplementation(project(":o3000y-testing-fixtures"))
    testImplementation(libs.logback.classic)
}
