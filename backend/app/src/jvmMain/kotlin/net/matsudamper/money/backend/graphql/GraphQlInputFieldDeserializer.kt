package net.matsudamper.money.backend.graphql

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.ContextualDeserializer
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import net.matsudamper.money.graphql.model.GraphQlInputField

internal class GraphQlInputFieldDeserializer(
    private val valueType: JavaType? = null,
) : StdDeserializer<GraphQlInputField<*>>(GraphQlInputField::class.java),
    ContextualDeserializer {

    override fun createContextual(ctxt: DeserializationContext, property: BeanProperty?): JsonDeserializer<*> {
        val type = property?.type?.containedType(0)
            ?: ctxt.contextualType?.containedType(0)
        return GraphQlInputFieldDeserializer(type)
    }

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): GraphQlInputField<*> {
        val value = if (valueType != null) ctxt.readValue(p, valueType)
        else ctxt.readValue(p, Any::class.java)
        return GraphQlInputField.Defined(value)
    }

    override fun getNullValue(ctxt: DeserializationContext): GraphQlInputField<*> =
        GraphQlInputField.Defined(null)
}
