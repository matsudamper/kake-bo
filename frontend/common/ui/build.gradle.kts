import org.jetbrains.compose.resources.ResourcesExtension

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("net.matsudamper.money.buildlogic.multiplatform.library")
    id("net.matsudamper.money.buildlogic.compose")
    alias(libs.plugins.paparazzi)
}

compose.resources {
    publicResClass = true
    packageOfResClass = "net.matsudamper.money.frontend.common.ui.generated.resources"
    generateResClass = ResourcesExtension.ResourceClassGeneration.Always
}

kotlin {
    android {
        namespace = "net.matsudamper.money.frontend.common.ui"
        // Android KMP Library Pluginはandroidリソースが既定で無効で、無効だとCompose Resourcesがassetsに含まれない
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
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
                implementation(projects.frontend.common.navigation)
                implementation(libs.kotlin.datetime)
                implementation(libs.kotlin.serialization.json)
                implementation(libs.coilCompose)
                implementation(libs.composeRuntime)
                implementation(libs.composeFoundation)
                implementation(libs.composeMaterial3)
                implementation(libs.composeUiToolingPreview)
                implementation(libs.composeComponentsResources)
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(libs.coilNetworkKtor3)
                implementation(libs.ktorClientJs)
                implementation(libs.ktorClientLogging)
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(projects.frontend.common.base)
                implementation(projects.frontend.common.feature.localstore)
                implementation(projects.frontend.common.graphql)
                implementation(libs.kotlin.datetime)
                implementation(libs.composeRuntime)
                implementation(libs.composeFoundation)
                implementation(libs.composeMaterial3)

                implementation(libs.androidActivityActivityCompose)
                implementation(libs.coilNetworkOkhttp)
                implementation(libs.zoomable)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidHostTest by getting {
            dependencies {
                implementation(libs.paparazzi)
                implementation(libs.composablePreviewScanner)
            }
        }
    }
    explicitApi()
}

val byteBuddyAgent: Configuration by configurations.creating

dependencies {
    byteBuddyAgent("net.bytebuddy:byte-buddy-agent:1.18.13")
}

val paparazziTaskRequested = gradle.startParameter.taskNames.any { requestedTask ->
    requestedTask.contains("paparazzi", ignoreCase = true)
}

tasks.withType<Test>().configureEach {
    useJUnit {
        if (paparazziTaskRequested || name.contains("paparazzi", ignoreCase = true)) {
            includeCategories("net.matsudamper.money.frontend.common.ui.screenshot.PaparazziTestCategory")
            // 回避策: Gradle 9.3.1 では Paparazzi のHTMLレポーターが
            // NoSuchMethodError(TestResultsProvider.hasOutput) で落ちるため、
            // Paparazzi実行時のみ Gradle のテストHTMLレポート生成を無効化する。
            // https://github.com/cashapp/paparazzi/issues/2111
            reports.html.required.set(false)
        } else {
            excludeCategories("net.matsudamper.money.frontend.common.ui.screenshot.PaparazziTestCategory")
        }
    }
    jvmArgs(
        "-javaagent:${byteBuddyAgent.asPath}",
        "-Djdk.attach.allowAttachSelf=true",
    )
}
