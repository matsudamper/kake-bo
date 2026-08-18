plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    id("com.android.kotlin.multiplatform.library")
}

val protocConfiguration: Configuration by configurations.creating

val isWindows = System.getProperty("os.name").lowercase().contains("windows")
val isMac = System.getProperty("os.name").lowercase().contains("mac")
val osClassifier = when {
    isWindows -> "windows-x86_64"
    isMac -> if (System.getProperty("os.arch") == "aarch64") "osx-aarch_64" else "osx-x86_64"
    else -> "linux-x86_64"
}

dependencies {
    protocConfiguration("com.google.protobuf:protoc:${libs.versions.protoBuf.get()}:$osClassifier@exe")
}

val generatedProtoDir = layout.buildDirectory.dir("generated/source/proto/androidMain/java")

val generateProto = tasks.register("generateProto", Exec::class) {
    val protoFile = file("src/androidMain/proto/session.proto")
    val outDir = generatedProtoDir.get().asFile

    inputs.file(protoFile)
    outputs.dir(outDir)

    doFirst {
        outDir.mkdirs()
        val protocExe = protocConfiguration.singleFile
        protocExe.setExecutable(true)
        executable(protocExe.absolutePath)
        args(
            "--java_out=lite:${outDir.absolutePath}",
            "-I${protoFile.parentFile.absolutePath}",
            protoFile.absolutePath,
        )
    }
}

kotlin {
    androidLibrary {
        namespace = "net.matsudamper.money.frontend.common.feature.localstore"
        compileSdk = 36
        minSdk = 34
        withJava()
    }
    js(IR) {
        browser()
    }
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
            kotlin.srcDir(generatedProtoDir)
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
    explicitApi()
}

tasks.matching { (it.name.contains("compile", ignoreCase = true) || it.name.contains("ktlint", ignoreCase = true)) && it.name.contains("Android", ignoreCase = true) }.configureEach {
    dependsOn(generateProto)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
