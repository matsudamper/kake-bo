package net.matsudamper.money.frontend.common.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import io.ktor.http.Url


private val LocalCustomFontsFlow = staticCompositionLocalOf {
    MutableStateFlow<Map<FontSet, Font>>(mapOf())
}

private data class FontSet(
    val fileName: String,
    val weight: FontWeight,
    val style: FontStyle,
)

private val fonts: List<FontSet> = listOf(
    FontSet("NotoSansJP-Medium.ttf", FontWeight.Medium, FontStyle.Normal),
    FontSet("NotoSansJP-Bold.ttf", FontWeight.Bold, FontStyle.Normal),
    FontSet("NotoSansJP-Regular.ttf", FontWeight.W400, FontStyle.Normal),
    FontSet("NotoSansJP-Black.ttf", FontWeight.Black, FontStyle.Normal),
    FontSet("NotoSansJP-ExtraBold.ttf", FontWeight.ExtraBold, FontStyle.Normal),
    FontSet("NotoSansJP-ExtraLight.ttf", FontWeight.ExtraLight, FontStyle.Normal),
    FontSet("NotoSansJP-Light.ttf", FontWeight.Light, FontStyle.Normal),
    FontSet("NotoSansJP-SemiBold.ttf", FontWeight.SemiBold, FontStyle.Normal),
    FontSet("NotoSansJP-Thin.ttf", FontWeight.Thin, FontStyle.Normal),
)

@Composable
public actual fun rememberCustomFontFamily(): FontFamily {
    val fontsFlow: MutableStateFlow<Map<FontSet, Font>> = LocalCustomFontsFlow.current

    LaunchedEffect(Unit) {
        fonts
            .filter { it !in fontsFlow.value.keys }
            .forEach { fontSet ->
                runCatching {
                    HttpClient(Js) {
                        install(Logging) {
                            logger = Logger.EMPTY
                            level = LogLevel.NONE
                        }
                    }.get(Url("/fonts/${fontSet.fileName}"))
                }.onFailure {
                    it.printStackTrace()
                }.onSuccess { response ->
                    val byteArray = response.readBytes()
                    fontsFlow.update {
                        it.plus(
                            fontSet to Font(
                                identity = fontSet.fileName,
                                data = byteArray,
                                weight = fontSet.weight,
                                style = fontSet.style,
                            ),
                        )
                    }
                }
            }
    }

    val fonts by fontsFlow.collectAsState()
    return remember {
        derivedStateOf {
            if (fonts.isEmpty()) {
                FontFamily.Default
            } else {
                FontFamily(fonts = fonts.values.toList())
            }
        }
    }.value
}