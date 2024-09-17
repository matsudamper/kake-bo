package net.matsudamper.money.ui.root

import kotlin.reflect.KClass
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import net.matsudamper.money.frontend.common.viewmodel.IObjectMapper
import net.matsudamper.money.frontend.graphql.GraphqlAdminQuery

object GlobalContainer {
    private val json = Json
    val graphqlClient = GraphqlAdminQuery()

    @OptIn(InternalSerializationApi::class)
    val objectMapper =
        object : IObjectMapper {
            override fun <T : Any> serialize(value: T, clazz: KClass<T>): String {
                return json.encodeToString(clazz.serializer(), value)
            }
        }
}
