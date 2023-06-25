import net.matsudamper.money.frontend.graphql.GraphqlAdminQuery
import net.matsudamper.money.frontend.common.viewmodel.IObjectMapper

object GlobalContainer {
    val graphqlClient = GraphqlAdminQuery()
    val objectMapper = object : IObjectMapper {
        override fun <T> serialize(value: T): String {
            return JSON.stringify(value)
        }
    }
}
