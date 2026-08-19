import java.util.Properties
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("com.android.kotlin.multiplatform.library")
    alias(libs.plugins.buildkonfig)
}

val localProperties = Properties().also { properties ->
    val propertiesFile = File("$rootDir/local.properties")
    if (propertiesFile.exists()) {
        properties.load(propertiesFile.inputStream())
    }
}

// Android KMP Library Pluginはvariantを持たずBuildConfigを生成しないため、公式が代替として案内しているBuildKonfigを使用する
buildkonfig {
    packageName = "net.matsudamper.money.frontend.graphql"
    defaultConfigs {
        buildConfigField(STRING, "SERVER_PROTOCOL", "https")
        buildConfigField(STRING, "SERVER_HOST", "")
    }
    targetConfigs {
        create("android") {
            buildConfigField(
                STRING,
                "SERVER_PROTOCOL",
                localProperties["net.matsudamper.money.android.serverProtocol"] as? String ?: "https",
            )
            buildConfigField(
                STRING,
                "SERVER_HOST",
                System.getenv("ANDROID_SERVER_HOST")
                    ?: localProperties["net.matsudamper.money.android.serverHost"] as? String
                    ?: "",
            )
        }
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
    }
}
