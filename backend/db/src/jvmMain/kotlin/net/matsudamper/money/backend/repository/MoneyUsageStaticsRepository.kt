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
            dbConnection.use { connection ->
                val count = DSL.coalesce(DSL.sum(usage.AMOUNT), 0.toBigDecimal())
                val result = DSL.using(connection)
                    .select(count)
                    .from(usage)
                    .where(usage.USER_ID.eq(userId.value))
                    .and(
                        usage.DATETIME.greaterOrEqual(sinceDateTimeAt)
                            .and(usage.DATETIME.lessThan(untilDateTimeAt))
                    )
                    .fetchOne()

                result?.get(count)!!.toLong()
            }
        }.onFailure {
            it.printStackTrace()
        }.fold(
            onSuccess = { it },
            onFailure = { null }
        )
    }
}
