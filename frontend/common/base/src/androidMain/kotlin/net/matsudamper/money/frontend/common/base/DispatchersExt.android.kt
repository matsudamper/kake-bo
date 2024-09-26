package net.matsudamper.money.frontend.common.base

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public actual val Dispatchers.IO: CoroutineDispatcher get() = kotlinx.coroutines.Dispatchers.IO