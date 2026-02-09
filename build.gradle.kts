plugins {
    java
    application
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

application {
    mainClass = "dev.tracequery.Main"
}

dependencies {
    implementation(fileTree("libs") { include("*.jar") })
}
