package net.matsudamper.money.backend.datasource.redis

import java.net.SocketAddress
import io.lettuce.core.protocol.RedisCommand
import io.lettuce.core.tracing.TraceContext
import io.lettuce.core.tracing.TraceContextProvider
import io.lettuce.core.tracing.Tracer
import io.lettuce.core.tracing.TracerProvider
import io.lettuce.core.tracing.Tracing
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.context.Context

internal class LettuceOtelTracing(openTelemetry: OpenTelemetry) : Tracing {
    private val otelTracer = openTelemetry.getTracer("io.lettuce.redis")

    override fun isEnabled() = true

    override fun getTracerProvider(): TracerProvider = OtelTracerProvider(otelTracer)

    override fun initialTraceContextProvider(): TraceContextProvider = OtelTraceContextProvider

    override fun includeCommandArgsInSpanTags() = false

    override fun createEndpoint(socketAddress: SocketAddress): Tracing.Endpoint? = null
}

private object OtelTraceContextProvider : TraceContextProvider {
    override fun getTraceContext(): TraceContext = OtelTraceContext(Context.current())
}

private class OtelTracerProvider(
    private val otelTracer: io.opentelemetry.api.trace.Tracer,
) : TracerProvider {
    override fun getTracer(): Tracer = OtelTracer(otelTracer)
}

private class OtelTraceContext(val context: Context) : TraceContext

private class OtelTracer(
    private val otelTracer: io.opentelemetry.api.trace.Tracer,
) : Tracer() {
    override fun nextSpan(): Span {
        val span = otelTracer.spanBuilder("redis.command")
            .setSpanKind(SpanKind.CLIENT)
            .setAttribute("db.system", "redis")
            .startSpan()
        return OtelSpan(span)
    }

    override fun nextSpan(parent: TraceContext): Span {
        val otelContext = (parent as? OtelTraceContext)?.context ?: Context.current()
        val span = otelTracer.spanBuilder("redis.command")
            .setSpanKind(SpanKind.CLIENT)
            .setParent(otelContext)
            .setAttribute("db.system", "redis")
            .startSpan()
        return OtelSpan(span)
    }
}

private class OtelSpan(
    private val span: io.opentelemetry.api.trace.Span,
) : Tracer.Span() {
    override fun name(name: String): Tracer.Span {
        span.updateName(name)
        return this
    }

    override fun tag(key: String, value: String): Tracer.Span {
        span.setAttribute(key, value)
        return this
    }

    override fun error(throwable: Throwable): Tracer.Span {
        span.recordException(throwable)
        span.setStatus(StatusCode.ERROR)
        return this
    }

    override fun start(command: RedisCommand<*, *, *>): Tracer.Span {
        span.updateName("redis.${command.type.bytes.decodeToString().lowercase()}")
        return this
    }

    override fun annotate(value: String): Tracer.Span {
        span.addEvent(value)
        return this
    }

    override fun remoteEndpoint(endpoint: Tracing.Endpoint?): Tracer.Span = this

    override fun finish() {
        span.end()
    }
}
