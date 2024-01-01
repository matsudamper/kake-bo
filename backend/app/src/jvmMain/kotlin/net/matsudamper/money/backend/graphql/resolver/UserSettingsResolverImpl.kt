package net.matsudamper.money.backend.graphql.resolver

import java.time.ZoneOffset
import java.util.Base64
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.base.ServerEnv
import net.matsudamper.money.backend.datasource.db.repository.UserConfigRepository
import net.matsudamper.money.backend.fido.AuthenticatorConverter
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.backend.lib.ChallengeModel
import net.matsudamper.money.graphql.model.QlFidoAddInfo
import net.matsudamper.money.graphql.model.QlRegisteredFidoInfo
import net.matsudamper.money.graphql.model.QlSession
import net.matsudamper.money.graphql.model.QlUserImapConfig
import net.matsudamper.money.graphql.model.QlUserSettings
import net.matsudamper.money.graphql.model.UserSettingsResolver

class UserSettingsResolverImpl : UserSettingsResolver {
    override fun imapConfig(userSettings: QlUserSettings, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<QlUserImapConfig?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()

        return CompletableFuture.supplyAsync {
            val result = UserConfigRepository().getImapConfig(userId) ?: return@supplyAsync null

            QlUserImapConfig(
                host = result.host,
                port = result.port,
                hasPassword = result.password.isNullOrBlank().not(),
                userName = result.userName,
            )
        }.toDataFetcher()
    }

    override fun fidoAddInfo(userSettings: QlUserSettings, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<QlFidoAddInfo>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()
        val userNameFuture = context.dataLoaders.userNameDataLoader.get(env)
            .load(userId)
        val challengeRepository = context.repositoryFactory.createChallengeRepository()
        return CompletableFuture.allOf(userNameFuture).thenApplyAsync {
            QlFidoAddInfo(
                id = userId.value.toString(),
                name = userNameFuture.get(),
                challenge = ChallengeModel(challengeRepository).generateChallenge(),
                domain = ServerEnv.domain!!,
            )
        }.toDataFetcher()
    }

    override fun registeredFidoList(userSettings: QlUserSettings, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<List<QlRegisteredFidoInfo>>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()
        val fidoRepository = context.repositoryFactory.createFidoRepository()

        return CompletableFuture.supplyAsync {
            fidoRepository.getFidoList(userId).map { fidoResult ->
                val authenticator = AuthenticatorConverter.convertFromBase64(
                    base64AttestationStatement = fidoResult.attestedStatement,
                    attestationStatementFormat = fidoResult.attestedStatementFormat,
                    base64AttestedCredentialData = fidoResult.attestedCredentialData,
                    counter = fidoResult.counter,
                )

                QlRegisteredFidoInfo(
                    id = fidoResult.fidoId,
                    name = fidoResult.name,
                    base64CredentialId = Base64.getEncoder().encodeToString(authenticator.credentialId),
                )
            }
        }.toDataFetcher()
    }

    override fun sessions(
        userSettings: QlUserSettings,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<List<QlSession>>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSession()
        val userSessionRepository = context.repositoryFactory.userSessionRepository()

        return CompletableFuture.supplyAsync {
            userSessionRepository.getSessions(userId).map { session ->
                QlSession(
                    name = session.name,
                    lastAccess = session.latestAccess.atOffset(ZoneOffset.UTC),
                )
            }
        }.toDataFetcher()
    }
}
