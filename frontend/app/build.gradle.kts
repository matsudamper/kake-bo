import java.util.Properties

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.jetbrainsCompose)
    id("com.android.application")
    id("net.matsudamper.money.buildlogic.compose")
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
                implementation(projects.shared)
                implementation(projects.frontend.common.root)
                implementation(projects.frontend.common.base)
                implementation(projects.frontend.common.di)
                implementation(projects.frontend.common.ui)
                implementation(projects.frontend.common.viewmodel)
                implementation(projects.frontend.common.graphql)

                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.runtime)

                implementation(libs.koinCore)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(projects.frontend.common.navigation)

                implementation(kotlin("stdlib"))
                implementation(libs.kotlin.serialization.json)

                implementation(libs.androidActivityActivityCompose)
                implementation(libs.androidAppCompatAppCompat)
                implementation(libs.kotlin.datetime)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(projects.frontend.common.navigation)

                implementation(kotlin("stdlib"))
                implementation(libs.kotlin.serialization.json)
                implementation(compose.html.core)
                implementation(libs.androidxComposeSaveable)
            }
        }
    }
}

val localProperties = Properties().also { properties ->
    val propertiesFile = File("$rootDir/local.properties")
    if (propertiesFile.exists()) {
        properties.load(propertiesFile.inputStream())
    }
}
android {
    compileSdk = 36
    namespace = "net.matsudamper.money"
    signingConfigs {
        create("release") {
            storeFile = (localProperties["KEYSTORE_PATH"] as? String)?.let { file(it) } ?: return@create
            storePassword = localProperties["KEYSTORE_PASSWORD"] as? String ?: return@create
            keyAlias = localProperties["KEY_ALIAS"] as? String ?: return@create
            keyPassword = localProperties["KEY_PASSWORD"] as? String ?: return@create
        }
    }
    buildTypes {
        debug {
            val isCI = System.getenv("CI")?.toBoolean() ?: false
            if (isCI) {
                applicationIdSuffix = ".ci"
                resValue("string", "app_name", "家計簿.$applicationIdSuffix")
            } else {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
    defaultConfig {
        resValue("string", "app_name", "家計簿")
        minSdk = 34
        targetSdk = 36
        manifestPlaceholders["SERVER_HOST"] = System.getenv("ANDROID_SERVER_HOST") ?: localProperties["net.matsudamper.money.android.serverHost"] as String
    }
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.java.get().toInt())
        targetCompatibility = JavaVersion.toVersion(libs.versions.java.get().toInt())
    }
}
