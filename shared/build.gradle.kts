plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    js(IR) {
        browser()
        nodejs()
    }
    jvm {
    }
    sourceSets {
        jvmToolchain(17)
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(libs.kotlin.serialization.json)
            }
        }
    }
}
