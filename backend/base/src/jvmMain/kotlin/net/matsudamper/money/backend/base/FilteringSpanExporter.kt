package net.matsudamper.money.backend.base

import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.SpanExporter

// CLIENT SETNAME・HELLO・SELECT は Lettuce が接続確立時に自動送信する初期化コマンド。
// Lettuce の OTel 計装はユーザー向け API にのみフックするため、これらは呼び出し元コンテキストと
// 紐づかない孤立したルートスパンになる。エクスポーター段階でフィルタして除外する。
internal class FilteringSpanExporter(private val delegate: SpanExporter) : SpanExporter {
    override fun export(spans: Collection<SpanData>): CompletableResultCode {

        val isRedis = spans.any { it.attributes.get(AttributeKey.stringKey("db.system")) == "redis" }
        if (isRedis.not()) return delegate.export(spans)

        val filtered = spans.filter { it.attributes.get(AttributeKey.stringKey("db.operation")) !in EXCLUDED_SPAN_NAMES }
        return if (filtered.isEmpty()) {
            CompletableResultCode.ofSuccess()
        } else {
            delegate.export(filtered)
        }
    }

    override fun flush(): CompletableResultCode = delegate.flush()

    override fun shutdown(): CompletableResultCode = delegate.shutdown()

    companion object {
        private val EXCLUDED_SPAN_NAMES = setOf("CLIENT", "HELLO", "SELECT", "SETNAME")
    }
}
