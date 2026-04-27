import org.jetbrains.compose.resources.ResourcesExtension

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("net.matsudamper.money.buildlogic.compose")
    id("net.matsudamper.money.buildlogic.androidLibrary")
    alias(libs.plugins.paparazzi)
}

compose.resources {
    publicResClass = false
    packageOfResClass = "net.matsudamper.money.frontend.common.ui.generated.resources"
    generateResClass = ResourcesExtension.ResourceClassGeneration.Always
}

kotlin {
    js(IR) {
        browser()
    }
    androidTarget()
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
        val androidUnitTest by getting {
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
    byteBuddyAgent("net.bytebuddy:byte-buddy-agent:1.18.8-jdk5")
    debugImplementation(libs.composeUiTooling)
}

android {
    namespace = "net.matsudamper.money.frontend.common.ui"
    val paparazziTaskRequested = gradle.startParameter.taskNames.any { requestedTask ->
        requestedTask.contains("paparazzi", ignoreCase = true)
    }
    testOptions {
        unitTests.all {
            it.useJUnit {
                if (paparazziTaskRequested || it.name.contains("paparazzi", ignoreCase = true)) {
                    includeCategories("net.matsudamper.money.frontend.common.ui.screenshot.PaparazziTestCategory")
                    // 回避策: Gradle 9.3.1 では Paparazzi のHTMLレポーターが
                    // NoSuchMethodError(TestResultsProvider.hasOutput) で落ちるため、
                    // Paparazzi実行時のみ Gradle のテストHTMLレポート生成を無効化する。
                    // https://github.com/cashapp/paparazzi/issues/2111
                    it.reports.html.required.set(false)
                } else {
                    excludeCategories("net.matsudamper.money.frontend.common.ui.screenshot.PaparazziTestCategory")
                }
            }
            it.jvmArgs(
                "-javaagent:${byteBuddyAgent.asPath}",
                "-Djdk.attach.allowAttachSelf=true",
            )
        }
    }
}
