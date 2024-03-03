package net.matsudamper.money.backend.mail

import java.util.Properties
import jakarta.mail.Authenticator
import jakarta.mail.Flags
import jakarta.mail.Folder
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import net.matsudamper.money.backend.app.interfaces.MailRepository
import net.matsudamper.money.backend.base.element.MailResult
import net.matsudamper.money.backend.base.mailparser.MailParser
import net.matsudamper.money.element.MailId
import org.eclipse.angus.mail.imap.IMAPMessage

class MailMailRepository(
    private val host: String,
    private val port: Int,
    private val userName: String,
    private val password: String,
) : MailRepository {
    override fun getMailCount(): Int {
        val session = getSession()

        val store = session.getStore("imap").also {
            it.connect()
        }
        return store.use { imap4 ->
            imap4.getFolder("INBOX").messageCount
        }
    }

    override fun getMails(
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
                    val messageCount = folder.messageCount
                    if (messageCount <= 0) return emptyList()

                    folder.getMessages(offset + 1, (offset + size).coerceAtMost(messageCount))
                        .map { it as IMAPMessage }
                        .map { message ->
                            MailParser.messageToResponse(message = message)
                        }
                }
            }
        }
    }

    override fun getMails(
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
                        yield(MailParser.messageToResponse(message = item))
                    }
                }
            }
        }
    }

    override fun deleteMessage(deleteMessageIDs: List<MailId>) {
        val session = getSession()

        val store = session.getStore("imap").also {
            it.connect()
        }
        store.use { imap4 ->
            val inbox = imap4.getFolder("INBOX").also { folder ->
                folder.open(Folder.READ_WRITE)
            }
            val trashFolder = imap4.getFolder("Trash").also { folder ->
                folder.open(Folder.READ_WRITE)
            }
            for (mail in getMailSequence(folder = inbox, mailIds = deleteMessageIDs)) {
                trashFolder.appendMessages(arrayOf(mail))
                mail.setFlag(Flags.Flag.DELETED, true)
            }
            trashFolder.expunge()
            trashFolder.close(false)
            inbox.expunge()
            inbox.close(false)
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
                it.setProperty("mail.imap.ssl.enable", "true")
                it.setProperty("mail.imap.host", host)
                it.setProperty("mail.imap.port", port.toString())
            },
            object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(userName, password)
                }
            },
        )
    }
}
