package net.matsudamper.money.frontend.common.viewmodel.root.settings.timezone

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.ApolloResponse
import net.matsudamper.money.frontend.graphql.GetConfigQuery
import net.matsudamper.money.frontend.graphql.SetTimezoneOffsetMutation

public class TimezoneSettingGraphqlApi(
    private val apolloClient: ApolloClient,
) {
    public suspend fun getTimezoneOffset(): ApolloResponse<GetConfigQuery.Data> {
        return apolloClient
            .query(GetConfigQuery())
            .execute()
    }

    public suspend fun setTimezoneOffset(offsetMinutes: Int): ApolloResponse<SetTimezoneOffsetMutation.Data> {
        return apolloClient
            .mutation(SetTimezoneOffsetMutation(offsetMinutes = offsetMinutes))
            .execute()
    }
}
