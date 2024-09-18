package net.matsudamper.money.frontend.common.base

public actual val Logger: ILogger = object : ILogger {
    override fun d(tag: String, message: String) {
        println("d: [$tag] $message")
    }

    override fun e(tag: String, message: String) {
        println("e: [$tag] $message")
    }

    override fun w(tag: String, message: String) {
        println("w: [$tag] $message")
    }

    override fun i(tag: String, message: String) {
        println("i: [$tag] $message")
    }
}
