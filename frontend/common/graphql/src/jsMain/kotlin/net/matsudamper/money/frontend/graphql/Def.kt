package net.matsudamper.money.frontend.graphql

import kotlinx.browser.window

actual val serverProtocol: String get() = window.location.protocol.removeSuffix(":")
actual val serverHost: String get() = window.location.host
