package net.matsudamper.money.frontend.common.base

import android.util.Log

public actual val Logger: ILogger = object : ILogger {
    override fun d(tag: String, message: String) {
        if (!BuildConfig.DEBUG) return
        Log.d(tag, message)
    }

    override fun e(tag: String, message: String) {
        if (!BuildConfig.DEBUG) return
        Log.e(tag, message)
    }

    override fun w(tag: String, message: String) {
        if (!BuildConfig.DEBUG) return
        Log.w(tag, message)
    }

    override fun i(tag: String, message: String) {
        if (!BuildConfig.DEBUG) return
        Log.i(tag, message)
    }
}
