package net.matsudamper.money.frontend.common.viewmodel.imported_mail

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import net.matsudamper.money.element.ImportedMailId
import net.matsudamper.money.frontend.graphql.GraphqlClient
import net.matsudamper.money.frontend.graphql.ImportedMailScreenQuery
import net.matsudamper.money.lib.ResultWrapper

public class ImportedMailScreenGraphqlApi(
    private val apolloClient: ApolloClient = GraphqlClient.apolloClient,
) {

    public fun get(
        id: ImportedMailId,
    ): Flow<ResultWrapper<ApolloResponse<ImportedMailScreenQuery.Data>>> {
        return flow {
            apolloClient
                .query(
                    ImportedMailScreenQuery(
                        id = id,
                    ),
                )
                .toFlow()
                .catch {
                    emit(ResultWrapper.failure(it))
                }
                .collect {
                    emit(ResultWrapper.success(it))
                }
        }
    }
}
