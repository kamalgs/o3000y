plugins {
    id("o3000y.java-conventions")
}

description = "Parquet serialization — Arrow schema, SpanToArrowConverter, ParquetSpanWriter"

dependencies {
    api(project(":o3000y-model"))
    implementation(libs.parquet.hadoop)
    implementation(libs.hadoop.common)
    implementation(libs.slf4j.api)

    testImplementation(libs.duckdb.jdbc)
    testImplementation(libs.logback.classic)
}
