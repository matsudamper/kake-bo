package net.matsudamper.money.backend.mail

import java.lang.IllegalStateException
import java.util.Properties
import jakarta.mail.Authenticator
import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMultipart
import org.eclipse.angus.mail.imap.IMAPMessage

class MailRepository(
    private val host: String,
    private val port: Int,
    private val userName: String,
    private val password: String,
) {
    fun getMailCount(): Int {
        val session = getSession()

        val store = session.getStore("imap").also {
            it.connect()
        }
        return store.use { imap4 ->
            imap4.getFolder("INBOX").messageCount
        }
    }

    fun getMail(): List<MailResult> {
        val session = getSession()

        val store = session.getStore("imap").also {
            it.connect()
        }
        return store.use { imap4 ->
            imap4.getFolder("INBOX").let { folder ->
                folder.open(Folder.READ_ONLY)
                folder.use {
                    folder.messages
                        .map { it as IMAPMessage }
                        .map { message ->
                            val contents = when (val content = message.dataHandler.content) {
                                is String,
                                is MimeMultipart,
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

                            MailResult(
                                subject = message.subject,
                                messageID = message.messageID,
                                content = contents,
                                flags = message.flags,
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
            }
        }
    }

    fun deleteMessage(deleteMessageID: String): Boolean {
        val session = getSession()

        val store = session.getStore("imap").also {
            it.connect()
        }
        store.use { imap4 ->
            val folder = imap4.getFolder("INBOX").also { folder ->
                folder.open(Folder.READ_WRITE)
            }
            val deleteMessage = folder.messages
                .map { it as IMAPMessage }
                .firstOrNull { it.messageID == deleteMessageID }
                ?: return false

            deleteMessage.setFlag(Flags.Flag.DELETED, true)
            return true
        }
    }

    private fun getSession(): Session {
        return Session.getDefaultInstance(
            Properties().also {
                it.setProperty("mail.imap.ssl.enable", "true");
                it.setProperty("mail.imap.host", host);
                it.setProperty("mail.imap.port", port.toString());
            },
            object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(userName, password)
                }
            },
        )
    }

    data class MailResult(
        val subject: String,
        val messageID: String,
        val content: List<Content>,
        val flags: Flags,
        val sender: String?,
        val from: List<String>,
        val forwardedFor: List<String>,
        val forwardedTo: List<String>,
    ) {
        sealed interface Content {
            data class Text(val text: String) : Content
            data class Html(val html: String) : Content
            data class Other(val contentType: String) : Content
        }
    }
}
