package net.matsudamper.money.backend.mail

import jakarta.mail.BodyPart
import jakarta.mail.internet.MimeMultipart

internal object MultipartParser {
    fun parseMultipart(multipart: MimeMultipart): List<ParseResult.Content> {
        return (0 until multipart.count).map { index ->
            multipart.getBodyPart(index)
        }.map { bodyPart ->
            parse(bodyPart)
        }.flatten()
    }

    private fun parse(bodyPart: BodyPart): List<ParseResult.Content> {
        return when {
            bodyPart.isMimeType("text/plain") -> {
                listOf(ParseResult.Content.Text(bodyPart.content.toString()))
            }

            bodyPart.isMimeType("text/html") -> {
                listOf(ParseResult.Content.Html(bodyPart.content.toString()))
            }

            bodyPart.content is MimeMultipart -> {
                parseMultipart(bodyPart.content as MimeMultipart)
            }

            else -> {
                listOf(ParseResult.Content.Other(bodyPart.contentType.orEmpty()))
            }
        }
    }

    data class ParseResult(
        val contents: List<Content>,
    ) {
        sealed interface Content {
            data class Text(val text: String) : Content
            data class Html(val html: String) : Content
            data class Other(val contentType: String) : Content
        }
    }
}
