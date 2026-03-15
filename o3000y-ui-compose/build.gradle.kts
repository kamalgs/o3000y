plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

repositories {
    mavenCentral()
    google()
}

kotlin {
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "o3000y-ui.js"
            }
        }
        binaries.executable()
        compilerOptions {
            moduleName.set("o3000y-ui")
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
        }

        val wasmJsMain by getting {
            dependencies {
                implementation(libs.ktor.client.js)
            }
        }
    }
}

// Copy production build to REST API static resources
tasks.register<Copy>("deployToStatic") {
    dependsOn("wasmJsBrowserDistribution")
    from("build/dist/wasmJs/productionExecutable")
    into("../o3000y-query-rest/src/main/resources/static")
}
