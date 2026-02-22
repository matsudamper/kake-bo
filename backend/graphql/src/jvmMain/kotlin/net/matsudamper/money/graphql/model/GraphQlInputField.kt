package net.matsudamper.money.graphql.model

sealed class GraphQlInputField<out T> {
    data class Defined<out T>(val value: T?) : GraphQlInputField<T>()
    data object Undefined : GraphQlInputField<Nothing>()
}
