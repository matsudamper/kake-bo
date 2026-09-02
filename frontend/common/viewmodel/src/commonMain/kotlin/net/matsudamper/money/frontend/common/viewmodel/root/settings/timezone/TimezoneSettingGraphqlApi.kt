package net.matsudamper.money.frontend.common.viewmodel.root.settings.timezone

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import net.matsudamper.money.frontend.common.base.IO
import net.matsudamper.money.frontend.graphql.GetConfigQuery
import net.matsudamper.money.frontend.graphql.SetTimezoneOffsetMutation

public class TimezoneSettingGraphqlApi(
    private val apolloClient: ApolloClient,
) {
    public suspend fun getTimezoneOffset(): ApolloResponse<GetConfigQuery.Data> {
        return withContext(Dispatchers.IO) {
            apolloClient
                .query(GetConfigQuery())
                .execute()
        }
    }

    public suspend fun setTimezoneOffset(offsetMinutes: Int): ApolloResponse<SetTimezoneOffsetMutation.Data> {
        return withContext(Dispatchers.IO) {
            apolloClient
                .mutation(SetTimezoneOffsetMutation(offsetMinutes = offsetMinutes))
                .execute()
        }
    }
}
