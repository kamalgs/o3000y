plugins {
    id("o3000y.java-conventions")
}

description = "DuckDB query engine wrapper"

dependencies {
    api(project(":o3000y-model"))
    implementation(libs.duckdb.jdbc)
    implementation(libs.slf4j.api)

    testImplementation(project(":o3000y-storage-parquet"))
    testImplementation(project(":o3000y-testing-fixtures"))
    testImplementation(libs.hadoop.common)
    testImplementation(libs.logback.classic)
}
