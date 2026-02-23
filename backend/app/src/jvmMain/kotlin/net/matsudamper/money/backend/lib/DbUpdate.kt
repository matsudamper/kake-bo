package net.matsudamper.money.backend.lib

import net.matsudamper.money.backend.app.interfaces.UpdateValue
import net.matsudamper.money.graphql.model.GraphQlInputField

internal fun <T> GraphQlInputField<T>.toDbUpdateValue(): UpdateValue<T> {
    return when (this) {
        is GraphQlInputField.Defined -> UpdateValue.Update(this.value)
        is GraphQlInputField.Undefined -> UpdateValue.NotUpdate
    }
}
