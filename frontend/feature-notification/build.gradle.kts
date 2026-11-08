import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    id("net.matsudamper.money.buildlogic.multiplatform.library")
    id("net.matsudamper.money.buildlogic.compose")
    alias(libs.plugins.paparazzi)
}

kotlin {
    android {
        namespace = "net.matsudamper.money.frontend.feature.notification"
        // Paparazzi が R クラスを参照するため、Android リソースを有効にする
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    sourceSets {
        getByName("commonMain") {
            dependencies {
                implementation(projects.shared)
                implementation(projects.frontend.common.base)
                implementation(projects.frontend.common.navigation)
                implementation(projects.frontend.common.ui)
                implementation(projects.frontend.common.viewmodel)
                implementation(projects.frontend.common.graphql)

                implementation(libs.apolloRuntime)
                implementation(libs.composeFoundation)
                implementation(libs.composeMaterial3)
                implementation(libs.composeRuntime)
                implementation(libs.composeUiToolingPreview)
                implementation(libs.composeComponentsResources)
                implementation(libs.kotlin.datetime)
                implementation(libs.kotlin.serialization.json)
            }
        }
        getByName("androidHostTest") {
            dependencies {
                implementation(libs.paparazzi)
                implementation(libs.composablePreviewScanner)
            }
        }
    }
    explicitApi()
}

val byteBuddyAgent: Configuration = configurations.create("byteBuddyAgent")

dependencies {
    byteBuddyAgent("net.bytebuddy:byte-buddy-agent:1.18.17-jdk5")
}

val paparazziTaskRequested = gradle.startParameter.taskNames.any { requestedTask ->
    requestedTask.contains("paparazzi", ignoreCase = true)
}

tasks.withType<Test>().configureEach {
    useJUnit {
        if (paparazziTaskRequested || name.contains("paparazzi", ignoreCase = true)) {
            includeCategories("net.matsudamper.money.frontend.feature.notification.screenshot.PaparazziTestCategory")
            // 回避策: Gradle 9.3.1 では Paparazzi のHTMLレポーターが
            // NoSuchMethodError(TestResultsProvider.hasOutput) で落ちるため、
            // Paparazzi実行時のみ Gradle のテストHTMLレポート生成を無効化する。
            // https://github.com/cashapp/paparazzi/issues/2111
            reports.html.required.set(false)
        } else {
            excludeCategories("net.matsudamper.money.frontend.feature.notification.screenshot.PaparazziTestCategory")
        }
    }
    jvmArgs(
        "-javaagent:${byteBuddyAgent.asPath}",
        "-Djdk.attach.allowAttachSelf=true",
    )
}
