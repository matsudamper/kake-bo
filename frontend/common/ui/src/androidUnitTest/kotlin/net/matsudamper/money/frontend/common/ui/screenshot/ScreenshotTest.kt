package net.matsudamper.money.frontend.common.ui.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import sergio.sastre.composable.preview.scanner.common.CommonComposablePreviewScanner
import sergio.sastre.composable.preview.scanner.common.CommonPreviewInfo
import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview

@RunWith(Parameterized::class)
@Category(PaparazziTestCategory::class)
class ScreenshotTest(
    private val preview: ComposablePreview<CommonPreviewInfo>,
) {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6,
    )

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun previews(): List<ComposablePreview<CommonPreviewInfo>> =
            CommonComposablePreviewScanner()
                .scanPackageTrees("net.matsudamper.money.frontend.common.ui")
                .getPreviews()
    }

    @Test
    fun snapshot() {
        paparazzi.snapshot(name = preview.methodName) {
            preview()
        }
    }
}
