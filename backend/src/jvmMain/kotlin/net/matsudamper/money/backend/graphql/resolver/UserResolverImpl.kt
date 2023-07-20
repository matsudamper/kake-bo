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
import net.matsudamper.money.graphql.model.UserMailAttributesResolver
import net.matsudamper.money.graphql.model.UserResolver

class UserResolverImpl : UserResolver {
    override fun settings(user: QlUser, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<QlUserSettings>> {
        return CompletableFuture.completedFuture(QlUserSettings()).toDataFetcher()
    }
}
