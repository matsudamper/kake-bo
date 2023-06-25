package net.matsudamper.money.frontend.graphql

import kotlinx.browser.window

internal actual val serverProtocol: String = window.location.protocol
internal actual val serverHost: String = window.location.host