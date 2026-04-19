package net.matsudamper.money.frontend.common.base

import android.util.Log

private var loggerDelegate: ILogger? = null

public fun setAndroidLoggerDelegate(logger: ILogger) {
    loggerDelegate = logger
}

private val defaultAndroidLogger: ILogger = object : ILogger {
    override fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun e(tag: String, message: String) {
        Log.e(tag, message)
    }

    override fun e(tag: String, throwable: Throwable) {
        Log.e(tag, throwable.message, throwable)
    }

    override fun w(tag: String, message: String) {
        Log.w(tag, message)
    }

    override fun i(tag: String, message: String) {
        Log.i(tag, message)
    }
}

public actual val Logger: ILogger = object : ILogger {
    private fun impl(): ILogger = loggerDelegate ?: defaultAndroidLogger

    override fun d(tag: String, message: String) = impl().d(tag, message)
    override fun e(tag: String, message: String) = impl().e(tag, message)
    override fun e(tag: String, throwable: Throwable) = impl().e(tag, throwable)
    override fun w(tag: String, message: String) = impl().w(tag, message)
    override fun i(tag: String, message: String) = impl().i(tag, message)
}
