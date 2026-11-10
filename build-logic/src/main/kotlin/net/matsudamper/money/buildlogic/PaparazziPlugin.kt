package net.matsudamper.money.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Preview を Paparazzi で撮影する。撮影用のテストは各モジュールの androidHostTest に置く ScreenshotTest で、
 * frontend:common:screenshot の PreviewScreenshotTest を継承する。
 */
@Suppress("unused")
class PaparazziPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("app.cash.paparazzi")

            extensions.configure<KotlinMultiplatformExtension> {
                targets.withType<KotlinMultiplatformAndroidLibraryTarget>().configureEach {
                    // Paparazzi が R クラスを参照するため
                    androidResources {
                        enable = true
                    }
                    withHostTest {
                        isIncludeAndroidResources = true
                    }
                }
                sourceSets.matching { it.name == "androidHostTest" }.configureEach {
                    dependencies {
                        implementation(project(SCREENSHOT_PROJECT_PATH))
                    }
                }
            }

            val byteBuddyAgent = configurations.create("byteBuddyAgent")
            dependencies.add(byteBuddyAgent.name, libs.library("byteBuddyAgent"))

            val paparazziTaskRequested = gradle.startParameter.taskNames.any { requestedTask ->
                requestedTask.contains("paparazzi", ignoreCase = true)
            }
            tasks.withType<Test>().configureEach {
                // Preview が無いモジュールや、撮影用のテストしか無いモジュールでも失敗させない
                filter.isFailOnNoMatchingTests = false
                if (paparazziTaskRequested || name.contains("paparazzi", ignoreCase = true)) {
                    filter.includeTestsMatching(SCREENSHOT_TEST_CLASS_PATTERN)
                    // 回避策: Gradle 9.3.1 では Paparazzi のHTMLレポーターが
                    // NoSuchMethodError(TestResultsProvider.hasOutput) で落ちるため、
                    // Paparazzi実行時のみ Gradle のテストHTMLレポート生成を無効化する。
                    // https://github.com/cashapp/paparazzi/issues/2111
                    reports.html.required.set(false)
                } else {
                    // 撮影は重く、通常のテストでは不要なため除外する
                    filter.excludeTestsMatching(SCREENSHOT_TEST_CLASS_PATTERN)
                }
                jvmArgs(
                    "-javaagent:${byteBuddyAgent.asPath}",
                    "-Djdk.attach.allowAttachSelf=true",
                )
            }
        }
    }

    private companion object {
        private const val SCREENSHOT_PROJECT_PATH = ":frontend:common:screenshot"
        private const val SCREENSHOT_TEST_CLASS_PATTERN = "*.ScreenshotTest"
    }
}
