package net.matsudamper.money.backend.graphql.resolver

import java.time.OffsetDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.backend.mail.MailRepository
import net.matsudamper.money.backend.repository.UserConfigRepository
import net.matsudamper.money.element.MailId
import net.matsudamper.money.graphql.model.QlMailQuery
import net.matsudamper.money.graphql.model.QlUser
import net.matsudamper.money.graphql.model.QlUserMail
import net.matsudamper.money.graphql.model.QlUserMailConnection
import net.matsudamper.money.graphql.model.QlUserMailError
import net.matsudamper.money.graphql.model.QlUserSettings
import net.matsudamper.money.graphql.model.UserResolver

class UserResolverImpl : UserResolver {
    override fun settings(user: QlUser, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<QlUserSettings>> {
        return CompletableFuture.completedFuture(QlUserSettings()).toDataFetcher()
    }

    override fun mail(user: QlUser, mailQuery: QlMailQuery, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<QlUserMailConnection>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()

        return CompletableFuture.supplyAsync {
            fun createError(error: QlUserMailError): QlUserMailConnection {
                return QlUserMailConnection(
                    error = error,
                    usrMails = listOf(),
                    cursor = null,
                )
            }

            val imapConfig = UserConfigRepository().getImapConfig(userId)
                ?: return@supplyAsync createError(QlUserMailError.InternalServerError)

            val host = imapConfig.host ?: return@supplyAsync createError(QlUserMailError.MailConfigNotFound)
            val port = imapConfig.port ?: return@supplyAsync createError(QlUserMailError.MailConfigNotFound)
            val userName = imapConfig.userName ?: return@supplyAsync createError(QlUserMailError.MailConfigNotFound)
            val password = imapConfig.password ?: return@supplyAsync createError(QlUserMailError.MailConfigNotFound)

            val mails = MailRepository(
                host = host,
                port = port,
                userName = userName,
                password = password,
            ).getMail()

            QlUserMailConnection(
                error = null,
                usrMails = mails.mapIndexed { index, mail ->
                    val html = mail.content.filterIsInstance<MailRepository.MailResult.Content.Html>()
                    val text = mail.content.filterIsInstance<MailRepository.MailResult.Content.Text>()

                    // TODO
                    // mail.forwardedForの先頭を見て、許可されているメールだけを取り込むようにする
                    QlUserMail(
                        id = MailId(index.toLong()), // TODO
                        plain = if (text.size > 1) {
                            text.joinToString("\n=====\n") { it.text }
                        } else {
                            text.getOrNull(0)?.text
                        }.toString(),
                        html = if (html.size > 1){
                            html.joinToString("\n=====\n") { it.html }
                        } else {
                            html.getOrNull(0)?.html
                        }.toString(),
                        time = OffsetDateTime.now(),
                        subject = mail.subject,
                        sender = mail.sender,
                        from = mail.from,
                    )
                },
                cursor = null,
            )
        }.toDataFetcher()
    }
}
