package net.matsudamper.money.buildlogic

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryTarget
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Preview を Paparazzi で撮影する。撮影用の ScreenshotTest は android の namespace 配下の Preview を探すクラスとして生成するため、
 * 各モジュールにテストを置く必要はない。
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
            }

            val generateScreenshotTest = tasks.register("generateScreenshotTest") {
                val outputDir = layout.buildDirectory.dir("generated/screenshotTest/kotlin")
                val previewPackageName = provider {
                    target.extensions.getByType(KotlinMultiplatformExtension::class.java)
                        .targets.withType<KotlinMultiplatformAndroidLibraryTarget>()
                        .single().namespace
                        ?: error("android の namespace が未設定です")
                }
                inputs.property("previewPackageName", previewPackageName)
                outputs.dir(outputDir)
                doLast {
                    val packageName = "${previewPackageName.get()}.screenshot"
                    val file = outputDir.get().file("${packageName.replace('.', '/')}/ScreenshotTest.kt").asFile
                    file.parentFile.mkdirs()
                    file.writeText(createScreenshotTestSource(packageName = packageName, previewPackageName = previewPackageName.get()))
                }
            }
            extensions.configure<KotlinMultiplatformExtension> {
                sourceSets.matching { it.name == "androidHostTest" }.configureEach {
                    kotlin.srcDir(generateScreenshotTest)
                    dependencies {
                        implementation(project(SCREENSHOT_PROJECT_PATH))
                    }
                }
            }

            val byteBuddyAgent = configurations.create("byteBuddyAgent")
            dependencies.add(byteBuddyAgent.name, libs.library("byteBuddyAgent"))

            tasks.withType<Test>().configureEach {
                // Preview が無いモジュールや、撮影用のテストしか無いモジュールでも失敗させない
                filter.isFailOnNoMatchingTests = false
                jvmArgs(
                    "-javaagent:${byteBuddyAgent.asPath}",
                    "-Djdk.attach.allowAttachSelf=true",
                )
            }
            // 撮影タスクは testAndroidHostTest を使うため、このプロジェクトの撮影タスクが実行されるかで絞り込みを切り替える。
            // 他のプロジェクトの撮影タスクだけを指定したときに、このプロジェクトの通常のテストを絞り込まないようにする
            gradle.taskGraph.whenReady {
                val paparazziTaskScheduled = allTasks.any { task ->
                    task.project == target && task.name.contains("paparazzi", ignoreCase = true)
                }
                tasks.withType<Test>().forEach { testTask ->
                    if (paparazziTaskScheduled) {
                        testTask.filter.includeTestsMatching(SCREENSHOT_TEST_CLASS_PATTERN)
                        // 回避策: Gradle 9.3.1 では Paparazzi のHTMLレポーターが
                        // NoSuchMethodError(TestResultsProvider.hasOutput) で落ちるため、
                        // Paparazzi実行時のみ Gradle のテストHTMLレポート生成を無効化する。
                        // https://github.com/cashapp/paparazzi/issues/2111
                        testTask.reports.html.required.set(false)
                    } else {
                        // 撮影は重く、通常のテストでは不要なため除外する
                        testTask.filter.excludeTestsMatching(SCREENSHOT_TEST_CLASS_PATTERN)
                    }
                }
            }
        }
    }

    private fun createScreenshotTestSource(
        packageName: String,
        previewPackageName: String,
    ): String {
        return """
            |package $packageName
            |
            |import net.matsudamper.money.frontend.common.screenshot.PreviewScreenshotTest
            |import org.junit.runner.RunWith
            |import org.junit.runners.Parameterized
            |import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
            |import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview
            |
            |@RunWith(Parameterized::class)
            |class ScreenshotTest(
            |    preview: ComposablePreview<AndroidPreviewInfo>,
            |) : PreviewScreenshotTest(preview) {
            |    companion object {
            |        @JvmStatic
            |        @Parameterized.Parameters(name = "{0}")
            |        fun previews(): List<ComposablePreview<AndroidPreviewInfo>> = scanPreviews(packageName = "$previewPackageName")
            |    }
            |}
            |
        """.trimMargin()
    }

    private companion object {
        private const val SCREENSHOT_PROJECT_PATH = ":frontend:common:screenshot"
        private const val SCREENSHOT_TEST_CLASS_PATTERN = "*.ScreenshotTest"
    }
}
