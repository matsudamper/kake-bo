package net.matsudamper.money.backend.base

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.exporter.logging.otlp.OtlpJsonLoggingLogRecordExporter
import io.opentelemetry.exporter.logging.otlp.OtlpJsonLoggingMetricExporter
import io.opentelemetry.exporter.logging.otlp.OtlpJsonLoggingSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.logs.SdkLoggerProvider
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
public object OpenTelemetryInitializer {
    private var sdk: OpenTelemetrySdk? = null

    public fun initialize(): OpenTelemetry {
        val serviceName = System.getenv("OTEL_SERVICE_NAME") ?: "kake-bo-backend"
        val resource = Resource.getDefault()
            .merge(Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), serviceName)))

        val tracerProvider = SdkTracerProvider.builder()
            .setResource(resource)
            .addSpanProcessor(BatchSpanProcessor.builder(OtlpJsonLoggingSpanExporter.create()).build())
            .build()

        val meterProvider = SdkMeterProvider.builder()
            .setResource(resource)
            .registerMetricReader(PeriodicMetricReader.builder(OtlpJsonLoggingMetricExporter.create()).build())
            .build()

        val loggerProvider = SdkLoggerProvider.builder()
            .setResource(resource)
            .addLogRecordProcessor(BatchLogRecordProcessor.builder(OtlpJsonLoggingLogRecordExporter.create()).build())
            .build()

        val openTelemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(tracerProvider)
            .setMeterProvider(meterProvider)
            .setLoggerProvider(loggerProvider)
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .buildAndRegisterGlobal()

        sdk = openTelemetry

        Runtime.getRuntime().addShutdownHook(Thread { openTelemetry.close() })

        return openTelemetry
    }

    public fun get(): OpenTelemetry {
        return checkNotNull(sdk) { "OpenTelemetry SDK has not been initialized" }
    }
}
