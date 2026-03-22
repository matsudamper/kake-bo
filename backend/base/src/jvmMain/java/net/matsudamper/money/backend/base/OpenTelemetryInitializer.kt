package net.matsudamper.money.backend.base

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk

public object OpenTelemetryInitializer {
    private var sdk: OpenTelemetry? = null

    public fun initialize(): OpenTelemetry {
        val openTelemetry = AutoConfiguredOpenTelemetrySdk.initialize()
            .openTelemetrySdk

        sdk = openTelemetry

        Runtime.getRuntime().addShutdownHook(Thread { openTelemetry.close() })

        return openTelemetry
    }

    public fun get(): OpenTelemetry {
        return checkNotNull(sdk) { "OpenTelemetry SDK has not been initialized" }
    }
}
