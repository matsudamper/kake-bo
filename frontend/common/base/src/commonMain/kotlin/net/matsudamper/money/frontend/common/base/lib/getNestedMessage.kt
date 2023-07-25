package net.matsudamper.money.frontend.common.base.lib

public fun Throwable.getNestedMessage(): String {
    return buildString {
        var e: Throwable? = this@getNestedMessage
        while (e != null) {
            append(e.message)
            e = e.cause
        }
    }
}