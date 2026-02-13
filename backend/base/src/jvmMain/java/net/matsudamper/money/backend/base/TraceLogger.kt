package net.matsudamper.money.backend.base

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode

public interface TraceLogger {
    public fun noticeThrowable(e: Throwable, isError: Boolean)
    public fun setAttribute(key: String, value: String)
    public fun noticeInfo(message: Any)

    public companion object {
        public fun impl(): TraceLogger {
            return OpenTracerRepository()
        }
    }
}

internal class OpenTracerRepository : TraceLogger {
    private val span: Span get() = Span.current()
    override fun noticeThrowable(e: Throwable, isError: Boolean) {
        if (isError) {
            span.setStatus(StatusCode.ERROR)
        }
        span.recordException(e)
        span.setAttribute("exception.${System.nanoTime()}", e.message.orEmpty())
    }

    override fun noticeInfo(message: Any) {
        span.setAttribute(
            "info",
            message.toString(),
        )
    }

    override fun setAttribute(key: String, value: String) {
        span.setAttribute(key, value)
    }
}
