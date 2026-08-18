plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    androidLibrary {
        namespace = "net.matsudamper.money.frontend.common.usecase"
        compileSdk = 36
        minSdk = 34
    }
    js(IR) {
        browser()
    }
    sourceSets {
        jvmToolchain(libs.versions.javaToolchain.get().toInt())
        val commonMain by getting {
            dependencies {
                implementation(projects.frontend.common.base)
                api(projects.frontend.common.feature.webauth)
                implementation(projects.frontend.common.ui)
                implementation(projects.frontend.common.graphql)
                implementation(projects.shared)
                implementation(libs.kotlin.datetime)

                implementation(libs.koinCore)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
    explicitApi()
}
