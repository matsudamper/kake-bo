plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm {
        withJava()
    }
    jvmToolchain(17)
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(project(":backend:base"))
                implementation(project(":backend:db:schema"))

                implementation(kotlin("stdlib"))
                implementation(kotlin("reflect"))
                implementation(libs.kotlin.serialization.json)

                val jooqVersion = "3.18.5"
                implementation("org.jooq:jooq:$jooqVersion")
                implementation("org.jooq:jooq-kotlin:$jooqVersion")

                implementation("org.mariadb.jdbc:mariadb-java-client:3.3.2")
                implementation("com.zaxxer:HikariCP:5.1.0")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
