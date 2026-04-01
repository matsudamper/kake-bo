package net.matsudamper.money.backend.base

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk

public object OpenTelemetryInitializer {
    private var sdk: OpenTelemetry? = null

    public fun initialize(): OpenTelemetry {
        // 新仕様で出力する
        System.setProperty("otel.semconv-stability.opt-in", "database")

        val openTelemetry = AutoConfiguredOpenTelemetrySdk.builder()
            .addPropertiesSupplier {
                mapOf(
                    "otel.exporter.otlp.protocol" to "http/protobuf",
                )
            }
            .addSpanExporterCustomizer { exporter, _ -> FilteringSpanExporter(exporter) }
            .build()
            .openTelemetrySdk

        sdk = openTelemetry

        Runtime.getRuntime().addShutdownHook(Thread { openTelemetry.close() })

        return openTelemetry
    }

    public fun get(): OpenTelemetry {
        return checkNotNull(sdk) { "OpenTelemetry SDK has not been initialized" }
    }
}
