package net.matsudamper.money.backend.app.interfaces

import net.matsudamper.money.backend.base.element.MailResult
import net.matsudamper.money.element.MailId

interface MailRepository {
    fun getMailCount(): Int

    fun getMails(
        size: Int,
        offset: Int,
    ): List<MailResult>

    fun getMails(mailIds: List<MailId>): Sequence<MailResult>

    fun deleteMessage(deleteMessageIDs: List<MailId>)
}
