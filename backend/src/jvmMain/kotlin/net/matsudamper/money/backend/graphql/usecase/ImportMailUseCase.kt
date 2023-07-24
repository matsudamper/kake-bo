package net.matsudamper.money.backend.graphql.usecase

import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import net.matsudamper.money.backend.di.RepositoryFactory
import net.matsudamper.money.backend.element.UserId
import net.matsudamper.money.backend.mail.MailRepository
import net.matsudamper.money.element.MailId

class ImportMailUseCase(
    private val repositoryFactory: RepositoryFactory,
) {
    fun insertMail(
        userId: UserId,
        mailIds: List<MailId>,
    ): Result {
        return runBlocking {
            runCatching {
                val imapConfig = repositoryFactory.createUserConfigRepository().getImapConfig(userId) ?: return@runBlocking Result.ImapConfigNotFound
                val host = imapConfig.host ?: return@runBlocking Result.ImapConfigNotFound
                val port = imapConfig.port ?: return@runBlocking Result.ImapConfigNotFound
                val userName = imapConfig.userName ?: return@runBlocking Result.ImapConfigNotFound
                val password = imapConfig.password ?: return@runBlocking Result.ImapConfigNotFound
                val mailRepository = repositoryFactory.createMailRepository(
                    host = host,
                    port = port,
                    userName = userName,
                    password = password,
                )

                val dbMailRepository = repositoryFactory.createDbMailRepository()
                mailRepository.getMails(mailIds).map { mail ->
                    val html = mail.content.filterIsInstance<MailRepository.MailResult.Content.Html>()
                    val text = mail.content.filterIsInstance<MailRepository.MailResult.Content.Text>()

                    dbMailRepository.addMail(
                        userId = userId,
                        plainText = text.firstOrNull()?.text,
                        html = html.firstOrNull()?.html,
                        dateTime = LocalDateTime.ofInstant(mail.sendDate, ZoneOffset.UTC),
                    )

                    async { mailRepository.deleteMessage(listOf(mail.messageID)) }
                }
            }.fold(
                onSuccess = {
                    runBlocking {
                        it.toList().map { it.await() }
                    }
                    Result.Success
                },
                onFailure = {
                    it.printStackTrace()
                    Result.Failure(it)
                },
            )
        }
    }

    sealed interface Result {
        object ImapConfigNotFound : Result
        data class Failure(val e: Throwable) : Result
        object Success : Result
    }
}