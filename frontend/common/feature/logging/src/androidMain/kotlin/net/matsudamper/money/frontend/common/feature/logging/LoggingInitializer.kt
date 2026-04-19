package net.matsudamper.money.frontend.common.feature.logging

import net.matsudamper.money.frontend.common.base.ILogger
import net.matsudamper.money.frontend.common.base.setAndroidLoggerDelegate
import timber.log.Timber

public fun initializeLogging() {
    Timber.plant(Timber.DebugTree())
    setAndroidLoggerDelegate(TimberLogger())
}

private class TimberLogger : ILogger {
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
