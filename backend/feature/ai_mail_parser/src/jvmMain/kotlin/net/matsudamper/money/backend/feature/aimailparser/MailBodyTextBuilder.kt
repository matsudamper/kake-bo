package net.matsudamper.money.backend.feature.aimailparser

import org.jsoup.Jsoup

/**
 * AIに渡すメール本文を組み立てる。
 * plainは送信元が整形したテキストなので優先する。
 * HTMLしか無いメールや、plainに「HTMLで閲覧してください」程度しか書かれていないメールもあるため、
 * 残りの文字数の範囲でHTMLから抽出したテキストも付ける。
 */
internal object MailBodyTextBuilder {
    private const val LINE_BREAK_MARKER = "[[LINE_BREAK]]"

    /**
     * 巨大な本文をそのまま正規化やDOM展開するとメモリと時間を大きく消費するため、処理前に切り詰める長さ。
     * 空白やタグを除くと本文は大きく縮むので、送信上限よりも十分大きくしておく。
     */
    private const val MAX_SOURCE_LENGTH = 1_000_000

    fun build(
        input: AiMailParseInput,
        maxBodyLength: Int,
    ): String {
        val plainText = input.plain?.let { normalizeWhitespace(it.take(MAX_SOURCE_LENGTH)) }.orEmpty()
        val truncatedPlain = plainText.take(maxBodyLength)
        val remainingLength = maxBodyLength - truncatedPlain.length
        val htmlText = if (remainingLength > 0) {
            input.html?.let { htmlToText(it.take(MAX_SOURCE_LENGTH)) }.orEmpty()
        } else {
            ""
        }
        val shouldIncludeHtml = htmlText.isNotBlank() && htmlText != plainText

        return buildString {
            appendLine("件名: ${input.subject}")
            appendLine("差出人: ${input.from}")
            appendLine("受信日時: ${input.dateTime}")
            if (truncatedPlain.isNotBlank()) {
                appendLine()
                appendLine("## テキスト本文")
                appendLine(truncatedPlain)
            }
            if (shouldIncludeHtml) {
                appendLine()
                appendLine("## HTML本文から抽出したテキスト")
                appendLine(htmlText.take(remainingLength))
            }
        }
    }

    private fun htmlToText(html: String): String {
        val document = Jsoup.parse(html)
        document.select("script, style, head").remove()
        // text()は改行を含む空白をすべて1つにまとめるため、改行位置に目印を入れてから復元する
        document.select("br").before(LINE_BREAK_MARKER)
        document.select("p, div, tr, li, table, h1, h2, h3, h4, h5, h6").after(LINE_BREAK_MARKER)
        document.select("td, th").after(" ")
        return normalizeWhitespace(document.text().replace(LINE_BREAK_MARKER, "\n"))
    }

    private fun normalizeWhitespace(text: String): String {
        return text.lines()
            .map { line -> line.replace(Regex("[\\t\\u00A0 　]+"), " ").trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n")
    }
}
