package net.matsudamper.money.frontend.common.ui.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import sergio.sastre.composable.preview.scanner.android.AndroidComposablePreviewScanner
import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview
import sergio.sastre.composable.preview.scanner.core.scanner.config.classpath.Classpath

@RunWith(Parameterized::class)
@Category(PaparazziTestCategory::class)
class ScreenshotTest(
    private val preview: ComposablePreview<AndroidPreviewInfo>,
) {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6,
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun previews(): List<ComposablePreview<AndroidPreviewInfo>> =
            AndroidComposablePreviewScanner()
                .setTargetSourceSet(
                    // Android KMP Library Pluginにはvariantが無く、コンパイル結果がbuild/tmp/kotlin-classes/debugに出ない
                    sourceSetClasspath = Classpath(packagePath = "classes/kotlin/android/main"),
                )
                .scanPackageTrees("net.matsudamper.money.frontend.common.ui")
                .includePrivatePreviews()
                .getPreviews()
    }

    @OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
    @Test
    fun snapshot() {
        org.jetbrains.compose.resources.setResourceReaderAndroidContext(paparazzi.context)
        paparazzi.snapshot(name = preview.methodName) {
            org.jetbrains.compose.resources.PreviewContextConfigurationEffect()
            preview()
        }
    }
}
