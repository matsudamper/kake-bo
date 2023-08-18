package net.matsudamper.money.frontend.common.base.nav.user

import io.ktor.http.ParametersBuilder
import io.ktor.http.formUrlEncode

internal fun buildParameter(block: ParametersBuilder.() -> Unit): String {
    return ParametersBuilder()
        .apply(block)
        .build()
        .formUrlEncode()
        .let { if (it.isEmpty()) it else "?$it" }
}
