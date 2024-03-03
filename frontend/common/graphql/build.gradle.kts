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
                implementation(projects.shared)
                implementation(projects.frontend.common.base)
                api(projects.frontend.common.graphql.schema)

                api(libs.apollo.runtime)
                implementation(libs.kotlin.datetime)
                api(libs.apollo.normalizedCache)
                implementation(libs.apollo.adapters)
            }
        }
    }
}
