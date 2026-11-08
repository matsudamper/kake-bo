package net.matsudamper.money.backend.graphql.resolver

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asPublisher
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.backend.graphql.GraphQlContext
import net.matsudamper.money.backend.graphql.usecase.AdminThumbnailBackfillUseCase
import net.matsudamper.money.graphql.model.QlAdminThumbnailBackfillEvent
import net.matsudamper.money.graphql.model.QlAdminThumbnailBackfillResult
import net.matsudamper.money.graphql.model.SubscriptionResolver
import org.reactivestreams.Publisher

class SubscriptionResolverImpl : SubscriptionResolver {
    override fun adminThumbnailBackfill(env: DataFetchingEnvironment): Publisher<QlAdminThumbnailBackfillEvent> {
        val context = env.graphQlContext.get<GraphQlContext>(GraphQlContext::class.java.name)
        context.verifyAdminSession()
        val imageResizerGateway = context.diContainer.createImageResizerGateway()
            ?: throw IllegalStateException("画像の変換サービスが設定されていません")

        return AdminThumbnailBackfillUseCase(
            adminImageRepository = context.diContainer.createAdminImageRepository(),
            imageResizerGateway = imageResizerGateway,
        ).run()
            .map { event ->
                when (event) {
                    is AdminThumbnailBackfillUseCase.Event.Success -> QlAdminThumbnailBackfillEvent(
                        imageId = event.imageId,
                        result = QlAdminThumbnailBackfillResult.SUCCESS,
                        message = null,
                    )

                    is AdminThumbnailBackfillUseCase.Event.Failed -> QlAdminThumbnailBackfillEvent(
                        imageId = event.imageId,
                        result = QlAdminThumbnailBackfillResult.FAILED,
                        message = event.message,
                    )
                }
            }
            .asPublisher()
    }
}
