package net.matsudamper.money.backend.mail

import java.time.Instant
import java.util.Properties
import jakarta.mail.Authenticator
import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMultipart
import net.matsudamper.money.element.MailId
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

    fun getMails(
        size: Int,
        offset: Int,
    ): List<MailResult> {
        val session = getSession()

        val store = session.getStore("imap").also {
            it.connect()
        }
        return store.use { imap4 ->
            imap4.getFolder("INBOX").let { folder ->
                folder.open(Folder.READ_ONLY)
                folder.use {
                    folder.getMessages(offset + 1, (offset + size).coerceAtMost(folder.messageCount))
                        .map { it as IMAPMessage }
                        .map { message ->
                            imapMessageToResponse(message = message)
                        }
                }
            }
        }
    }

    fun getMails(
        mailIds: List<MailId>,
    ): Sequence<MailResult> = sequence {
        val session = getSession()

        val store = session.getStore("imap").also {
            it.connect()
        }
        store.use { imap4 ->
            imap4.getFolder("INBOX").let { folder ->
                folder.open(Folder.READ_ONLY)
                folder.use {
                    for (item in getMailSequence(folder = it, mailIds)) {
                        yield(imapMessageToResponse(message = item))
                    }
                }
            }
        }
    }

    fun deleteMessage(deleteMessageIDs: List<MailId>) {
        val session = getSession()

        val store = session.getStore("imap").also {
            it.connect()
        }
        store.use { imap4 ->
            val folder = imap4.getFolder("INBOX").also { folder ->
                folder.open(Folder.READ_WRITE)
            }
            getMailSequence(folder = folder, mailIds = deleteMessageIDs)
                .onEach { it.setFlag(Flags.Flag.DELETED, true) }
            folder.expunge()
            folder.close(false)
        }
    }

    private fun getMailSequence(folder: Folder, mailIds: List<MailId>): Sequence<IMAPMessage> {
        val reamingRawMailIds = mailIds.map { it.id }.toMutableList()
        return sequence {
            for (item in folder.messages.asSequence().map { it as IMAPMessage }) {
                if (item.messageID !in reamingRawMailIds) {
                    continue
                } else {
                    yield(item)
                    reamingRawMailIds.remove(item.messageID)
                }

                if (reamingRawMailIds.isEmpty()) break
            }
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

    private fun imapMessageToResponse(message: IMAPMessage): MailResult {
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

        return MailResult(
            subject = message.subject,
            messageID = MailId(message.messageID),
            content = contents,
            flags = message.flags,
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

    data class MailResult(
        val subject: String,
        val messageID: MailId,
        val content: List<Content>,
        val flags: Flags,
        val sender: String?,
        val from: List<String>,
        val forwardedFor: List<String>,
        val forwardedTo: List<String>,
        val sendDate: Instant,
    ) {
        sealed interface Content {
            data class Text(val text: String) : Content
            data class Html(val html: String) : Content
            data class Other(val contentType: String) : Content
        }
    }
}
