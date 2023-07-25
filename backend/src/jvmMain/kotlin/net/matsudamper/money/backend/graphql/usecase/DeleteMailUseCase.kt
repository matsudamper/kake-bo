package net.matsudamper.money.backend.graphql.usecase

import kotlinx.coroutines.runBlocking
import net.matsudamper.money.backend.di.RepositoryFactory
import net.matsudamper.money.backend.element.UserId
import net.matsudamper.money.element.MailId

class DeleteMailUseCase(
    private val repositoryFactory: RepositoryFactory,
) {
    fun delete(
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

                mailRepository.deleteMessage(mailIds)
            }.fold(
                onSuccess = { result ->
                    Result.Success
                },
                onFailure = {
                    it.printStackTrace()
                    Result.Exception(it)
                },
            )
        }
    }

    sealed interface Result {
        object ImapConfigNotFound : Result
        object Failure : Result
        class Exception(val e: Throwable) : Result
        object Success : Result
    }
}