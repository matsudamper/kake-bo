package net.matsudamper.money.frontend.common.ui.lib

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.UrlAnnotation
import androidx.compose.ui.text.buildAnnotatedString

@OptIn(ExperimentalTextApi::class, ExperimentalTextApi::class)
@Suppress("RegExpRedundantEscape")
internal fun AnnotatedString.applyHtml(color: Color): AnnotatedString {
    val matches =
        Regex("""(https?:\/\/\S+)\b""")
            .findAll(this)
            .toList()

    return buildAnnotatedString {
        append(this@applyHtml)
        matches.map { match ->
            addStyle(
                SpanStyle(
                    color = color,
                ),
                match.range.first,
                match.range.last + 1,
            )
            addUrlAnnotation(UrlAnnotation(match.value), match.range.first, match.range.last + 1)
        }
    }
}
