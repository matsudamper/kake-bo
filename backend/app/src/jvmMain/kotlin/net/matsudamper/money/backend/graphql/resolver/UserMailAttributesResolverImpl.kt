package net.matsudamper.money.backend.graphql.resolver

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Base64
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.base.element.MailResult
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.graphql.model.GraphQlInputField
import net.matsudamper.money.graphql.model.QlMailQuery
import net.matsudamper.money.graphql.model.QlUserMail
import net.matsudamper.money.graphql.model.QlUserMailAttributes
import net.matsudamper.money.graphql.model.QlUserMailConnection
import net.matsudamper.money.graphql.model.QlUserMailError
import net.matsudamper.money.graphql.model.UserMailAttributesResolver

class UserMailAttributesResolverImpl : UserMailAttributesResolver {
    override fun mails(
        userMailAttributes: QlUserMailAttributes,
        mailQuery: QlMailQuery,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlUserMailConnection>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()
        val userConfigRepository = context.diContainer.createUserConfigRepository()

        return CompletableFuture.supplyAsync {
            fun createError(error: QlUserMailError): QlUserMailConnection {
                return QlUserMailConnection(
                    error = error,
                    usrMails = listOf(),
                    cursor = null,
                )
            }

            val imapConfig = userConfigRepository.getImapConfig(userId)
                ?: return@supplyAsync createError(QlUserMailError.InternalServerError)

            val host = imapConfig.host ?: return@supplyAsync createError(QlUserMailError.MailConfigNotFound)
            val port = imapConfig.port ?: return@supplyAsync createError(QlUserMailError.MailConfigNotFound)
            val userName = imapConfig.userName ?: return@supplyAsync createError(QlUserMailError.MailConfigNotFound)
            val password = imapConfig.password ?: return@supplyAsync createError(QlUserMailError.MailConfigNotFound)
            val cursor = mailQuery.cursor.getOrNull()?.let {
                UserMailQueryCursor.fromString(it) ?: throw IllegalStateException("cursor parse failed: $it")
            }

            val mails = context.diContainer.createMailRepository(
                host = host,
                port = port,
                userName = userName,
                password = password,
            ).getMails(
                size = mailQuery.size,
                offset = cursor?.offset ?: 0,
            )

            QlUserMailConnection(
                error = null,
                usrMails = mails.map { mail ->
                    val html = mail.content.filterIsInstance<MailResult.Content.Html>()
                    val text = mail.content.filterIsInstance<MailResult.Content.Text>()

                    // TODO: mail.forwardedForの先頭を見て、許可されているメールだけを取り込むようにする
                    QlUserMail(
                        id = mail.messageID,
                        plain = text.getOrNull(0)?.text,
                        html = html.getOrNull(0)?.html,
                        time = OffsetDateTime.ofInstant(mail.sendDate, ZoneOffset.UTC),
                        subject = mail.subject,
                        sender = mail.sender,
                        from = mail.from,
                    )
                },
                cursor = if (mails.isEmpty()) {
                    null
                } else {
                    UserMailQueryCursor(mails.size + (cursor?.offset ?: 0)).createCursorString()
                },
            )
        }.toDataFetcher()
    }

    override fun mailCount(
        userMailAttributes: QlUserMailAttributes,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<Int?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()

        return CompletableFuture.supplyAsync {
            val imapConfig = context.diContainer.createUserConfigRepository().getImapConfig(userId) ?: return@supplyAsync null

            val host = imapConfig.host ?: return@supplyAsync 0
            val port = imapConfig.port ?: return@supplyAsync 0
            val userName = imapConfig.userName ?: return@supplyAsync 0
            val password = imapConfig.password ?: return@supplyAsync 0

            return@supplyAsync context.diContainer.createMailRepository(
                host = host,
                port = port,
                userName = userName,
                password = password,
            ).getMailCount()
        }.toDataFetcher()
    }
}

private class UserMailQueryCursor(
    val offset: Int,
) {
    fun createCursorString(): String {
        return encoder.encodeToString("$OFFSET_KEY=$offset".toByteArray(Charsets.UTF_8))
    }

    companion object {
        private const val OFFSET_KEY = "offset"
        private val decoder = Base64.getDecoder()
        private val encoder = Base64.getEncoder()

        fun fromString(cursor: String): UserMailQueryCursor? {
            val keyValues = decoder.decode(cursor).toString(Charsets.UTF_8)
                .split("&")
                .map {
                    val keyValue = it.split("=")
                    keyValue.getOrNull(0) to keyValue.getOrNull(1)
                }
            val offsetValue = keyValues.firstOrNull { it.first == OFFSET_KEY }
                ?.second?.toIntOrNull() ?: return null
            return UserMailQueryCursor(
                offset = offsetValue,
            )
        }
    }
}


internal fun <T> GraphQlInputField<T>.getOrNull() : T? {
    return when(this) {
        is GraphQlInputField.Defined<T> -> value
        is GraphQlInputField.Undefined -> null
    }
}
