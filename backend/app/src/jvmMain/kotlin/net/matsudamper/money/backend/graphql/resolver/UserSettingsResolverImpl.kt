package net.matsudamper.money.backend.graphql.resolver

import java.util.Base64
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.base.ServerEnv
import net.matsudamper.money.backend.fido.AuthenticatorConverter
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.backend.lib.ChallengeModel
import net.matsudamper.money.graphql.model.QlFidoAddInfo
import net.matsudamper.money.graphql.model.QlRegisteredFidoInfo
import net.matsudamper.money.graphql.model.QlUserImapConfig
import net.matsudamper.money.graphql.model.QlUserSettings
import net.matsudamper.money.graphql.model.UserSettingsResolver

class UserSettingsResolverImpl : UserSettingsResolver {
    override fun imapConfig(
        userSettings: QlUserSettings,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlUserImapConfig?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()

        return CompletableFuture.supplyAsync {
            val result = context.diContainer.createUserConfigRepository().getImapConfig(userId) ?: return@supplyAsync null

            QlUserImapConfig(
                host = result.host,
                port = result.port,
                hasPassword = result.password.isNullOrBlank().not(),
                userName = result.userName,
            )
        }.toDataFetcher()
    }

    override fun fidoAddInfo(
        userSettings: QlUserSettings,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlFidoAddInfo>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()
        val userNameFuture = context.dataLoaders.userNameDataLoader.get(env)
            .load(userId)
        val challengeRepository = context.diContainer.createChallengeRepository()
        return CompletableFuture.allOf(userNameFuture).thenApplyAsync {
            QlFidoAddInfo(
                id = userId.value.toString(),
                name = userNameFuture.get(),
                challenge = ChallengeModel(challengeRepository).generateChallenge(),
                domain = ServerEnv.domain!!,
            )
        }.toDataFetcher()
    }

    override fun registeredFidoList(
        userSettings: QlUserSettings,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<List<QlRegisteredFidoInfo>>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val userId = context.verifyUserSessionAndGetUserId()
        val fidoRepository = context.diContainer.createFidoRepository()

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
}
