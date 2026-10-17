package net.matsudamper.money.frontend.common.base

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

public actual val Dispatchers.PlatformIO: CoroutineDispatcher get() = kotlinx.coroutines.Dispatchers.IO
