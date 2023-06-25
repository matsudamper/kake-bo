plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm {
        withJava()
    }
    sourceSets {
        jvmToolchain(17)
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(kotlin("reflect"))

                implementation(libs.kotlin.serialization.json)
                implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
                implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
                implementation(libs.log4j.api)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

//tasks.test {
//    useJUnitPlatform()
//}