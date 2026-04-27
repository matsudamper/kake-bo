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
                implementation(projects.frontend.common.feature.localstore)

                implementation(libs.composeFoundation)
                implementation(libs.composeMaterial3)
                implementation(libs.composeRuntime)

                implementation(libs.koinCore)
                implementation(libs.coilRuntime)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(projects.frontend.android.featureNotificationUsage)
                implementation(projects.frontend.common.navigation)
                implementation(projects.frontend.common.feature.uploader)
                implementation(projects.frontend.common.feature.logging)
                implementation(libs.workRuntimeKtx)

                implementation(kotlin("stdlib"))
                implementation(libs.kotlin.serialization.json)

                implementation(libs.androidActivityActivityCompose)
                implementation(libs.androidAppCompatAppCompat)
                implementation(libs.kotlin.datetime)
                implementation(libs.coilNetworkOkhttp)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(projects.frontend.common.navigation)

                implementation(kotlin("stdlib"))
                implementation(libs.kotlin.serialization.json)
                implementation(libs.composeHtmlCore)
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
        val keystorePath = localProperties["KEYSTORE_PATH"] as? String
        if (keystorePath != null) {
            create("release") {
                storeFile = (localProperties["KEYSTORE_PATH"] as? String)?.let { file(it) } ?: return@create
                storePassword = localProperties["KEYSTORE_PASSWORD"] as? String ?: return@create
                keyAlias = localProperties["KEY_ALIAS"] as? String ?: return@create
                keyPassword = localProperties["KEY_PASSWORD"] as? String ?: return@create
            }
        }
    }
    val appName = "家計簿"
    buildTypes {
        debug {
            val isCI = System.getenv("CI")?.toBoolean() ?: false
            if (isCI) {
                applicationIdSuffix = ".ci"
                resValue("string", "app_name", "$appName.$applicationIdSuffix")
            } else {
                signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            }
        }
        release {
            signingConfig = signingConfigs.findByName("release")
        }
    }
    defaultConfig {
        resValue("string", "app_name", appName)
        minSdk = 34
        targetSdk = 36
        manifestPlaceholders["SERVER_HOST"] = System.getenv("ANDROID_SERVER_HOST")
            ?: localProperties["net.matsudamper.money.android.serverHost"] as? String
                    ?: ""
    }
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(libs.versions.java.get().toInt())
        targetCompatibility = JavaVersion.toVersion(libs.versions.java.get().toInt())
    }
}
