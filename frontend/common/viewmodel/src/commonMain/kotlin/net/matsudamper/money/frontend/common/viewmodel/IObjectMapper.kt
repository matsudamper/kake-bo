package net.matsudamper.money.frontend.common.viewmodel

public interface IObjectMapper {

    public fun <T> serialize(value: T): String
}