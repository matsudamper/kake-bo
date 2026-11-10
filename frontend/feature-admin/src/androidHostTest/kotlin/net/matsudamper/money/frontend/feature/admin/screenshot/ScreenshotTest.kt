package net.matsudamper.money.frontend.feature.admin.screenshot

import net.matsudamper.money.frontend.common.screenshot.PreviewScreenshotTest
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview

@RunWith(Parameterized::class)
class ScreenshotTest(
    preview: ComposablePreview<AndroidPreviewInfo>,
) : PreviewScreenshotTest(preview) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun previews(): List<ComposablePreview<AndroidPreviewInfo>> = scanPreviews(packageName = "net.matsudamper.money.frontend.feature.admin")
    }
}
