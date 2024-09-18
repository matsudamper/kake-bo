package net.matsudamper.money.frontend.common.base

public interface ILogger {
    public fun d(tag: String, message: String)
    public fun e(tag: String, message: String)
    public fun w(tag: String, message: String)
    public fun i(tag: String, message: String)
}

public expect val Logger: ILogger
