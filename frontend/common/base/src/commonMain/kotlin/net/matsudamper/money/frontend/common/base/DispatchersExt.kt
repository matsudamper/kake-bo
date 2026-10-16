package net.matsudamper.money.frontend.common.base

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

// wasmJs には Dispatchers.IO が無いため、JVM/Android ではメンバーに隠れても拡張として残す
@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
public expect val Dispatchers.IO: CoroutineDispatcher
