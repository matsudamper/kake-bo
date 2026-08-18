import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("com.android.kotlin.multiplatform.library")
}

val localProperties = Properties().also { properties ->
    val propertiesFile = File("$rootDir/local.properties")
    if (propertiesFile.exists()) {
        properties.load(propertiesFile.inputStream())
    }
}

val generatedBuildConfigDir = layout.buildDirectory.dir("generated/source/buildConfig/androidMain")

val generateBuildConfig = tasks.register("generateBuildConfig") {
    val serverProtocol = localProperties["net.matsudamper.money.android.serverProtocol"] as? String ?: "https"
    val serverHost = System.getenv("ANDROID_SERVER_HOST")
        ?: localProperties["net.matsudamper.money.android.serverHost"] as? String
        ?: ""

    val outputFile = generatedBuildConfigDir.get().file("net/matsudamper/money/frontend/graphql/BuildConfig.kt").asFile

    inputs.property("serverProtocol", serverProtocol)
    inputs.property("serverHost", serverHost)
    outputs.file(outputFile)

    doLast {
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            """
            |package net.matsudamper.money.frontend.graphql
            |
            |public object BuildConfig {
            |    public const val DEBUG: Boolean = false
            |    public const val SERVER_PROTOCOL: String = "$serverProtocol"
            |    public const val SERVER_HOST: String = "$serverHost"
            |}
            |
            """.trimMargin(),
        )
    }
}

kotlin {
    androidLibrary {
        namespace = "net.matsudamper.money.frontend.graphql"
        compileSdk = 37
        minSdk = 34
    }
    js(IR) {
        browser()
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
                implementation(libs.apolloAdaptersCore)
            }
        }
        val androidMain by getting {
            kotlin.srcDir(generatedBuildConfigDir)
        }
    }
}

tasks.matching { (it.name.contains("compile", ignoreCase = true) || it.name.contains("ktlint", ignoreCase = true)) && it.name.contains("Android", ignoreCase = true) }.configureEach {
    dependsOn(generateBuildConfig)
}
