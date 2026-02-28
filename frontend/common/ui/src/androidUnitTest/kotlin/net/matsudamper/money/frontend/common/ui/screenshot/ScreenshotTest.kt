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
import sergio.sastre.composable.preview.scanner.core.scanner.config.classpath.SourceSet

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
                    sourceSetClasspath = Classpath(
                        sourceSet = SourceSet.MAIN,
                        variantName = "debug",
                    ),
                )
                .scanPackageTrees("net.matsudamper.money.frontend.common.ui")
                .includePrivatePreviews()
                .getPreviews()
    }

    @Test
    fun snapshot() {
        paparazzi.snapshot(name = preview.methodName) {
            preview()
        }
    }
}
