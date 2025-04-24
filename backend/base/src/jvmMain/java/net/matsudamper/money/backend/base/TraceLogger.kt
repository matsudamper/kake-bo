package net.matsudamper.money.backend.base

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode

public interface TraceLogger {
    public fun noticeThrowable(e: Throwable, params: Map<String, Any>, isError: Boolean)
    public fun setTag(key: String, value: String)
    public fun noticeInfo(message: Any)

    public companion object {
        public fun impl(): TraceLogger {
            return OpenTracerRepository()
        }
    }
}

internal class OpenTracerRepository : TraceLogger {
    private val span: Span get() = Span.current()
    override fun noticeThrowable(e: Throwable, params: Map<String, Any>, isError: Boolean) {
        for ((key, value) in params) {
            span.setAttribute(key, value.toString())
        }
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

    override fun setTag(key: String, value: String) {
        span.setAttribute(key, value)
    }
}
