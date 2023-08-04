package net.matsudamper.money.backend.dsl

import java.time.LocalDateTime
import java.time.ZoneId
import org.jooq.impl.DSL

internal object DslExt {
    fun utcDateTimeNow() = DSL.localDateTime(LocalDateTime.now(ZoneId.of("UTC")))
}

object LocalDateTimeExt {
    fun nowUtc() = LocalDateTime.now(ZoneId.of("UTC"))
}
