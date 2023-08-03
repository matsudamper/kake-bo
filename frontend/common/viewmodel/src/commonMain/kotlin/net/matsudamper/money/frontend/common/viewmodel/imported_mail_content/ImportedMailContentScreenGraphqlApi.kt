package net.matsudamper.money.frontend.common.viewmodel.imported_mail_content

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.apollographql.apollo3.cache.normalized.watch
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.ImportedMailContentScreenQuery
import net.matsudamper.money.lib.ResultWrapper
import net.matsudamper.money.lib.toResultWrapper

public class ImportedMailContentScreenGraphqlApi(
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {

    public fun get(
        id: ImportedMailId,
    ): Flow<ResultWrapper<ApolloResponse<ImportedMailContentScreenQuery.Data>>> {
        return flow {
            apolloClient
                .query(
                    ImportedMailContentScreenQuery(
                        id = id,
                    ),
                )
                .toFlow()
                .catch {
                    emit(ResultWrapper.failure(it))
                }.collect {
                    emit(ResultWrapper.success(it))
                }
        }
    }
}
