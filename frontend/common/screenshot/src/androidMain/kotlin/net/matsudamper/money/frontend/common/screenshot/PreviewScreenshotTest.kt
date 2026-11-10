package net.matsudamper.money.frontend.common.screenshot

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.PreviewContextConfigurationEffect
import org.jetbrains.compose.resources.setResourceReaderAndroidContext
import org.junit.Rule
import org.junit.Test
import sergio.sastre.composable.preview.scanner.android.AndroidComposablePreviewScanner
import sergio.sastre.composable.preview.scanner.android.AndroidPreviewInfo
import sergio.sastre.composable.preview.scanner.core.preview.ComposablePreview
import sergio.sastre.composable.preview.scanner.core.scanner.config.classpath.Classpath

/**
 * 継承したクラスを各モジュールの androidHostTest に ScreenshotTest という名前で置き、
 * Parameterized の引数に [scanPreviews] の結果を渡す。
 */
public abstract class PreviewScreenshotTest(
    private val preview: ComposablePreview<AndroidPreviewInfo>,
) {
    @get:Rule
    public val paparazzi: Paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6,
    )

    @OptIn(ExperimentalResourceApi::class)
    @Test
    public fun snapshot() {
        setResourceReaderAndroidContext(paparazzi.context)
        paparazzi.snapshot(name = preview.methodName) {
            PreviewContextConfigurationEffect()
            preview()
        }
    }

    public companion object {
        public fun scanPreviews(packageName: String): List<ComposablePreview<AndroidPreviewInfo>> {
            return AndroidComposablePreviewScanner()
                .setTargetSourceSet(
                    // Android KMP Library Pluginにはvariantが無く、コンパイル結果がbuild/tmp/kotlin-classes/debugに出ない
                    sourceSetClasspath = Classpath(packagePath = "classes/kotlin/android/main"),
                )
                .scanPackageTrees(packageName)
                .includePrivatePreviews()
                .getPreviews()
        }
    }
}
