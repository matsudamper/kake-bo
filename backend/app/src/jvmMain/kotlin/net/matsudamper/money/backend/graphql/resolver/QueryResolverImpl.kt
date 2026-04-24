package net.matsudamper.money.backend.graphql.resolver

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.app.interfaces.AdminImageRepository
import net.matsudamper.money.backend.app.interfaces.UserSessionRepository
import net.matsudamper.money.backend.base.ServerEnv
import net.matsudamper.money.backend.feature.image.ImageApiPath
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.GraphqlMoneyException
import net.matsudamper.money.backend.graphql.otelSupplyAsync
import net.matsudamper.money.backend.graphql.toDataFetcher
import net.matsudamper.money.backend.lib.ChallengeModel
import net.matsudamper.money.graphql.model.QlAdminUnlinkedImage
import net.matsudamper.money.graphql.model.QlAdminUnlinkedImagesConnection
import net.matsudamper.money.graphql.model.QlAdminUnlinkedImagesInput
import net.matsudamper.money.graphql.model.QlFidoLoginInfo
import net.matsudamper.money.graphql.model.QlImportedMailAttributes
import net.matsudamper.money.graphql.model.QlUser
import net.matsudamper.money.graphql.model.QlUserMailAttributes
import net.matsudamper.money.graphql.model.QueryResolver

class QueryResolverImpl : QueryResolver {
    override fun adminUnlinkedImages(
        input: QlAdminUnlinkedImagesInput,
        env: DataFetchingEnvironment,
    ): CompletionStage<DataFetcherResult<QlAdminUnlinkedImagesConnection>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        context.verifyAdminSession()
        val domain = ServerEnv.domain
            ?: throw IllegalStateException("DOMAIN is not configured")

        return otelSupplyAsync {
            val size = input.size.coerceAtMost(MAX_ADMIN_UNLINKED_IMAGE_SIZE)
            if (size == 0) {
                QlAdminUnlinkedImagesConnection(
                    nodes = listOf(),
                    cursor = null,
                    hasMore = false,
                )
            } else {
                val result = context.diContainer.createAdminImageRepository().getUnlinkedImages(
                    size = size,
                    cursor = input.cursor?.let(AdminUnlinkedImagesCursor::fromString)?.let {
                        AdminImageRepository.Cursor(imageId = it.imageId)
                    },
                )
                QlAdminUnlinkedImagesConnection(
                    nodes = result.items.map { item ->
                        QlAdminUnlinkedImage(
                            id = item.imageId,
                            url = ImageApiPath.adminImageV1AbsoluteByDisplayId(
                                domain = domain,
                                displayId = item.displayId,
                            ),
                            userId = item.userId,
                            userName = item.userName,
                        )
                    },
                    cursor = result.cursor?.let { cursor ->
                        AdminUnlinkedImagesCursor(imageId = cursor.imageId).toCursorString()
                    },
                    hasMore = result.cursor != null,
                )
            }
        }.toDataFetcher()
    }

    override fun user(env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<QlUser?>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        context.verifyUserSessionAndGetUserId()
        return CompletableFuture.completedFuture(
            QlUser(
                userMailAttributes = QlUserMailAttributes(),
                importedMailAttributes = QlImportedMailAttributes(),
            ),
        ).toDataFetcher()
    }

    override fun isLoggedIn(env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<Boolean>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val info = context.getSessionInfo()
        return otelSupplyAsync {
            when (info) {
                is UserSessionRepository.VerifySessionResult.Failure -> false
                is UserSessionRepository.VerifySessionResult.Success -> true
            }
        }.toDataFetcher()
    }

    override fun isAdminLoggedIn(env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<Boolean>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        return otelSupplyAsync {
            try {
                context.verifyAdminSession()
                true
            } catch (_: GraphqlMoneyException.SessionNotVerify) {
                false
            }
        }.toDataFetcher()
    }

    override fun fidoLoginInfo(env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<QlFidoLoginInfo>> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        val challengeRepository = context.diContainer.createChallengeRepository()
        return CompletableFuture.completedFuture(
            QlFidoLoginInfo(
                challenge = ChallengeModel(challengeRepository).generateChallenge(),
                domain = ServerEnv.domain!!,
            ),
        ).toDataFetcher()
    }

    private companion object {
        private const val MAX_ADMIN_UNLINKED_IMAGE_SIZE = 100
    }
}
