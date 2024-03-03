package net.matsudamper.money.backend.base.mail_parser

import java.time.Instant
import jakarta.mail.Multipart
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import net.matsudamper.money.element.MailId

public object MailParser {
    public fun messageToResponse(message: MimeMessage): MailResult {
        val contents = when (val content = message.dataHandler.content) {
            is String,
            is Multipart,
            -> {
                when (content) {
                    is String -> {
                        MultipartParser.parse(message)
                    }

                    is MimeMultipart -> {
                        MultipartParser.parseMultipart(content)
                    }

                    else -> throw IllegalStateException("")
                }.map {
                    when (it) {
                        is MultipartParser.ParseResult.Content.Html -> MailResult.Content.Html(it.html)
                        is MultipartParser.ParseResult.Content.Text -> MailResult.Content.Text(it.text)
                        is MultipartParser.ParseResult.Content.Other -> MailResult.Content.Other(it.contentType)
                    }
                }
            }

            else -> {
                listOf(MailResult.Content.Other(message.contentType.orEmpty()))
            }
        }

        return MailResult(
            subject = message.subject,
            messageID = MailId(message.messageID),
            content = contents,
            sendDate = Instant.ofEpochMilli(message.sentDate.time),
            sender = (message.sender as InternetAddress).address,
            from = message.from
                .map { it as InternetAddress }
                .mapNotNull { it.address },
            forwardedFor = message.getHeader("X-Forwarded-For")
                .orEmpty()
                .flatMap { it.split(" ") },
            forwardedTo = message.getHeader("X-Forwarded-To")
                .orEmpty()
                .flatMap { it.split(" ") },
        )
    }
}

public data class MailResult(
    val subject: String,
    val messageID: MailId,
    val content: List<Content>,
    val sender: String?,
    val from: List<String>,
    val forwardedFor: List<String>,
    val forwardedTo: List<String>,
    val sendDate: Instant,
) {
    public sealed interface Content {
        public data class Text(val text: String) : Content
        public data class Html(val html: String) : Content
        public data class Other(val contentType: String) : Content
    }
}
