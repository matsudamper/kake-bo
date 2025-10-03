plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm {
    }
    jvmToolchain(21)
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(projects.shared)
                implementation(projects.backend.app.interfaces)
                implementation(projects.backend.base)
                implementation(projects.backend.datasource.db.schema)

                implementation(kotlin("stdlib"))
                implementation(kotlin("reflect"))
                implementation(libs.kotlin.serialization.json)

                implementation(libs.jooq)
                implementation(libs.jooqKotlin)

                implementation(libs.mariadbClient)
                implementation(libs.hikariCP)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
