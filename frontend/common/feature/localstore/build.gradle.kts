import com.google.protobuf.gradle.proto

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.protobuf)
    id("net.matsudamper.money.buildlogic.androidLibrary")
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    androidTarget()
    sourceSets {
        jvmToolchain(libs.versions.javaToolchain.get().toInt())
        val commonMain by getting {
            dependencies {
            }
        }
        val jsMain by getting {
            dependencies {
            }
        }
        val androidMain by getting {
            dependencies {
                api(libs.androidxDatastoreDatastore)
                api(libs.protobufProtobufJavalite)
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
    explicitApi()
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

android {
    namespace = "net.matsudamper.money.frontend.common.feature.localstore"
    buildFeatures {
        buildConfig = true
    }
    sourceSets["main"].proto {
        srcDir("src/androidMain/proto")
    }
}

protobuf {
    protoc {
        artifact = libs.protobufProtoc.get().toString()
    }

    generateProtoTasks {
        all().configureEach {
            builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}
