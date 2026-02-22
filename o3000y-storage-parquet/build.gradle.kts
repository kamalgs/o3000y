plugins {
    id("o3000y.java-conventions")
}

description = "Parquet serialization — Arrow schema, SpanToArrowConverter, ParquetSpanWriter"

dependencies {
    api(project(":o3000y-model"))
    implementation(libs.arrow.vector)
    implementation(libs.arrow.memory.netty)
    implementation(libs.arrow.c.data)
    implementation(libs.parquet.arrow)
    implementation(libs.parquet.common)
    implementation(libs.hadoop.common) {
        // Exclude unnecessary transitive dependencies
        exclude(group = "org.apache.hadoop", module = "hadoop-auth")
        exclude(group = "org.apache.curator")
        exclude(group = "org.apache.zookeeper")
        exclude(group = "org.apache.kerby")
        exclude(group = "com.sun.jersey")
    }
    implementation(libs.slf4j.api)

    testImplementation(libs.duckdb.jdbc)
    testImplementation(libs.logback.classic)
}
