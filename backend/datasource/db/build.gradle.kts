plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm {
        withJava()
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

                val jooqVersion = "3.20.3"
                implementation("org.jooq:jooq:$jooqVersion")
                implementation("org.jooq:jooq-kotlin:$jooqVersion")

                implementation("org.mariadb.jdbc:mariadb-java-client:3.5.2")
                implementation("com.zaxxer:HikariCP:6.2.1")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
