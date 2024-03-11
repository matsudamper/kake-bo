package net.matsudamper.money.frontend.common.ui.layout

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import net.matsudamper.money.frontend.common.ui.lib.applyHtml

@OptIn(ExperimentalTextApi::class)
@Composable
internal fun UrlClickableText(
    text: String,
    onClickUrl: (String) -> Unit,
    onLongClickUrl: (String) -> Unit,
) {
    val color = MaterialTheme.colorScheme.primary
    val annotatedText =
        remember(text) {
            AnnotatedString(text)
                .applyHtml(color)
        }
    var layoutResult: TextLayoutResult? by remember { mutableStateOf(null) }
    Text(
        modifier =
        Modifier.pointerInput(onClickUrl, onLongClickUrl) {
            detectTapGestures(
                onLongPress = { offset ->
                    val index = layoutResult?.getOffsetForPosition(offset) ?: return@detectTapGestures
                    annotatedText.getUrlAnnotations(index, index).forEach {
                        onLongClickUrl(it.item.url)
                    }
                },
                onTap = { offset ->
                    val index = layoutResult?.getOffsetForPosition(offset) ?: return@detectTapGestures
                    annotatedText.getUrlAnnotations(index, index).forEach {
                        onClickUrl(it.item.url)
                    }
                },
            )
        },
        text = annotatedText,
        style =
        LocalTextStyle.current.merge(
            SpanStyle(
                color = LocalContentColor.current,
            ),
        ),
        onTextLayout = {
            layoutResult = it
        },
    )
}
