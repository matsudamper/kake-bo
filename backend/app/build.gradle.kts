plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm {
        withJava()
    }
    sourceSets {
        jvmToolchain(17)
        val jvmMain by getting {
            dependencies {
                implementation(project(":shared"))
                implementation(project(":backend:base"))
                implementation(project(":backend:graphql"))
                implementation(project(":backend:datasource:db"))
                implementation(project(":backend:datasource:mail"))
                implementation(project(":backend:feature:mail_parser"))
                implementation(project(":backend:feature:fido"))

                implementation(kotlin("stdlib"))
                implementation(libs.graphqlJava.extendedScalars)
                implementation(libs.graphqlJava)
                implementation(libs.jackson.jsr310)
                implementation(libs.kotlin.serialization.json)
                implementation(libs.kotlin.coroutines.core)
                implementation(libs.logback.classic)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("io.kotest:kotest-runner-junit5:5.8.0")
                implementation("io.mockk:mockk:1.13.8")
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
