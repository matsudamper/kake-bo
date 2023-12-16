package net.matsudamper.money.backend.graphql.resolver.setting

import java.util.concurrent.CompletionStage
import graphql.execution.DataFetcherResult
import graphql.schema.DataFetchingEnvironment
import net.matsudamper.money.graphql.model.FidoInfoResolver
import net.matsudamper.money.graphql.model.QlFidoInfo

class FidoInfoResolverImpl: FidoInfoResolver {
    override fun challenge(fidoInfo: QlFidoInfo, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<String>> {
        TODO("Not yet implemented")
    }

    override fun domain(fidoInfo: QlFidoInfo, env: DataFetchingEnvironment): CompletionStage<DataFetcherResult<String>> {
        TODO("Not yet implemented")
    }
}