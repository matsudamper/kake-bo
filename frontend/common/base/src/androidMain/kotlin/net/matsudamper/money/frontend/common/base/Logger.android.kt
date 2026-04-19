package net.matsudamper.money.frontend.common.base

import timber.log.Timber

public actual val Logger: ILogger = object : ILogger {
    override fun d(tag: String, message: String) {
        Timber.tag(tag).d(message)
    }

    override fun e(tag: String, message: String) {
        Timber.tag(tag).e(message)
    }

    override fun e(tag: String, throwable: Throwable) {
        Timber.tag(tag).e(throwable)
    }

    override fun w(tag: String, message: String) {
        Timber.tag(tag).w(message)
    }

    override fun i(tag: String, message: String) {
        Timber.tag(tag).i(message)
    }
}
