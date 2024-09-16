package net.matsudamper.money.backend.graphql

import graphql.schema.DataFetchingEnvironment

inline fun <reified T> DataFetchingEnvironment.requireLocalContext() = requireNotNull(getLocalContext<T>()) {
    "${T::class.java.name} is not found in local context"
}
