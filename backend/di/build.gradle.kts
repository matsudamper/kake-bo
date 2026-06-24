plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm {
    }
    sourceSets {
        jvmToolchain(libs.versions.javaToolchain.get().toInt())
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))

                implementation(projects.backend.base)
                implementation(projects.backend.app.interfaces)
                implementation(projects.backend.datasource.db)
                implementation(projects.backend.datasource.mail)
                implementation(projects.backend.datasource.inmemory)
                implementation(projects.backend.feature.oidc)
                implementation(projects.backend.feature.objectStorage)
                implementation(projects.backend.feature.imageStorageLocal)
            }
        }
    }
}
