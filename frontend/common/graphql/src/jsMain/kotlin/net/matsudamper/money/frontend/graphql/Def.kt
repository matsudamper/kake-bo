package net.matsudamper.money.frontend.graphql

import kotlinx.browser.window

internal actual val serverProtocol: String get() = window.location.protocol.removeSuffix(":")
internal actual val serverHost: String get() = window.location.host
