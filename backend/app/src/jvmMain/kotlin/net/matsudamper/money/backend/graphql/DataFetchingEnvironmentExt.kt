package net.matsudamper.money.backend.graphql

import graphql.schema.DataFetchingEnvironment

@Suppress("UNCHECKED_CAST")
inline fun <reified T> DataFetchingEnvironment.requireLocalContext(): T = requireNotNull(getLocalContext<Any>() as? T) {
    "${T::class.java.name} is not found in local context"
}
