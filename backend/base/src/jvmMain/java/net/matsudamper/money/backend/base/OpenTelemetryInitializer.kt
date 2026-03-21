package net.matsudamper.money.backend.base

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
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
        val endpoint = System.getenv("OTEL_EXPORTER_OTLP_ENDPOINT") ?: "http://localhost:4318"
        val serviceName = System.getenv("OTEL_SERVICE_NAME") ?: "unknown"
        val resource = Resource.getDefault()
            .merge(Resource.create(Attributes.of(AttributeKey.stringKey("service.name"), serviceName)))

        val tracerProvider = SdkTracerProvider.builder()
            .setResource(resource)
            .addSpanProcessor(
                BatchSpanProcessor.builder(
                    OtlpHttpSpanExporter.builder()
                        .setEndpoint("$endpoint/v1/traces")
                        .build(),
                ).build(),
            )
            .build()

        val meterProvider = SdkMeterProvider.builder()
            .setResource(resource)
            .registerMetricReader(
                PeriodicMetricReader.builder(
                    OtlpHttpMetricExporter.builder()
                        .setEndpoint("$endpoint/v1/metrics")
                        .build(),
                ).build(),
            )
            .build()

        val loggerProvider = SdkLoggerProvider.builder()
            .setResource(resource)
            .addLogRecordProcessor(
                BatchLogRecordProcessor.builder(
                    OtlpHttpLogRecordExporter.builder()
                        .setEndpoint("$endpoint/v1/logs")
                        .build(),
                ).build(),
            )
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
