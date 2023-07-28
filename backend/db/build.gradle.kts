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
                api(project(":shared"))
                api(project(":backend:base"))
                api(project(":backend:db:schema"))

                api(kotlin("stdlib"))
                api(kotlin("reflect"))
                api(libs.kotlin.serialization.json)

                val jooqVersion = "3.18.5"
                api("org.jooq:jooq:$jooqVersion")
                api("org.jooq:jooq-kotlin:$jooqVersion")

                api("org.mariadb.jdbc:mariadb-java-client:3.1.4")
                api("com.zaxxer:HikariCP:5.0.1")
            }
        }
        val jvmTest by getting {
            dependencies {
                api(kotlin("test"))
            }
        }
    }
}
