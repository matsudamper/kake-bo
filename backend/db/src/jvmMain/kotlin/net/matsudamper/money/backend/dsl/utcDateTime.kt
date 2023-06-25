package net.matsudamper.money.backend.dsl

import org.jooq.impl.DSL
import java.time.LocalDateTime
import java.time.ZoneId

internal object DslExt {
    fun utcDateTimeNow() = DSL.localDateTime(LocalDateTime.now(ZoneId.of("UTC")))
}

object LocalDateTimeExt {
    fun nowUtc() = LocalDateTime.now(ZoneId.of("UTC"))
}
