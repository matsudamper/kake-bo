package net.matsudamper.money.backend.graphql.usecase

import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import net.matsudamper.money.backend.app.interfaces.ImportedMailRepository
import net.matsudamper.money.backend.base.TraceLogger
import net.matsudamper.money.backend.base.element.MailResult
import net.matsudamper.money.backend.di.DiContainer
import net.matsudamper.money.element.MailId
import net.matsudamper.money.element.UserId

class ImportMailUseCase(
    private val repositoryFactory: DiContainer,
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
                    val html = mail.content.filterIsInstance<MailResult.Content.Html>()
                    val text = mail.content.filterIsInstance<MailResult.Content.Text>()

                    val result = dbMailRepository.addMail(
                        userId = userId,
                        subject = mail.subject,
                        plainText = text.firstOrNull()?.text,
                        html = html.firstOrNull()?.html,
                        dateTime = LocalDateTime.ofInstant(mail.sendDate, ZoneOffset.UTC),
                        from = mail.from.firstOrNull().orEmpty(),
                    )

                    when (result) {
                        is ImportedMailRepository.AddUserResult.Failed -> {
                            when (val error = result.error) {
                                is ImportedMailRepository.AddUserResult.ErrorType.InternalServerError -> {
                                    throw error.e
                                }
                            }
                        }
                        ImportedMailRepository.AddUserResult.Success -> {
                            async { mailRepository.deleteMessage(listOf(mail.messageID)) }
                        }
                    }
                }
            }.fold(
                onSuccess = {
                    val allDeleteSuccess = runBlocking {
                        it.toList().all {
                            runCatching { it.await() }.getOrNull() != null
                        }
                    }
                    if (allDeleteSuccess) {
                        Result.Success
                    } else {
                        Result.Failure(IllegalStateException("delete failed"))
                    }
                },
                onFailure = {
                    TraceLogger.impl().noticeThrowable(it, mapOf(), true)
                    Result.Failure(it)
                },
            )
        }
    }

    sealed interface Result {
        data object ImapConfigNotFound : Result

        data class Failure(val e: Throwable) : Result

        data object Success : Result
    }
}
