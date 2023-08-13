package net.matsudamper.money.backend.repository

import net.matsudamper.money.backend.DbConnection
import net.matsudamper.money.backend.element.UserId
import net.matsudamper.money.db.schema.tables.JMoneyUsages
import net.matsudamper.money.db.schema.tables.JMoneyUsagesMailsRelation
import org.jooq.impl.DSL
import java.time.LocalDateTime

class MoneyUsageStaticsRepository(
    private val dbConnection: DbConnection,
) {
    private val usage = JMoneyUsages.MONEY_USAGES

    fun getTotalAmount(
        userId: UserId,
        sinceDateTimeAt: LocalDateTime,
        untilDateTimeAt: LocalDateTime,
    ): Long? {
        return runCatching {
            val result = dbConnection.use { connection ->
                DSL.using(connection)
                    .select(DSL.sum(usage.AMOUNT))
                    .from(usage)
                    .where(usage.USER_ID.eq(userId.value))
                    .and(
                        usage.DATETIME.greaterOrEqual(sinceDateTimeAt)
                            .and(usage.DATETIME.lessThan(untilDateTimeAt))
                    )
                    .fetchOne()
            }
            result?.get(DSL.sum(usage.AMOUNT))?.toLong()
        }.onFailure {
            it.printStackTrace()
        }.fold(
            onSuccess = { it },
            onFailure = { null }
        )
    }
}
