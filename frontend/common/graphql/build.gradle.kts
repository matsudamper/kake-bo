plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(project(":frontend:common:base"))
                api(project(":frontend:common:graphql:schema"))

                api(libs.apollo.runtime)
                implementation(libs.kotlin.datetime)
                api(libs.apollo.normalizedCache)
                implementation(libs.apollo.adapters)
            }
        }
    }
}
