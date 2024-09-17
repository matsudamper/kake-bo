package net.matsudamper.money.frontend.common.viewmodel

import kotlin.reflect.KClass

public interface IObjectMapper {
    public fun <T : Any> serialize(value: T, clazz: KClass<T>): String
}
