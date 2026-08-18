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

// Android KMP Library Pluginはvariantを持たないためBuildConfigを生成しない。公式はBuildKonfig等での代替を案内しているが、
// 必要なのは定数2つだけなのでプラグインを増やさずKotlinソースを生成している。
val generateBuildConfig = tasks.register("generateBuildConfig") {
    val serverProtocol = localProperties["net.matsudamper.money.android.serverProtocol"] as? String ?: "https"
    val serverHost = System.getenv("ANDROID_SERVER_HOST")
        ?: localProperties["net.matsudamper.money.android.serverHost"] as? String
        ?: ""

    val outputDir = layout.buildDirectory.dir("generated/source/buildConfig/androidMain")

    inputs.property("serverProtocol", serverProtocol)
    inputs.property("serverHost", serverHost)
    outputs.dir(outputDir)

    doLast {
        val outputFile = outputDir.get().file("net/matsudamper/money/frontend/graphql/BuildConfig.kt").asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            """
            |package net.matsudamper.money.frontend.graphql
            |
            |public object BuildConfig {
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
        compileSdk = 36
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
            kotlin.srcDir(generateBuildConfig)
        }
    }
}
