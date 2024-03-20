package net.matsudamper.money.backend.base.mailparser

import java.time.Instant
import jakarta.mail.Address
import jakarta.mail.Multipart
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import net.matsudamper.money.backend.base.element.MailResult
import net.matsudamper.money.element.MailId

public object MailParser {
    public fun rawContentToResponse(rawContent: String): MailResult {
        val message = MimeMessage(Session.getDefaultInstance(System.getProperties()), rawContent.byteInputStream())
        return messageToResponse(message)
    }

    public fun messageToResponse(message: MimeMessage): MailResult {
        val contents =
            when (val content = message.dataHandler.content) {
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
            sender = run sender@{
                val sender: Address = message.sender ?: return@sender null

                (sender as InternetAddress).address
            },
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
