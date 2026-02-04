import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("net.matsudamper.money.buildlogic.androidLibrary")
}

val localProperties = Properties().also { properties ->
    val propertiesFile = File("$rootDir/local.properties")
    if (propertiesFile.exists()) {
        properties.load(propertiesFile.inputStream())
    }
}

val generateBuildConfig = tasks.register("generateAndroidBuildConfig") {
    val outputDir = layout.buildDirectory.dir("generated/buildConfig/androidMain/kotlin")
    outputs.dir(outputDir)

    val serverProtocol = localProperties["net.matsudamper.money.android.serverProtocol"]?.toString().orEmpty()
    val serverHost = (System.getenv("ANDROID_SERVER_HOST") ?: localProperties["net.matsudamper.money.android.serverHost"]?.toString()).orEmpty()

    doLast {
        val dir = outputDir.get().asFile.resolve("net/matsudamper/money/frontend/graphql")
        dir.mkdirs()
        dir.resolve("GeneratedBuildConfig.kt").writeText(
            buildString {
                appendLine("package net.matsudamper.money.frontend.graphql")
                appendLine()
                appendLine("internal object GeneratedBuildConfig {")
                appendLine("    const val SERVER_PROTOCOL: String = \"$serverProtocol\"")
                appendLine("    const val SERVER_HOST: String = \"$serverHost\"")
                appendLine("}")
                appendLine()
            },
        )
    }
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    androidLibrary {
        namespace = "net.matsudamper.money.frontend.graphql"
    }
    sourceSets {
        jvmToolchain(libs.versions.javaToolchain.get().toInt())
        val commonMain by getting {
            dependencies {
                implementation(projects.shared)
                implementation(projects.frontend.common.base)
                api(projects.frontend.common.graphql.schema)

                api(libs.apolloRuntime)
                implementation(libs.kotlin.datetime)
                api(libs.apolloNormalizedCache)
                implementation(libs.apolloAdapters)
            }
        }
        val androidMain by getting {
            kotlin.srcDir(generateBuildConfig.map { it.outputs.files.singleFile })
        }
    }
}
